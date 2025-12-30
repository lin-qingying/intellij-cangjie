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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

/**
 * 表达式类型检查内部接口
 *
 * 扩展 [ExpressionTypingFacade]，提供表达式类型检查的内部实现方法。
 *
 * ## 核心职责
 *
 * ### 1. in 表达式检查
 * 处理成员检查表达式 (`x in collection`)，包括：
 * - 调用元素和操作符的语义分析
 * - 左操作数和右操作数的类型检查
 * - contains 操作符的重载解析
 *
 * ### 2. 语句类型检查
 * 验证表达式在语句上下文中的使用：
 * - 检查是否符合语句的类型要求
 * - 处理类型强制转换（如 Unit 强制转换）
 * - 报告不当的表达式使用
 *
 * ### 3. 模式匹配中的变量定义
 * 从模式中提取和定义局部变量：
 * - 解析 case 模式中的绑定变量
 * - 处理解构绑定
 * - 将变量添加到词法作用域
 *
 * ### 4. let 表达式检查
 * 处理 let 表达式的特殊语义：
 * - 模式绑定的作用域管理
 * - 绑定变量的类型推导
 * - 数据流分析
 *
 * ### 5. 组件访问
 * 提供对表达式类型检查组件的访问
 *
 * ## 使用场景
 *
 * 此接口主要由表达式类型检查的访问者实现使用，提供核心的类型检查逻辑。
 *
 * ## 实现说明
 *
 * - **包级可见性**: 此接口为包内部接口，仅在类型检查实现中使用
 * - **配合访问者模式**: 通常与 [ExpressionTypingVisitor] 配合使用
 * - **上下文传递**: 所有方法都接收 [ExpressionTypingContext] 参数
 *
 * ## 示例
 *
 * ```kotlin
 * class ExpressionTypingVisitorImpl(
 *     override val facade: ExpressionTypingInternals
 * ) : ExpressionTypingVisitor(facade) {
 *
 *     override fun visitBinaryExpression(expression: CjBinaryExpression, context: ExpressionTypingContext): CangJieTypeInfo {
 *         val operationSign = expression.operationReference
 *         if (operationSign.getReferencedNameElementType() == CjTokens.IN_KEYWORD) {
 *             return facade.checkInExpression(
 *                 expression,
 *                 operationSign,
 *                 expression.left,
 *                 expression.right,
 *                 context
 *             )
 *         }
 *         // ... 其他二元操作符处理
 *     }
 * }
 * ```
 */
interface ExpressionTypingInternals : ExpressionTypingFacade {

    /**
     * 检查 in 表达式
     *
     * 处理成员检查表达式，如 `x in collection` 或 `x !in set`。
     *
     * ## 检查流程
     *
     * 1. 检查左操作数类型
     * 2. 检查右操作数类型（容器）
     * 3. 解析 contains 操作符（可能是操作符重载）
     * 4. 验证类型兼容性
     * 5. 返回 Boolean 类型
     *
     * ## 操作符重载
     *
     * in 表达式会被转换为 contains 调用：
     * ```kotlin
     * x in collection  →  collection.contains(x)
     * x !in set        →  !set.contains(x)
     * ```
     *
     * @param callElement 调用元素（整个 in 表达式）
     * @param operationSign 操作符引用（in 或 !in）
     * @param leftArgument 左操作数（被检查的值）
     * @param right 右操作数（容器表达式），可以为 null
     * @param context 表达式类型检查上下文
     * @return 类型信息，包含 Boolean 类型和数据流信息
     */
    fun checkInExpression(
        callElement: CjElement,
        operationSign: CjSimpleNameExpression,
        leftArgument: ValueArgument,
        right: CjExpression?,
        context: ExpressionTypingContext
    ): CangJieTypeInfo

    /**
     * 检查语句类型
     *
     * 验证表达式在语句上下文中的使用是否合法。
     *
     * ## 检查内容
     *
     * - 表达式是否可以作为语句使用
     * - 是否需要 Unit 强制转换
     * - 未使用的返回值警告
     * - 不可达代码检测
     *
     * ## 语句与表达式的区别
     *
     * 语句不期望返回值（期望类型为 Unit），表达式期望特定类型：
     * ```kotlin
     * fun example() {
     *     42          // 语句：需要检查（未使用的值）
     *     val x = 42  // 表达式：42 的类型被使用
     * }
     * ```
     *
     * @param expression 要检查的表达式
     * @param context 表达式类型检查上下文
     */
    fun checkStatementType(expression: CjExpression, context: ExpressionTypingContext)

    /**
     * 从模式中定义局部变量
     *
     * 解析 case 模式，提取绑定变量并添加到词法作用域。
     *
     * ## 模式类型
     *
     * ### 简单绑定模式
     * ```kotlin
     * match (value) {
     *     case x => // x 绑定到 value
     * }
     * ```
     *
     * ### 解构模式
     * ```kotlin
     * match (pair) {
     *     case (a, b) => // a, b 绑定到 pair 的元素
     * }
     * ```
     *
     * ### 类型模式
     * ```kotlin
     * match (obj) {
     *     case x: String => // x 绑定为 String 类型
     * }
     * ```
     *
     * ## 作用域管理
     *
     * 绑定的变量会被添加到提供的可写作用域中，仅在模式匹配分支内可见。
     *
     * @param writableScope 可写词法作用域，用于存储定义的变量
     * @param casePattern case 模式，包含变量绑定信息
     * @param receiver 接收者值（被匹配的值）
     * @param initializer 初始化表达式（如果有）
     * @param context 表达式类型检查上下文
     */
    fun defineLocalVariablesFromPattern(
        writableScope: LexicalWritableScope,
        casePattern: CjCasePatternElement,
        receiver: ReceiverValue,
        initializer: CjExpression?,
        context: ExpressionTypingContext
    )

    /**
     * 检查 let 表达式
     *
     * 处理 let 表达式的类型检查，包括模式绑定和作用域管理。
     *
     * ## let 表达式语义
     *
     * let 表达式引入新的绑定变量：
     * ```kotlin
     * let x = 42
     * let (a, b) = pair
     * let Some(value) = option
     * ```
     *
     * ## 检查流程
     *
     * 1. 检查初始化表达式类型
     * 2. 解析模式绑定
     * 3. 创建新的作用域
     * 4. 定义绑定变量
     * 5. 更新数据流信息
     *
     * ## 模式匹配失败
     *
     * 某些模式可能在运行时失败（如 `let Some(x) = option`），
     * 需要检查可穷尽性和提供失败处理。
     *
     * @param pattern let 表达式的模式部分
     * @param context 表达式类型检查上下文
     */
    fun checkLetExpression(pattern: CjLetExpression, context: ExpressionTypingContext)

    /**
     * 获取表达式类型检查组件
     *
     * 提供对类型检查所需各种组件的访问：
     * - 内置类型信息
     * - 调用解析器
     * - 数据流分析器
     * - 局部类型检查器
     *
     * @return 表达式类型检查组件
     */
    val components: ExpressionTypingComponents
}
