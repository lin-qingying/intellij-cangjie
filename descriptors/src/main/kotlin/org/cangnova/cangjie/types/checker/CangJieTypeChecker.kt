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

import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.types.*

/**
 * 仓颉类型检查器接口
 *
 * 提供类型相等性与子类型判断的统一入口。
 *
 * 核心功能：
 * - isSubtypeOf: 子类型关系检查
 * - equalTypes: 类型相等性检查
 * - equalsIgnoringGenerics: 忽略泛型参数的类型比较
 *
 * 使用示例：
 * ```kotlin
 * val checker = CangJieTypeChecker.DEFAULT
 *
 * // 检查子类型关系
 * if (checker.isSubtypeOf(dogType, animalType)) {
 *     println("Dog is a subtype of Animal")
 * }
 *
 * // 检查类型相等
 * if (checker.equalTypes(type1, type2)) {
 *     println("Types are equal")
 * }
 * ```
 */
interface CangJieTypeChecker {
    /**
     * 类型构造器相等性的策略函数接口
     *
     * 当比较两个类型时，有时需要自定义如何判断它们的 TypeConstructor 是否相等
     */
    fun interface TypeConstructorEquality {
        /**
         * 判断两个 TypeConstructor 是否相等
         *
         * @param a 第一个类型构造器
         * @param b 第二个类型构造器
         * @return true 表示相等，false 表示不相等
         */
        fun equals(a: TypeConstructor, b: TypeConstructor): Boolean
    }

    /** 类型精化器，用于精化类型信息 */
    val cangjieTypeRefiner: CangJieTypeRefiner
        get() = CangJieTypeRefiner.Default

    /** 类型准备器，用于准备类型进行检查 */
    val cangjieTypePreparator: CangJieTypePreparator
        get() = CangJieTypePreparator.Default

    /** 重写工具，用于检查方法重写 */
    val overridingUtil: OverridingUtil
        get() = OverridingUtil.DEFAULT

    /**
     * 在比较两个类型时忽略泛型参数，仅比较原始/裸类型是否相同
     *
     * 例如 List<String> 与 List<Int> 在此比较下被视为相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return true 表示在忽略泛型后两类型相等
     */
    fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean

    /**
     * 判断 subtype 是否为 supertype 的子类型
     *
     * 基于仓颉语言的子类型规则：
     * - Nothing <: T <: Any（对于所有类型 T）
     * - 用户自定义泛型类型全部不变
     * - 函数类型：参数逆变，返回值协变
     * - 元组类型：元素协变
     *
     * @param subtype 待检查的子类型
     * @param supertype 待检查的父类型
     * @return true 表示 subtype 是 supertype 的子类型
     */
    fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean

    /**
     * 判断两个类型是否完全相等（考虑泛型等所有细节）
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return true 表示完全相等，false 表示不相等
     */
    fun equalTypes(a: CangJieType, b: CangJieType): Boolean

    /**
     * 检查子类型约束并收集嵌套约束
     *
     * 在类型推导过程中检查子类型关系，同时通过 context 收集嵌套的类型约束。
     * 例如检查 `List<T> <: List<String>` 时，会通过 context 添加 `T <: String` 约束。
     *
     * @param subtype 子类型
     * @param supertype 父类型
     * @param context 约束检查上下文，用于收集嵌套约束
     * @return true 表示约束满足，false 表示约束不满足
     */
    fun checkSubtypeConstraint(
        subtype: CangJieType,
        supertype: CangJieType,
        context: ConstraintCheckContext
    ): Boolean {
        // 默认实现：仅检查子类型关系，不收集约束
        return isSubtypeOf(subtype, supertype)
    }

    /**
     * 检查相等约束并收集嵌套约束
     *
     * 在类型推导过程中检查类型相等性，同时通过 context 收集嵌套的类型约束。
     * 例如检查 `Pair<T, U> == Pair<Int, String>` 时，会通过 context 添加：
     * - `T == Int`
     * - `U == String`
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @param context 约束检查上下文，用于收集嵌套约束
     * @return true 表示约束满足，false 表示约束不满足
     */
    fun checkEqualityConstraint(
        type1: CangJieType,
        type2: CangJieType,
        context: ConstraintCheckContext
    ): Boolean {
        // 默认实现：仅检查类型相等性，不收集约束
        return equalTypes(type1, type2)
    }

