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
package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import cn.cangnova.cangjie.descriptors.EnumDescriptor
import cn.cangnova.cangjie.descriptors.EnumKind
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.builtIns
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner

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
    private val enumDescriptor: EnumDescriptor,
    typeArguments: List<TypeProjection> = emptyList(),
    override val isOption: Boolean = false,
    attributes: TypeAttributes = TypeAttributes.Empty
) : SimpleType() {

    /**
     * 类型构造函数
     */
    override val constructor: TypeConstructor = EnumTypeConstructor(enumDescriptor)

    /**
     * 类型参数
     */
    override val arguments: List<TypeProjection> = typeArguments

    /**
     * 类型属性
     */
    override val attributes: TypeAttributes = attributes

    /**
     * 成员作用域
     *
     * 委托给枚举描述符的成员作用域
     */
    override val memberScope: MemberScope
        get() = enumDescriptor.unsubstitutedMemberScope

    /**
     * 枚举描述符
     */
    val descriptor: EnumDescriptor = enumDescriptor

    /**
     * 枚举名称
     */
    val name: Name
        get() = enumDescriptor.name

    /**
     * 枚举类型
     */
    val enumKind: EnumKind
        get() = enumDescriptor.enumKind

    /**
     * 是否有关联值
     */
    val hasArguments: Boolean
        get() = enumDescriptor.hasArguments

    /**
     * 是否为非穷尽性枚举
     */
    val isNonExhaustive: Boolean
        get() = enumDescriptor.isNonExhaustive

    /**
     * 是否为Option类型
     */
    val isOptionType: Boolean
        get() = enumDescriptor.isOptionType

    /**
     * 枚举构造函数列表
     */
    val constructors: Collection<EnumConstructorDescriptor>
        get() = enumDescriptor.constructors

    /**
     * 枚举成员函数列表
     */
    val members: Collection<FunctionDescriptor>
        get() = enumDescriptor.members

    /**
     * 替换类型属性
     *
     * 创建具有新属性的枚举类型副本
     *
     * @param newAttributes 新的类型属性
     * @return 具有新属性的枚举类型
     */
    override fun replaceAttributes(newAttributes: TypeAttributes): EnumType {
        return EnumType(enumDescriptor, arguments, isOption, newAttributes)
    }


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
            EnumType(enumDescriptor, arguments, isOption, attributes)
        }
    }

    /**
     * 类型精化
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 精化后的枚举类型
     */
    @TypeRefinement
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

        return enumDescriptor == other.enumDescriptor &&
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
        var result = enumDescriptor.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + isOption.hashCode()
        result = 31 * result + attributes.hashCode()
        return result
    }
}

/**
 * 枚举类型构造函数
 *
 * 表示枚举类型的构造函数，负责创建枚举类型实例。
 *
 * 特点：
 * - 包含枚举描述符
 * - 支持类型参数
 * - 支持类型参数声明
 * - 支持声明描述符
 *
 * 示例：
 * ```kotlin
 * val enumConstructor = EnumTypeConstructor(enumDescriptor)
 * val enumType = enumConstructor.createType(typeArguments)
 * ```
 */
class EnumTypeConstructor(
    private val enumDescriptor: EnumDescriptor
) : TypeConstructor {

    /**
     * 声明描述符
     */
    override val declarationDescriptor: EnumDescriptor = enumDescriptor

    /**
     * 内置类型信息
     */
    override val builtIns: CangJieBuiltIns
        get() = enumDescriptor.builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
        return this
    }

    /**
     * 类型参数声明
     */
    override val parameters: List<cn.cangnova.cangjie.descriptors.TypeParameterDescriptor>
        get() = enumDescriptor.declaredTypeParameters

    /**
     * 超类型
     */
    override val supertypes: Collection<CangJieType>
        get() = emptyList() // 枚举类型没有超类型

    /**
     * 是否最终
     */
    override val isFinal: Boolean = true

    /**
     * 是否拒绝
     */
    override val isDenotable: Boolean = true

    /**
     * 创建类型
     *
     * @param arguments 类型参数
     * @return 枚举类型
     */
    fun createType(arguments: List<TypeProjection>): EnumType {
        return EnumType(enumDescriptor, arguments)
    }

    /**
     * 字符串表示
     *
     * @return 枚举类型构造函数的字符串表示
     */
    override fun toString(): String {
        return "EnumTypeConstructor(${enumDescriptor.name})"
    }

    /**
     * 相等性比较
     *
     * @param other 要比较的对象
     * @return true如果相等
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnumTypeConstructor) return false

        return enumDescriptor == other.enumDescriptor
    }

    /**
     * 哈希码
     *
     * @return 哈希码
     */
    override fun hashCode(): Int {
        return enumDescriptor.hashCode()
    }
} 