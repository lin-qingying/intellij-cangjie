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

package org.cangnova.cangjie.quickDoc.cdoc


/**
 * 仓颉 IDE 描述符渲染器的语法高亮管理器接口。
 *
 * 该接口定义了在渲染描述符时如何为不同的语法元素添加高亮显示。
 * 通过为不同类型的语法元素（关键字、标识符、符号等）分配不同的属性，
 * 可以实现语法高亮、颜色主题等功能。
 *
 * ## 核心功能
 *
 * 1. **文本高亮**: 通过 [appendHighlighted] 为文本添加指定的高亮属性
 * 2. **代码片段高亮**: 通过 [appendCodeSnippetHighlightedByLexer] 对代码片段进行词法高亮
 * 3. **语法元素属性**: 为各种语法元素提供预定义的属性（如关键字、类名、操作符等）
 *
 * ## 使用方式
 *
 * ```kotlin
 * class MyHighlightingManager : CangJieIdeDescriptorRendererHighlightingManager<MyAttributes> {
 *     override fun StringBuilder.appendHighlighted(value: String, attributes: MyAttributes) {
 *         // 根据属性类型添加 HTML 标签或 ANSI 颜色代码
 *         append("<span class='${attributes.cssClass}'>$value</span>")
 *     }
 *
 *     override val asKeyword = MyAttributes("keyword")
 *     override val asClassName = MyAttributes("class-name")
 *     // ... 其他属性
 * }
 * ```
 *
 * ## 语法元素分类
 *
 * ### 符号元素
 * - [asDot]: 点号 `.`
 * - [asComma]: 逗号 `,`
 * - [asColon]: 冒号 `:`
 * - [asDoubleColon]: 双冒号 `::`
 * - [asLtColon]: 小于冒号 `<:`（继承符号）
 * - [asParentheses]: 圆括号 `()`
 * - [asBrackets]: 方括号 `[]`
 * - [asBraces]: 花括号 `{}`
 * - [asArrow]: 箭头 `->`
 * - [asOperationSign]: 操作符（如 `=`、`+` 等）
 *
 * ### 关键字和修饰符
 * - [asKeyword]: 通用关键字
 * - [asLet]: `let` 关键字
 * - [asVar]: `var` 关键字
 * - [asMut]: `mut` 关键字
 * - [asProp]: `prop` 关键字
 * - [asStatic]: `static` 关键字
 * - [asConst]: `const` 关键字
 *
 * ### 标识符
 * - [asClassName]: 类名
 * - [asPackageName]: 包名
 * - [asTypeAlias]: 类型别名
 * - [asTypeParameterName]: 类型参数名称
 * - [asInstanceProperty]: 实例属性
 * - [asParameter]: 函数参数
 * - [asLocalVarOrLet]: 局部变量或常量
 * - [asFunDeclaration]: 函数声明
 * - [asFunCall]: 函数调用
 *
 * ### 注解
 * - [asAnnotationName]: 注解名称
 * - [asAnnotationAttributeName]: 注解属性名称
 *
 * ### 类型标记
 * - [asNullityMarker]: 可空标记 `?`
 * - [asNonNullAssertion]: 非空断言 `& Any`
 *
 * ### 信息和错误
 * - [asInfo]: 信息文本（如注释）
 * - [asError]: 错误文本（如类型错误）
 *
 * @param TAttributes 属性类型，必须继承自 [Attributes]
 *
 * @see CangJieIdeDescriptorRenderer
 * @see Attributes
 */
interface CangJieIdeDescriptorRendererHighlightingManager<TAttributes : CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes> {

    /**
     * 向 StringBuilder 添加高亮显示的文本。
     *
     * @receiver StringBuilder 字符串构建器
     * @param value 要添加的文本内容
     * @param attributes 高亮属性，决定如何显示该文本
     */
    fun StringBuilder.appendHighlighted(value: String, attributes: TAttributes)

    /**
     * 向 StringBuilder 添加通过词法分析器高亮的代码片段。
     *
     * 该方法通常用于渲染代码示例、默认值等需要语法高亮的代码片段。
     *
     * @receiver StringBuilder 字符串构建器
     * @param codeSnippet 代码片段内容
     */
    fun StringBuilder.appendCodeSnippetHighlightedByLexer(codeSnippet: String)

    // 错误和信息

    /** 错误文本属性（如类型错误、未解析的引用等）*/
    val asError: TAttributes

    /** 信息文本属性（如注释、提示信息等）*/
    val asInfo: TAttributes

    // 符号和操作符

    /** 点号 `.` 属性 */
    val asDot: TAttributes

    /** const 关键字属性 */
    val asConst: TAttributes

    /** 逗号 `,` 属性 */
    val asComma: TAttributes

    /** static 关键字属性 */
    val asStatic: TAttributes

    /** 冒号 `:` 属性 */
    val asColon: TAttributes

    /** 小于冒号 `<:` 属性（继承符号）*/
    val asLtColon: TAttributes

    /** 双冒号 `::` 属性 */
    val asDoubleColon: TAttributes

    /** 圆括号 `()` 属性 */
    val asParentheses: TAttributes

    /** 箭头 `->` 属性（用于函数类型）*/
    val asArrow: TAttributes

    /** 方括号 `[]` 属性 */
    val asBrackets: TAttributes

    /** 花括号 `{}` 属性 */
    val asBraces: TAttributes

    /** 操作符属性（如 `=`、`+`、`-` 等）*/
    val asOperationSign: TAttributes

