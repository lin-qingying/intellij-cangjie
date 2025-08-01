/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.types.expressions.match

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.builtins.isBuiltinTupleType
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.source.getPsi
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils
import cn.cangnova.cangjie.types.util.deccriptorClass
import cn.cangnova.cangjie.types.util.isEnum
import cn.cangnova.cangjie.types.util.isStruct
import cn.cangnova.cangjie.types.util.source
import cn.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl


class CheckMatchException(message: String) : CangJieExceptionWithAttachmentsImpl(message)
typealias Matrix = List<List<Pattern>>

/** Calculates the pattern matrix by splitting or-patterns across different rows */
@Throws(CheckMatchException::class)
fun List<CjMatchEntry>.calculateMatrix(context: BindingContext): Matrix =
    flatMap { arm -> arm.conditions.map { listOf(getPattern(context, it)) } }

fun CjCasePattern.calculateMatrix(context: BindingContext): Matrix = listOf(
    listOf(getPattern(context, this))
)

fun getPattern(context: BindingContext, element: CjCasePattern): Pattern {
    return context[BindingContext.PATTERN, element] ?: Pattern.Error


}

/**
 * 检查所有模式的类型是否相同
 */
fun Matrix.isWellTyped(): Boolean {
    val variantPatternsTypesAreValid = flatten().all { (type, kind) ->
        when (kind) {
            is PatternKind.Enum -> kind.enum == type.deccriptorClass?.source?.getPsi()
            else -> true
        }
    }
    if (!variantPatternsTypesAreValid) return false

    val types = flatten().map { it.type }
    return types.isEmpty() ||
            types.customDistinct { a, b ->

                CangJieTypeChecker.DEFAULT.equalTypes(a, b)
            }.size == 1

}

fun <T> List<T>.customDistinct(comparator: (T, T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        // 如果 result 中没有符合 comparator 条件的元素，则添加
        if (result.none { comparator(it, item) }) {
            result.add(item)
        }
    }
    return result
}

/**
 * 非穷尽性的证据,在穷尽性检查结束时，该证据将仅包含一个模式，但在算法的中间阶段，它可能包含多个模式。
 */
class Witness(val patterns: MutableList<Pattern> = mutableListOf()) {
    override fun toString() = patterns.toString()
    fun clone(): Witness =
        Witness(patterns.toMutableList())

    fun pushWildConstructor(constructor: Constructor, type: CangJieType): Witness {
        val subPatternTypes = constructor.subTypes(type)
        for (ty in subPatternTypes) {
            patterns.add(Pattern.wild(ty))
        }
        return applyConstructor(constructor, type)
    }

    fun applyConstructor(constructor: Constructor, type: CangJieType): Witness {
        val arity = constructor.arity(type)
        val len = patterns.size
        val oldPatterns = patterns.subList(len - arity, len)
        val pats = oldPatterns.reversed().toList()
        oldPatterns.clear()
        val kind = when {
            type.isBuiltinTupleType -> {
                PatternKind.Tuple(pats)

            }

            type.isEnum() -> {
                PatternKind.Enum(
                    type.source as CjEnum,
                    (constructor as Constructor.Enum).entry,
                    pats
                )
            }

            else -> if (constructor is Constructor.ConstantValue) {
                PatternKind.Const(constructor.value)
            } else {
                PatternKind.Wild
            }
        }

        patterns.add(Pattern(type, kind))
        return this
    }
}


sealed class Usefulness {
    class UsefulWithWitness(val witnesses: List<Witness>) : Usefulness() {
        companion object {
            val Empty: UsefulWithWitness get() = UsefulWithWitness(listOf(Witness()))
        }
    }

    data object Useful : Usefulness()
    data object Useless : Usefulness()

    val isUseful: Boolean get() = this !== Useless
}

