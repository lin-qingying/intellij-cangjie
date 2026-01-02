/*
 * Copyright 2025 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.types.expressions.match.exhaustive.inria

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.*
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.isEnum
import org.cangnova.cangjie.types.isStruct

/**
 * Maranget 穷举性检查算法实现
 *
 * 基于 Luc Maranget 的论文 "Warnings for pattern matching" (2007)
 * 该算法是模式匹配穷举性检查的标准实现。
 *
 * ## 算法概述
 *
 * 核心思想是通过"有用性"（usefulness）来判断穷举性：
 * - 如果在已有模式矩阵后添加一个通配符模式仍然"有用"，
 *   说明存在未被覆盖的情况，即不穷举。
 * - "有用"意味着存在某个值能被新模式匹配但不被已有模式匹配。
 *
 * ## 算法步骤
 *
 * 1. 构造模式矩阵（每行是一个 match 分支）
 * 2. 添加通配符行 [_, _, ..., _]
 * 3. 检查通配符行是否有用
 * 4. 如果有用，构造反例（witness）
 *
 * ## 时间复杂度
 *
 * 最坏情况 O(2^n)，但通过剪枝在实际情况下通常是多项式时间
 */
class MarangetChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.MARANGET

    override val priority: Int = 100

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        // Maranget 算法是通用算法，始终适用
        return true
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        // 检查矩阵类型一致性
        if (!matrix.isWellTyped()) {
            return ExhaustivenessResult.Error("模式矩阵类型不一致")
        }

        // 创建通配符模式
        val wildPattern = Pattern.wild(type)

        // 检查通配符模式是否有用
        val useful = isUseful(
            matrix = matrix,
            patterns = listOf(wildPattern),
            withWitness = true,
            crateRoot = crateRoot,
            isTopLevel = true
        )

        return when (useful) {
            is Usefulness.UsefulWithWitness -> {
                val missingPatterns = useful.witnesses.mapNotNull { it.patterns.singleOrNull() }
                ExhaustivenessResult.NonExhaustive(missingPatterns, source)
            }
            is Usefulness.Useful -> {
                // 有用但没有 witness，说明配置错误，应该构造 witness
                ExhaustivenessResult.NonExhaustive(emptyList(), source)
            }
            Usefulness.Useless -> {
                ExhaustivenessResult.Exhaustive
            }
        }
    }

    /**
     * 检查给定模式在已有矩阵上下文中是否"有用"
     *
     * 这是 Maranget 算法的核心函数。
     *
     * @param matrix 当前的模式矩阵
     * @param patterns 需要检查的新模式列表
     * @param withWitness 是否需要生成反例证据
     * @param crateRoot 模式的根模块
     * @param isTopLevel 是否是顶层调用
     * @return 有用性结果
     */
    fun isUseful(
        matrix: Matrix,
        patterns: List<Pattern>,
        withWitness: Boolean,
        crateRoot: CjFile?,
        isTopLevel: Boolean
    ): Usefulness {
        // 辅助函数：展开构造器
        fun expandConstructors(constructors: List<Constructor>, type: CangJieType): Usefulness {
            // 对每个构造器尝试特化
            for (constructor in constructors) {
                val result = isUsefulSpecialized(
                    matrix, patterns, constructor, type, withWitness, crateRoot
                )
                if (result.isUseful) {
                    return result
                }
            }
            return Usefulness.Useless
        }

        // 基本情况：模式列表为空
        if (patterns.isEmpty()) {
            return if (matrix.isEmpty()) {
                // 矩阵也为空，说明有用（存在未覆盖的空元组）
                if (withWitness) Usefulness.UsefulWithWitness.Empty else Usefulness.Useful
            } else {
                // 矩阵非空，说明无用（已被覆盖）
                Usefulness.Useless
            }
        }

        // 获取第一个模式和类型
        val pattern = patterns.first()
        val type = matrix.firstColumnType ?: pattern.ergonomicType

        // 获取第一个模式的构造器
        val constructors = pattern.constructors
        if (constructors != null) {
            // 如果模式是构造器模式，检查它的有用性
            return expandConstructors(constructors, type)
        }

        // 否则，模式是通配符或绑定模式
        // 需要检查所有可能的构造器

        // 获取矩阵第一列中使用的所有构造器
        val usedConstructors = matrix.firstColumn
            .flatMap { it.constructors.orEmpty() }
            .toSet()

        // 获取类型的所有可能构造器
        val allConstructors = Constructor.allConstructors(type)
        val missingConstructors = allConstructors.minus(usedConstructors)

        // 检查类型是否为"私有空"或"非穷举"
        val isPrivatelyEmpty = allConstructors.isEmpty()
        val isInDifferentCrate = type.isTyAdt() && type.source?.containingFile != crateRoot
        val isDeclaredNonExhaustive = type.isTyAdt() && hasNonExhaustiveAttribute(type)
        val isNonExhaustive = isPrivatelyEmpty || (isDeclaredNonExhaustive && isInDifferentCrate)

        if (missingConstructors.isEmpty() && !isNonExhaustive) {
            // 所有构造器都已存在，需要检查通配符是否覆盖了新情况
            return expandConstructors(allConstructors, type)
        }

        // 存在缺失的构造器，检查剩余模式
        val wildcardRows = matrix.filter { row ->
            when (val kind = row.firstOrNull()?.kind) {
                PatternKind.Wild, is PatternKind.Binding -> true
                is PatternKind.Type -> CangJieTypeChecker.DEFAULT.equalTypes(type, kind.type)
                else -> false
            }
        }

        val wildcardSubmatrix = wildcardRows.map { it.drop(1) }
        val remainingPatterns = patterns.drop(1)
        val res = isUseful(wildcardSubmatrix, remainingPatterns, withWitness, crateRoot, isTopLevel = false)

        // 构造 witness
        if (res is Usefulness.UsefulWithWitness) {
            val reportConstructors = isTopLevel && !CangJieBuiltIns.isIntegral(type)
            val newWitness = if (!reportConstructors && (isNonExhaustive || usedConstructors.isEmpty())) {
                res.witnesses.map { witness ->
                    witness.patterns.add(Pattern.wild(type))
                    witness
                }
            } else {
                res.witnesses.flatMap { witness ->
                    missingConstructors.map { constructor ->
                        witness.clone().pushWildConstructor(constructor, type)
                    }
                }
            }
            return Usefulness.UsefulWithWitness(newWitness)
        }

        return res
    }

    /**
     * 对矩阵和模式进行特化后检查有用性
     */
    private fun isUsefulSpecialized(
        matrix: Matrix,
        patterns: List<Pattern>,
        constructor: Constructor,
        type: CangJieType,
        withWitness: Boolean,
        crateRoot: CjFile?
    ): Usefulness {
        // 特化模式行
        val newPatterns = RowSpecializer.specializeRow(patterns, constructor, type)
            ?: return Usefulness.Useless

        // 特化矩阵
        val newMatrix = matrix.mapNotNull { row ->
            RowSpecializer.specializeRow(row, constructor, type)
        }

        // 递归检查
        val useful = isUseful(newMatrix, newPatterns, withWitness, crateRoot, isTopLevel = false)

        // 如果有用，应用构造器到 witness
        return when (useful) {
            is Usefulness.UsefulWithWitness -> {
                Usefulness.UsefulWithWitness(
                    useful.witnesses.map { it.applyConstructor(constructor, type) }
                )
            }
            else -> useful
        }
    }

    /**
     * 检查类型是否标记为非穷举
     *
     * 非穷尽性枚举（带 ... 的枚举）允许在 match 时不穷尽所有分支。
     * 这个特性主要用于：
     * 1. 向后兼容性：新增枚举变体不会破坏现有代码
     * 2. 外部枚举：导入的库枚举可能在未来版本增加变体
     *
     * @param type 要检查的类型
     * @return 如果类型是非穷尽性枚举则返回 true
     */
    private fun hasNonExhaustiveAttribute(type: CangJieType): Boolean {
        val enumDescriptor = type.deccriptorClass
        if (enumDescriptor is org.cangnova.cangjie.descriptors.EnumDescriptor) {
            return enumDescriptor.isNonExhaustive
        }
        return false
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = MarangetChecker()
    }
}

// 类型辅助扩展
private fun CangJieType.isTyAdt(): Boolean = isEnum || isStruct

private val CangJieType.source: CjFile?
    get() = constructor.declarationDescriptor?.source?.let {
        it.getPsi()?.containingFile as? CjFile
    }
