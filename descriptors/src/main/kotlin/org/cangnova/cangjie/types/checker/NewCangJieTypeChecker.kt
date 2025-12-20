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
import org.cangnova.cangjie.types.AbstractNullabilityChecker.hasNotNullSupertype

/**
 * 简单的经典类型系统上下文
 *
 * 提供类型检查所需的基本类型系统上下文
 */
object SimpleClassicTypeSystemContext : ClassicTypeSystemContext

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

/**
 * 新的仓颉类型检查器接口
 *
 * 扩展了 CangJieTypeChecker，添加了类型精化、类型准备和重写工具的支持
 */
interface NewCangJieTypeChecker : CangJieTypeChecker {
    /** 类型精化器，用于精化类型信息 */
    val cangjieTypeRefiner: CangJieTypeRefiner

    /** 类型准备器，用于准备类型进行检查 */
    val cangjieTypePreparator: CangJieTypePreparator

    /** 重写工具，用于检查方法重写 */
    val overridingUtil: OverridingUtil

    companion object {
        /** 默认的类型检查器实例 */
        val Default = NewCangJieTypeCheckerImpl(CangJieTypeRefiner.Default)
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
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
    }

    /**
     * 严格检查两个简单类型是否相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两个类型严格相等时返回 true
     */
    fun strictEqualTypes(a: SimpleType, b: SimpleType): Boolean {
        return AbstractStrictEqualityTypeChecker.strictEqualTypes(SimpleClassicTypeSystemContext, a, b)
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
object ErrorTypesAreEqualToAnything : CangJieTypeChecker {
    /**
     * 检查两个类型是否相等（忽略泛型参数）
     *
     * 错误类型会与任何类型匹配
     */
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalsIgnoringGenerics(
                a.unwrap(),
                b.unwrap()
            )
        }

    /**
     * 检查子类型关系
     *
     * 错误类型被认为是任何类型的子类型，也是任何类型的父类型
     */
    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).isSubtypeOf(
                subtype.unwrap(),
                supertype.unwrap()
            )
        }

    /**
     * 检查两个类型是否相等
     *
     * 错误类型与任何类型都相等
     */
    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        NewCangJieTypeChecker.Default.run {
            createClassicTypeCheckerState(isErrorTypeEqualsToAnything = true).equalTypes(a.unwrap(), b.unwrap())
        }
}

/**
 * 新的仓颉类型检查器实现
 *
 * 提供完整的类型检查功能，包括：
 * - 类型相等性检查
 * - 子类型关系检查
 * - 忽略泛型的类型比较
 * - 类型精化支持
 *
 * @param cangjieTypeRefiner 类型精化器，用于精化类型信息
 * @param cangjieTypePreparator 类型准备器，用于准备类型进行检查（默认使用 Default）
 */
class NewCangJieTypeCheckerImpl(
    override val cangjieTypeRefiner: CangJieTypeRefiner,
    override val cangjieTypePreparator: CangJieTypePreparator = CangJieTypePreparator.Default
) : NewCangJieTypeChecker {
    /** 重写工具，使用类型精化器创建 */
    override val overridingUtil: OverridingUtil = OverridingUtil.createWithTypeRefiner(cangjieTypeRefiner)

    /**
     * 检查两个类型是否相等（忽略泛型参数）
     *
     * 示例：List<String> 和 List<Int> 会被认为相等
     */
    override fun equalsIgnoringGenerics(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalsIgnoringGenerics(a.unwrap(), b.unwrap())

    /**
     * 检查子类型关系
     *
     * @param subtype 子类型
     * @param supertype 父类型
     * @return 如果 subtype 是 supertype 的子类型则返回 true
     */
    override fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean =
        createClassicTypeCheckerState(
            true, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).isSubtypeOf(subtype.unwrap(), supertype.unwrap()) // todo fix flag errorTypeEqualsToAnything

    /**
     * 检查两个类型是否完全相等
     *
     * 包括泛型参数的比较
     */
    override fun equalTypes(a: CangJieType, b: CangJieType): Boolean =
        createClassicTypeCheckerState(
            false, cangjieTypeRefiner = cangjieTypeRefiner, cangjieTypePreparator = cangjieTypePreparator
        ).equalTypes(a.unwrap(), b.unwrap())

    /** 使用抽象类型检查器检查类型相等性 */
    fun TypeCheckerState.equalTypes(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalTypes(this, a, b)
    }

    /** 使用抽象类型检查器检查类型相等性（忽略泛型） */
    fun TypeCheckerState.equalsIgnoringGenerics(a: UnwrappedType, b: UnwrappedType): Boolean {
        return AbstractTypeChecker.equalsIgnoringGenerics(this, a, b)
    }

    /** 使用抽象类型检查器检查子类型关系 */
    fun TypeCheckerState.isSubtypeOf(subType: UnwrappedType, superType: UnwrappedType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(this, subType, superType)
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
     * 如果类型有非空的父类型，则它是 Any 的子类型。
     *
     * @param type 要检查的类型
     * @return 如果类型是 Any 的子类型则返回 true
     */
    fun isSubtypeOfAny(type: UnwrappedType): Boolean =
        SimpleClassicTypeSystemContext
            .newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true)
            .hasNotNullSupertype(type.lowerIfFlexible(), TypeCheckerState.SupertypesPolicy.LowerIfFlexible)
}