/**
 * 检查给定模式集合（`patterns`）是否在已有模式矩阵（`matrix`）的上下文中仍然“有用”，即是否覆盖新的匹配情况。
 * 该方法通过应用构造函数（`constructor`）和类型（`type`）进行模式特化，以识别未被覆盖的模式，并生成非穷尽性证据。
 *
 * @param matrix 当前的模式矩阵，包含已经匹配的模式行。
 * @param patterns 需要检查的新模式列表。
 * @param constructor 构造函数，用于构建 `type` 类型的新模式。
 * @param type 构造函数应用的类型。
 * @param withWitness 是否需要生成非穷尽性证据，用于标记未被覆盖的情况。
 * @param crateRoot 模式的根模块，用于访问模块级的上下文信息。
 * @return 返回 `Usefulness` 类型，表示新模式是否有用，并可能包含非穷尽性证据。
 */
private fun isUsefulSpecialized(
    matrix: Matrix,
    patterns: List<Pattern>,
    constructor: Constructor,
    type: CangJieType,
    withWitness: Boolean,
    crateRoot: CjFile?
): Usefulness {
    val newPatterns = specializeRow(patterns, constructor, type) ?: return Usefulness.Useless
    val newMatrix = matrix.mapNotNull { row -> specializeRow(row, constructor, type) }

    return when (val useful = isUseful(newMatrix, newPatterns, withWitness, crateRoot, isTopLevel = false)) {
        is Usefulness.UsefulWithWitness -> Usefulness.UsefulWithWitness(useful.witnesses.map {
            it.applyConstructor(
                constructor,
                type
            )
        })

        else -> useful
    }
}

fun CangJieType.isTyAdt(): Boolean {
    return isEnum() || isStruct()
}

/**
 * 预留方法，检查属性
 * 目前无作用，用来消除警告
 */
fun hasAtomAttribute(attr: String): Boolean {

    return false
}

/**
 * INRIA的原始算法
 */
fun isUseful(
    matrix: Matrix,
    patterns: List<Pattern>,
    withWitness: Boolean,
    crateRoot: CjFile?,
    isTopLevel: Boolean
): Usefulness {
    fun expandConstructors(constructors: List<Constructor>, type: CangJieType): Usefulness = constructors
        .map { isUsefulSpecialized(matrix, patterns, it, type, withWitness, crateRoot) }
        .find { it.isUseful }
        ?: Usefulness.Useless


    if (patterns.isEmpty()) {
        if (matrix.isEmpty()) {
            return if (withWitness) Usefulness.UsefulWithWitness.Empty else Usefulness.Useful
        }
        return Usefulness.Useless
    }
    val pattern = patterns.first()
    val type = matrix.firstColumnType ?: pattern.ergonomicType

    val constructors = pattern.constructors
    if (constructors != null) {
        /**
         * If `pattern` is a constructor pattern, then its usefulness can be reduced to whether it is useful when
         * we ignore all the patterns in the first column of [matrix] that involve other constructors
         */
        return expandConstructors(constructors, type)
    }

    /**
     * Otherwise, `pattern` is wildcard (or binding which is basically the same).
     * We look at the list of constructors that appear in the first column of [matrix].
     */
    val usedConstructors = matrix.firstColumn.map { it.constructors.orEmpty() }.flatten()

    val allConstructors = Constructor.allConstructors(type)
    val missingConstructors = allConstructors.minus(usedConstructors.toSet())

    val isPrivatelyEmpty = allConstructors.isEmpty()
    val isInDifferentCrate = type.isTyAdt() && type.source?.containingFile != crateRoot

    val isDeclaredNonExhaustive = type.isTyAdt() && hasAtomAttribute("non_exhaustive")
    val isNonExhaustive = isPrivatelyEmpty || (isDeclaredNonExhaustive && isInDifferentCrate)

    if (missingConstructors.isEmpty() && !isNonExhaustive) {
        /**
         * If all possible constructors are present, we must check whether the wildcard `pattern` covers any unmatched value.
         * The wildcard pattern is useful in this case if it is useful when specialized to one of the possible constructors.
         * For example, if `Some(<something>)` and `None` constructors of `Option` are covered, we should specialize
         * wildcard `pattern` to `Some(<something else>)` and check its usefulness
         */
        return expandConstructors(allConstructors, type)
    }


    /**
     * If there are `missingConstructors`, then our wildcard `pattern` might be useful.
     * But `missingConstructors` can be matched by wildcards in the beginning of rows, so we need to check
     * usefulness of the remaining patterns in a submatrix containing all rows starting with a wildcard.
     */
    val wildcardRows = matrix.filter { row ->
        when (val kind = row.firstOrNull()?.kind) {
            PatternKind.Wild, is PatternKind.Binding -> true
            is PatternKind.Type ->
                CangJieTypeChecker.DEFAULT.equalTypes(type, kind.type)

            else -> false
        }
    }
    val wildcardSubmatrix = wildcardRows.map { it.drop(1) }
    val remainingPatterns = patterns.drop(1)
    val res = isUseful(wildcardSubmatrix, remainingPatterns, withWitness, crateRoot, isTopLevel = false)

    if (res is Usefulness.UsefulWithWitness) {
        val reportConstructors = isTopLevel && !CangJieBuiltIns.isIntegral(type)
        val newWitness = if (!reportConstructors && (isNonExhaustive || usedConstructors.isEmpty())) {
            res.witnesses.map { witness ->
                witness.patterns.add(Pattern.wild(type))
                witness
            }
        } else {
            res.witnesses.flatMap { witness ->
                missingConstructors.map { witness.clone().pushWildConstructor(it, type) }
            }
        }
        return Usefulness.UsefulWithWitness(newWitness)
    }

    return res

}

