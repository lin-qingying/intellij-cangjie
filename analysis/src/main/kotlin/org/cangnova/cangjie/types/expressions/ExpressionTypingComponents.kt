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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.extensions.TypeResolutionInterceptor
import org.cangnova.cangjie.resolve.*
import jakarta.inject.Inject
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.PlatformToCangJieClassMapper
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.contracts.EffectSystem
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.CollectionLiteralResolver
import org.cangnova.cangjie.resolve.DescriptorResolver
import org.cangnova.cangjie.resolve.FlowOperatorResolver
import org.cangnova.cangjie.resolve.FunctionDescriptorResolver
import org.cangnova.cangjie.resolve.FunctionReturnResolver
import org.cangnova.cangjie.resolve.IdentifierChecker
import org.cangnova.cangjie.resolve.LocalVariableResolver
import org.cangnova.cangjie.resolve.MissingSupertypesResolver
import org.cangnova.cangjie.resolve.ModifiersChecker
import org.cangnova.cangjie.resolve.OverloadChecker
import org.cangnova.cangjie.resolve.RangeLiteralResolver
import org.cangnova.cangjie.resolve.SpawnExpressionResolver
import org.cangnova.cangjie.resolve.SyncExpressionResolver
import org.cangnova.cangjie.resolve.TypeResolver
import org.cangnova.cangjie.resolve.UnsafeExpressionResolver
import org.cangnova.cangjie.resolve.VArrayResolver
import org.cangnova.cangjie.resolve.calls.CallExpressionResolver
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.checkers.AssignmentChecker
import org.cangnova.cangjie.resolve.calls.checkers.CallChecker
import org.cangnova.cangjie.resolve.calls.checkers.RttiExpressionChecker
import org.cangnova.cangjie.resolve.calls.model.CangJieCallComponents
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

import org.cangnova.cangjie.types.expressions.match.PatternMatchingTypingVisitor

