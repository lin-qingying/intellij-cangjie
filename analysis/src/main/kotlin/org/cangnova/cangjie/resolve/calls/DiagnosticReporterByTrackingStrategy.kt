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

package org.cangnova.cangjie.resolve.calls

import org.cangnova.cangjie.types.contains

import org.cangnova.cangjie.builtins.UnsignedTypes
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.DiagnosticFactory2
import org.cangnova.cangjie.diagnostics.reportDiagnosticOnce
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.lastBlockStatementOrThis
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.smartcasts.SingleSmartCast
import org.cangnova.cangjie.resolve.calls.smartcasts.SmartCastManager
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.tower.*
import org.cangnova.cangjie.resolve.calls.util.extractCallableReferenceExpression
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.calls.util.reportTrailingLambdaErrorOr
import org.cangnova.cangjie.resolve.constants.CompileTimeConstantChecker
import org.cangnova.cangjie.resolve.constants.TypedCompileTimeConstant
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.controlFlow.multiParentElementReports
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import org.cangnova.cangjie.types.checker.intersectWrappedTypes
import org.cangnova.cangjie.types.expressions.ControlStructureTypingUtils
import org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate
import org.cangnova.cangjie.types.model.TypeVariableMarker
import org.cangnova.cangjie.types.model.freshTypeConstructor
import org.cangnova.cangjie.utils.shouldNotBeCalled
import io.github.classgraph.TypeArgument
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.*
import org.cangnova.cangjie.diagnostics.reportUnresolvedReference
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.makeOption
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 基于跟踪策略的诊断报告器
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 本类是仓颉语言调用解析系统中的核心组件，负责将类型推断和调用解析过程中
 * 产生的各种诊断信息（错误、警告）报告给用户。
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 一、整体架构                                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 在调用解析流程中，诊断报告器处于最后阶段：
 *
 * ```
 * 源代码 → 词法分析 → 语法分析 → PSI 构建 → 调用解析 → 类型推断 → 诊断报告
 *                                                              ↑
 *                                                    DiagnosticReporterByTrackingStrategy
 * ```
 *
 * 调用解析系统产生的诊断信息通过 [DiagnosticReporter] 接口传递给本类，
 * 本类根据诊断类型和上下文，将其转换为 IDE 可以显示的错误/警告信息。
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 二、诊断分类                                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 本类处理的诊断信息按照发生位置分为以下几类：
 *
 * 1. **调用级别诊断** ([onCall])
 *    - 缺少调用操作符 (NoCallOperatorFunction)
 *    - 静态/实例成员访问错误
 *    - 可见性错误
 *    - 缺少参数值
 *
 * 2. **类型参数诊断** ([onTypeArguments])
 *    - 类型参数数量错误 (WrongCountOfTypeArguments)
 *    - 枚举项后不允许类型参数
 *    - 无法推断泛型参数
 *
 * 3. **接收者诊断** ([onCallReceiver])
 *    - 不安全的调用（可空类型调用非空方法）
 *
 * 4. **参数诊断** ([onCallArgument])
 *    - 智能转换相关
 *    - 参数数量过多
 *    - 可调用引用解析错误
 *    - Lambda 参数类型推断失败
 *
 * 5. **命名参数诊断** ([onCallArgumentName])
 *    - 参数名未找到
 *    - 参数名歧义
 *    - 参数重复传递
 *
 * 6. **约束系统错误** ([constraintError])
 *    - 类型不匹配
 *    - 类型参数信息不足
 *    - 空交集类型推断
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 三、关键概念                                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ## 3.1 跟踪策略 (TracingStrategy)
 *
 * [TracingStrategy] 是一个辅助接口，提供了一些通用的错误报告方法。
 * 它封装了如何定位错误位置和如何格式化错误消息的逻辑。
 *
 * ## 3.2 绑定跟踪 (TrackingBindingTrace)
 *
 * [TrackingBindingTrace] 用于记录诊断信息，同时跟踪哪些错误已经报告过，
 * 避免重复报告相同的错误。
 *
 * ## 3.3 智能转换 (Smart Cast)
 *
 * 智能转换是指编译器根据控制流分析自动推断更精确的类型。例如：
 * ```cangjie
 * var x: Any = "hello"
 * if (x is String) {
 *     // 这里 x 被智能转换为 String 类型
 *     print(x.length)  // 可以访问 String 的成员
 * }
 * ```
 *
 * ## 3.4 约束系统错误 (ConstraintSystemError)
 *
 * 在类型推断过程中，约束系统收集类型变量的约束，如果约束冲突，
 * 就会产生约束系统错误。常见的错误包括：
 * - 类型不匹配 (ConstraintMismatch)
 * - 类型参数信息不足 (NotEnoughInformationForTypeParameter)
 * - 捕获类型逃逸 (CapturedTypeFromSubtyping)
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 四、错误定位策略                                                              │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 错误定位的原则是：**尽可能在用户需要修改的位置报告错误**。
 *
 * 例如，对于 `container.method<String>()` 调用：
 * - 如果 `method` 找不到，错误应该标记在 `method` 上
 * - 如果 `String` 类型参数不匹配，错误应该标记在 `<String>` 上
 * - 如果 `container` 的类型参数无法推断，错误应该标记在 `container` 上
 *
 * ## 4.1 类类型参数的特殊处理
 *
 * 对于泛型类的静态方法调用，如 `GenericClass.staticMethod<R>()`：
 * - 如果 `R` 是方法的类型参数，错误标记在 `staticMethod<R>` 上
 * - 如果无法推断的是 `GenericClass<T>` 中的 `T`，错误应该标记在 `GenericClass` 上
 *
 * 这是因为用户需要修改的位置是 `GenericClass` 而不是 `staticMethod<R>`。
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 五、与其他组件的交互                                                          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ```
 *   ┌────────────────────┐
 *   │  CangJieCallResolver │ ─── 调用解析器，产生诊断信息
 *   └────────────────────┘
 *            ↓
 *   ┌────────────────────┐
 *   │  CangJieCallCompleter │ ─── 调用补全器，完成类型推断
 *   └────────────────────┘
 *            ↓
 *   ┌────────────────────────────────────┐
 *   │  DiagnosticReporterByTrackingStrategy │ ─── 本类，报告诊断
 *   └────────────────────────────────────┘
 *            ↓
 *   ┌────────────────────┐
 *   │  TrackingBindingTrace │ ─── 记录诊断到绑定上下文
 *   └────────────────────┘
 *            ↓
 *   ┌────────────────────┐
 *   │  IDE 错误高亮       │ ─── 在编辑器中显示错误
 *   └────────────────────┘
 * ```
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 六、使用示例                                                                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ## 6.1 类型不匹配错误
 *
 * ```cangjie
 * func foo(x: Int) {}
 * foo("hello")  // 错误：类型不匹配，期望 Int，实际 String
 *     ^^^^^^^
 * ```
 *
 * ## 6.2 类型参数推断失败
 *
 * ```cangjie
 * class Container<T> {
 *     static func create<R>(): Container<T> { ... }
 * }
 * let x = Container.create<String>()  // 错误：无法推断 T
 *         ^^^^^^^^^
 * ```
 *
 * ## 6.3 智能转换不可能
 *
 * ```cangjie
 * var x: Any = "hello"
 * // x 可能被重新赋值，无法智能转换
 * if (x is String) {
 *     print(x.length)  // 警告：智能转换不可能，因为 x 是可变变量
 *           ^
 * }
 * ```
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * @property constantExpressionEvaluator 常量表达式求值器，用于检查常量类型错误
 * @property context 基本调用解析上下文，包含作用域、数据流等信息
 * @property psiCangJieCall PSI 仓颉调用对象，包含调用的 PSI 元素
 * @property dataFlowValueFactory 数据流值工厂，用于创建智能转换所需的数据流值
 * @property allDiagnostics 所有诊断信息列表，用于检查是否有更精确的诊断
 * @property smartCastManager 智能转换管理器，处理智能转换相关的记录和检查
 * @property typeSystemContext 类型系统上下文，提供类型系统相关的操作
 *
 * @see DiagnosticReporter 诊断报告器接口
 * @see TracingStrategy 跟踪策略接口
 * @see TrackingBindingTrace 跟踪绑定追踪
 * @see ConstraintSystemError 约束系统错误
 */
