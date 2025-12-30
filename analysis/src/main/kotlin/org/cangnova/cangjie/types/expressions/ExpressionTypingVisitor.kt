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

import org.cangnova.cangjie.psi.CjVisitor

/**
 * 表达式类型检查访问者基类
 *
 * 扩展 [CjVisitor] 以支持表达式类型检查，遍历 AST 并为每个表达式计算类型信息。
 *
 * ## 访问者模式
 *
 * 此类实现了访问者模式，用于遍历 Cangjie 语言的抽象语法树（AST）：
 * - **访问目标**: [CjExpression] 及其所有子类
 * - **返回类型**: [CangJieTypeInfo] - 包含类型和数据流信息
 * - **上下文参数**: [ExpressionTypingContext] - 提供类型检查所需的上下文
 *
 * ## 核心组件
 *
 * ### facade
 * 类型检查的内部接口，提供核心类型检查方法：
 * - in 表达式检查
 * - 语句类型检查
 * - 模式匹配变量定义
 * - let 表达式检查
 *
 * ### components
 * 表达式类型检查所需的各种组件：
 * - 内置类型信息
 * - 调用解析器
 * - 数据流分析器
 * - 局部类型检查器
 * - 可变集合包装器
 *
 * ## 使用示例
 *
 * ### 实现具体访问者
 *
 * ```kotlin
 * class MyExpressionTypingVisitor(
 *     facade: ExpressionTypingInternals
 * ) : ExpressionTypingVisitor(facade) {
 *
 *     override fun visitBinaryExpression(
 *         expression: CjBinaryExpression,
 *         context: ExpressionTypingContext
 *     ): CangJieTypeInfo {
 *         val left = expression.left?.accept(this, context)
 *         val right = expression.right?.accept(this, context)
 *
 *         // 使用 components 进行类型检查
 *         val resultType = components.callResolver.resolveOperatorCall(
 *             expression.operationReference,
 *             left?.type,
 *             right?.type,
 *             context
 *         )
 *
 *         return CangJieTypeInfo(resultType, context.dataFlowInfo)
 *     }
 * }
 * ```
 *
 * ### 使用 facade 进行特殊检查
 *
 * ```kotlin
 * override fun visitInExpression(
 *     expression: CjBinaryExpression,
 *     context: ExpressionTypingContext
 * ): CangJieTypeInfo {
 *     // 委托给 facade 处理复杂的 in 表达式逻辑
 *     return facade.checkInExpression(
 *         expression,
 *         expression.operationReference,
 *         expression.left,
 *         expression.right,
 *         context
 *     )
 * }
 * ```
 *
 * ## 设计模式
 *
 * ### 访问者模式
 * - 分离数据结构（AST）和操作（类型检查）
 * - 通过重写 visit 方法实现不同表达式的类型检查逻辑
 *
 * ### 门面模式
 * - [facade] 提供简化的接口访问复杂的类型检查逻辑
 * - 隐藏类型检查的实现细节
 *
 * ### 策略模式
 * - 不同的访问者子类可以实现不同的类型检查策略
 * - 如：严格模式 vs 宽松模式、不同的类型推导策略
 *
 * ## 数据流传递
 *
 * 类型检查过程中需要正确传递数据流信息：
 * ```kotlin
 * override fun visitIfExpression(
 *     expression: CjIfExpression,
 *     context: ExpressionTypingContext
 * ): CangJieTypeInfo {
 *     val condition = expression.condition?.accept(this, context)
 *
 *     // 在 then 分支中使用更新的数据流信息
 *     val thenInfo = condition?.let { conditionInfo ->
 *         val thenContext = context.replaceDataFlowInfo(
 *             conditionInfo.dataFlowInfo.extractDefinitelyNotNull()
 *         )
 *         expression.then?.accept(this, thenContext)
 *     }
 *
 *     // 合并分支的数据流信息
 *     // ...
 * }
 * ```
 *
 * ## 子类实现指南
 *
 * 1. **重写 visit 方法**: 为需要特殊处理的表达式类型重写对应的 visit 方法
 * 2. **使用 components**: 访问类型检查所需的工具和服务
 * 3. **调用 facade**: 对于复杂的类型检查逻辑，委托给 facade 处理
 * 4. **传递上下文**: 正确传递和更新 [ExpressionTypingContext]
 * 5. **返回类型信息**: 每个 visit 方法必须返回 [CangJieTypeInfo]
 *
 * @param facade 表达式类型检查内部接口，提供核心类型检查方法
 *
 * @see CjVisitor
 * @see ExpressionTypingContext
 * @see CangJieTypeInfo
 * @see ExpressionTypingInternals
 * @see ExpressionTypingComponents
 */
abstract class ExpressionTypingVisitor(
    /**
     * 表达式类型检查的内部接口
     *
     * 提供核心的类型检查方法，如 in 表达式检查、语句类型检查等。
     * 子类可以通过此字段调用复杂的类型检查逻辑。
     */
    protected val facade: ExpressionTypingInternals
) : CjVisitor<CangJieTypeInfo, ExpressionTypingContext>() {

    /**
     * 表达式类型检查组件
     *
     * 提供类型检查所需的各种组件和工具：
     * - **builtIns**: 内置类型信息（Int、String、Boolean 等）
     * - **callResolver**: 调用解析器，用于解析函数调用和操作符重载
     * - **dataFlowAnalyzer**: 数据流分析器，跟踪类型信息在控制流中的变化
     * - **localClassifierAnalyzer**: 局部分类器分析器，处理局部类和匿名对象
     * - **mutableDataFlowFactory**: 可变数据流工厂，创建可修改的数据流信息
     *
     * ## 使用示例
     *
     * ```kotlin
     * // 访问内置类型
     * val intType = components.builtIns.intType
     *
     * // 解析调用
     * val resolvedCall = components.callResolver.resolveCall(
     *     callExpression,
     *     context
     * )
     *
     * // 执行数据流分析
     * val updatedDataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfo(
     *     condition,
     *     context
     * )
     * ```
     */
    protected val components: ExpressionTypingComponents = facade.components
}