/**
 * 表达式类型检查组件集合
 *
 * 聚合表达式类型检查所需的所有组件和解析器，通过依赖注入进行初始化。
 *
 * ## 组件分类
 *
 * ### 核心类型系统组件
 * - [builtIns] - 内置类型（Int、String、Boolean 等）
 * - [cangjieTypeChecker] - 类型检查器，执行子类型判断
 * - [dataFlowValueFactory] - 数据流值工厂，创建数据流分析所需的值
 * - [dataFlowAnalyzer] - 数据流分析器，跟踪类型信息在控制流中的变化
 *
 * ### 表达式解析器
 * - [callExpressionResolver] - 调用表达式解析器
 * - [constantExpressionEvaluator] - 常量表达式求值器
 * - [collectionLiteralResolver] - 集合字面量解析器
 * - [rangeLiteralResolver] - 范围字面量解析器
 * - [vArrayResolver] - 可变数组解析器
 * - [spawnExpressionResolver] - spawn 表达式解析器
 * - [unsafeExpressionResolver] - unsafe 表达式解析器
 * - [syncExpressionResolver] - sync 表达式解析器
 * - [flowOperatorResolver] - 流操作符解析器
 *
 * ### 声明解析器
 * - [descriptorResolver] - 描述符解析器
 * - [functionDescriptorResolver] - 函数描述符解析器
 * - [localVariableResolver] - 局部变量解析器
 * - [valueParameterResolver] - 值参数解析器
 * - [destructuringDeclarationResolver] - 解构声明解析器
 * - [missingSupertypesResolver] - 缺失父类型解析器
 *
 * ### 调用和重载解析
 * - [callResolver] - 调用解析器，处理函数和操作符调用
 * - [callComponents] - 调用组件，提供调用解析所需的工具
 * - [overloadChecker] - 重载检查器，验证重载合法性
 *
 * ### 检查器
 * - [callCheckers] - 调用检查器集合（参数检查、类型检查等）
 * - [assignmentCheckers] - 赋值检查器集合（常量赋值、类型兼容性等）
 * - [rttiExpressionCheckers] - RTTI 表达式检查器集合（is、as 等）
 * - [modifiersChecker] - 修饰符检查器
 * - [forLoopConventionsChecker] - for 循环约定检查器
 * - [identifierChecker] - 标识符检查器
 *
 * ### 其他组件
 * - [typeResolver] - 类型解析器，将类型引用解析为类型描述符
 * - [typeResolutionInterceptor] - 类型解析拦截器，自定义类型解析逻辑
 * - [functionReturnResolver] - 函数返回值解析器
 * - [controlStructureTypingUtils] - 控制结构类型工具
 * - [patternMatchingTypingVisitor] - 模式匹配类型访问者
 * - [deprecationResolver] - 废弃标记解析器
 * - [effectSystem] - 效应系统，处理副作用和契约
 * - [platformToCangJieClassMapper] - 平台到仓颉类映射器
 *
 * ### 配置和上下文
 * - [languageVersionSettings] - 语言版本设置
 * - [moduleDescriptor] - 模块描述符
 * - [expressionTypingServices] - 表达式类型检查服务
 *
 * ## 依赖注入
 *
 * 此类使用 Jakarta Inject (JSR-330) 进行依赖注入。所有字段通过 setter 方法注入：
 *
 * ```kotlin
 * val container = // ... 创建 DI 容器
 * val components = container.getInstance(ExpressionTypingComponents::class.java)
 *
 * // 所有组件已自动注入
 * val intType = components.builtIns.intType
 * ```
 *
 * ## 使用示例
 *
 * ### 在表达式访问者中使用
 *
 * ```kotlin
 * class MyExpressionVisitor(
 *     facade: ExpressionTypingInternals
 * ) : ExpressionTypingVisitor(facade) {
 *
 *     override fun visitCallExpression(
 *         expression: CjCallExpression,
 *         context: ExpressionTypingContext
 *     ): CangJieTypeInfo {
 *         // 使用调用解析器
 *         val resolvedCall = components.callResolver.resolveCall(
 *             expression,
 *             context
 *         )
 *
 *         // 使用类型检查器
 *         val returnType = resolvedCall.resultingDescriptor.returnType
 *         if (components.cangjieTypeChecker.isSubtypeOf(returnType, context.expectedType)) {
 *             // 类型匹配
 *         }
 *
 *         return CangJieTypeInfo(returnType, context.dataFlowInfo)
 *     }
 * }
 * ```
 *
 * ### 常量求值
 *
 * ```kotlin
 * val constantValue = components.constantExpressionEvaluator.evaluateToConstantValue(
 *     expression,
 *     components.builtIns,
 *     context.trace,
 *     context.expectedType
 * )
 * ```
 *
 * ### 数据流分析
 *
 * ```kotlin
 * val updatedDataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(
 *     condition,
 *     true, // whenTrue
 *     context
 * )
 * ```
 *
 * ## 设计说明
 *
 * ### Setter 注入 vs 构造器注入
 *
 * 此类使用 setter 注入而非构造器注入的原因：
 * - 组件数量众多（40+ 个依赖）
 * - 避免构造器参数过多
 * - 支持可选依赖和延迟初始化
 * - 更好的循环依赖处理
 *
 * ### lateinit var
 *
 * 所有字段使用 `lateinit var` 修饰：
 * - 延迟初始化，由依赖注入框架负责
 * - 避免使用可空类型 `var field: Type?`
 * - 访问未初始化字段会抛出清晰的异常
 *
 * @see ExpressionTypingVisitor
 * @see ExpressionTypingInternals
 * @see ExpressionTypingContext
 */
class ExpressionTypingComponents {

    // ========== 核心类型系统组件 ==========

    /** 内置类型信息（Int、String、Boolean 等） */
    @set:Inject
    lateinit var builtIns: CangJieBuiltIns

    /** 类型检查器，执行子类型判断和类型等价性检查 */
    @set:Inject
    lateinit var cangjieTypeChecker: CangJieTypeChecker

    /** 数据流值工厂，创建数据流分析所需的值 */
    @set:Inject
    lateinit var dataFlowValueFactory: DataFlowValueFactory

    /** 数据流分析器，跟踪类型信息在控制流中的变化 */
    @set:Inject
    lateinit var dataFlowAnalyzer: DataFlowAnalyzer

    // ========== 表达式解析器 ==========

    /** 调用表达式解析器，处理函数调用表达式 */
    @set:Inject
    lateinit var callExpressionResolver: CallExpressionResolver

    /** 常量表达式求值器，编译期计算常量值 */
    @set:Inject
    lateinit var constantExpressionEvaluator: ConstantExpressionEvaluator

    /** 集合字面量解析器，处理 [1, 2, 3] 等字面量 */
    @set:Inject
    lateinit var collectionLiteralResolver: CollectionLiteralResolver

    /** 范围字面量解析器，处理 1..10、'a'..'z' 等范围表达式 */
    @set:Inject
    lateinit var rangeLiteralResolver: RangeLiteralResolver

    /** 可变数组解析器，处理可变数组字面量 */
    @set:Inject
    lateinit var vArrayResolver: VArrayResolver

    /** spawn 表达式解析器，处理并发创建表达式 */
    @set:Inject
    lateinit var spawnExpressionResolver: SpawnExpressionResolver

