/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.error.ErrorTypeKind
import java.util.*

/**
 * 交集类型处理
 *
 * 提供用于计算多个类型的交集的功能。类型交集是类型系统中的一个重要概念，
 * 表示同时满足多个类型约束的类型。
 *
 * 在仓颉语言中，类型交集主要用于：
 * - 泛型类型参数的上界计算
 * - 类型推断过程中的类型合并
 * - 重载解析时的类型兼容性检查
 * - 智能类型转换的类型计算
 *
 * 例如：如果一个变量既是 Comparable 又是 Serializable，
 * 其类型就是这两个接口的交集类型。
 */

/**
 * 计算简单类型列表的交集
 *
 * @param types 要计算交集的简单类型列表
 * @return 交集结果的简单类型
 */
fun intersectTypes(types: List<SimpleType>) = intersectTypes(types as List<UnwrappedType>) as SimpleType

/**
 * 计算解包类型列表的交集
 *
 * 处理包含简单类型和灵活类型的混合列表，计算它们的交集。
 * 对于灵活类型，分别处理上界和下界。
 *
 * @param types 要计算交集的解包类型列表
 * @return 交集结果的解包类型
 */
fun intersectTypes(types: List<UnwrappedType>): UnwrappedType {
    when (types.size) {
        0 -> error("Expected some types")
        1 -> return types.single()
    }
    var hasFlexibleTypes = false
    var hasErrorType = false

    // 提取所有类型的下界
    val lowerBounds = types.map {
        hasErrorType = hasErrorType || it.isError
        when (it) {
            is SimpleType -> it
            is FlexibleType -> {
                // 动态类型直接返回，不参与交集计算
                if (it.isDynamic()) return it

                hasFlexibleTypes = true
                it.lowerBound
            }
        }
    }

    // 如果有错误类型，返回错误类型
    if (hasErrorType) {
        return ErrorUtils.createErrorType(ErrorTypeKind.INTERSECTION_OF_ERROR_TYPES, types.toString())
    }

    // 如果没有灵活类型，直接计算简单类型的交集
    if (!hasFlexibleTypes) {
        return TypeIntersector.intersectTypes(lowerBounds)
    }

    // 提取所有类型的上界
    val upperBounds = types.map { it.upperIfFlexible() }

    /**
     * 我们应该保持以下规则：
     *  - 如果每个类型都是 A 的子类型，那么交集类型也应该是 A 的子类型
     *  - 对于是所有类型子类型的类型 B 也是如此
     *
     *  注意：当我们构造动态类型（或 Raw 类型）与其他类型的交集时，可能得到非动态类型。// todo 讨论
     */
    return CangJieTypeFactory.flexibleType(
        TypeIntersector.intersectTypes(lowerBounds),
        TypeIntersector.intersectTypes(upperBounds)
    )
}

/**
 * 类型交集器
 *
 * 负责执行具体的类型交集计算逻辑。处理可选性、Nothing 类型、
 * 子类型关系等复杂情况。
 */
object TypeIntersector {
    fun isIntersectionEmpty(typeA: CangJieType, typeB: CangJieType): Boolean {
        return intersectTypes(
            LinkedHashSet(
                listOf(
                    typeA,
                    typeB
                )
            )
        ) == null
    }

    fun getUpperBoundsAsType(descriptor: TypeParameterDescriptor): CangJieType {
        return intersectUpperBounds(descriptor, descriptor.upperBounds)
    }

    fun intersectUpperBounds(descriptor: TypeParameterDescriptor, upperBounds: List<CangJieType>): CangJieType {
        assert(!upperBounds.isEmpty()) { "Upper bound list is empty: $descriptor" }
        val upperBoundsAsType: CangJieType? = intersectTypes(upperBounds)
        return if (upperBoundsAsType != null) upperBoundsAsType else descriptor.builtIns.nothingType
    }

