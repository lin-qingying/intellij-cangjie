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

import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.error.ErrorType

/**
 * 特殊类型检查器
 *
 * 处理仓颉类型系统中的特殊类型：
 * - Nothing: 所有类型的子类型（底类型）
 * - Any: 所有类型的父类型（顶类型）
 * - Option: 可选类型处理
 * - Unit: 单元类型
 * - Error: 错误类型
 */
object SpecialTypeChecker {

    // Nothing 类型名称
    private const val NOTHING_TYPE_NAME = "Nothing"

    // Any 类型名称
    private const val ANY_TYPE_NAME = "Any"

    // Unit 类型名称
    private const val UNIT_TYPE_NAME = "Unit"

    /**
     * 检查类型是否为 Nothing 类型
     *
     * Nothing 是所有类型的子类型（底类型）
     * Nothing <: T 对于所有类型 T 成立
     *
     * @param type 待检查的类型
     * @return 如果是 Nothing 类型返回 true
     */
    fun isNothing(type: CangJieType): Boolean {
        val unwrapped = type.unwrap()
        return isNothingSimple(unwrapped)
    }

    /**
     * 检查简单类型是否为 Nothing
     */
    fun isNothing(type: SimpleType): Boolean {
        return isNothingSimple(type)
    }

    private fun isNothingSimple(type: UnwrappedType): Boolean {
        if (type !is SimpleType) return false

        // 非 Option 的 Nothing
        if (type.isOption) return false

        val constructor = type.constructor
        return constructor.isNothingConstructor()
    }

    /**
     * 检查类型构造器是否为 Nothing 构造器
     */
    private fun TypeConstructor.isNothingConstructor(): Boolean {
        val descriptor = this.declarationDescriptor ?: return false
        return descriptor.name.toString() == NOTHING_TYPE_NAME
    }

    /**
     * 检查类型是否为 Any 类型
     *
     * Any 是所有类型的父类型（顶类型）
     * T <: Any 对于所有类型 T 成立
     *
     * @param type 待检查的类型
     * @return 如果是 Any 类型返回 true
     */
    fun isAny(type: CangJieType): Boolean {
        val unwrapped = type.unwrap()
        return isAnySimple(unwrapped)
    }

    /**
     * 检查简单类型是否为 Any
     */
    fun isAny(type: SimpleType): Boolean {
        return isAnySimple(type)
    }

    private fun isAnySimple(type: UnwrappedType): Boolean {
        if (type !is SimpleType) return false

        val constructor = type.constructor
        return constructor.isAnyConstructor()
    }

    /**
     * 检查类型构造器是否为 Any 构造器
     */
    private fun TypeConstructor.isAnyConstructor(): Boolean {
        val descriptor = this.declarationDescriptor ?: return false
        return descriptor.name.toString() == ANY_TYPE_NAME
    }

    /**
     * 检查类型是否为 Unit 类型
     *
     * Unit 是仓颉语言中的单元类型，类似于 void
     *
     * @param type 待检查的类型
     * @return 如果是 Unit 类型返回 true
     */
    fun isUnit(type: CangJieType): Boolean {
        val unwrapped = type.unwrap()
        return isUnitSimple(unwrapped)
    }

    /**
     * 检查简单类型是否为 Unit
     */
    fun isUnit(type: SimpleType): Boolean {
        return isUnitSimple(type)
    }

    private fun isUnitSimple(type: UnwrappedType): Boolean {
        if (type !is SimpleType) return false

        val constructor = type.constructor
        return constructor.isUnitConstructor()
    }

    /**
     * 检查类型构造器是否为 Unit 构造器
     */
    private fun TypeConstructor.isUnitConstructor(): Boolean {
        val descriptor = this.declarationDescriptor ?: return false
        return descriptor.name.toString() == UNIT_TYPE_NAME
    }