val Matrix.firstColumn: List<Pattern> get() = mapNotNull { row -> row.firstOrNull() }

/**
 * 矩阵第一列的类型
 *
 * @return 如果矩阵为空，则返回 `null`
 * @throws [CheckMatchException] 如果第一列中的模式类型不一致
 */
val Matrix.firstColumnType: CangJieType?
    get() {
        val firstColumnTypes = firstColumn.map { it.type }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return firstColumnTypes.customDistinct { a, b ->
            CangJieTypeChecker.DEFAULT.equalTypes(a, b)
        }.singleOrNull()
            ?: throw CheckMatchException("Ambiguous type of the first column")
    }

/**
 * 对给定的一行模式（`row`）进行特化，将行中的第一个模式与构造函数（`constructor`）进行匹配，
 * 并根据构造函数类型和模式类型返回一个新的特化模式行。
 *
 * @param row 当前的模式行，由多个模式组成。
 * @param constructor 用于匹配模式的构造函数。
 * @param type 当前构造函数应用的类型。
 * @return 如果特化成功，则返回特化后的模式列表；否则返回 null。
 */
private fun specializeRow(row: List<Pattern>, constructor: Constructor, type: CangJieType): List<Pattern>? {
    // 获取行中的第一个模式，如果行为空，则返回一个空列表
    val firstPattern = row.firstOrNull() ?: return emptyList()

    // 使用构造函数的子类型创建一个通配模式的列表，用于后续填充特定的子模式
    val wildPatterns = constructor
        .subTypes(type)
        .map { subType -> Pattern.wild(subType) } // 将每个子类型初始化为通配模式
        .toMutableList()


//    // 根据第一个模式的类型（`kind`）进行特化
    val head: List<Pattern>? = when (val kind = firstPattern.kind) {
        is PatternKind.Enum -> {
            // 若第一个模式是枚举变体类型，检查构造函数是否匹配
            if (constructor == firstPattern.constructors?.first()) {
                // 如果匹配，则将该模式的子模式填充到 `wildPatterns`
                wildPatterns.apply { fillWithSubPatterns(kind.subPatterns) }
            } else {
                // 否则返回 null 表示特化失败
                null
            }
        }

        is PatternKind.Tuple -> wildPatterns.apply { fillWithSubPatterns(kind.subPatterns) }


        is PatternKind.Const ->
            // 如果是常量模式，检查构造函数是否为切片模式
            when {

                // 检查常量值是否在构造函数范围内
                constructor.coveredByRange(kind.value, kind.value, true) -> emptyList()

                // 否则返回 null 表示特化失败
                else -> null
            }

        is PatternKind.Type -> {

            if (CangJieTypeChecker.DEFAULT.equalTypes(kind.type, type)) {
                wildPatterns
            } else {
                null
            }
        }

        PatternKind.Wild, is PatternKind.Binding ->
            // 如果是通配模式或绑定模式，直接返回 `wildPatterns`
            wildPatterns

        PatternKind.Error -> null

    }

    // 返回特化后的模式行，将特化的 `head` 与剩余的模式组合起来
    return head?.plus(row.subList(1, row.size))

}

