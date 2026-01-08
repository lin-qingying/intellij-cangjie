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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.resolve.calls.components.*
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.inference.NewConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.typeForTypeVariable
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.types.TypeConstructor

/**
 * 调用信息基类
 *
 * 封装了函数调用解析过程中的关键信息，包括解析结果、上下文和追踪策略。
 *
 * @property callResolutionResult 单次调用解析结果，可能是部分解析或完整解析
 * @property context 基础调用解析上下文，包含作用域、预期类型等信息
 * @property tracingStrategy 追踪策略，用于诊断和错误报告
 */
abstract class CallInfo(
    open val callResolutionResult: SingleCallResolutionResult,
    val context: BasicCallResolutionContext,
    val tracingStrategy: TracingStrategy
)

/**
 * PSI 部分调用信息
 *
 * 表示尚未完成类型推导的调用解析信息，包含部分约束系统。
 * 常见场景：泛型函数调用时类型参数尚未完全确定。
 *
 * 例如：
 * ```
 * func identity<T>(x: T): T { return x }
 * let result = identity(42)  // T 需要被推导为 Int64
 * ```
 */
class PSIPartialCallInfo(
    override val callResolutionResult: PartialCallResolutionResult,
    context: BasicCallResolutionContext,
    tracingStrategy: TracingStrategy
) : CallInfo(callResolutionResult, context, tracingStrategy), PartialCallInfo

/**
 * PSI 完整调用信息
 *
 * 表示已完成类型推导的调用解析信息，所有类型参数已确定。
 *
 * @property resolvedCall 完全解析的调用，包含确定的参数映射和类型替换
 */
class PSICompletedCallInfo(
    override val callResolutionResult: CompletedCallResolutionResult,
    context: BasicCallResolutionContext,
    val resolvedCall: NewAbstractResolvedCall<*>,
    tracingStrategy: TracingStrategy
) : CallInfo(callResolutionResult, context, tracingStrategy), CompletedCallInfo

/**
 * PSI 错误调用信息
 *
 * 表示解析失败的调用信息，包含重载解析的错误结果。
 * 常见场景：找不到匹配的函数、参数类型不匹配、歧义调用等。
 *
 * @param D 可调用描述符类型（函数、构造器等）
 * @property result 重载解析结果，包含所有候选和失败原因
 */
class PSIErrorCallInfo<D : CallableDescriptor>(
    override val callResolutionResult: CallResolutionResult,
    val result: OverloadResolutionResults<D>
) : ErrorCallInfo

/**
 * 基于桩类型的类型推导会话
 *
 * 这是仓颉语言类型推导系统的核心类，负责管理函数调用的类型推导过程。
 * 采用约束求解策略，支持泛型类型参数推导、延迟参数分析和嵌套推导。
 *
 * ## 工作流程
 *
 * 1. **收集候选**：通过 [addPartialCallInfo] 收集部分解析的调用信息
 * 2. **构建约束系统**：为每个候选构建类型约束（相等性、子类型等）
 * 3. **完成推导**：通过 [resolveCandidates] 运行约束系统完成器
 * 4. **处理错误**：通过 [addErrorCallInfo] 收集解析失败的候选
 *
 * ## 关键概念
 *
 * - **Stub Type（桩类型）**：类型推导过程中的临时占位类型
 * - **Constraint System（约束系统）**：类型变量和约束的集合
 * - **Atom（原子）**：推导过程中的最小单元（调用、lambda、可调用引用等）
 * - **Postponed Arguments（延迟参数）**：需要在上下文确定后才能分析的参数（如 lambda）
 *
 * ## 示例场景
 *
 * ```
 * // 场景 1: 简单泛型推导
 * func identity<T>(x: T): T { return x }
 * let result = identity(42)  // 推导 T = Int64
 *
 * // 场景 2: 延迟参数推导（lambda）
 * func map<T, R>(list: Array<T>, transform: (T) -> R): Array<R>
 * let numbers = [1, 2, 3]
 * let strings = map(numbers, { x => x.toString() })  // 推导 T = Int64, R = String
 *
 * // 场景 3: 重载解析
 * func process(x: Int64): String
 * func process(x: Float64): String
 * let result = process(42)  // 选择第一个重载
 * ```
 *
 * @param D 可调用描述符类型（FunctionDescriptor、ConstructorDescriptor 等）
 * @param psiCallResolver PSI 调用解析器，用于将解析结果转换为重载解析结果
 * @param postponedArgumentsAnalyzer 延迟参数分析器，处理 lambda 和可调用引用
 * @param cangjieConstraintSystemCompleter 约束系统完成器，执行类型推导算法
 * @param callComponents 调用组件集合，提供类型细化、约束注入等服务
 * @param builtIns 内置类型系统，提供基本类型定义
 */
