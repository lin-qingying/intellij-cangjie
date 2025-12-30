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
 */

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isError

/**
 * 模式绑定（PatternBinding）
 *
 * 表示模式匹配中引入的变量绑定。当模式中包含绑定模式或类型模式时，
 * 会创建一个或多个 PatternBinding 对象来记录需要声明的变量。
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 绑定模式
 * match (value) {
 *     case x => ...  // PatternBinding(name="x", type=value的类型)
 * }
 *
 * // 类型模式
 * match (obj) {
 *     case s: String => ...  // PatternBinding(name="s", type=String)
 * }
 *
 * // 变量声明（解构）
 * let (a, b) = tuple  // PatternBinding(name="a"), PatternBinding(name="b")
 *
 * // for-in 循环
 * for ((k, v) in map) { ... }  // PatternBinding(name="k"), PatternBinding(name="v")
 * ```
 *
 * ## 属性说明
 *
 * @property name 绑定的变量名称
 * @property type 绑定变量的类型
 * @property element 关联的 PSI 元素，用于错误报告和导航
 * @property isMutable 变量是否可变（仓颉中模式绑定默认不可变）
 * @property visibility 变量的可见性（仅用于顶层声明）
 *
 * @see PatternSource
 * @see BindingCollector
 */
data class PatternBinding(
    val name: String,
    val type: CangJieType,
    val element: CjElement,
    val isMutable: Boolean = false,
    val visibility: DescriptorVisibility? = null
) {
    /**
     * 检查绑定是否有效
     *
     * 无效的绑定包括：空名称、错误类型等。
     */
    val isValid: Boolean
        get() = name.isNotBlank() && !type.isError
}

/**
 * 模式来源（PatternSource）
 *
 * 标识模式出现的语法上下文。不同的来源有不同的语义约束，
 * 例如 let 声明只允许 irrefutable 模式。
 *
 * ## 来源与约束
 *
 * | 来源 | 允许 refutable | 示例 |
 * |------|---------------|------|
 * | MATCH_EXPRESSION | 是 | `match (x) { case ... }` |
 * | LET_DECLARATION | 否 | `let x = 1`, `let (a, b) = tuple` |
 * | FOR_IN_EXPRESSION | 否 | `for (x in list)` |
 * | LET_EXPRESSION | 是 | `if (let Some(x) <- opt)` |
 *
 * ## Refutability 规则
 *
 * - **Irrefutable 模式**: 通配符 `_`、绑定模式 `x`、单构造器枚举
 * - **Refutable 模式**: 常量模式、类型模式、多构造器枚举模式
 *
 * @see RefutabilityChecker
 */
enum class PatternSource {
    /**
     * match 表达式
     *
     * 允许任何模式类型，包括 refutable 模式。
     * 必须满足穷举性要求。
     *
     * ```cangjie
     * match (opt) {
     *     case Some(x) => ...
     *     case None => ...
     * }
     * ```
     */
    MATCH_EXPRESSION,

    /**
     * let 变量声明
     *
     * 只允许 irrefutable 模式，因为声明必须成功。
     *
     * ```cangjie
     * let x = 1
     * let (a, b) = tuple
     * ```
     */
    LET_DECLARATION,

    /**
     * for-in 循环
     *
     * 只允许 irrefutable 模式，用于解构迭代元素。
     *
     * ```cangjie
     * for (x in list) { ... }
     * for ((k, v) in map) { ... }
     * ```
     */
    FOR_IN_EXPRESSION,

    /**
     * let 表达式（条件绑定）
     *
     * 允许 refutable 模式，用于条件解构。
     * 匹配失败时条件为 false。
     *
     * ```cangjie
     * if (let Some(x) <- opt) {
     *     // x 在此作用域内可用
     * }
     * ```
     */
    LET_EXPRESSION;

    /**
     * 是否允许 refutable 模式
     */
    val allowsRefutable: Boolean
        get() = when (this) {
            MATCH_EXPRESSION, LET_EXPRESSION -> true
            LET_DECLARATION, FOR_IN_EXPRESSION -> false
        }

    /**
     * 获取来源的显示名称（用于错误消息）
     */
    val displayName: String
        get() = when (this) {
            MATCH_EXPRESSION -> "match 表达式"
            LET_DECLARATION -> "变量声明"
            FOR_IN_EXPRESSION -> "for-in 循环"
            LET_EXPRESSION -> "let 表达式"
        }
}

/**
 * 扩展的模式分析上下文
 *
 * 在原有 PatternContext 的基础上添加更多信息，
 * 支持完整的模式分析流程。
 *
 * @property subject 被匹配的对象
 * @property typingContext 类型检查上下文
 * @property source 模式来源
 * @property allowRefutable 是否允许 refutable 模式（由 source 决定）
 * @property expectedType 期望的类型（用于类型推导）
 *
 * @see PatternSource
 * @see PatternResolver
 */
data class ExtendedPatternContext(
    val subject: Subject,
    val typingContext: org.cangnova.cangjie.types.expressions.ExpressionTypingContext,
    val source: PatternSource,
    val expectedType: CangJieType? = null
) {
    /**
     * 是否允许 refutable 模式
     */
    val allowRefutable: Boolean
        get() = source.allowsRefutable

    /**
     * 获取被匹配对象的类型
     */
    val subjectType: CangJieType
        get() = subject.type

    /**
     * 创建一个具有不同期望类型的新上下文
     */
    fun withExpectedType(type: CangJieType?): ExtendedPatternContext =
        copy(expectedType = type)
}