    companion object {
        /**
         * 默认的类型检查器实例
         *
         * 使用新的仓颉类型检查系统实现
         */
        val DEFAULT: CangJieTypeChecker = DefaultCangJieTypeChecker()
    }
}

/**
 * 默认的仓颉类型检查器实现
 *
 * 基于仓颉编译器 TypeManager.cpp 的类型检查逻辑实现
 *
 * 特点：
 * - 严格遵循仓颉语言规范
 * - 用户自定义泛型类型全部不变（invariant）
 * - 函数类型参数逆变，返回值协变
 * - 元组类型元素协变
 *
 * @param cangjieTypeRefiner 类型精化器，用于精化类型信息
 * @param cangjieTypePreparator 类型准备器，用于准备类型进行检查（默认使用 Default）
 */
class DefaultCangJieTypeChecker(
    override val cangjieTypeRefiner: CangJieTypeRefiner = CangJieTypeRefiner.Default,
    override val cangjieTypePreparator: CangJieTypePreparator = CangJieTypePreparator.Default
) : CangJieTypeChecker {

    /** 重写工具，使用类型精化器创建 */
    override val overridingUtil: OverridingUtil by lazy {
        OverridingUtil.createWithTypeRefiner(cangjieTypeRefiner)
    }

    /**
     * 判断 subtype 是否为 supertype 的子类型
     */
    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        return CangJieSubtypeChecker.isSubtypeOf(
            subtype,
            supertype,
            implicitBoxed = true
        )
    }

    /**
     * 判断两个类型是否完全相等
     */
    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        return CangJieTypeEquality.areEqual(a, b)
    }

    /**
     * 判断两个类型是否相等（忽略泛型参数）
     */
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean {
        return CangJieTypeEquality.areEqualIgnoringGenerics(a, b)
    }

    /**
     * 检查子类型约束并收集嵌套约束
     */
    override fun checkSubtypeConstraint(
        subtype: CangJieType,
        supertype: CangJieType,
        context: ConstraintCheckContext
    ): Boolean {
        return checkSubtypeConstraintInternal(subtype.unwrap(), supertype.unwrap(), context)
    }

    /**
     * 内部方法：检查子类型约束并收集嵌套约束
     */
    private fun checkSubtypeConstraintInternal(
        subtype: UnwrappedType,
        supertype: UnwrappedType,
        context: ConstraintCheckContext
    ): Boolean {
        // 1. 特殊类型快速路径
        if (isSpecialType(subtype) || isSpecialType(supertype)) {
            return isSubtypeOf(subtype, supertype)
        }

        // 2. 如果其中一个是类型变量，添加约束
        if (subtype.constructor.isTypeVariable() || supertype.constructor.isTypeVariable()) {
            context.addSubtypeConstraint(subtype, supertype)
            return true
        }

        // 3. FlexibleType 处理
        if (subtype is FlexibleType) {
            return checkSubtypeConstraintInternal(subtype.lowerBound, supertype, context)
        }
        if (supertype is FlexibleType) {
            return checkSubtypeConstraintInternal(subtype, supertype.upperBound, context)
        }

        // 4. 都是 SimpleType
        if (subtype is SimpleType && supertype is SimpleType) {
            return checkSimpleSubtypeConstraint(subtype, supertype, context)
        }

        return isSubtypeOf(subtype, supertype)
    }

    /**
     * 检查简单类型的子类型约束并收集嵌套约束
     */
    private fun checkSimpleSubtypeConstraint(
        subtype: SimpleType,
        supertype: SimpleType,
        context: ConstraintCheckContext
    ): Boolean {
        // 1. 首先检查基本的子类型关系
        if (!isSubtypeOf(subtype, supertype)) {
            context.reportConstraintError()
            return false
        }

        // 2. 如果类型构造器不同，不需要收集嵌套约束
        if (subtype.constructor != supertype.constructor) {
            return true
        }

        // 3. 收集泛型类型参数的约束
        val subtypeArgs = subtype.arguments
        val supertypeArgs = supertype.arguments

        if (subtypeArgs.size != supertypeArgs.size) {
            return true
        }

        // 仓颉语言的泛型类型参数都是不变的，因此需要相等约束
        for (i in subtypeArgs.indices) {
            val subArg = subtypeArgs[i]
            val superArg = supertypeArgs[i]

            // 添加相等约束
            context.addEqualityConstraint(subArg.type, superArg.type)
        }

        return true
    }

    /**
     * 检查相等约束并收集嵌套约束
     */
    override fun checkEqualityConstraint(
        type1: CangJieType,
        type2: CangJieType,
        context: ConstraintCheckContext
    ): Boolean {
        return checkEqualityConstraintInternal(type1.unwrap(), type2.unwrap(), context)
    }

    /**
     * 内部方法：检查相等约束并收集嵌套约束
     */
    private fun checkEqualityConstraintInternal(
        type1: UnwrappedType,
        type2: UnwrappedType,
        context: ConstraintCheckContext
    ): Boolean {
        // 1. 特殊类型快速路径
        if (isSpecialType(type1) || isSpecialType(type2)) {
            return equalTypes(type1, type2)
        }

        // 2. 如果其中一个是类型变量，添加约束
        if (type1.constructor.isTypeVariable() || type2.constructor.isTypeVariable()) {
            context.addEqualityConstraint(type1, type2)
            return true
        }

        // 3. FlexibleType 处理
        if (type1 is FlexibleType && type2 is FlexibleType) {
            val lowerEqual = checkEqualityConstraintInternal(type1.lowerBound, type2.lowerBound, context)
            val upperEqual = checkEqualityConstraintInternal(type1.upperBound, type2.upperBound, context)
            return lowerEqual && upperEqual
        }

        // 4. 都是 SimpleType
        if (type1 is SimpleType && type2 is SimpleType) {
            return checkSimpleEqualityConstraint(type1, type2, context)
        }

        return equalTypes(type1, type2)
    }

    /**
     * 检查简单类型的相等约束并收集嵌套约束
     */
    private fun checkSimpleEqualityConstraint(
        type1: SimpleType,
        type2: SimpleType,
        context: ConstraintCheckContext
    ): Boolean {
        // 1. Option 状态必须相同
        if (type1.isOption != type2.isOption) {
            context.reportConstraintError()
            return false
        }

        // 2. 类型构造器必须相同
        if (type1.constructor != type2.constructor) {
            context.reportConstraintError()
            return false
        }

        // 3. 收集泛型类型参数的约束
        val args1 = type1.arguments
        val args2 = type2.arguments

        if (args1.size != args2.size) {
            context.reportConstraintError()
            return false
        }

        // 递归检查所有类型参数
        for (i in args1.indices) {
            val arg1 = args1[i]
            val arg2 = args2[i]

            // 递归添加相等约束
            context.addEqualityConstraint(arg1.type, arg2.type)
        }

        return true
    }

    /**
     * 检查类型构造器是否是类型变量
     */
    private fun TypeConstructor.isTypeVariable(): Boolean {
        return declarationDescriptor is org.cangnova.cangjie.descriptors.TypeParameterDescriptor
    }
}