class DiagnosticReporterByTrackingStrategy(
    val constantExpressionEvaluator: ConstantExpressionEvaluator,
    val context: BasicCallResolutionContext,
    val psiCangJieCall: PSICangJieCall,
    val dataFlowValueFactory: DataFlowValueFactory,
    private val allDiagnostics: List<CangJieCallDiagnostic>,
    private val smartCastManager: SmartCastManager,
    private val typeSystemContext: TypeSystemInferenceExtensionContextDelegate
) : DiagnosticReporter {

    // ═══════════════════════════════════════════════════════════════════════════
    // 私有属性
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 跟踪绑定追踪器
     *
     * 用于记录诊断信息到绑定上下文中，同时跟踪哪些诊断已经报告过。
     * 通过强制类型转换确保 context.trace 是 [TrackingBindingTrace] 类型。
     */
    private val trace = context.trace as TrackingBindingTrace

    /**
     * 跟踪策略
     *
     * 提供通用的错误报告方法，封装了错误定位和消息格式化的逻辑。
     * 从 [PSICangJieCall] 中获取。
     */
    private val tracingStrategy: TracingStrategy get() = psiCangJieCall.tracingStrategy

    /**
     * 原始调用对象
     *
     * 从 [PSICangJieCall] 中提取的 [Call] 对象，包含调用的 PSI 元素。
     */
    private val call: Call get() = psiCangJieCall.psiCall

    /**
     * 是否报告额外的错误信息
     *
     * 默认为 true，表示报告所有可能的错误信息。
     * 在某些情况下（如仅收集所有候选项时），可能需要禁用额外错误报告。
     */
    private val reportAdditionalErrors: Boolean
        get() = true

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 显式接收者诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理显式接收者相关的诊断
     *
     * 显式接收者是指在调用中明确指定的接收者表达式，例如 `receiver.method()` 中的 `receiver`。
     *
     * 当前实现为空，因为大多数显式接收者的错误通过其他方法（如 [onCallReceiver]）处理。
     *
     * @param diagnostic 诊断信息
     */
    override fun onExplicitReceiver(diagnostic: CangJieCallDiagnostic) {
        // 当前不需要处理，大多数显式接收者错误通过其他方法处理
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 调用级别诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理调用级别的诊断信息
     *
     * 调用级别的诊断是指与整个调用表达式相关的错误，而不是特定参数或类型参数的错误。
     *
     * ## 处理的诊断类型
     *
     * ### NoCallOperatorFunction
     * 当尝试调用一个没有定义 `operator()` 的对象时报告此错误。
     * ```cangjie
     * class Foo {}
     * let f = Foo()
     * f()  // 错误：Foo 没有定义 operator()
     * ```
     *
     * ### StaticContextAccessNonStaticMemberDiagnostic
     * 在静态上下文中访问非静态成员时报告此错误。
     * ```cangjie
     * class Foo {
     *     var x: Int = 0
     *     static func bar() {
     *         print(x)  // 错误：不能在静态方法中访问实例成员
     *     }
     * }
     * ```
     *
     * ### NonStaticContextAccessStaticMemberDiagnostic
     * 通过实例访问静态成员时报告此错误（在某些情况下是警告）。
     * ```cangjie
     * class Foo {
     *     static var x: Int = 0
     * }
     * let f = Foo()
     * f.x  // 错误/警告：应该使用 Foo.x 而不是 f.x
     * ```
     *
     * ### VisibilityError
     * 访问不可见成员时报告此错误。
     * ```cangjie
     * class Foo {
     *     private func bar() {}
     * }
     * let f = Foo()
     * f.bar()  // 错误：bar 是私有的
     * ```
     *
     * ### NoValueForParameter
     * 调用时缺少必需参数时报告此错误。
     * ```cangjie
     * func foo(x: Int, y: String) {}
     * foo(1)  // 错误：缺少参数 y
     * ```
     *
     * @param diagnostic 调用级别的诊断信息
     */
    override fun onCall(diagnostic: CangJieCallDiagnostic) {
        when (diagnostic) {
            // ─────────────────────────────────────────────────────────────────
            // 调用操作符缺失
            // ─────────────────────────────────────────────────────────────────
            is NoCallOperatorFunction -> {
                trace.report(
                    NO_CALL_OPERATOR.on(
                        psiCangJieCall.psiCall.callElement as CjCallExpression,
                        diagnostic.descriptor
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 静态上下文访问非静态成员
            // ─────────────────────────────────────────────────────────────────
            is StaticContextAccessNonStaticMemberDiagnostic -> {
                trace.report(
                    STATIC_CONTEXT_REFERENCE_ERROR.on(
                        psiCangJieCall.psiCall.callElement,
                        diagnostic.kind,
                        diagnostic.descriptor
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 实例上下文访问静态成员
            // ─────────────────────────────────────────────────────────────────
            is NonStaticContextAccessStaticMemberDiagnostic -> {
                trace.report(
                    INSTANCE_ACCESS_STATIC_MEMBER_ERROR.on(
                        psiCangJieCall.psiCall.callElement,
                        diagnostic.kind,
                        diagnostic.descriptor
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 可见性错误：访问了不可见的成员
            // ─────────────────────────────────────────────────────────────────
            is VisibilityError -> tracingStrategy.invisibleMember(trace, diagnostic.invisibleMember)

            // ─────────────────────────────────────────────────────────────────
            // 缺少参数值：调用时没有提供必需的参数
            // ─────────────────────────────────────────────────────────────────
            is NoValueForParameter -> tracingStrategy.noValueForParameter(trace, diagnostic.parameterDescriptor)

            // ─────────────────────────────────────────────────────────────────
            // 未知诊断类型：报告通用错误
            // ─────────────────────────────────────────────────────────────────
            else -> {
                unknownError(diagnostic, "onCall")
            }
        }
    }

    /**
     * 报告未知类型的诊断错误
     *
     * 当遇到未处理的诊断类型时，调用此方法报告通用错误。
     * 这有助于在开发过程中发现遗漏的诊断类型处理。
     *
     * @param diagnostic 未知的诊断信息
     * @param onTarget 诊断发生的目标位置描述（如 "onCall", "onCallArgument" 等）
     */
    private fun unknownError(diagnostic: CangJieCallDiagnostic, onTarget: String) {
        // 在生产环境中不抛出异常，而是报告一个通用错误
        // RUN_SLOW_ASSERTIONS 已移除 - 在不变类型系统中不需要
        if (false) {
            throw AssertionError("$onTarget should not be called with ${diagnostic::class.java}")
        } else if (reportAdditionalErrors) {
            trace.report(
                NEW_INFERENCE_UNKNOWN_ERROR.on(
                    psiCangJieCall.psiCall.callElement,
                    diagnostic.candidateApplicability,
                    onTarget
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 类型参数诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理类型参数相关的诊断信息
     *
     * 类型参数诊断是指与泛型调用中的类型参数相关的错误。
     *
     * ## 处理的诊断类型
     *
     * ### TypeArgumentsCompilerError
     * 编译器无法推断泛型参数时报告此错误。
     * ```cangjie
     * func foo<T>(): T { ... }
     * let x = foo()  // 错误：无法推断 T
     * ```
     *
     * ### TypeArgumentsAfterEnumEntry
     * 在枚举项后指定类型参数时报告此错误。
     * 在仓颉语言中，枚举项的类型参数应该在枚举类型上指定，而不是枚举项上。
     * ```cangjie
     * enum Option<T> {
     *     Some(T),
     *     None
     * }
     * let x = Option.Some<Int>(1)  // 错误：类型参数应在 Option 上指定
     * ```
     *
     * ### WrongCountOfTypeArguments
     * 类型参数数量错误时报告此错误。
     * ```cangjie
     * class Pair<A, B> {}
     * let p: Pair<Int> = ...  // 错误：期望 2 个类型参数，实际 1 个
     * ```
     *
     * @param diagnostic 类型参数相关的诊断信息
     */
    override fun onTypeArguments(diagnostic: CangJieCallDiagnostic) {
        // 确定错误报告的 PSI 元素位置
        val psiCallElement = psiCangJieCall.psiCall.callElement
        val reportElement =
            if (psiCallElement is CjCallExpression)
            // 优先在类型参数列表上报告，其次是被调用表达式，最后是整个调用元素
                psiCallElement.typeArgumentList ?: psiCallElement.calleeExpression ?: psiCallElement
            else
                psiCallElement

        when (diagnostic) {
            // ─────────────────────────────────────────────────────────────────
            // 编译器无法推断泛型参数
            // ─────────────────────────────────────────────────────────────────
            is TypeArgumentsCompilerError -> {
                trace.report(
                    COMPILER_AFFECTED_SYNTAX_ERROR_BY_MESSAGE.on(
                        reportElement,
                        "unable to infer generic argument of this function"
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 枚举项后不允许类型参数
            // ─────────────────────────────────────────────────────────────────
            is TypeArgumentsAfterEnumEntry -> {
                trace.report(
                    TYPE_ARGUMENTS_NOT_AFTER_ENUMENTRY.on(
                        reportElement,
                        diagnostic.enumEntry,
                        diagnostic.enum
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 类型参数数量错误
            // ─────────────────────────────────────────────────────────────────
            is WrongCountOfTypeArguments -> {
                val expectedTypeArgumentsCount = diagnostic.descriptor.typeParameters.size
                trace.report(
                    WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                        reportElement,
                        expectedTypeArgumentsCount,
                        diagnostic.descriptor
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 未知诊断类型
            // ─────────────────────────────────────────────────────────────────
            else -> {
                unknownError(diagnostic, "onTypeArguments")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 调用名称和单个类型参数诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理调用名称相关的诊断
     *
     * 当前实现为空，因为大多数调用名称相关的错误（如未解析的引用）
     * 通过其他机制处理。
     *
     * @param diagnostic 调用名称相关的诊断信息
     */
    override fun onCallName(diagnostic: CangJieCallDiagnostic) {
        // 当前不需要处理
    }

    /**
     * 处理单个类型参数相关的诊断
     *
     * 当前实现为空，因为大多数单个类型参数的错误（如上界违反）
     * 通过 [constraintError] 方法处理。
     *
     * @param typeArgument 类型参数
     * @param diagnostic 诊断信息
     */
    override fun onTypeArgument(typeArgument: TypeArgument, diagnostic: CangJieCallDiagnostic) {
        // 当前不需要处理
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 调用接收者诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理调用接收者相关的诊断信息
     *
     * 调用接收者是指方法调用中的对象，例如 `receiver.method()` 中的 `receiver`。
     *
     * ## 处理的诊断类型
     *
     * ### UnsafeCallError
     * 在可空类型上调用非空方法时报告此错误。
     *
     * **注意**：仓颉语言没有可空类型（使用 Option<T> 枚举代替），
     * 但此诊断仍然处理隐式调用的情况。
     *
     * ```cangjie
     * var x: Option<String> = None
     * // 假设存在隐式调用场景
     * x.someMethod()  // 错误：不安全的调用
     * ```
     *
     * @param callReceiver 调用接收者参数
     * @param diagnostic 诊断信息
     */
    override fun onCallReceiver(callReceiver: SimpleCangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        when (diagnostic) {
            // ─────────────────────────────────────────────────────────────────
            // 不安全的调用（可空类型相关）
            // ─────────────────────────────────────────────────────────────────
            is UnsafeCallError -> {
                // 仓颉没有扩展函数类型，只需检查是否为隐式调用
                val isForImplicitInvoke = when (callReceiver) {
                    is ReceiverExpressionCangJieCallArgument -> callReceiver.isForImplicitInvoke
                    else -> diagnostic.isForImplicitInvoke
                }

                tracingStrategy.unsafeCall(trace, callReceiver.receiver.receiverValue.type, isForImplicitInvoke)
            }

            // ─────────────────────────────────────────────────────────────────
            // 未知诊断类型
            // ─────────────────────────────────────────────────────────────────
            else -> {
                unknownError(diagnostic, "onCallReceiver")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 智能转换处理
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 报告智能转换诊断
     *
     * 智能转换是编译器根据控制流分析自动推断更精确类型的机制。
     * 此方法处理成功的智能转换，记录转换后的类型信息。
     *
     * ## 示例
     *
     * ```cangjie
     * var x: Any = "hello"
     * if (x is String) {
     *     // 智能转换：x 被推断为 String 类型
     *     print(x.length)  // 可以访问 String 的成员
     * }
     * ```
     *
     * ## 处理流程
     *
     * 1. 获取表达式参数
     * 2. 创建数据流值
     * 3. 检查并记录可能的智能转换
     * 4. 如果有已解析的调用，更新其分发接收者的智能转换类型
     *
     * @param smartCastDiagnostic 智能转换诊断信息
     */
    private fun reportSmartCast(smartCastDiagnostic: SmartCastDiagnostic) {
        val expressionArgument = smartCastDiagnostic.argument

        // 根据参数类型处理智能转换
        val smartCastResult = when (expressionArgument) {
            // ─────────────────────────────────────────────────────────────────
            // 处理表达式调用参数的智能转换
            // ─────────────────────────────────────────────────────────────────
            is ExpressionCangJieCallArgumentImpl -> {
                trace.markAsReported()

                // 使用参数之前的数据流信息
                val context = context.replaceDataFlowInfo(expressionArgument.dataFlowInfoBeforeThisArgument)

                // 获取去括号后的参数表达式
                val argumentExpression = CjPsiUtil.getLastElementDeparenthesized(
                    expressionArgument.valueArgument.getArgumentExpression(),
                    context.statementFilter
                )

                // 创建数据流值
                val dataFlowValue =
                    dataFlowValueFactory.createDataFlowValue(expressionArgument.receiver.receiverValue, context)

                // 二元表达式不使用调用信息
                val call = if (call.callElement is CjBinaryExpression) null else call

                // 检查并记录智能转换（非外部参数）
                if (!expressionArgument.valueArgument.isExternal()) {
                    smartCastManager.checkAndRecordPossibleCast(
                        dataFlowValue, smartCastDiagnostic.smartCastType, argumentExpression, context, call,
                        recordExpressionType = false
                    )
                } else null
            }

            // ─────────────────────────────────────────────────────────────────
            // 处理接收者表达式参数的智能转换
            // ─────────────────────────────────────────────────────────────────
            is ReceiverExpressionCangJieCallArgument -> {
                trace.markAsReported()

                val receiverValue = expressionArgument.receiver.receiverValue
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverValue, context)

                smartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue,
                    smartCastDiagnostic.smartCastType,
                    (receiverValue as? ExpressionReceiver)?.expression,
                    context,
                    call,
                    recordExpressionType = true
                )
            }

            else -> null
        }

        // 如果有已解析的调用，更新分发接收者的智能转换类型
        val resolvedCall =
            smartCastDiagnostic.cangjieCall?.psiCangJieCall?.psiCall?.getResolvedCall(trace.bindingContext) as? ResolvedCallImpl<*>
        if (resolvedCall != null && smartCastResult != null) {
            if (resolvedCall.dispatchReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.smartCastDispatchReceiverType = smartCastResult.resultType
            }
        }
    }

    /**
     * 报告不稳定的智能转换
     *
     * 当智能转换目标是不稳定的变量（如可变局部变量、属性等）时，
     * 编译器无法保证智能转换的安全性，因此报告警告。
     *
     * ## 示例
     *
     * ```cangjie
     * var x: Any = "hello"  // 可变变量
     * if (x is String) {
     *     // x 可能在其他线程中被修改
     *     print(x.length)  // 警告：智能转换不可能，x 是可变变量
     * }
     * ```
     *
     * ## 稳定性规则
     *
     * - **稳定**：不可变局部变量、不可变属性
     * - **不稳定**：可变变量、可变属性、可能被并发修改的变量
     *
     * @param unstableSmartCast 不稳定智能转换诊断信息
     */
    private fun reportUnstableSmartCast(unstableSmartCast: UnstableSmartCast) {
        // 创建数据流值
        val dataFlowValue =
            dataFlowValueFactory.createDataFlowValue(unstableSmartCast.argument.receiver.receiverValue, context)

        // 获取智能转换可能的类型
        val possibleTypes = unstableSmartCast.argument.receiver.typesFromSmartCasts
        val argumentExpression = unstableSmartCast.argument.psiExpression ?: return

        // 确保有可能的类型
        require(possibleTypes.isNotEmpty()) { "Receiver for unstable smart cast without possible types" }

        // 计算交集类型
        val intersectWrappedTypes = intersectWrappedTypes(possibleTypes)

        // 记录不稳定智能转换信息
        trace.record(
            BindingContext.UNSTABLE_SMARTCAST,
            argumentExpression,
            SingleSmartCast(null, intersectWrappedTypes)
        )

        // 报告警告：智能转换不可能
        trace.report(
            SMARTCAST_IMPOSSIBLE.on(
                argumentExpression,
                intersectWrappedTypes,
                argumentExpression.text,
                dataFlowValue.kind.description
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 调用参数诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理调用参数相关的诊断信息
     *
     * 调用参数诊断是指与函数调用的具体参数相关的错误。
     *
     * ## 处理的诊断类型
     *
     * ### SmartCastDiagnostic
     * 成功的智能转换，需要记录转换后的类型。
     *
     * ### UnstableSmartCast
     * 不稳定的智能转换，报告警告。
     *
     * ### VisibilityErrorOnArgument
     * 参数引用了不可见的成员。
     *
     * ### TooManyArguments
     * 传递了过多的参数。
     * ```cangjie
     * func foo(x: Int) {}
     * foo(1, 2)  // 错误：参数过多
     * ```
     *
     * ### VarargArgumentOutsideParentheses
     * 可变参数在括号外部。
     *
     * ### MissingNamedArgumentPrefix
     * 缺少命名参数前缀。
     *
     * ### PositionalAfierNamedArgument
     * 命名参数后出现位置参数。
     * ```cangjie
     * func foo(x: Int, y: String) {}
     * foo(x: 1, "hello")  // 错误：命名参数后不能跟位置参数
     * ```
     *
     * ### MixingNamedAndPositionArguments
     * 混合使用命名参数和位置参数（在不允许的情况下）。
     *
     * ### NoneCallableReferenceCallCandidates
     * 可调用引用无法解析到任何候选项。
     * ```cangjie
     * let f = ::nonExistentFunction  // 错误：找不到函数
     * ```
     *
     * ### CallableReferenceCallCandidatesAmbiguity
     * 可调用引用解析到多个候选项（歧义）。
     * ```cangjie
     * func foo(x: Int) {}
     * func foo(x: String) {}
     * let f = ::foo  // 错误：歧义，不知道引用哪个 foo
     * ```
     *
     * ### NotEnoughInformationForLambdaParameter
     * 无法推断 Lambda 参数的类型。
     * ```cangjie
     * let f = { x -> x.toString() }  // 错误：无法推断 x 的类型
     * ```
     *
     * ### CompatibilityWarningOnArgument
     * 参数兼容性警告。
     *
     * @param callArgument 调用参数
     * @param diagnostic 诊断信息
     */
    override fun onCallArgument(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        when (diagnostic) {
            // ─────────────────────────────────────────────────────────────────
            // 智能转换相关诊断
            // ─────────────────────────────────────────────────────────────────
            is SmartCastDiagnostic -> reportSmartCast(diagnostic)
            is UnstableSmartCast -> reportUnstableSmartCast(diagnostic)

            // ─────────────────────────────────────────────────────────────────
            // 参数可见性错误
            // ─────────────────────────────────────────────────────────────────
            is VisibilityErrorOnArgument -> {
                val invisibleMember = diagnostic.invisibleMember
                val argumentExpression =
                    diagnostic.argument.psiCallArgument.valueArgument.getArgumentExpression()
                        ?.lastBlockStatementOrThis()

                if (argumentExpression != null) {
                    trace.report(
                        INVISIBLE_MEMBER.on(
                            argumentExpression,
                            invisibleMember,
                            invisibleMember.visibility,
                            invisibleMember
                        )
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 参数数量过多
            // ─────────────────────────────────────────────────────────────────
            is TooManyArguments -> {
                trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                    TOO_MANY_ARGUMENTS.on(expr, diagnostic.descriptor)
                }
                trace.markAsReported()
            }

            // ─────────────────────────────────────────────────────────────────
            // 可变参数在括号外部
            // ─────────────────────────────────────────────────────────────────
            is VarargArgumentOutsideParentheses -> trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                VARARG_OUTSIDE_PARENTHESES.on(expr)
            }

            // ─────────────────────────────────────────────────────────────────
            // 缺少命名参数前缀
            // ─────────────────────────────────────────────────────────────────
            is MissingNamedArgumentPrefix -> {
                trace.report(
                    NAMED_PARAMETER_PREFIX_MISSING.on(
                        callArgument.psiCallArgument.valueArgument.asElement(),
                        diagnostic.names
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 命名参数后出现位置参数
            // ─────────────────────────────────────────────────────────────────
            is PositionalAfierNamedArgument -> {
                trace.report(POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            // ─────────────────────────────────────────────────────────────────
            // 混合使用命名参数和位置参数
            // ─────────────────────────────────────────────────────────────────
            is MixingNamedAndPositionArguments -> {
                trace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            // ─────────────────────────────────────────────────────────────────
            // 可调用引用无候选项
            // ─────────────────────────────────────────────────────────────────
            is NoneCallableReferenceCallCandidates -> {
                val argument = diagnostic.argument
                val expression = (argument as? CallableReferenceCangJieCallArgumentImpl)?.cjCallableReferenceExpression
                if (expression != null) {
                    // 使用统一的未解析引用错误报告方法
                    trace.reportUnresolvedReference(expression)
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 可调用引用歧义
            // ─────────────────────────────────────────────────────────────────
            is CallableReferenceCallCandidatesAmbiguity -> {
                val expression = when (val psiExpression = diagnostic.argument.psiExpression) {
                    is CjPsiUtil.CjExpressionWrapper -> psiExpression.baseExpression
                    else -> psiExpression
                }

                val candidates = diagnostic.candidates.map { it.candidate }
                if (expression != null) {
                    trace.reportDiagnosticOnce(
                        CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY.on(
                            expression,
                            candidates
                        )
                    )
                    // 记录歧义引用目标，供 IDE 使用
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, expression, candidates)
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // Lambda 参数类型推断失败
            // ─────────────────────────────────────────────────────────────────
            is NotEnoughInformationForLambdaParameter -> {
                val lambdaArgument = diagnostic.lambdaArgument
                val parameterIndex = diagnostic.parameterIndex

                val valueArgument = lambdaArgument.psiCallArgument.valueArgument

                // 获取 Lambda 或匿名函数的参数列表
                val valueParameters =
                    when (val argumentExpression = CjPsiUtil.deparenthesize(valueArgument.getArgumentExpression())) {
                        is CjLambdaExpression -> argumentExpression.valueParameters
                        is CjNamedFunction -> argumentExpression.valueParameters // 匿名函数
                        else -> return
                    }

                // 报告特定参数的类型推断失败
                val parameter = valueParameters.getOrNull(parameterIndex)
                if (parameter != null) {
                    trace.report(CANNOT_INFER_PARAMETER_TYPE.on(parameter))
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 兼容性警告
            // ─────────────────────────────────────────────────────────────────
            is CompatibilityWarningOnArgument -> {
                trace.report(
                    COMPATIBILITY_WARNING.on(
                        callArgument.psiCallArgument.valueArgument.asElement(),
                        diagnostic.candidate
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 其他地方处理的诊断
            // ─────────────────────────────────────────────────────────────────
            is NotCallableMemberReference, is NotCallableExpectedType -> {
                // NotCallableMemberReference -> 在 DoubleColonExpressionResolver 中报告 UNSUPPORTED
                // NotCallableExpectedType -> 在 reportConstraintErrorByPosition 中报告 TYPE_MISMATCH
                return
            }

            // ─────────────────────────────────────────────────────────────────
            // 未知诊断类型
            // ─────────────────────────────────────────────────────────────────
            else -> {
                unknownError(diagnostic, "onCallArgument")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 命名参数诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理命名参数名称相关的诊断信息
     *
     * 命名参数诊断是指与参数名称相关的错误，如参数名不存在、歧义等。
     *
     * ## 处理的诊断类型
     *
     * ### NamedArgumentReference
     * 成功解析的命名参数引用，需要记录引用关系。
     *
     * ### NameForAmbiguousParameter
     * 参数名歧义。
     * ```cangjie
     * func foo(x: Int, x: String) {}  // 假设允许重名
     * foo(x: 1)  // 错误：不知道引用哪个 x
     * ```
     *
     * ### NameNotFound
     * 参数名未找到。
     * ```cangjie
     * func foo(x: Int) {}
     * foo(y: 1)  // 错误：没有名为 y 的参数
     * ```
     *
     * ### NamedArgumentNotAllowed
     * 不允许使用命名参数（如调用外部函数）。
     *
     * ### ArgumentPassedTwice
     * 参数被传递了两次。
     * ```cangjie
     * func foo(x: Int) {}
     * foo(1, x: 2)  // 错误：x 被传递了两次
     * ```
     *
     * @param callArgument 调用参数
     * @param diagnostic 诊断信息
     */
    override fun onCallArgumentName(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        // 获取参数名称的引用表达式
        val nameReference = callArgument.psiCallArgument.valueArgument.getArgumentName()?.referenceExpression ?: return

        when (diagnostic) {
            // ─────────────────────────────────────────────────────────────────
            // 成功解析的命名参数：记录引用关系
            // ─────────────────────────────────────────────────────────────────
            is NamedArgumentReference -> {
                trace.record(BindingContext.REFERENCE_TARGET, nameReference, diagnostic.parameterDescriptor)
                trace.markAsReported()
            }

            // ─────────────────────────────────────────────────────────────────
            // 参数名歧义
            // ─────────────────────────────────────────────────────────────────
            is NameForAmbiguousParameter -> trace.report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))

            // ─────────────────────────────────────────────────────────────────
            // 参数名未找到
            // ─────────────────────────────────────────────────────────────────
            is NameNotFound -> trace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))

            // ─────────────────────────────────────────────────────────────────
            // 不允许使用命名参数
            // ─────────────────────────────────────────────────────────────────
            is NamedArgumentNotAllowed -> trace.report(
                NAMED_ARGUMENTS_NOT_ALLOWED.on(
                    nameReference,
                    when (diagnostic.descriptor) {
                        // 在仓颉语言中，主要是非仓颉函数不支持命名参数
                        else -> BadNamedArgumentsTarget.NON_CANGJIE_FUNCTION
                    }
                )
            )

            // ─────────────────────────────────────────────────────────────────
            // 参数被传递了两次
            // ─────────────────────────────────────────────────────────────────
            is ArgumentPassedTwice -> trace.report(ARGUMENT_PASSED_TWICE.on(nameReference))

            // ─────────────────────────────────────────────────────────────────
            // 未知诊断类型
            // ─────────────────────────────────────────────────────────────────
            else -> {
                unknownError(diagnostic, "onCallArgumentName")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 展开参数诊断
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理展开参数（spread）相关的诊断信息
     *
     * 展开参数是指使用展开操作符（如 `*array`）将数组展开为多个参数。
     *
     * 当前实现为空，因为仓颉语言的展开参数处理较为简单。
     *
     * @param callArgument 调用参数
     * @param diagnostic 诊断信息
     */
    override fun onCallArgumentSpread(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic) {
        // 当前不需要处理展开参数的诊断
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 约束错误报告 - 参数约束错误
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 根据位置报告参数约束错误
     *
     * 约束错误是类型推断过程中约束冲突产生的错误。
     * 此方法根据错误位置确定在哪个 PSI 元素上报告错误。
     *
     * ## 处理流程
     *
     * 1. 如果是 Lambda 参数，尝试在 Lambda 参数声明上报告
     * 2. 如果没有表达式，在调用点报告接收者类型不匹配
     * 3. 检查是否是常量类型错误
     * 4. 检查是否是无符号类型转换情况
     * 5. 报告通用类型不匹配错误
     *
     * @param error 约束不匹配错误
     * @param argument 调用参数
     * @param isWarning 是否作为警告报告
     * @param typeMismatchDiagnostic 类型不匹配诊断工厂
     * @param selectorCall 选择器调用（用于链式调用）
     * @param report 报告函数
     */
    private fun reportArgumentConstraintErrorByPosition(
        error: ConstraintMismatch,
        argument: CangJieCallArgument,
        isWarning: Boolean,
        typeMismatchDiagnostic: DiagnosticFactory2<CjExpression, CangJieType, CangJieType>,
        selectorCall: CangJieCall?,
        report: (Diagnostic) -> Unit
    ) {
        // ─────────────────────────────────────────────────────────────────────
        // 处理 Lambda 参数的特殊情况
        // ─────────────────────────────────────────────────────────────────────
        if (argument is LambdaCangJieCallArgument) {
            val parameterTypes = argument.parametersTypes?.toList()
            if (parameterTypes != null) {
                // 尝试找到类型不匹配的参数
                val index = parameterTypes.indexOf(error.upperCangJieType.unwrap())
                val lambdaExpression = argument.psiExpression as? CjLambdaExpression
                val parameter = lambdaExpression?.valueParameters?.getOrNull(index)
                if (parameter != null) {
                    // 在 Lambda 参数声明上报告期望参数类型不匹配
                    val diagnosticFactory =
                        if (isWarning) EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING else EXPECTED_PARAMETER_TYPE_MISMATCH
                    report(diagnosticFactory.on(parameter, error.lowerCangJieType))
                    return
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // 处理没有表达式的情况（如隐式接收者）
        // ─────────────────────────────────────────────────────────────────────
        val expression = argument.psiExpression ?: run {
            val psiCall = (selectorCall as? PSICangJieCall)?.psiCall ?: psiCangJieCall.psiCall

            // 在调用点报告接收者类型不匹配
            if (reportAdditionalErrors) {
                report(
                    RECEIVER_TYPE_MISMATCH.on(
                        psiCall.calleeExpression ?: psiCall.callElement, error.upperCangJieType, error.lowerCangJieType
                    )
                )
            }
            return
        }

        // ─────────────────────────────────────────────────────────────────────
        // 检查常量类型错误
        // ─────────────────────────────────────────────────────────────────────
        val deparenthesized = CjPsiUtil.safeDeparenthesize(expression)
        if (reportConstantTypeMismatch(error, deparenthesized)) return

        // ─────────────────────────────────────────────────────────────────────
        // 检查无符号类型转换的特殊情况
        // ─────────────────────────────────────────────────────────────────────
        val compileTimeConstant = trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized] as? TypedCompileTimeConstant
        if (compileTimeConstant != null) {
            val expressionType = trace[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type
            if (expressionType != null &&
                !UnsignedTypes.isUnsignedType(compileTimeConstant.type) && UnsignedTypes.isUnsignedType(expressionType)
            ) {
                // 无符号类型转换的特殊情况，不报告错误
                return
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // 报告通用类型不匹配错误
        // ─────────────────────────────────────────────────────────────────────
        report(typeMismatchDiagnostic.on(deparenthesized, error.upperCangJieType, error.lowerCangJieType))
    }

    /**
     * 报告常量类型不匹配错误
     *
     * 当常量表达式的类型与期望类型不匹配时，报告专门的常量类型错误。
     *
     * @param constraintError 约束错误
     * @param expression 常量表达式
     * @return 如果报告了错误返回 true，否则返回 false
     */
    private fun reportConstantTypeMismatch(constraintError: ConstraintMismatch, expression: CjExpression): Boolean {
        if (expression is CjConstantExpression) {
            val module = context.scope.ownerDescriptor.module

            // 求值常量表达式
            val constantValue =
                constantExpressionEvaluator.evaluateToConstantValue(expression, trace, context.expectedType)

            // 检查常量类型
            val hasConstantTypeError = CompileTimeConstantChecker(context, module, true)
                .checkConstantExpressionType(constantValue, expression, constraintError.upperCangJieType)

            if (hasConstantTypeError) return true
        }
        return false
    }

    /**
     * 报告可调用引用约束错误
     *
     * 当可调用引用的类型与期望类型不匹配时报告错误。
     *
     * @param error 约束不匹配错误
     * @param rhsExpression 可调用引用右侧的名称表达式
     */
    private fun reportCallableReferenceConstraintError(
        error: ConstraintMismatch,
        rhsExpression: CjSimpleNameExpression
    ) {
        trace.report(TYPE_MISMATCH.on(rhsExpression, error.lowerCangJieType, error.upperCangJieType))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 约束错误报告 - 根据约束位置分发
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 根据约束位置报告约束错误
     *
     * 约束位置决定了错误应该在代码的哪个位置报告。
     * 此方法根据不同的约束位置类型，将错误报告到相应的 PSI 元素上。
     *
     * ## 约束位置类型
     *
     * ### ArgumentConstraintPosition
     * 参数约束位置：错误在参数表达式上报告。
     *
     * ### ReceiverConstraintPosition
     * 接收者约束位置：错误在接收者表达式上报告。
     *
     * ### LambdaArgumentConstraintPosition
     * Lambda 参数约束位置：错误在 Lambda 表达式上报告。
     *
     * ### ExpectedTypeConstraintPosition
     * 期望类型约束位置：错误在整个调用表达式上报告。
     *
     * ### ExplicitTypeParameterConstraintPosition
     * 显式类型参数约束位置：错误在类型参数上报告（上界违反）。
     *
     * ### FixVariableConstraintPosition
     * 固定变量约束位置：错误在被调用表达式上报告。
     *
     * ### DeclaredUpperBoundConstraintPosition
     * 声明上界约束位置：类型参数违反了声明的上界。
     *
     * @param error 约束不匹配错误
     * @param position 约束位置
     */
    private fun reportConstraintErrorByPosition(error: ConstraintMismatch, position: ConstraintPosition) {
        // ─────────────────────────────────────────────────────────────────────
        // 可调用引用约束位置的特殊处理
        // ─────────────────────────────────────────────────────────────────────
        if (position is CallableReferenceConstraintPositionImpl) {
            val callableReferenceExpression = position.callableReferenceCall.call.extractCallableReferenceExpression()

            require(callableReferenceExpression != null) {
                "There should be the corresponding callable reference expression for `CallableReferenceConstraintPositionImpl`"
            }

            reportCallableReferenceConstraintError(error, callableReferenceExpression.callableReference)
            return
        }

        // 确定是否作为警告报告
        val isWarning = error is ConstraintWarning
        val typeMismatchDiagnostic = if (isWarning) TYPE_MISMATCH_WARNING else TYPE_MISMATCH
        val report = if (isWarning) trace::reportDiagnosticOnce else trace::report

        // ─────────────────────────────────────────────────────────────────────
        // 根据约束位置类型分发处理
        // ─────────────────────────────────────────────────────────────────────
        when (position) {
            // 参数约束位置
            is ArgumentConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, position.argument as CangJieCallArgument,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = null, report
                )
            }

            // 接收者约束位置
            is ReceiverConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, position.argument as CangJieCallArgument,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = (position as ReceiverConstraintPositionImpl).selectorCall, report
                )
            }

            // Lambda 参数约束位置
            is LambdaArgumentConstraintPosition<*> -> {
                reportArgumentConstraintErrorByPosition(
                    error, (position.lambda as ResolvedLambdaAtom).atom,
                    isWarning, typeMismatchDiagnostic,
                    selectorCall = null, report
                )
            }

            // 期望类型约束位置
            is ExpectedTypeConstraintPosition<*> -> {
                val call =
                    (position.topLevelCall as? CangJieCall)?.psiCangJieCall?.psiCall?.callElement as? CjExpression

                // 计算推断类型（如果是 Nothing 则使用 Option 包装期望类型）
                val inferredType =
                    if (!error.lowerCangJieType.isNothing()) error.lowerCangJieType
                    else error.upperCangJieType.makeOption()

                if (call != null) {
                    report(typeMismatchDiagnostic.on(call, error.upperCangJieType, inferredType))
                }
            }

            // 构建器推断替换约束位置：递归处理初始约束
            is BuilderInferenceSubstitutionConstraintPosition<*> -> {
                reportConstraintErrorByPosition(error, position.initialConstraint.position)
            }

            // 显式类型参数约束位置：报告上界违反
            is ExplicitTypeParameterConstraintPosition<*> -> {
                val typeArgumentReference =
                    (position.typeArgument as SimpleTypeArgumentImpl).typeProjection.typeReference ?: return
                val diagnosticFactory = if (isWarning) UPPER_BOUND_VIOLATED_WARNING else UPPER_BOUND_VIOLATED
                report(diagnosticFactory.on(typeArgumentReference, error.upperCangJieType, error.lowerCangJieType))
            }

            // 固定变量约束位置
            is FixVariableConstraintPosition<*> -> {
                // 检查是否有更精确的诊断（避免重复报告）
                val morePreciseDiagnosticExists = allDiagnostics.any { other ->
                    val otherError = other.constraintSystemError ?: return@any false
                    otherError is ConstraintError && otherError.position.from !is FixVariableConstraintPositionImpl
                }
                if (morePreciseDiagnosticExists) return

                val call = ((position.resolvedAtom as? ResolvedAtom)?.atom as? PSICangJieCall)?.psiCall ?: call
                val expression = call.calleeExpression ?: return

                trace.reportDiagnosticOnce(
                    typeMismatchDiagnostic.on(
                        expression,
                        error.upperCangJieType,
                        error.lowerCangJieType
                    )
                )
            }

            // 构建器推断位置：稍后报告错误
            BuilderInferencePosition -> {
                // 错误将在稍后报告
            }

            // 声明上界约束位置
            is DeclaredUpperBoundConstraintPosition<*> -> {
                val originalCall = (position as DeclaredUpperBoundConstraintPositionImpl).cangjieCall
                val typeParameterDescriptor = position.typeParameter
                val ownerDescriptor = typeParameterDescriptor.containingDeclaration
                if (reportAdditionalErrors) {
                    trace.reportDiagnosticOnce(
                        UPPER_BOUND_VIOLATION_IN_CONSTRAINT.on(
                            (originalCall as PSICangJieCall).psiCall.callElement,
                            typeParameterDescriptor.name,
                            ownerDescriptor.name,
                            error.upperCangJieType,
                            error.lowerCangJieType
                        )
                    )
                }
            }

            // 已知类型参数约束位置：稍后报告 UPPER_BOUND_VIOLATED
            is KnownTypeParameterConstraintPosition<*> -> {
                // 稍后报告
            }

            // 其他约束位置：报告通用类型不匹配
            is CallableReferenceConstraintPosition<*>,
            is IncorporationConstraintPosition,
            is InjectedAnotherStubTypeConstraintPosition<*>,
            is SimpleConstraintSystemConstraintPosition
                -> {
                if (false) {
                    throw AssertionError("Constraint error in unexpected position: $position")
                } else if (reportAdditionalErrors) {
                    report(
                        TYPE_MISMATCH_IN_CONSTRAINT.on(
                            psiCangJieCall.psiCall.callElement,
                            error.upperCangJieType,
                            error.lowerCangJieType,
                            position
                        )
                    )
                }
            }

            // 基于索引的参数约束位置（用于迭代类型推断）
            is ArgumentConstraintPositionByIndex -> {
                if (reportAdditionalErrors) {
                    report(
                        TYPE_MISMATCH_IN_CONSTRAINT.on(
                            psiCangJieCall.psiCall.callElement,
                            error.upperCangJieType,
                            error.lowerCangJieType,
                            position
                        )
                    )
                }
            }

            // Lambda 返回类型位置（用于 Last Resort 机制）
            is LambdaReturnTypePosition -> {
                if (reportAdditionalErrors) {
                    report(
                        TYPE_MISMATCH_IN_CONSTRAINT.on(
                            psiCangJieCall.psiCall.callElement,
                            error.upperCangJieType,
                            error.lowerCangJieType,
                            position
                        )
                    )
                }
            }

            // 类型变量固定位置（用于迭代推断中的变量固定）
            is TypeVariableFixationPosition -> {
                // 检查是否有更精确的诊断
                val morePreciseDiagnosticExists = allDiagnostics.any { other ->
                    val otherError = other.constraintSystemError ?: return@any false
                    otherError is ConstraintError && otherError.position.from !is TypeVariableFixationPosition
                }
                if (morePreciseDiagnosticExists) return

                val expression = psiCangJieCall.psiCall.calleeExpression ?: return
                trace.reportDiagnosticOnce(
                    typeMismatchDiagnostic.on(
                        expression,
                        error.upperCangJieType,
                        error.lowerCangJieType
                    )
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 辅助方法 - 未推断类型参数检查
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 检查类型是否包含未推断的类型参数
     *
     * @param uninferredTypeVariable 未推断的类型变量
     * @return 如果类型包含未推断的类型参数则返回 true
     */
    private fun CangJieType.containsUninferredTypeParameter(uninferredTypeVariable: TypeVariableMarker) = contains {
        ErrorUtils.isUninferredTypeVariable(it) || it == TypeUtils.DONT_CARE
                || it.constructor == uninferredTypeVariable.freshTypeConstructor(typeSystemContext)
    }

    /**
     * 检查约束错误是否涉及指定的类型变量
     *
     * 通过检查 ConstraintError 的 lowerType 或 upperType 是否包含指定的类型变量来判断。
     * 这用于避免将不同类型变量的错误误判为冗余诊断。
     *
     * 例如，对于 `class B<T> { static func a<D>(b: D) }` 调用 `B.a<String>(123)`：
     * - D 的约束错误（期望 String，实际 Int）不应该抑制 T 的类型参数信息不足错误
     * - 只有当约束错误涉及同一个类型变量时，才应该被认为是冗余的
     *
     * @param typeVariable 要检查的类型变量
     * @return 如果约束错误涉及指定的类型变量则返回 true
     */
    private fun ConstraintMismatch.involvesTypeVariable(typeVariable: TypeVariableMarker): Boolean {
        val freshConstructor = typeVariable.freshTypeConstructor(typeSystemContext)
        val lowerCangJieType = lowerType as? CangJieType
        val upperCangJieType = upperType as? CangJieType
        return lowerCangJieType?.contains { it.constructor == freshConstructor } == true ||
                upperCangJieType?.contains { it.constructor == freshConstructor } == true
    }

    /**
     * 获取特殊调用中需要报告未推断类型参数错误的子解析原子
     *
     * 特殊调用（如 if/when/try 等控制结构）可能包含多个子表达式，
     * 需要找出哪些子表达式的类型包含未推断的类型参数。
     *
     * @param resolvedAtom 已解析的原子
     * @param uninferredTypeVariable 未推断的类型变量
     * @return 需要报告错误的子解析原子集合
     */
    private fun getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(
        resolvedAtom: ResolvedAtom,
        uninferredTypeVariable: TypeVariableMarker
    ): Set<ResolvedAtom> =
        buildSet {
            for (subResolvedAtom in resolvedAtom.subResolvedAtoms ?: return@buildSet) {
                val atom = subResolvedAtom.atom

                // 获取需要检查的类型
                val typeToCheck = when {
                    subResolvedAtom is PostponedResolvedAtom -> subResolvedAtom.expectedType ?: return@buildSet
                    atom is SimpleCangJieCallArgument -> atom.receiver.receiverValue.type
                    else -> return@buildSet
                }

                // 如果类型包含未推断的类型参数，添加到结果集
                if (typeToCheck.containsUninferredTypeParameter(uninferredTypeVariable)) {
                    add(subResolvedAtom)
                }

                // 递归检查子原子
                if (!subResolvedAtom.subResolvedAtoms.isNullOrEmpty()) {
                    addAll(
                        getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(
                            subResolvedAtom,
                            uninferredTypeVariable
                        )
                    )
                }
            }
        }

    /**
     * 获取参数表达式或块中的最后一个表达式
     *
     * @param atom PSI 调用参数
     * @return 参数表达式，如果是块则返回最后一个语句
     */
    private fun getArgumentsExpressionOrLastExpressionInBlock(atom: PSICangJieCallArgument): CjExpression? {
        val valueArgumentExpression = atom.valueArgument.getArgumentExpression()

        return if (valueArgumentExpression is CjBlockExpression)
            valueArgumentExpression.statements.lastOrNull()
        else
            valueArgumentExpression
    }

    /**
     * 为特殊调用报告类型参数信息不足错误
     *
     * 特殊调用（如 if/when/try）的错误需要在具体的子表达式上报告，
     * 而不是在整个调用上报告。
     *
     * @param resolvedAtom 已解析的调用原子
     * @param error 类型参数信息不足错误
     */
    private fun reportNotEnoughInformationForTypeParameterForSpecialCall(
        resolvedAtom: ResolvedCallAtom,
        error: NotEnoughInformationForTypeParameterImpl
    ) {
        // 找到需要报告错误的子原子
        val subResolvedAtomsToReportError =
            getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(resolvedAtom, error.typeVariable)

        if (subResolvedAtomsToReportError.isEmpty()) return

        // 在每个子原子上报告错误
        for (subResolvedAtom in subResolvedAtomsToReportError) {
            val atom = subResolvedAtom.atom as? PSICangJieCallArgument ?: continue
            val argumentsExpression = getArgumentsExpressionOrLastExpressionInBlock(atom)

            if (argumentsExpression != null) {
                // 找到特殊函数名称
                val specialFunctionName = requireNotNull(
                    ControlStructureTypingUtils.ResolveConstruct.entries.find { specialFunction ->
                        specialFunction.specialFunctionName == resolvedAtom.candidateDescriptor.name
                    }
                ) { "Unsupported special construct: ${resolvedAtom.candidateDescriptor.name} not found in special construct names" }

                trace.reportDiagnosticOnce(
                    NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(
                        argumentsExpression, " for subcalls of ${specialFunctionName.name} expression", null
                    )
                )
            }
        }
    }

    /**
     * 检查原子是否是特殊函数（控制结构）
     *
     * 特殊函数包括 if、when、try 等控制结构，它们在类型推断中有特殊处理。
     *
     * @param atom 解析原子
     * @return 如果是特殊函数返回 true
     */
    @OptIn(ExperimentalContracts::class)
    private fun isSpecialFunction(atom: ResolvedAtom): Boolean {
        contract {
            returns(true) implies (atom is ResolvedCallAtom)
        }
        if (atom !is ResolvedCallAtom) return false

        return ControlStructureTypingUtils.ResolveConstruct.entries.any { specialFunction ->
            specialFunction.specialFunctionName == atom.candidateDescriptor.name
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DiagnosticReporter 接口实现 - 约束系统错误
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 处理约束系统错误
     *
     * 约束系统错误是类型推断过程中约束冲突或无法求解产生的错误。
     * 这是类型推断错误报告的核心方法。
     *
     * ## 处理的错误类型
     *
     * ### ConstraintMismatch
     * 类型约束不匹配，如期望 Int 但实际是 String。
     *
     * ### CapturedTypeFromSubtyping
     * 捕获类型从子类型检查中逃逸。
     *
     * ### MultipleMinimalCommonSupertypes
     * 存在多个最小公共超类型（无法确定唯一类型）。
     *
     * ### InferredIntoDeclaredUpperBounds
     * 类型被推断为声明的上界（可能是不期望的）。
     *
     * ### NotEnoughInformationForTypeParameterImpl
     * 没有足够的信息来推断类型参数。
     *
     * ### OnlyInputTypesDiagnostic
     * 只有输入类型的诊断。
     *
     * ### InferredEmptyIntersectionError/Warning
     * 推断出了空交集类型。
     *
     * ### ConstrainingTypeIsError
     * 约束类型本身是错误类型（已在其他地方报告）。
     *
     * ### LowerPriorityToPreserveCompatibility
     * 为保持兼容性降低优先级（不报告）。
     *
     * @param error 约束系统错误
     */
    override fun constraintError(error: ConstraintSystemError) {
        when (error) {
            // ─────────────────────────────────────────────────────────────────
            // 约束不匹配：类型不兼容
            // ─────────────────────────────────────────────────────────────────
            is ConstraintMismatch -> reportConstraintErrorByPosition(error, error.position.from)

            // ─────────────────────────────────────────────────────────────────
            // 捕获类型逃逸
            // ─────────────────────────────────────────────────────────────────
            is CapturedTypeFromSubtyping -> {
                val position = error.position
                // 尝试找到参数位置
                val argumentPosition: ArgumentConstraintPositionImpl? =
                    position as? ArgumentConstraintPositionImpl
                        ?: (position as? IncorporationConstraintPosition)?.from as? ArgumentConstraintPositionImpl

                argumentPosition?.let {
                    val expression = it.argument.psiExpression ?: return
                    trace.reportDiagnosticOnce(
                        NEW_INFERENCE_ERROR.on(
                            expression,
                            "Capture type from subtyping ${error.constraintType} for variable ${error.typeVariable}"
                        )
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 多个最小公共超类型
            // ─────────────────────────────────────────────────────────────────
            is MultipleMinimalCommonSupertypes -> {
                val psiCall = psiCangJieCall.psiCall
                val expression = if (psiCall is CallTransformer.CallForImplicitInvoke) {
                    psiCall.outerCall.calleeExpression
                } else {
                    psiCall.calleeExpression?.takeIf { it.isPhysical } ?: psiCall.callElement
                } ?: return

                // 只在允许多父元素报告的表达式类型上报告
                if (multiParentElementReports.contains(expression::class)) {
                    trace.reportDiagnosticOnce(
                        TYPE_MISMATCH_MULTIPLE_SUPERTYPES.on(
                            expression,
                            error.candidates
                        )
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 推断到声明的上界
            // ─────────────────────────────────────────────────────────────────
            is InferredIntoDeclaredUpperBounds -> {
                val psiCall = psiCangJieCall.psiCall
                val expression = if (psiCall is CallTransformer.CallForImplicitInvoke) {
                    psiCall.outerCall.calleeExpression
                } else {
                    psiCall.calleeExpression?.takeIf { it.isPhysical } ?: psiCall.callElement
                } ?: return
                val typeVariable = error.typeVariable as? TypeVariableFromCallableDescriptor ?: return

                trace.reportDiagnosticOnce(
                    INFERRED_INTO_DECLARED_UPPER_BOUNDS.on(
                        expression,
                        typeVariable.originalTypeParameter.name.asString()
                    )
                )
            }

            // ─────────────────────────────────────────────────────────────────
            // 类型参数信息不足
            // ─────────────────────────────────────────────────────────────────
            is NotEnoughInformationForTypeParameterImpl -> {
                val resolvedAtom = error.resolvedAtom

                // 检查是否是冗余诊断（已有更具体的诊断）
                val isDiagnosticRedundant = !isSpecialFunction(resolvedAtom) && allDiagnostics.any {
                    when (it) {
                        is WrongCountOfTypeArguments -> true
                        is CangJieConstraintSystemDiagnostic -> {
                            val otherError = it.error
                            (otherError is ConstrainingTypeIsError && otherError.typeVariable == error.typeVariable)
                                    || (otherError is ConstraintMismatch && otherError.involvesTypeVariable(error.typeVariable))
                        }
                        else -> false
                    }
                }

                if (isDiagnosticRedundant) return

                // 确定错误报告位置
                val expression = when (val atom = error.resolvedAtom.atom) {
                    is PSICangJieCall -> {
                        val psiCall = atom.psiCall
                        if (psiCall is CallTransformer.CallForImplicitInvoke) {
                            psiCall.outerCall.calleeExpression
                        } else {
                            psiCall.calleeExpression
                        }
                    }
                    is PSICangJieCallArgument -> atom.valueArgument.getArgumentExpression()
                    else -> call.calleeExpression
                } ?: return

                // 特殊函数的错误报告
                if (isSpecialFunction(resolvedAtom)) {
                    // 在特殊调用的某些参数上局部报告错误
                    reportNotEnoughInformationForTypeParameterForSpecialCall(resolvedAtom, error)
                } else {
                    // 获取类型变量信息
                    val (typeVariable, typeVariableName) = when (val typeVariable = error.typeVariable) {
                        is TypeVariableFromCallableDescriptor ->
                            typeVariable.originalTypeParameter to typeVariable.originalTypeParameter.name.asString()
                        is TypeVariableForLambdaReturnType ->
                            null to "return type of lambda"
                        else -> error("Unsupported type variable: $typeVariable")
                    }

                    // 解包块表达式
                    val unwrappedExpression = if (expression is CjBlockExpression) {
                        expression.statements.lastOrNull() ?: expression
                    } else expression

                    val diagnostic = NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER

                    // 数组字面量的特殊处理
                    if (unwrappedExpression is CjCollectionLiteralExpression && diagnostic == NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER) {
                        trace.reportDiagnosticOnce(
                            ARRAY_LITERAL_TYPE_INFERENCE_FAILED.on(
                                unwrappedExpression
                            )
                        )
                    } else {
                        trace.reportDiagnosticOnce(
                            diagnostic.on(
                                unwrappedExpression,
                                typeVariableName,
                                typeVariable?.containingDeclaration
                            )
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 只有输入类型的诊断
            // ─────────────────────────────────────────────────────────────────
            is OnlyInputTypesDiagnostic -> {
                error.typeVariable as? TypeVariableFromCallableDescriptor ?: return
                psiCangJieCall.psiCall.calleeExpression?.let {
                    TODO()
                    // 待实现：TYPE_INFERENCE_ONLY_INPUT_TYPES
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 空交集类型推断
            // ─────────────────────────────────────────────────────────────────
            is InferredEmptyIntersectionError, is InferredEmptyIntersectionWarning -> {
                val typeVariable = (error as InferredEmptyIntersection).typeVariable
                psiCangJieCall.psiCall.calleeExpression?.let { expression ->
                    val typeVariableText =
                        (typeVariable as? TypeVariableFromCallableDescriptor)?.originalTypeParameter?.name?.asString()
                            ?: typeVariable.toString()

                    @Suppress("UNCHECKED_CAST")
                    val incompatibleTypes = error.incompatibleTypes as List<CangJieType>

                    @Suppress("UNCHECKED_CAST")
                    val causingTypes = error.causingTypes as List<CangJieType>
                    val causingTypesText =
                        if (incompatibleTypes == causingTypes) "" else ": ${causingTypes.joinToString()}"

                    val diagnostic = if (error.kind.isDefinitelyEmpty) {
                        TODO()
                        // 待实现：INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION
                    } else {
                        INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION.on(
                            expression, typeVariableText,
                            incompatibleTypes, error.kind.description, causingTypesText
                        )
                    }

                    trace.reportDiagnosticOnce(diagnostic)
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // 约束类型是错误类型：已在其他地方报告
            // ─────────────────────────────────────────────────────────────────
            is ConstrainingTypeIsError -> {
                // 不报告，因为错误类型本身已经报告过了
            }

            // ─────────────────────────────────────────────────────────────────
            // 为保持兼容性降低优先级：不需要报告
            // ─────────────────────────────────────────────────────────────────
            is LowerPriorityToPreserveCompatibility -> {
                // 不报告
            }

            // ─────────────────────────────────────────────────────────────────
            // 无成功分支：不应该被调用
            // ─────────────────────────────────────────────────────────────────
            is NoSuccessfulFork -> shouldNotBeCalled()

            // ─────────────────────────────────────────────────────────────────
            // NotEnoughInformationForTypeParameter 基类：不应该直接处理
            // ─────────────────────────────────────────────────────────────────
            is NotEnoughInformationForTypeParameter<*> -> {
                throw AssertionError("constraintError should not be called with ${error::class.java}")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 扩展属性 - 约束不匹配类型访问
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * 获取约束不匹配错误的上界类型（作为 CangJieType）
 *
 * 约束不匹配错误中的 upperType 是 CangJieTypeMarker 类型，
 * 此扩展属性将其转换为 CangJieType 以便于使用。
 */
val ConstraintMismatch.upperCangJieType get() = upperType as CangJieType

/**
 * 获取约束不匹配错误的下界类型（作为 CangJieType）
 *
 * 约束不匹配错误中的 lowerType 是 CangJieTypeMarker 类型，
 * 此扩展属性将其转换为 CangJieType 以便于使用。
 */
val ConstraintMismatch.lowerCangJieType get() = lowerType as CangJieType