    /**
     * 检查类型是否为 Option 类型
     *
     * @param type 待检查的类型
     * @return 如果是 Option 类型返回 true
     */
    fun isOptionType(type: CangJieType): Boolean {
        return type.isOption
    }

    /**
     * 检查类型是否为 Option<Nothing>（即 ?Nothing）
     *
     * @param type 待检查的类型
     * @return 如果是 ?Nothing 类型返回 true
     */
    fun isOptionNothing(type: CangJieType): Boolean {
        val unwrapped = type.unwrap()
        if (unwrapped !is SimpleType) return false
        if (!unwrapped.isOption) return false

        val constructor = unwrapped.constructor
        return constructor.isNothingConstructor()
    }

    /**
     * 检查类型是否为 Option<Any>（即 ?Any）
     *
     * @param type 待检查的类型
     * @return 如果是 ?Any 类型返回 true
     */
    fun isOptionAny(type: CangJieType): Boolean {
        val unwrapped = type.unwrap()
        if (unwrapped !is SimpleType) return false
        if (!unwrapped.isOption) return false

        val constructor = unwrapped.constructor
        return constructor.isAnyConstructor()
    }

    /**
     * 检查类型是否为错误类型
     *
     * 错误类型用于表示解析或类型推断失败的情况
     *
     * @param type 待检查的类型
     * @return 如果是错误类型返回 true
     */
    fun isErrorType(type: CangJieType): Boolean {
        return type.unwrap() is ErrorType
    }

    /**
     * 获取类型的非 Option 版本
     *
     * 如果类型是 Option 类型，返回其内部类型；
     * 否则返回原类型
     *
     * @param type 待处理的类型
     * @return 非 Option 版本的类型
     */
    fun unwrapOption(type: CangJieType): CangJieType {
        val unwrapped = type.unwrap()
        if (unwrapped is OptionType) {
            return unwrapped.innerType
        }
        if (unwrapped is SimpleType && unwrapped.isOption) {
            return unwrapped.makeOptionAsSpecified(false)
        }
        return type
    }

    /**
     * 将类型包装为 Option 类型
     *
     * @param type 待包装的类型
     * @return Option 版本的类型
     */
    fun wrapAsOption(type: CangJieType): CangJieType {
        val unwrapped = type.unwrap()
        if (unwrapped.isOption) {
            return type
        }
        return OptionType(type)
    }

    /**
     * 检查 Option 类型的兼容性
     *
     * 规则：
     * - T <: ?T （非 Option 类型可以赋值给 Option 类型）
     * - ?T 不是 T 的子类型
     *
     * @param subType 子类型
     * @param superType 父类型
     * @return 如果满足 Option 兼容性规则返回 true
     */
    fun checkOptionCompatibility(subType: SimpleType, superType: SimpleType): Boolean {
        // 相同的 Option 状态
        if (subType.isOption == superType.isOption) {
            return true
        }

        // 非 Option 可以赋值给 Option
        if (!subType.isOption && superType.isOption) {
            // 检查内部类型是否兼容
            val unwrappedSuper = superType.makeOptionAsSpecified(false)
            return CangJieSubtypeChecker.isSubtypeOf(subType, unwrappedSuper)
        }

        // Option 不能赋值给非 Option
        return false
    }

    /**
     * 检查类型是否可能为 null/None
     *
     * 在仓颉语言中，只有 Option 类型可以为 None
     *
     * @param type 待检查的类型
     * @return 如果类型可能为 None 返回 true
     */
    fun canBeNone(type: CangJieType): Boolean {
        return type.isOption
    }

    /**
     * 获取两个类型的 Option 兼容类型
     *
     * 如果任一类型是 Option，返回 Option 类型；
     * 否则返回非 Option 类型
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return Option 兼容的类型
     */
    fun getOptionCompatibleType(a: SimpleType, b: SimpleType): SimpleType {
        return if (a.isOption || b.isOption) {
            a.makeOptionAsSpecified(true)
        } else {
            a
        }
    }
}