/**
 * 错误类型等于任何类型的检查器
 *
 * 在类型检查时将错误类型视为与任何类型都兼容。
 * 这对于在有编译错误的情况下仍能提供合理的代码补全和类型推断很有用。
 *
 * 示例：如果 foo() 返回错误类型，那么 foo().bar() 仍然可以进行类型检查
 */
object ErrorTypesAreEqualToAnythingChecker : CangJieTypeChecker {

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        // 如果任一类型是错误类型，返回 true
        if (SpecialTypeChecker.isErrorType(subtype) || SpecialTypeChecker.isErrorType(supertype)) {
            return true
        }
        return CangJieSubtypeChecker.isSubtypeOf(subtype, supertype)
    }

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        // 如果任一类型是错误类型，返回 true
        if (SpecialTypeChecker.isErrorType(a) || SpecialTypeChecker.isErrorType(b)) {
            return true
        }
        return CangJieTypeEquality.areEqual(a, b)
    }

    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean {
        // 如果任一类型是错误类型，返回 true
        if (SpecialTypeChecker.isErrorType(a) || SpecialTypeChecker.isErrorType(b)) {
            return true
        }
        return CangJieTypeEquality.areEqualIgnoringGenerics(a, b)
    }
}

/**
 * 严格类型检查器
 *
 * 使用严格的类型相等性检查，不会忽略任何类型差异：
 * - 不同的 Option 状态视为不相等
 * - 错误类型不与任何类型相等
 * - 严格的类型参数比较
 */