    /** 非空断言属性（`& Any` 标记）*/
    val asNonNullAssertion: TAttributes

    /** 可空标记属性（`?` 符号）*/
    val asNullityMarker: TAttributes

    // 关键字

    /** 通用关键字属性（如 `class`、`func`、`if` 等）*/
    val asKeyword: TAttributes

    /** `let` 关键字属性 */
    val asLet: TAttributes

    /** `mut` 关键字属性 */
    val asMut: TAttributes

    /** `prop` 关键字属性 */
    val asProp: TAttributes

    /** `var` 关键字属性 */
    val asVar: TAttributes

    // 注解

    /** 注解名称属性（如 `@Deprecated`）*/
    val asAnnotationName: TAttributes

    /** 注解属性名称属性 */
    val asAnnotationAttributeName: TAttributes

    // 标识符

    /** 类名属性 */
    val asClassName: TAttributes

    /** 包名属性 */
    val asPackageName: TAttributes

    /** 实例属性名称属性 */
    val asInstanceProperty: TAttributes

    /** 类型别名属性 */
    val asTypeAlias: TAttributes

    /** 参数名称属性 */
    val asParameter: TAttributes

    /** 类型参数名称属性 */
    val asTypeParameterName: TAttributes

    /** 局部变量或常量属性 */
    val asLocalVarOrLet: TAttributes

    /** 函数声明名称属性 */
    val asFunDeclaration: TAttributes

    /** 函数调用名称属性 */
    val asFunCall: TAttributes


    /**
     * 伴生对象，包含工具方法和默认实现。
     */
    companion object {

        /**
         * 属性标记接口。
         *
         * 所有高亮属性类型都必须实现此接口。
         */
        interface Attributes

        /**
         * 擦除类型参数，返回通用的高亮管理器。
         *
         * 该方法用于类型转换，将特定的属性类型擦除为通用的 [Attributes] 类型。
         *
         * @receiver CangJieIdeDescriptorRendererHighlightingManager<TAttributes>
         * @return 擦除类型后的高亮管理器
         */
        fun <TAttributes : Attributes> CangJieIdeDescriptorRendererHighlightingManager<TAttributes>.eraseTypeParameter():
                CangJieIdeDescriptorRendererHighlightingManager<Attributes> {
            @Suppress("UNCHECKED_CAST")
            return this as CangJieIdeDescriptorRendererHighlightingManager<Attributes>
        }

        /** 空属性实例，用于无高亮场景 */
        private val EMPTY_ATTRIBUTES = object : Attributes {}

        /**
         * 无高亮实现。
         *
         * 该实现不添加任何高亮效果，所有文本都以纯文本形式输出。
         * 主要用于不需要语法高亮的场景（如测试、日志等）。
         *
         * ## 使用场景
         *
         * - 单元测试中验证文本内容
         * - 导出纯文本格式的文档
         * - 命令行工具的输出
         *
         * ## 示例
         *
         * ```kotlin
         * val renderer = CangJieIdeDescriptorRenderer.withOptions {
         *     highlightingManager = NO_HIGHLIGHTING
         * }
         * ```
         */
        val NO_HIGHLIGHTING = object : CangJieIdeDescriptorRendererHighlightingManager<Attributes> {
            override fun StringBuilder.appendHighlighted(value: String, attributes: Attributes) {
                append(value)
            }

            override fun StringBuilder.appendCodeSnippetHighlightedByLexer(codeSnippet: String) {
                append(codeSnippet)
            }

            override val asLtColon = EMPTY_ATTRIBUTES
            override val asError = EMPTY_ATTRIBUTES
            override val asInfo = EMPTY_ATTRIBUTES
            override val asDot = EMPTY_ATTRIBUTES
            override val asComma = EMPTY_ATTRIBUTES
            override val asColon = EMPTY_ATTRIBUTES
            override val asStatic = EMPTY_ATTRIBUTES
            override val asConst = EMPTY_ATTRIBUTES

            override val asDoubleColon = EMPTY_ATTRIBUTES
            override val asParentheses = EMPTY_ATTRIBUTES
            override val asArrow = EMPTY_ATTRIBUTES
            override val asBrackets = EMPTY_ATTRIBUTES
            override val asBraces = EMPTY_ATTRIBUTES
            override val asOperationSign = EMPTY_ATTRIBUTES
            override val asNonNullAssertion = EMPTY_ATTRIBUTES
            override val asNullityMarker = EMPTY_ATTRIBUTES
            override val asKeyword = EMPTY_ATTRIBUTES
            override val asLet = EMPTY_ATTRIBUTES
            override val asVar = EMPTY_ATTRIBUTES
            override val asAnnotationName = EMPTY_ATTRIBUTES
            override val asAnnotationAttributeName = EMPTY_ATTRIBUTES
            override val asClassName = EMPTY_ATTRIBUTES
            override val asPackageName = EMPTY_ATTRIBUTES
            override val asInstanceProperty = EMPTY_ATTRIBUTES
            override val asTypeAlias = EMPTY_ATTRIBUTES
            override val asParameter = EMPTY_ATTRIBUTES
            override val asTypeParameterName = EMPTY_ATTRIBUTES
            override val asLocalVarOrLet = EMPTY_ATTRIBUTES
            override val asFunDeclaration = EMPTY_ATTRIBUTES
            override val asFunCall = EMPTY_ATTRIBUTES

            override val asMut: Attributes = EMPTY_ATTRIBUTES
            override val asProp: Attributes = EMPTY_ATTRIBUTES
        }
    }
}
