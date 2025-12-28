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

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils

/**
 * 生成枚举构造器的初始化文本
 *
 * @param subPatterns 子模式列表
 * @param ctx 上下文元素，用于生成文本
 * @return 如果有类型参数则返回 "(subPattern1, subPattern2, ...)"，否则返回空字符串
 */
private fun CjEnumConstructor.initializer(subPatterns: List<Pattern>, ctx: CjElement?): String = when {
    typeEntry != null -> subPatterns.joinToString(",", "(", ")") { it.text(ctx) }
    else -> ""
}

/**
 * 模式（Pattern）
 *
 * 表示仓颉语言中 match 表达式的单个模式。模式用于在穷举性检查算法中
 * 表示和操作匹配分支。
 *
 * ## 仓颉语言支持的模式类型
 *
 * - **常量模式**: 整数、浮点数、字符、布尔、字符串字面量
 * - **通配符模式**: `_`，匹配任何值
 * - **绑定模式**: `x`，匹配任何值并绑定到变量
 * - **元组模式**: `(p1, p2, ...)`，匹配元组
 * - **类型模式**: `x: Type`，匹配特定类型的值
 * - **枚举模式**: `EnumVariant(p1, p2, ...)`，匹配枚举变体
 *
 * ## 在穷举性检查中的作用
 *
 * ```
 * match (value) {
 *     case Some(x) => ...  // Pattern(type=Option<T>, kind=Enum(Some, [Binding]))
 *     case None => ...     // Pattern(type=Option<T>, kind=Enum(None, []))
 * }
 * ```
 *
 * @property type 模式匹配的类型
 * @property kind 模式的种类，决定模式的具体行为
 * @see PatternKind
 * @see Constructor
 */
data class Pattern(val type: CangJieType, val kind: PatternKind) {

    /**
     * 生成模式的文本表示
     *
     * 用于在诊断消息中显示缺失的模式。
     *
     * @param ctx 上下文元素，用于解析符号名称
     * @return 模式的可读文本表示
     *
     * ## 示例
     * - 通配符: `_`
     * - 绑定: `x`
     * - 枚举: `Some(x)` 或 `None`
     * - 元组: `(_, _)`
     * - 常量: `42`
     */
    fun text(ctx: CjElement?): String =
        when (kind) {
            is PatternKind.Wild -> "_"
            is PatternKind.Binding -> kind.name
            is PatternKind.Enum -> {
                val entryName = kind.entry.name.orEmpty()
                val initializer = kind.entry.initializer(kind.subPatterns, ctx)
                "$entryName$initializer"
            }
            is PatternKind.Type -> kind.name
            is PatternKind.Const -> kind.value.toString()
            PatternKind.Error -> ""
            is PatternKind.Tuple -> kind.subPatterns.joinToString(",", "(", ")") { it.text(ctx) }
        }

    /**
     * 获取模式对应的构造器列表
     *
     * 构造器用于穷举性检查算法中确定模式覆盖的值空间。
     *
     * @return 构造器列表，对于通配符和绑定模式返回 null（表示匹配所有构造器）
     *
     * ## 构造器映射
     * - 通配符/绑定 → null（匹配所有）
     * - 枚举变体 → [Constructor.Enum]
     * - 常量值 → [Constructor.ConstantValue]
     * - 元组 → [Constructor.Single]
     * - 类型模式 → [Constructor.Type]
     */
    val constructors: List<Constructor>?
        get() = when (kind) {
            is PatternKind.Wild, is PatternKind.Binding -> null
            is PatternKind.Enum -> listOf(Constructor.Enum(kind.entry))
            is PatternKind.Const -> listOf(Constructor.ConstantValue(kind.value))
            is PatternKind.Tuple -> listOf(Constructor.Single)
            is PatternKind.Error -> null
            is PatternKind.Type -> listOf(Constructor.Type(kind.type))
        }

    /**
     * 获取适合生成构造器的类型
     *
     * 在某些情况下（如多重引用），需要解引用类型以获取实际的枚举类型。
     *
     * @return 适合生成构造器的类型，通常与 [type] 相同
     */
    val ergonomicType: CangJieType
        get() = type

    companion object {
        /**
         * 错误模式
         *
         * 表示解析失败或类型错误的模式。
         */
        val Error = Pattern(ErrorUtils.errorVariableType, PatternKind.Error)

        /**
         * 创建通配符模式
         *
         * @param ty 模式的类型，默认为错误类型
         * @return 通配符模式实例
         */
        fun wild(ty: CangJieType = ErrorUtils.errorVariableType): Pattern = Pattern(ty, PatternKind.Wild)
    }
}