private fun MutableList<Pattern>.fillWithSubPatterns(subPatterns: List<Pattern>) {
    for ((index, pattern) in subPatterns.withIndex()) {
        while (size <= index) add(Pattern.wild()) // TODO: maybe it's better to throw an exception?
        this[index] = pattern
    }
}

/**
 * 执行匹配表达式的穷尽性检查。
 * 该函数旨在确定给定的匹配表达式是否覆盖了所有可能的情况，即是否为穷尽的。
 *
 * @param match 匹配表达式对象，包含匹配的主体表达式和条目列表。
 * @param context 绑定上下文，用于获取类型信息。
 * @return 如果匹配表达式不是穷尽的，则返回一个模式列表；否则返回 null。
 */
fun doCheckExhaustive(match: CjMatchExpression, context: BindingContext): List<Pattern>? {
    // 获取匹配表达式的主体表达式的类型，如果主体表达式不存在则返回 null
    val matchedExprType = match.subjectExpression?.let { context.getType(it) } ?: return null

    // 计算匹配矩阵并检查其类型是否正确，如果类型不正确则返回 null
    val matrix = match.entries
        .calculateMatrix(context)
        .takeIf { it.isWellTyped() }
        ?: return null

    // 创建一个通配符模式，用于表示所有可能的值
    val wild = Pattern.wild(matchedExprType)

    // 检查通配符模式是否在当前矩阵中是有用的
    val useful = isUseful(matrix, listOf(wild), true, match.containingCjFile, isTopLevel = true)

    // 如果通配符模式是有用的，说明匹配表达式不是穷尽的
    if (useful is Usefulness.UsefulWithWitness) {
        return useful.witnesses.mapNotNull { it.patterns.singleOrNull() }
    }

    // 匹配表达式是穷尽的，返回 null
    return null
}


/**
 * 根据单个模式获取穷尽情况
 * @param expression 模式表达式
 * @param context 上下文
 * @return  如果匹配表达式不是穷尽的，则返回一个模式列表；否则返回 null。
 */
fun CjCasePattern.getExhaustive(expression: CjExpression?, context: BindingContext): List<Pattern>? {

    val type = expression?.let { context.getType(it) } ?: ErrorUtils.invalidType


    val matrix = calculateMatrix(context)
        .takeIf { it.isWellTyped() }
        ?: return null

    val wild = Pattern.wild(type)
    val useful = isUseful(matrix, listOf(wild), true, this.containingCjFile, isTopLevel = true)
//
//    /** If `_` pattern is useful, the match is not exhaustive */
    if (useful is Usefulness.UsefulWithWitness) {
        return useful.witnesses.mapNotNull { it.patterns.singleOrNull() }
    }


    return null
}

//fun isOverwriteForForInExpr(pattern: CjCasePattern, context: BindingContext):Boolean {
//    doCheckExhaustive()
//}
