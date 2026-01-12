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
        return createClassicTypeCheckerState(
            true, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    }

    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this, a, b)
    }

    fun TypeCheckerState.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(this, subType, superType)
    }

    /**
     * 判断两个类型是否完全相等
     */
    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        return createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalTypes(a.unwrap(), b.unwrap())

    }


}



/**
 * 简单的经典类型系统上下文
 *
 * 提供类型检查所需的基本类型系统上下文
 */
object SimpleClassicTypeSystemContext : ClassicTypeSystemContext



/**
 * 可选类型检查器
 *
 * 用于检查可选类型（Option）相关的类型关系
 */
object OptionChecker {
    /**
     * 检查类型是否是 Any 的子类型
     *
     * 用于判断一个类型是否可以安全地转换为非 Option 类型。
     * 如果类型不是 Nothing 且不是 Option 类型，则它是 Any 的子类型。
     *
     * 仓颉语言语义：
     * - 非 Option 类型（如 Int）是 Any 的子类型
     * - Option 类型（如 ?Int）不是 Any 的子类型（?Int 是 ?Any 的子类型）
     * - Nothing 不是 Any 的子类型
     *
     * @param type 要检查的类型
     * @return 如果类型是 Any 的子类型则返回 true
     */
    fun isSubtypeOfAny(type: UnwrappedType): Boolean {
        val state = SimpleClassicTypeSystemContext
            .newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)

        with(SimpleClassicTypeSystemContext) {
            val simpleType = type.lowerIfFlexible()

            // 如果类型本身不是 Option，则检查是否有非 Option 的超类型（即是 Any 的子类型）
            // 使用 anySupertype 遍历超类型层次，查找非 Option 的超类型
            return state.anySupertype(
                start = simpleType,
                predicate = { supertype ->
                    // 找到非 Option 的超类型，表示原类型是 Any 的子类型
                    !supertype.isMarkedOption() && !supertype.typeConstructor().isNothingConstructor()
                },
                supertypesPolicy = { TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )
        }
    }
}


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
