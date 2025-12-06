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
package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.EnumKind
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

/**
 * 枚举类型
 *
 * 表示仓颉语言中的枚举类型，基于编译器中的EnumTy实现。
 * 枚举类型是一种特殊的类型，支持：
 * - 枚举构造函数（cases）
 * - 枚举成员函数
 * - 继承接口
 * - 泛型支持
 * - 非穷尽性枚举
 *
 * 示例：
 * ```cangjie
 * enum Color {
 *     Red,
 *     Green,
 *     Blue
 * }
 *
 * enum Result<T> {
 *     Success(T),
 *     Error(String)
 * }
 *
 * enum NonExhaustive {
 *     Case1,
 *     Case2,
 *     ...  // 非穷尽性枚举
 * }
 * ```
 *
 * 特点：
 * - 继承自SimpleType
 * - 包含枚举描述符（enumDescriptor）
 * - 支持类型参数（typeArguments）
 * - 支持Option类型（isOption）
 * - 支持类型属性（attributes）
 * - 支持成员作用域（memberScope）
 *
 * 与编译器EnumTy的对应关系：
 * - enumDescriptor 对应 EnumDecl
 * - typeArguments 对应 typeArgs
 * - isOption 对应 hasCorrespondRefEnumTy
 * - memberScope 对应 枚举成员作用域
 */
class EnumType(
    override val constructor: EnumTypeConstructor,
    arguments: List<TypeProjection>,
    memberScope: MemberScope,

    isOption: Boolean = false,
    refinedTypeFactory: RefinedTypeFactory
) : SimpleTypeImpl(
    constructor, arguments, isOption, memberScope, refinedTypeFactory
) {

    /**
     * 枚举名称
     */
    val name: Name
        get() = constructor.declarationDescriptor.name

    /**
     * 枚举类型
     */
    val enumKind: EnumKind
        get() = constructor.declarationDescriptor.enumKind

    /**
     * 是否有关联值
     */
    val hasArguments: Boolean
        get() = constructor.declarationDescriptor.hasArguments

    /**
     * 是否为非穷尽性枚举
     */
    val isNonExhaustive: Boolean
        get() = constructor.declarationDescriptor.isNonExhaustive

    /**
     * 是否为Option类型
     */
    val isOptionType: Boolean
        get() = constructor.declarationDescriptor.isOptionType

    /**
     * 枚举构造函数列表
     */
    val constructors: Collection<EnumConstructorDescriptor>
        get() = constructor.declarationDescriptor.constructors




    /**
     * 转换为指定的Option状态
     *
     * @param isOption 目标Option状态
     * @return 转换后的简单类型
     */
    override fun makeOptionAsSpecified(isOption: Boolean): SimpleType {
        return if (isOption == this.isOption) {
            this
        } else {
            EnumType(constructor, arguments, memberScope, isOption, refinedTypeFactory)
        }
    }

    /**
     * 类型精化
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 精化后的枚举类型
     */
    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): EnumType {
        return cangjieTypeRefiner.refineEnumType(this)
    }

    /**
     * 字符串表示
     *
     * 格式：枚举名<类型参数>?（如果是Option类型）
     *
     * 示例：
     * - Color
     * - Result<Int>
     * - Option<String>?
     *
     * @return 枚举类型的字符串表示
     */
    override fun toString(): String {
        return buildString {
            // 添加注解
            for (annotation in attributes.annotations) {
                append("[", annotation, "] ")
            }

            // 添加枚举名
            append(name)

            // 添加类型参数
            if (arguments.isNotEmpty()) {
                arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
            }

            // 添加Option标记
            if (isOption) {
                append("?")
            }
        }
    }

    /**
     * 相等性比较
     *
     * 两个枚举类型相等当且仅当：
     * - 枚举描述符相同
     * - 类型参数相同
     * - Option状态相同
     * - 类型属性相同
     *
     * @param other 要比较的类型
     * @return true如果相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnumType) return false

        return constructor == other.constructor &&
                arguments == other.arguments &&
                isOption == other.isOption &&
                attributes == other.attributes
    }

    /**
     * 哈希码
     *
     * @return 哈希码
     */
    override fun hashCode(): Int {
        var result = constructor.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + isOption.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }
}