object StrictCangJieTypeChecker : CangJieTypeChecker {

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        return CangJieSubtypeChecker.isSubtypeOf(
            subtype,
            supertype,
            implicitBoxed = false,  // 不允许隐式装箱
            allowOptionBox = false
        )
    }

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        return CangJieTypeEquality.areStrictlyEqual(a, b)
    }

    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean {
        return CangJieTypeEquality.areEqualIgnoringGenerics(a, b)
    }
}
/**
 * 简单的经典类型系统上下文
 *
 * 提供类型检查所需的基本类型系统上下文
 */
object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

/**
 * 使用自定义类型构造器相等性公理的类型检查器
 *
 * @param equalityAxioms 自定义的类型构造器相等性判断
 */
class AxiomBasedTypeChecker(
    private val equalityAxioms: CangJieTypeChecker.TypeConstructorEquality
) : CangJieTypeChecker {

    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        return CangJieSubtypeChecker.isSubtypeOf(
            subtype,
            supertype,
            implicitBoxed = true,
            typeConstructorEquality = equalityAxioms
        )
    }

    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        return CangJieTypeEquality.areEqual(a, b, equalityAxioms)
    }

    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean {
        return CangJieTypeEquality.areEqualIgnoringGenerics(a, b, equalityAxioms)
    }

    companion object {
        /**
         * 创建使用自定义公理的类型检查器
         */
        fun withAxioms(equalityAxioms: CangJieTypeChecker.TypeConstructorEquality): CangJieTypeChecker {
            return AxiomBasedTypeChecker(equalityAxioms)
        }
    }
}

/**
 * 严格相等性类型检查器
 *
 * 使用严格的类型相等性检查，不会忽略任何类型差异：
 * - String! != String （平台类型 vs 非空类型）
 * - A<String!> != A<String> （泛型参数的平台类型差异）
 * - A<in Nothing> != A<out Any?> （不同的型变）
 * - A<*> != A<out Any?> （星投影 vs 显式边界）
 * - 不同的错误类型互不相等（即使 errorTypeEqualToAnything 为 true）
 */
object StrictEqualityTypeChecker {

    /**
     * 严格检查两个未包装类型是否相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两个类型严格相等时返回 true
     */
    fun strictEqualTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return CangJieTypeEquality.areStrictlyEqual(a, b)
    }

    /**
     * 严格检查两个简单类型是否相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两个类型严格相等时返回 true
     */
    fun strictEqualTypes(a: SimpleType, b: SimpleType): Boolean {
        return CangJieTypeEquality.areStrictlyEqual(a, b)
    }
}

/**
 * 可选类型检查器
 *
 * 用于检查可选类型（Option）相关的类型关系
 */
object OptionChecker {
    /**
     * 检查类型是否是 Any 的子类型
     *
     * 用于判断一个类型是否可以安全地转换为非空类型。
     * 如果类型不是 Nothing 且不是可空类型，则它是 Any 的子类型。
     *
     * @param type 要检查的类型
     * @return 如果类型是 Any 的子类型则返回 true
     */
    fun isSubtypeOfAny(type: UnwrappedType): Boolean {
        // 仓颉语言的实现：非 Nothing 且非 Option 的类型都是 Any 的子类型
        return !SpecialTypeChecker.isNothing(type) && !type.isOption
    }

    /**
     * 辅助方法：检查类型是否为特殊类型
     *
     * 特殊类型包括：Nothing, Any, Unit, Error
     */
    private fun isSpecialType(type: CangJieType): Boolean {
        return SpecialTypeChecker.isNothing(type) ||
                SpecialTypeChecker.isAny(type) ||
                SpecialTypeChecker.isUnit(type) ||
                SpecialTypeChecker.isErrorType(type)
    }
}

/**
 * 错误类型等于任何类型的检查器（别名，兼容旧代码）
 *
 * @see ErrorTypesAreEqualToAnythingChecker
 */
val ErrorTypesAreEqualToAnything: CangJieTypeChecker = ErrorTypesAreEqualToAnythingChecker

/**
 * 检查类型是否有给定类型构造器的父类型
 *
 * 示例：检查 ArrayList 是否有 List 作为父类型
 *
 * @param typeConstructor 要查找的类型构造器
 * @return 如果存在这样的父类型则返回 true
 */
fun UnwrappedType.hasSupertypeWithGivenTypeConstructor(typeConstructor: TypeConstructor) =
    createClassicTypeCheckerState(isErrorTypeEqualsToAnything = false).anySupertype(lowerIfFlexible(), {
        require(it is SimpleType)
        it.constructor == typeConstructor
    }, { TypeCheckerState.SupertypesPolicy.LowerIfFlexible })
