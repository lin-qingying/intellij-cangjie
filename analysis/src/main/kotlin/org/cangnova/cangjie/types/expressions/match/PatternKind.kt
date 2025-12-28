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
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext


data class PatternContext(
    val subject: Subject,
    val context: ExpressionTypingContext
)
/**
 * 模式种类（PatternKind）
 *
 * 表示仓颉语言中模式匹配的各种模式类型。这是一个密封类层次结构，
 * 对应仓颉语言规范中定义的所有模式类型。
 *
 * ## 仓颉语言模式类型对照
 *
 * | 仓颉语法 | PatternKind | 示例 |
 * |---------|-------------|------|
 * | 通配符 | [Wild] | `_` |
 * | 绑定模式 | [Binding] | `x`, `value` |
 * | 常量模式 | [Const] | `0`, `"hello"`, `true` |
 * | 类型模式 | [Type] | `x: Int64` |
 * | 元组模式 | [Tuple] | `(x, y)`, `(_, _)` |
 * | 枚举模式 | [Enum] | `Some(x)`, `None` |
 *
 * ## 穷举性（Exhaustiveness）
 *
 * 在穷举性检查中，不同的模式种类有不同的覆盖特性：
 *
 * - **通配符和绑定模式**: 覆盖该类型的所有值（irrefutable）
 * - **常量模式**: 仅覆盖特定值
 * - **枚举模式**: 覆盖特定的枚举变体
 * - **元组模式**: 递归检查每个分量
 *
 * @see Pattern
 * @see Constructor
 */
sealed class PatternKind {

    override fun toString(): String = this::class.simpleName ?: ""

    /**
     * 生成模式的显示字符串
     *
     * 用于诊断消息和调试输出。
     *
     * @return 模式的可读字符串表示
     */
    open fun showString(): String = ""

    /**
     * 错误模式
     *
     * 表示解析失败或语义错误的模式。
     * 在穷举性检查中，错误模式不参与匹配。
     */
    data object Error : PatternKind()

    /**
     * 通配符模式
     *
     * 匹配任何值，在仓颉语言中写作 `_`。
     * 通配符模式是 irrefutable 的，可以用于变量定义和 for-in 表达式。
     *
     * ## 示例
     * ```cangjie
     * match (x) {
     *     case _ => println("matches anything")
     * }
     * ```
     */
    data object Wild : PatternKind()

    /**
     * 绑定模式
     *
     * 匹配任何值并将其绑定到一个变量。
     * 绑定模式也是 irrefutable 的。
     *
     * ## 示例
     * ```cangjie
     * match (x) {
     *     case value => println("value = ${value}")
     * }
     * ```
     *
     * @property type 绑定变量的类型
     * @property name 绑定变量的名称
     */
    data class Binding(val type: CangJieType, val name: String) : PatternKind()

    /**
     * 类型模式
     *
     * 匹配特定类型的值。在仓颉语言中用于类型判断。
     *
     * ## 示例
     * ```cangjie
     * match (obj) {
     *     case s: String => println("is string: ${s}")
     *     case n: Int64 => println("is int: ${n}")
     * }
     * ```
     *
     * @property type 要匹配的类型
     * @property name 绑定变量的名称
     */
    data class Type(val type: CangJieType, val name: String) : PatternKind()

    /**
     * 常量模式
     *
     * 匹配特定的常量值。支持整数、浮点数、字符、布尔、字符串字面量。
     *
     * ## 示例
     * ```cangjie
     * match (x) {
     *     case 0 => println("zero")
     *     case 1 => println("one")
     *     case _ => println("other")
     * }
     * ```
     *
     * @property value 要匹配的常量值
     */
    data class Const(val value: ConstantValue<*>) : PatternKind() {
        override fun showString(): String = value.toString()
    }

    /**
     * 元组模式
     *
     * 匹配元组类型的值，递归匹配每个分量。
     *
     * ## 示例
     * ```cangjie
     * match (tuple) {
     *     case (0, y) => println("first is zero, second is ${y}")
     *     case (x, 0) => println("first is ${x}, second is zero")
     *     case (_, _) => println("other")
     * }
     * ```
     *
     * ## 穷举性
     * 元组模式的穷举性取决于每个分量模式的穷举性。
     * 如果所有分量都是 irrefutable 的，则整个元组模式也是 irrefutable 的。
     *
     * @property subPatterns 元组各分量的子模式列表
     */
    data class Tuple(val subPatterns: List<Pattern>) : PatternKind() {
        override fun showString(): String {
            return subPatterns.joinToString(",", "(", ")") { it.text(null) }
        }
    }

    /**
     * 枚举模式
     *
     * 匹配枚举类型的特定变体。可以包含子模式用于解构枚举携带的数据。
     *
     * ## 示例
     * ```cangjie
     * enum Option<T> {
     *     | Some(T)
     *     | None
     * }
     *
     * match (opt) {
     *     case Some(value) => println("has value: ${value}")
     *     case None => println("no value")
     * }
     * ```
     *
     * ## 穷举性要求
     * 对于枚举类型的 match 表达式，必须覆盖所有枚举变体：
     * - 显式列出所有变体，或
     * - 使用通配符 `_` 作为默认分支
     *
     * @property enum 枚举类型的 PSI 元素
     * @property entry 枚举变体（构造器）
     * @property subPatterns 解构枚举携带数据的子模式列表
     */
    data class Enum(val enum: CjEnum, val entry: CjEnumConstructor, val subPatterns: List<Pattern>) : PatternKind() {
        override fun showString(): String {
            val str = StringBuilder()
            str.append(entry.name ?: "")
            if (subPatterns.isNotEmpty()) {
                str.append("(")
                str.append(subPatterns.joinToString(",") { it.text(null) })
                str.append(")")
            }
            return str.toString()
        }
    }
}