    /**
     * 计算类型集合的交集
     *
     * 核心的类型交集计算方法，处理以下情况：
     * - Nothing 类型的特殊处理
     * - 可选性的统一处理
     * - 子类型关系的过滤
     * - 错误类型的处理
     *
     * @param types 要计算交集的类型集合
     * @return 交集结果，如果无法计算则返回 null
     */
    fun intersectTypes(types: Collection<CangJieType>): CangJieType? {
        assert(!types.isEmpty()) { "Attempting to intersect empty collection of types, this case should be dealt with on the call site." }

        if (types.size == 1) {
            return types.iterator().next()
        }

        // T1..Tn 的交集是它们非可选版本的交集，
        // 如果它们都是可选的，则结果也是可选的
        var nothingOrOptionNothing: CangJieType? = null
        var allOption = true
        val optionStripped: MutableList<CangJieType> = java.util.ArrayList<CangJieType>(types.size)
        for (type in types) {
            // 跳过错误类型
            if (type.isError) continue

            if (CangJieBuiltIns.isNothing(type)) {
                nothingOrOptionNothing = type
            }
            allOption = allOption and type.isOption
            optionStripped.add(TypeUtils.makeNonOption(type))
        }

        // 如果有 Nothing 类型，根据整体可选性返回相应的 Nothing 类型
        if (nothingOrOptionNothing != null) {
            return TypeUtils.makeOptionalAsSpecified(nothingOrOptionNothing, allOption)
        }

        // 如果所有类型都是错误类型
        if (optionStripped.isEmpty()) {
            return ErrorUtils.createErrorType(ErrorTypeKind.INTERSECTION_OF_ERROR_TYPES, types.toString())
        }

        val typeChecker = CangJieTypeChecker.DEFAULT
        // 现在移除列表中有子类型的类型
        val resultingTypes: MutableList<CangJieType> = java.util.ArrayList<CangJieType>()
        outer@ for (type in optionStripped) {
            // TODO: 添加对不能有子类型的类型的特殊处理
//            if (!TypeUtils.canHaveSubtypes(typeChecker, type)) {
//                var relativeToAll = true
//                for (other in optionStripped) {
//                    // 检查子类型关系是有意义的 (other <: type)，尽管
//                    // 类型不应该是开放的，但对于枚举等情况是必要的
//                    val mayBeEqual: Boolean =
//                      TypeIntersector.TypeUnifier.mayBeEqual(type, other)
//                    val relative = typeChecker.isSubtypeOf(type, other) || typeChecker.isSubtypeOf(other, type)
//                    if (!mayBeEqual && !relative) {
//                        return null
//                    } else if (!relative) {
//                        // 构建 T & (final A) 时，不是只返回 A 作为交集
//                        relativeToAll = false
//                        break
//                    }
//                }
//                if (relativeToAll) return TypeUtils.makeOptionalAsSpecified(type, allOption)
//            }

            // 检查当前类型是否被其他类型包含（是其他类型的超类型）
            for (other in optionStripped) {
                if (!type.equals(other) && typeChecker.isSubtypeOf(other, type)) {
                    continue@outer
                }
            }

            // 避免添加已存在的类型，防止结果中出现平凡的类型交集
            for (other in resultingTypes) {
                if (typeChecker.equalTypes(other, type)) {
                    continue@outer
                }
            }
            resultingTypes.add(type)
        }

        if (resultingTypes.isEmpty()) {
            // 如果到这里，意味着 `optionStripped` 中的所有类型都被上面的代码排除了
            // 最可能的原因是它们在语义上是可互换的（例如 List<Foo>! 和 List<Foo>），
            // 在这种情况下，我们可以安全地从该集合中选择最佳代表并返回它
            // TODO: 也许返回 `optionStripped` 中对所有其他类型都是子类型的最具体类型？
            // TODO: 例如在 {Int, Int?, Int!} 中，返回 `Int`（现在返回 `Int!`）。
            var bestRepresentative = optionStripped.singleBestRepresentative()

            if (bestRepresentative == null) {
                bestRepresentative = hackForTypeIntersector(optionStripped)
            }

            if (bestRepresentative == null) {
                return null
            }
            return TypeUtils.makeOptionalAsSpecified(bestRepresentative, allOption)
        }

        if (resultingTypes.size == 1) {
            return TypeUtils.makeOptionalAsSpecified(resultingTypes[0], allOption)
        }

        return IntersectionTypeConstructor(resultingTypes).createType()
    }

    /**
     * 计算简单类型列表的交集（内部方法）
     *
     * 专门处理简单类型的交集计算，包括展开嵌套的交集类型构造器。
     *
     * @param types 要计算交集的简单类型列表
     * @return 交集结果的简单类型
     */
    internal fun intersectTypes(types: List<SimpleType>): SimpleType {
        assert(types.size > 1) {
            "Size should be at least 2, but it is ${types.size}"
        }

        // 展开嵌套的交集类型构造器
        val inputTypes = ArrayList<SimpleType>()
        for (type in types) {
            if (type.constructor is IntersectionTypeConstructor) {
                inputTypes.addAll(type.constructor.supertypes.map {
                    it.upperIfFlexible().let { if (type.isOption) it.makeOptionAsSpecified(true) else it }
                })
            } else {
                inputTypes.add(type)
            }
        }

        // 计算结果的可选性
        val resultOption = inputTypes.fold(ResultOption.START, ResultOption::combine)

        /**
         * resultOption 值描述：
         * ACCEPT_OPTION 表示所有类型都标记为可选
         *
         * NON_OPTION 表示有一个类型是 Any 的子类型 => 所有类型都可以确定为非可选，
         * 使类型确定为非可选（不只是非可选）在类型参数交集如 {T!! & S} 时是有意义的
         *
         * UNKNOWN 表示我们不知道，即更准确地说，所有单分类器类型如果有的话标记为可选，
         * 其他类型是捕获类型或没有非可选上界的类型参数。例如：`String? & T` 这样的类型我们应该保持原样。
         */
        val correctOption = inputTypes.mapTo(LinkedHashSet()) {
            if (resultOption == ResultOption.NON_OPTION) {
                (if (it is CapturedType) it.withNonOptionProjection() else it).makeSimpleTypeDefinitelyNonOptionOrNonOption()
            } else it
        }

        val resultAttributes = types.map { it.attributes }.reduce { x, y -> x.intersect(y) }
        return intersectTypesWithoutIntersectionType(correctOption).replaceAttributes(resultAttributes)
    }

