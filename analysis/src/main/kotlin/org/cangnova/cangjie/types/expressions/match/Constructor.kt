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

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.resolve.caches.type
import org.cangnova.cangjie.resolve.constants.*
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.expressions.match.exhaustive.inria.MarangetException
import org.cangnova.cangjie.types.isBoolean
import org.cangnova.cangjie.types.isBuiltinTupleType
import org.cangnova.cangjie.types.isEnum
import org.cangnova.cangjie.types.isUnit
import org.cangnova.cangjie.resolve.constants.ConstantValue as CV
import org.cangnova.cangjie.types.substitute as Usubstitute

/**
 * 从类型引用列表中提取类型列表
 *
 * @return 非空类型的列表
 */
fun List<CjTypeReference>.types(): List<CangJieType> {
    return mapNotNull { it.type }
}

/**
 * 类型替换
 *
 * 在泛型上下文中，将类型参数替换为实际类型。
 *
 * @param type 提供类型参数的外层类型
 * @return 替换后的类型
 */
private fun CangJieType.substitute(type: CangJieType): CangJieType {
    val thisName = this.constructor.declarationDescriptor?.name ?: return this

    // 查找泛型参数的索引
    var index: Int? = null
    type.deccriptorClass?.declaredTypeParameters?.forEachIndexed { index1, typeParameterDescriptor ->
        if (typeParameterDescriptor.name == thisName) {
            index = index1
        }
    }
    if (index != null) {
        return this.Usubstitute(type.arguments[index].type)
    }
    return this
}

/**
 * 构造器（Constructor）
 *
 * 在穷举性检查算法中，构造器表示类型可能取值的"形状"。
 * 构造器用于确定模式覆盖的值空间，是 Maranget 算法的核心概念。
 *
 * ## 概念说明
 *
 * 在模式匹配理论中，每个类型都有一组构造器：
 *
 * - **布尔类型**: `true` 和 `false` 两个构造器
 * - **枚举类型**: 每个枚举变体是一个构造器
 * - **元组类型**: 单一构造器（元组本身）
 * - **整数类型**: 理论上无限个构造器（每个整数值）
 *
 * ## 在穷举性检查中的作用
 *
 * ```
 * 类型: Option<T>
 * 构造器: [Some, None]
 *
 * match (opt) {
 *     case Some(x) => ...  // 覆盖 Some 构造器
 *     case None => ...     // 覆盖 None 构造器
 * }
 * // 所有构造器都被覆盖 → 穷举
 * ```
 *
 * ## 子类型
 *
 * - [Enum] - 枚举变体构造器
 * - [Type] - 类型构造器（用于类型模式）
 * - [Single] - 单一构造器（元组、结构体等）
 * - [ConstantValue] - 常量值构造器
 *
 * @see Pattern
 * @see PatternKind
 */
sealed class Constructor {

    /**
     * 计算构造器的元数（arity）
     *
     * 元数表示构造器接受的参数数量。
     *
     * @param type 构造器应用的类型
     * @return 参数数量
     *
     * ## 示例
     * - `Some(T)` 的元数为 1
     * - `None` 的元数为 0
     * - `(A, B, C)` 元组的元数为 3
     */
    fun arity(type: CangJieType): Int {
        return when {
            type.isEnum && this is Enum -> entry.typeReferences.size
            type.isBuiltinTupleType && this is Single -> type.arguments.size
            else -> 0
        }
    }