    /** unsafe 表达式解析器，处理 unsafe 块 */
    @set:Inject
    lateinit var unsafeExpressionResolver: UnsafeExpressionResolver

    /** sync 表达式解析器，处理同步块 */
    @set:Inject
    lateinit var syncExpressionResolver: SyncExpressionResolver

    /** 流操作符解析器，处理 yield、await 等流操作 */
    @set:Inject
    lateinit var flowOperatorResolver: FlowOperatorResolver

    // ========== 声明解析器 ==========

    /** 描述符解析器，将声明解析为描述符 */
    @set:Inject
    lateinit var descriptorResolver: DescriptorResolver

    /** 函数描述符解析器，专门处理函数声明 */
    @set:Inject
    lateinit var functionDescriptorResolver: FunctionDescriptorResolver

    /** 局部变量解析器，处理局部变量声明 */
    @set:Inject
    lateinit var localVariableResolver: LocalVariableResolver

    /** 值参数解析器，处理函数参数 */
    @set:Inject
    lateinit var valueParameterResolver: ValueParameterResolver

    /** 解构声明解析器，处理 let (a, b) = pair */
    @set:Inject
    lateinit var destructuringDeclarationResolver: DestructuringDeclarationResolver

    /** 缺失父类型解析器，处理父类型缺失情况 */
    @set:Inject
    lateinit var missingSupertypesResolver: MissingSupertypesResolver

    // ========== 调用和重载解析 ==========

    /** 调用解析器，处理函数调用和操作符重载 */
    @set:Inject
    lateinit var callResolver: CallResolver

    /** 调用组件，提供调用解析所需的工具和配置 */
    @set:Inject
    lateinit var callComponents: CangJieCallComponents

    /** 重载检查器，验证函数重载的合法性 */
    @set:Inject
    lateinit var overloadChecker: OverloadChecker

    // ========== 检查器 ==========

    /** 调用检查器集合，检查调用的各个方面（参数、类型、契约等） */
    @set:Inject
    lateinit var callCheckers: Iterable<CallChecker>

    /** 赋值检查器集合，检查赋值操作的合法性 */
    @set:Inject
    lateinit var assignmentCheckers: Iterable<AssignmentChecker>

    /** RTTI 表达式检查器集合，检查 is、as 等类型判断表达式 */
    @set:Inject
    lateinit var rttiExpressionCheckers: Iterable<RttiExpressionChecker>

    /** 修饰符检查器，验证修饰符的正确使用 */
    @set:Inject
    lateinit var modifiersChecker: ModifiersChecker

    /** for 循环约定检查器，验证 for 循环的迭代器约定 */
    @set:Inject
    lateinit var forLoopConventionsChecker: ForLoopConventionsChecker

    /** 标识符检查器，检查标识符命名规范 */
    @set:Inject
    lateinit var identifierChecker: IdentifierChecker

    // ========== 其他组件 ==========

    /** 类型解析器，将类型引用解析为类型描述符 */
    @set:Inject
    lateinit var typeResolver: TypeResolver

    /** 类型解析拦截器，自定义类型解析逻辑 */
    @set:Inject
    lateinit var typeResolutionInterceptor: TypeResolutionInterceptor

    /** 函数返回值解析器，处理函数返回类型推导 */
    @set:Inject
    lateinit var functionReturnResolver: FunctionReturnResolver

    /** 控制结构类型工具，处理 if、when、loop 等控制结构 */
    @set:Inject
    lateinit var controlStructureTypingUtils: ControlStructureTypingUtils

    /** 模式匹配类型访问者，处理模式匹配表达式 */
    @set:Inject
    lateinit var patternMatchingTypingVisitor: PatternMatchingTypingVisitor

    /** 废弃标记解析器，处理 @Deprecated 注解 */
    @set:Inject
    lateinit var deprecationResolver: DeprecationResolver

    /** 效应系统，处理副作用和契约 */
    @set:Inject
    lateinit var effectSystem: EffectSystem

    /** 平台到仓颉类映射器，处理平台特定类型映射 */
    @set:Inject
    lateinit var platformToCangJieClassMapper: PlatformToCangJieClassMapper

    // ========== 配置和上下文 ==========

    /** 语言版本设置，控制语言特性的启用/禁用 */
    @set:Inject
    lateinit var languageVersionSettings: LanguageVersionSettings

    /** 模块描述符，表示当前编译的模块 */
    @set:Inject
    lateinit var moduleDescriptor: ModuleDescriptor

    /** 表达式类型检查服务，提供高层的类型检查接口 */
    @set:Inject
    lateinit var expressionTypingServices: ExpressionTypingServices
}