abstract class StubTypesBasedInferenceSession<D : CallableDescriptor>(
    private val psiCallResolver: PSICallResolver,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    protected val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    protected val callComponents: CangJieCallComponents,
    val builtIns: CangJieBuiltIns
) : InferenceSession {
    /** 部分解析的调用列表（类型尚未完全确定） */
    protected val commonPartiallyResolvedCalls = arrayListOf<PSIPartialCallInfo>()

    /** 解析失败的调用列表（参数不匹配、找不到候选等） */
    val errorCallsInfo = arrayListOf<PSIErrorCallInfo<D>>()

    /** 已完成推导的原子集合，用于避免重复处理 */
    private val completedCalls = hashSetOf<ResolvedAtom>()

    /** 嵌套推导会话集合（用于处理嵌套泛型调用） */
    protected val nestedInferenceSessions = hashSetOf<StubTypesBasedInferenceSession<*>>()

    /**
     * 添加嵌套推导会话
     *
     * 当函数调用的参数本身也是泛型调用时，会创建嵌套推导会话。
     * 例如：`map(filter(list, { x => x > 0 }), { y => y * 2 })`
     *
     * @param inferenceSession 嵌套的推导会话
     */
    fun addNestedInferenceSession(inferenceSession: StubTypesBasedInferenceSession<*>) {
        nestedInferenceSessions.add(inferenceSession)
    }

    /**
     * 完成前的准备工作（钩子方法）
     *
     * 在运行约束系统完成器之前执行的准备工作，子类可以覆盖此方法
     * 添加额外的约束或进行特殊处理。
     *
     * @param commonSystem 公共约束系统，包含所有候选的合并约束
     * @param resolvedCallsInfo 待完成的部分调用信息列表
     */
    open fun prepareForCompletion(commonSystem: NewConstraintSystem, resolvedCallsInfo: List<PSIPartialCallInfo>) {
        // 默认不做任何操作，子类可覆盖
    }

    /**
     * 判断是否应该立即运行完成器
     *
     * 对于某些特殊候选（如扩展函数、运算符重载），可能需要立即完成推导。
     * 默认返回 false，表示延迟到所有候选收集完成后统一处理。
     *
     * @param candidate 待判断的解析候选
     * @return true 表示立即运行完成器，false 表示延迟处理
     */
    override fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean {
        return false
    }

    /**
     * 添加部分调用信息
     *
     * 在候选解析过程中，当找到匹配的函数但类型推导尚未完成时调用。
     * 此方法收集所有待推导的调用信息，等待统一完成推导。
     *
     * @param callInfo 部分调用信息，必须是 PSIPartialCallInfo 类型
     * @throws AssertionError 如果传入的不是 PSIPartialCallInfo 实例
     */
    override fun addPartialCallInfo(callInfo: PartialCallInfo) {
        if (callInfo !is PSIPartialCallInfo) {
            throw AssertionError("Call info for $callInfo should be instance of PSIPartialCallInfo")
        }
        commonPartiallyResolvedCalls.add(callInfo)
    }

    /**
     * 添加完整调用信息
     *
     * 当调用的类型推导已经完成时调用。默认实现不做任何处理，
     * 因为完整调用不需要进一步推导。子类可以覆盖此方法记录完整调用信息。
     *
     * @param callInfo 完整调用信息
     */
    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {
        // 不做任何操作，完整调用已经推导完成
    }

    /**
     * 添加错误调用信息
     *
     * 当候选解析失败时调用，例如参数类型不匹配、找不到候选函数等。
     * 这些错误信息会被保留用于生成诊断消息。
     *
     * @param callInfo 错误调用信息，必须是 PSIErrorCallInfo 类型
     * @throws AssertionError 如果传入的不是 PSIErrorCallInfo 实例
     */
    override fun addErrorCallInfo(callInfo: ErrorCallInfo) {
        if (callInfo !is PSIErrorCallInfo<*>) {
            throw AssertionError("Error call info for $callInfo should be instance of PSIErrorCallInfo")
        }
        @Suppress("UNCHECKED_CAST")
        errorCallsInfo.add(callInfo as PSIErrorCallInfo<D>)
    }

    /**
     * 获取当前约束系统
     *
     * 返回最后一个部分调用的约束系统存储，如果没有部分调用则返回空存储。
     * 这个方法用于嵌套推导会话获取父会话的约束上下文。
     *
     * @return 当前约束系统的存储，如果没有则返回 [ConstraintStorage.Empty]
     */
    override fun currentConstraintSystem(): ConstraintStorage {
        return commonPartiallyResolvedCalls.lastOrNull()?.callResolutionResult?.constraintSystem?.getBuilder()
            ?.currentStorage()
            ?: ConstraintStorage.Empty
    }

    /**
     * 标记调用为已完成
     *
     * 返回 true 表示该原子之前已经完成过推导，false 表示首次完成。
     * 用于避免对同一个调用重复进行类型推导。
     *
     * @param resolvedAtom 待标记的解析原子
     * @return true 表示已经完成过，false 表示首次完成
     */
    override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean =
        !completedCalls.add(resolvedAtom)

    /**
     * 判断是否应该完成子原子
     *
     * 确定在完成当前调用原子时，是否应该递归完成其子原子（如 lambda 参数）。
     * 默认返回 true，表示递归完成所有子原子。
     *
     * @param resolvedCallAtom 待判断的调用原子
     * @return true 表示应该完成子原子，false 表示跳过
     */
    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true

    /**
     * 解析候选并完成类型推导
     *
     * 这是类型推导系统的核心方法，负责对所有收集到的候选执行类型推导并选择最佳候选。
     *
     * ## 算法流程
     *
     * 1. **检测候选状态**：判断是否存在"一个成功 + 一个失败"的候选组合
     * 2. **选择完成策略**：
     *    - 策略 A：对每个候选单独运行约束系统完成器（处理部分成功的情况）
     *    - 策略 B：使用公共约束系统统一完成所有候选（正常情况）
     * 3. **运行约束完成器**：
     *    - 固定类型变量（确定泛型参数的具体类型）
     *    - 分析延迟参数（lambda、可调用引用等）
     *    - 收集诊断信息（类型不匹配、缺失约束等）
     * 4. **转换结果**：将内部解析结果转换为 IDE 可用的重载解析结果
     *
     * ## 特殊情况处理
     *
     * ### 场景 1: 委托属性的 getValue/setValue
     *
     * 考虑以下代码：
     * ```
     * var x by lazy { "" }
     * ```
     *
     * 这里 `lazy` 返回一个泛型委托，它有：
     * - `getValue` 方法（总是成功）
     * - `setValue` 方法（可能失败，因为 lazy 是只读的）
     *
     * 问题：两个方法共享同一个接收者原子和类型变量。如果先完成 `getValue` 的推导，
     * 类型变量被固定后，`setValue` 的推导会因为找不到类型变量而失败。
     *
     * 解决方案：当处理失败的候选时，从成功候选的约束系统中提取固定类型变量的
     * 相等性约束，并创建桩原子（StubResolvedAtom）以满足完成器的要求。
     *
     * ### 场景 2: 重载函数的类型推导
     *
     * 考虑以下代码：
     * ```
     * func process<T>(x: T): String
     * func process<T>(x: T, format: (T) -> String): String
     * process(42) { it.toString() }
     * ```
     *
     * 两个重载都可能匹配，需要分别完成类型推导后比较优先级。
     *
     * @param resolutionCallbacks 解析回调，用于处理延迟参数和生成诊断
     * @return 所有候选的解析结果列表，包含重载解析结果和诊断信息
     */
    fun resolveCandidates(resolutionCallbacks: CangJieResolutionCallbacks): List<ResolutionResultCallInfo<D>> {
        val resolvedCallsInfo = commonPartiallyResolvedCalls.toList()

        val diagnosticHolder = CangJieDiagnosticsHolder.SimpleHolder()

        // 检测是否存在"一个成功 + 一个失败"的候选组合
        // 这种情况需要特殊处理以避免类型变量共享问题
        val hasOneSuccessfulAndOneErrorCandidate = if (resolvedCallsInfo.size > 1) {
            val hasErrors = resolvedCallsInfo.map {
                it.callResolutionResult.constraintSystem.errors.isNotEmpty() || it.callResolutionResult.diagnostics.isNotEmpty()
            }
            hasErrors.any { it } && !hasErrors.all { it }
        } else {
            false
        }

        /**
         * 运行约束系统完成器
         *
         * @param constraintSystem 待完成的约束系统
         * @param atoms 待分析的原子列表（调用、lambda 等）
         */
        fun runCompletion(constraintSystem: NewConstraintSystem, atoms: List<ResolvedAtom>) {
            val completionMode = ConstraintSystemCompletionMode.FULL
            cangjieConstraintSystemCompleter.runCompletion(
                constraintSystem.asConstraintSystemCompleterContext(),
                completionMode,
                atoms,
                builtIns.unitType,
                diagnosticHolder
            ) {
                // 分析延迟参数（如 lambda 表达式）
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    resolutionCallbacks,
                    it,
                    completionMode,
                    diagnosticHolder
                )
            }

        }

        val allCandidates = arrayListOf<ResolutionResultCallInfo<D>>()

        // 策略 A：对成功和失败的候选分别运行完成器
        // 用于处理委托属性等特殊场景，避免类型变量共享问题
        if (hasOneSuccessfulAndOneErrorCandidate) {
            // 找到成功的候选（没有错误和诊断）
            val goodCandidate = resolvedCallsInfo.first {
                it.callResolutionResult.constraintSystem.errors.isEmpty() && it.callResolutionResult.diagnostics.isEmpty()
            }
            // 找到失败的候选（有错误或诊断）
            val badCandidate = resolvedCallsInfo.first {
                it.callResolutionResult.constraintSystem.errors.isNotEmpty() || it.callResolutionResult.diagnostics.isNotEmpty()
            }

            for (callInfo in listOf(goodCandidate, badCandidate)) {
                val atomsToAnalyze = mutableListOf<ResolvedAtom>(callInfo.callResolutionResult)
                val system = NewConstraintSystemImpl(
                    callComponents.constraintInjector,
                    builtIns,
                    callComponents.cangjieTypeRefiner,
                    callComponents.languageVersionSettings
                ).apply {
                    // 复制候选的约束系统
                    addOtherSystem(callInfo.callResolutionResult.constraintSystem.getBuilder().currentStorage())

                    /*
                     * 处理委托属性的 getValue/setValue 场景
                     *
                     * 问题：当我们有一个委托（如 var x by lazy { "" }），它的 `getValue` 是好的，
                     * `setValue` 是坏的，但它们是由带有泛型的函数调用提供的。我们想分别完成
                     * `getValue` 和 `setValue` 的候选，这样 `setValue` 的诊断不会泄漏到
                     * `getValue` 的解析调用中，但两个调用可能有同一个接收者原子（以及其中的
                     * 类型变量）。在完成第一个调用后，第二个调用的完成会失败，因为它的原子
                     * 不包含接收者的类型变量（它们已经在第一个调用中完成了）。
                     *
                     * 解决方案：我们从第一个调用添加相等性约束到第二个调用的系统中，并创建
                     * 桩原子，这样完成器调用就不会失败。
                     */
                    if (callInfo === badCandidate) {
                        val storage = allCandidates[0].resolutionResult.constraintSystem.getBuilder().currentStorage()
                        for ((typeVariable, fixedType) in storage.fixedTypeVariables) {
                            if (typeVariable in this.notFixedTypeVariables) {
                                val type = (typeVariable as TypeConstructor).typeForTypeVariable()
                                // 添加从成功候选获取的类型变量的相等性约束
                                addEqualityConstraint(
                                    type,
                                    fixedType,
                                    SimpleConstraintSystemConstraintPosition
                                )
                                // 创建桩原子以满足完成器要求
                                atomsToAnalyze += StubResolvedAtom(typeVariable)
                            }
                        }
                    }
                }
                runCompletion(system, atomsToAnalyze)
                val resolutionResult = callInfo.asCallResolutionResult(diagnosticHolder, system)
                allCandidates += ResolutionResultCallInfo(
                    resolutionResult,
                    psiCallResolver.convertToOverloadResolutionResults(
                        callInfo.context,
                        resolutionResult,
                        callInfo.tracingStrategy
                    )
                )
            }
        } else {
            // 策略 B：使用公共约束系统统一完成所有候选
            // 这是正常情况，所有候选要么都成功，要么都失败
            val commonSystem = NewConstraintSystemImpl(
                callComponents.constraintInjector,
                builtIns,
                callComponents.cangjieTypeRefiner,
                callComponents.languageVersionSettings
            ).apply {
                // 将当前会话的约束系统添加到公共系统
                addOtherSystem(currentConstraintSystem())
            }

            // 子类可以在此添加额外的准备工作
            prepareForCompletion(commonSystem, resolvedCallsInfo)

            // 统一运行完成器处理所有候选
            runCompletion(commonSystem, resolvedCallsInfo.map { it.callResolutionResult })

            // 将每个候选转换为解析结果
            resolvedCallsInfo.mapTo(allCandidates) {
                val resolutionResult = it.asCallResolutionResult(diagnosticHolder, commonSystem)
                ResolutionResultCallInfo(
                    resolutionResult,
                    psiCallResolver.convertToOverloadResolutionResults(it.context, resolutionResult, it.tracingStrategy)
                )
            }
        }

        // 添加之前收集的错误调用信息（如找不到候选、参数不匹配等）
        val results = allCandidates.map { it.resolutionResult }
        errorCallsInfo.filter { it.callResolutionResult !in results }.mapTo(allCandidates) {
            ResolutionResultCallInfo(it.callResolutionResult, it.result)
        }
        return allCandidates
    }

    /**
     * 计算约束系统完成模式
     *
     * 确定如何完成候选的类型推导。不同的完成模式会影响推导的行为：
     * - FULL: 完全推导所有类型变量
     * - PARTIAL: 部分推导，保留某些类型变量
     * - UNTIL_FIRST_LAMBDA: 推导到第一个 lambda 参数为止
     *
     * 默认返回 null，表示使用默认的完成模式。
     *
     * @param candidate 待计算的解析候选
     * @return 完成模式，null 表示使用默认模式
     */
    override fun computeCompletionMode(candidate: ResolutionCandidate): ConstraintSystemCompletionMode? = null

    /**
     * 判断是否独立解析接收者
     *
     * 确定是否应该在解析调用之前先独立解析接收者表达式。
     * 默认返回 false，表示在调用解析过程中一起处理接收者。
     *
     * @return true 表示独立解析接收者，false 表示一起处理
     */
    override fun resolveReceiverIndependently(): Boolean = false

    /**
     * 将部分调用信息转换为完整的调用解析结果
     *
     * 合并约束系统的诊断信息和错误，创建完整的解析结果。
     *
     * @param diagnosticsHolder 诊断信息持有者，包含类型推导过程中产生的诊断
     * @param commonSystem 公共约束系统，包含最终的类型推导结果
     * @return 完整的调用解析结果
     */
    private fun PartialCallInfo.asCallResolutionResult(
        diagnosticsHolder: CangJieDiagnosticsHolder.SimpleHolder,
        commonSystem: NewConstraintSystem
    ): CallResolutionResult {
        // 合并三种来源的诊断信息：
        // 1. 诊断持有者中的诊断（类型推导过程中产生）
        // 2. 部分调用结果的原始诊断
        // 3. 约束系统的错误转换为诊断
        val diagnostics =
            diagnosticsHolder.getDiagnostics() + callResolutionResult.diagnostics + commonSystem.errors.asDiagnostics()
        return CompletedCallResolutionResult(callResolutionResult.resultCallAtom, diagnostics, commonSystem)
    }
}

/**
 * 解析结果调用信息
 *
 * 封装了类型推导的最终结果，包含内部解析结果和转换后的重载解析结果。
 *
 * @param D 可调用描述符类型
 * @property resolutionResult 内部调用解析结果，包含约束系统和诊断信息
 * @property overloadResolutionResults 重载解析结果，供 IDE 使用（代码补全、高亮等）
 */
data class ResolutionResultCallInfo<D : CallableDescriptor>(
    val resolutionResult: CallResolutionResult,
    val overloadResolutionResults: OverloadResolutionResults<D>
)