    /**
     * 不使用交集类型构造器的类型交集计算
     *
     * 在可选性已经正确处理的情况下，计算类型交集。
     *
     * @param inputTypes 输入的简单类型集合（可选性已处理）
     * @return 交集结果的简单类型
     */
    // 这里的可选性是正确的
    private fun intersectTypesWithoutIntersectionType(inputTypes: Set<SimpleType>): SimpleType {
        if (inputTypes.size == 1) return inputTypes.single()

        // Any 和 Nothing 应该保留
        // 注意重复项应该被丢弃，因为这里我们有 Set
        val errorMessage = { "This collections cannot be empty! input types: ${inputTypes.joinToString()}" }

        // 过滤掉有严格超类型的类型
        val filteredEqualTypes = filterTypes(inputTypes, ::isStrictSupertype)
        assert(filteredEqualTypes.isNotEmpty(), errorMessage)

        // 检查整数字面量类型的特殊交集
        IntegerLiteralTypeConstructor.findIntersectionType(filteredEqualTypes)?.let { return it }

        // 过滤掉相等的类型
        val filteredSuperAndEqualTypes = filterTypes(filteredEqualTypes, CangJieTypeChecker.DEFAULT::equalTypes)
        assert(filteredSuperAndEqualTypes.isNotEmpty(), errorMessage)

        if (filteredSuperAndEqualTypes.size < 2) return filteredSuperAndEqualTypes.single()

        return IntersectionTypeConstructor(inputTypes).createType()
    }

    /**
     * 过滤类型集合
     *
     * 根据给定的谓词过滤类型，移除满足条件的类型。
     *
     * @param inputTypes 输入类型集合
     * @param predicate 过滤条件谓词
     * @return 过滤后的类型集合
     */
    private fun filterTypes(
        inputTypes: Collection<SimpleType>,
        predicate: (lower: SimpleType, upper: SimpleType) -> Boolean
    ): Collection<SimpleType> {
        val filteredTypes = ArrayList(inputTypes)
        val iterator = filteredTypes.iterator()
        while (iterator.hasNext()) {
            val upper = iterator.next()
            val shouldFilter = filteredTypes.any { lower -> lower !== upper && predicate(lower, upper) }

            if (shouldFilter) iterator.remove()
        }
        return filteredTypes
    }

    /**
     * 判断是否为严格超类型
     *
     * @param subtype 子类型
     * @param supertype 超类型
     * @return 如果 subtype 是 supertype 的严格子类型则返回 true
     */
    private fun isStrictSupertype(subtype: CangJieType, supertype: CangJieType): Boolean {
        return with(CangJieTypeChecker.DEFAULT) {
            isSubtypeOf(subtype, supertype) && !isSubtypeOf(supertype, subtype)
        }
    }

    /**
     * 结果可选性枚举
     *
     * 设 T 是具有上界 Any? 的类型参数。resultOption(String? & T) = UNKNOWN => String? & T
     *
     * 定义了类型交集计算过程中可选性的处理策略。
     */
    private enum class ResultOption {
        /** 起始状态 */
        START {
            override fun combine(nextType: UnwrappedType) = nextType.resultOption
        },

        /** 接受可选类型 - 所有类型都是可选的 */
        ACCEPT_OPTION {
            override fun combine(nextType: UnwrappedType) = nextType.resultOption
        },

        /** 未知状态 - 例如：没有非可选超类型的类型参数 */
        UNKNOWN {
            override fun combine(nextType: UnwrappedType) =
                nextType.resultOption.let {
                    if (it == ACCEPT_OPTION) this else it
                }
        },

        /** 非可选类型 */
        NON_OPTION {
            override fun combine(nextType: UnwrappedType) = this
        };

        /**
         * 合并可选性状态
         *
         * @param nextType 下一个要处理的类型
         * @return 合并后的可选性状态
         */
        abstract fun combine(nextType: UnwrappedType): ResultOption

        /**
         * 获取解包类型的结果可选性
         */
        protected val UnwrappedType.resultOption: ResultOption
            get() = when {
                isOption -> ACCEPT_OPTION
                this is DefinitelyNonOptionType && this.original is StubTypeForBuilderInference -> NON_OPTION
                this is StubTypeForBuilderInference -> UNKNOWN
                OptionChecker.isSubtypeOfAny(this) -> NON_OPTION
                else -> UNKNOWN
            }
    }
}

/**
 * 计算包装类型集合的交集
 *
 * 对包装的类型进行解包后计算交集。这是一个便利函数，
 * 用于处理可能包含包装层的类型集合。
 *
 * @param types 要计算交集的包装类型集合
 * @return 交集结果的解包类型
 */
fun intersectWrappedTypes(types: Collection<CangJieType>) = intersectTypes(types.map { it.unwrap() })