    /**
     * 获取构造器参数的子类型列表
     *
     * 用于在模式特化时确定子模式的类型。
     *
     * @param type 构造器应用的类型
     * @return 参数类型列表
     *
     * ## 示例
     * - 对于 `Some(T)` 应用于 `Option<Int64>`，返回 `[Int64]`
     * - 对于 `(A, B)` 元组，返回 `[A的类型, B的类型]`
     */
    fun subTypes(type: CangJieType): List<CangJieType> {
        return when (this) {
            is Enum -> entry.typeReferences.types().map { it.substitute(type) }
            is Single -> {
                if (type.isBuiltinTupleType) {
                    type.arguments.map { it.type }
                } else {
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    /**
     * 检查构造器是否被值范围覆盖
     *
     * 用于常量模式和区间模式的匹配检查。
     *
     * @param from 范围起始值
     * @param to 范围结束值
     * @param included 是否包含结束值（闭区间）
     * @return 如果构造器被范围覆盖则返回 true
     */
    open fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = false

    /**
     * 枚举变体构造器
     *
     * 表示枚举类型的一个变体。
     *
     * @property entry 枚举变体的 PSI 元素
     */
    data class Enum(val entry: CjEnumConstructor) : Constructor()

    /**
     * 类型构造器
     *
     * 用于类型模式匹配。
     *
     * @property type 要匹配的类型
     */
    data class Type(val type: CangJieType) : Constructor()

    /**
     * 单一构造器
     *
     * 用于没有多个变体的类型，如元组、结构体等。
     * 这类类型只有一种"形状"，因此只需要一个构造器。
     */
    data object Single : Constructor() {
        override fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = true
    }

    /**
     * 常量值构造器
     *
     * 表示特定的常量值。
     *
     * @property value 常量值
     */
    data class ConstantValue(val value: CV<*>) : Constructor() {
        override fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean =
            if (included) {
                value >= from && value <= to
            } else {
                value >= from && value < to
            }
    }

    companion object {
        /**
         * 延迟获取类型的所有构造器
         *
         * @param ty 类型
         * @return 构造器序列
         */
        private fun allConstructorsLazy(ty: CangJieType): Sequence<Constructor> =
            when {
                ty.isBoolean -> sequenceOf(true, false).map { ConstantValue(BoolValue(it)) }
                ty.isUnit -> sequenceOf(true, false).map { ConstantValue(UnitValue) }
                ty.isEnum -> (ty.deccriptorClass?.source?.getPsi() as? CjEnum)
                    ?.constructor?.asSequence()?.map { Enum(it) } ?: emptySequence()
                else -> sequenceOf(Single)
            }

        /**
         * 获取类型的所有可能构造器
         *
         * 这是穷举性检查的核心：通过枚举类型的所有构造器，
         * 检查 match 表达式是否覆盖了所有情况。
         *
         * @param ty 类型
         * @return 构造器列表
         *
         * ## 示例
         * - `Bool` → `[ConstantValue(true), ConstantValue(false)]`
         * - `Option<T>` → `[Enum(Some), Enum(None)]`
         * - `(A, B)` → `[Single]`
         */
        fun allConstructors(ty: CangJieType): List<Constructor> = allConstructorsLazy(ty).toList()
    }
}

/**
 * 常量值比较运算符
 *
 * 用于常量模式的范围检查。
 *
 * @param other 要比较的另一个常量值
 * @return 比较结果：负数表示小于，零表示等于，正数表示大于
 * @throws MarangetException 如果比较不兼容的类型
 */
private operator fun CV<*>.compareTo(other: CV<*>): Int {
    return when (this) {
        is UnitValue if other is UnitValue -> 0
        is BoolValue if other is BoolValue -> value.compareTo(other.value)
        is Int64Value if other is Int64Value -> value.compareTo(other.value)
        is Int32Value if other is Int32Value -> value.compareTo(other.value)
        is Int16Value if other is Int16Value -> value.compareTo(other.value)
        is Int8Value if other is Int8Value -> value.compareTo(other.value)
        is UInt64Value if other is UInt64Value -> value.compareTo(other.value)
        is UInt32Value if other is UInt32Value -> value.compareTo(other.value)
        is UInt16Value if other is UInt16Value -> value.compareTo(other.value)
        is UInt8Value if other is UInt8Value -> value.compareTo(other.value)
        is Float64Value if other is Float64Value -> value.compareTo(other.value)
        is Float32Value if other is Float32Value -> value.compareTo(other.value)
        is Float16Value if other is Float16Value -> value.compareTo(other.value)
        is StringValue if other is StringValue -> value.compareTo(other.value)
        is RuneValue if other is RuneValue -> value.compareTo(other.value)
        else -> throw MarangetException("Comparison of incompatible types: $javaClass and ${other.javaClass}")
    }
}
