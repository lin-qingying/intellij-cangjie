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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.cangnova.cangjie.resolve.calls.inference.model.ExpectedTypeConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.CandidateFactory
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.tower.forceResolution
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.error.ErrorTypeKind

/**
 * 从约束系统获取内置类型定义
 *
 * 扩展属性，用于从约束系统实现中提取仓颉语言的内置类型（如 Int、String、Unit 等）
 */
internal val ConstraintSystem.builtIns: CangJieBuiltIns
    get() = ((this as ConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns

/**
 * 仓颉调用补全器
 *
 * 负责处理函数调用解析的最后阶段——补全（Completion）。
 * 补全阶段的主要任务包括：
 * 1. 固定所有类型变量（完成类型推断）
 * 2. 分析延迟参数（lambda 表达式等）
 * 3. 收集诊断信息（错误、警告）
 * 4. 生成最终的解析结果
 *
 * ## 工作流程
 *
 * 调用解析通常分为多个阶段：
 * 1. **候选收集阶段**: 找到所有可能的重载候选
 * 2. **约束收集阶段**: 为每个候选收集类型约束
 * 3. **补全阶段 (本类负责)**: 完成类型推断，分析延迟参数，生成最终结果
 *
 * ## 补全模式
 *
 * 补全可以在不同模式下进行：
 * - **FULL**: 完全补全，固定所有类型变量，分析所有延迟参数
 * - **PARTIAL**: 部分补全，保留一些类型变量未固定，用于推断会话
 * - **UNTIL_FIRST_LAMBDA**: 遇到第一个 lambda 就停止，用于特定的推断场景
 *
 * @property trivialConstraintTypeInferenceOracle 简单约束类型推断预言器
 *   - 用于判断是否可以通过简单的约束推断出类型
 *   - 避免不必要的复杂推断过程
 *
 * @property cangjieConstraintSystemCompleter 约束系统补全器
 *   - 执行实际的类型变量固定操作
 *   - 运行约束求解算法
 *
 * @property postponedArgumentsAnalyzer 延迟参数分析器
 *   - 分析 lambda 表达式、可调用引用等延迟参数
 *   - 在类型上下文明确后进行分析
 */
class CangJieCallCompleter(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) {
    /**
     * 准备候选项以进行补全
     *
     * 在正式开始补全之前，需要做一些准备工作：
     * 1. 从候选集合中选择唯一的候选（或创建错误候选）
     * 2. 如果候选包含 lambda 参数，需要提前绑定描述符
     * 3. 禁用不必要的合约检查
     *
     * ## 为什么需要提前绑定描述符？
     *
     * 当函数参数包含 lambda 时，lambda 内部可能使用非局部返回（non-local return）。
     * 为了正确检查非局部返回，需要知道外层函数的描述符。
     * 因此，在分析 lambda 之前，必须先绑定外层调用的描述符。
     *
     * 示例：
     * ```kotlin
     * fun foo(block: () -> Unit) {
     *     block()
     * }
     *
     * fun bar() {
     *     foo {
     *         return  // 非局部返回，返回到 bar，不是 foo
     *     }
     * }
     * ```
     *
     * @param factory 候选工厂，用于创建错误候选
     * @param candidates 候选集合（理想情况下只有一个候选）
     * @param resolutionCallbacks 解析回调，提供绑定描述符等操作
     * @return 准备好的候选项，如果没有有效候选则返回错误候选
     */
    private fun prepareCandidateForCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: Collection<ResolutionCandidate>,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): ResolutionCandidate {
        // 尝试获取唯一的候选项
        val candidate = candidates.singleOrNull()

        // 对于包含 lambda 参数的调用，需要提前绑定描述符
        candidate?.resolvedCall?.let {
            // 检查是否有 lambda 参数
            val mayNeedDescriptor = it.argumentToCandidateParameter.keys.any { arg ->
                arg is LambdaCangJieCallArgument
            }

            if (mayNeedDescriptor) {
                // 绑定桩解析调用，用于非局部返回检查
                resolutionCallbacks.bindStubResolvedCallForCandidate(it)
            }

            // 如果需要，禁用合约（Contracts）
            // 合约是 Kotlin 的一个特性，用于向编译器提供额外的类型信息
            resolutionCallbacks.disableContractsIfNecessary(it)
        }

        // 如果没有候选，创建一个错误候选并强制解析
        return candidate ?: factory.createErrorCandidate().forceResolution()
    }

    /**
     * 创建所有候选项的解析结果
     *
     * 用于 "all candidates" 模式，在这种模式下：
     * - 不选择最佳候选，而是保留所有候选
     * - 为每个候选执行补全并收集诊断信息
     * - 用于提供详细的错误信息，说明为什么每个候选不适用
     *
     * ## 使用场景
     *
     * 当重载解析失败（有多个同样好的候选或没有合适的候选）时，
     * IDE 需要显示所有候选及其失败原因，以帮助开发者理解问题。
     *
     * 示例：
     * ```kotlin
     * fun foo(x: Int) {}
     * fun foo(x: String) {}
     *
     * foo(1.0)  // 类型不匹配，需要显示两个候选都失败的原因
     * ```
     *
     * @param candidates 所有候选项
     * @param expectedType 期望类型（从调用上下文推断）
     * @param resolutionCallbacks 解析回调
     * @return 包含所有候选及其诊断信息的结果
     */
    fun createAllCandidatesResult(
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult {
        val completedCandidates = candidates.map { candidate ->
            val diagnosticsHolder = CangJieDiagnosticsHolder.SimpleHolder()

            // 添加期望类型约束
            candidate.addExpectedTypeConstraint(
                candidate.substitutedReturnType(), expectedType
            )

            // 执行补全（在 all candidates 模式下）
            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true  // 关键：启用 all candidates 模式
            )

            // 返回候选及其诊断信息
            CandidateWithDiagnostics(candidate, diagnosticsHolder.getDiagnostics() + candidate.diagnostics)
        }

        return AllCandidatesResolutionResult(completedCandidates, resolutionCallbacks.createEmptyConstraintSystem())
    }

    /**
     * 检查是否为带有可变参数的 SAM 转换，并在适用情况下添加诊断信息
     *
     * ## SAM 转换
     *
     * SAM (Single Abstract Method) 转换允许将 lambda 转换为只有一个抽象方法的接口：
     * ```kotlin
     * interface Callback {
     *     fun onComplete()
     * }
     *
     * fun register(callback: Callback) {}
     *
     * register { println("Done") }  // lambda 自动转换为 Callback
     * ```
     *
     * ## 可变参数问题
     *
     * 在某些情况下，SAM 转换与可变参数（vararg）组合使用可能导致问题：
     * ```kotlin
     * fun foo(callback: Callback, vararg args: String) {}
     *
     * foo({ }, "a", "b")  // 在旧版本中可能有歧义
     * ```
     *
     * 此方法检测这种情况并添加警告。
     *
     * @param diagnosticHolder 诊断信息持有者
     */
    private fun ResolutionCandidate.checkSamWithVararg(diagnosticHolder: CangJieDiagnosticsHolder.SimpleHolder) {
        // 检查语言版本是否支持相关特性
        // val samConversionPerArgumentWithWarningsForVarargAfterSam =
        //     callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
        //     !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

        // 检查描述符是否为合成成员（SAM 转换产生）
        // val descriptor = resolvedCall.descriptor
        // if (descriptor is SyntheticMemberDescriptor<*>) {
        //     val declarationDescriptor = descriptor.baseDescriptorForSynthetic as? FunctionDescriptor ?: return
        //
        //     // 检查最后一个参数是否为 vararg
        //     if (declarationDescriptor.valueParameters.lastOrNull()?.isVararg == true) {
        //         diagnosticHolder.addDiagnostic(
        //             ResolvedToSamWithVarargDiagnostic(resolvedCall.atom.argumentsInParenthesis.lastOrNull() ?: return)
        //         )
        //     }
        // }
    }

    /**
     * 计算替换后的返回类型
     *
     * 函数的返回类型可能包含泛型参数，需要经过多次替换才能得到具体类型：
     *
     * ## 替换步骤
     *
     * 1. **新鲜变量替换**: 将声明中的类型参数替换为类型推断中的新鲜类型变量
     *    ```kotlin
     *    fun <T> foo(): T  // 声明
     *    // 推断时: T -> α (新鲜类型变量)
     *    ```
     *
     * 2. **结果替换**: 将新鲜类型变量替换为推断出的具体类型
     *    ```kotlin
     *    val x: Int = foo()
     *    // α -> Int (从上下文推断)
     *    ```
     *
     * ## 示例
     *
     * ```kotlin
     * fun <T> identity(x: T): T = x
     *
     * val result: String = identity("hello")
     * // 1. 返回类型: T
     * // 2. 新鲜变量替换: α
     * // 3. 结果替换: String
     * ```
     *
     * @return 完全替换后的返回类型，如果无法替换则返回 null
     */
    private fun ResolutionCandidate.substitutedReturnType(): UnwrappedType? {
        // 获取候选函数的原始返回类型
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null

        // 第一步：应用新鲜变量替换器
        val substitutedReturnTypeWithVariables =
            (resolvedCall.freshVariablesSubstitutor as ComposableTypeSubstitutor).safeSubstitute(returnType)

        // 第二步：应用结果替换器（基于推断结果）
        return (getResultingSubstitutor() as ComposableTypeSubstitutor).safeSubstitute(
            substitutedReturnTypeWithVariables
        ) ?: substitutedReturnTypeWithVariables  // 如果失败，返回第一步的结果
    }

    /**
     * 计算替换后的反射类型
     *
     * 用于可调用引用的类型推断。可调用引用有特殊的反射类型：
     *
     * ## 反射类型示例
     *
     * ```kotlin
     * fun foo(x: Int): String = x.toString()
     *
     * val ref: (Int) -> String = ::foo
     * //       ^^^^^^^^^^^^^^^^ 反射类型：函数类型
     * ```
     *
     * 对于泛型函数：
     * ```kotlin
     * fun <T> identity(x: T): T = x
     *
     * val ref = ::identity
     * // 反射类型: (T) -> T
     * // 替换后: (α) -> α (新鲜类型变量)
     * ```
     *
     * @return 替换后的反射类型
     */
    private fun CallableReferenceResolutionCandidate.substitutedReflectionType(): UnwrappedType {
        return resolvedCall.freshVariablesSubstitutor.safeSubstitute(this.reflectionCandidateType)
    }

    /**
     * 执行补全操作以解析给定的调用原子
     *
     * 这是补全过程的核心方法，协调整个补全流程：
     *
     * ## 补全流程
     *
     * 1. **确定返回类型**: 使用新鲜返回类型或默认 Unit 类型
     * 2. **运行约束系统补全**: 固定类型变量，求解约束
     * 3. **分析延迟参数**: 分析 lambda、可调用引用等
     * 4. **收集错误**: 从约束系统中提取错误信息
     * 5. **检测递归类型**: 防止类型推断陷入无限递归
     *
     * ## 示例
     *
     * ```kotlin
     * fun <T> process(x: T, f: (T) -> Unit) {}
     *
     * process(42) { it.toString() }
     * // 1. 返回类型: Unit
     * // 2. 固定 T = Int
     * // 3. 分析 lambda: { it.toString() }
     * //    - it: Int
     * //    - it.toString(): String
     * ```
     *
     * @param resolvedCallAtom 已解析的调用原子
     * @param completionMode 补全模式（FULL/PARTIAL）
     * @param diagnosticsHolder 诊断信息收集器
     * @param constraintSystem 约束系统
     * @param resolutionCallbacks 解析回调
     * @param collectAllCandidatesMode 是否为 all candidates 模式
     */
    private fun runCompletion(
        resolvedCallAtom: ResolvedCallAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        constraintSystem: ConstraintSystem,
        resolutionCallbacks: CangJieResolutionCallbacks,
        collectAllCandidatesMode: Boolean = false
    ) {
        // 确定返回类型（如果没有则使用 Unit）
        val returnType = resolvedCallAtom.freshReturnType ?: constraintSystem.builtIns.unitType

        // 运行约束系统补全器
        cangjieConstraintSystemCompleter.runCompletion(
            constraintSystem.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(resolvedCallAtom),
            returnType,
            diagnosticsHolder
        ) {
            // 补全完成后的回调
            if (collectAllCandidatesMode) {
                // all candidates 模式：不分析子原子，设置空结果
                it.setEmptyAnalyzedResults()
            } else {
                // 正常模式：分析延迟参数（lambda、可调用引用等）
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    resolutionCallbacks,
                    it,
                    completionMode,
                    diagnosticsHolder
                )
            }
        }

        // 收集约束系统中的错误
        constraintSystem.errors.forEach(diagnosticsHolder::addError)

        // 检测递归类型（防止类型推断陷入无限循环）
        if (returnType is ErrorType && returnType.kind == ErrorTypeKind.RECURSIVE_TYPE) {
            diagnosticsHolder.addDiagnostic(TypeCheckerHasRanIntoRecursion)
        }
    }

    /**
     * ResolutionCandidate 的补全快捷方法
     *
     * 简化调用，自动从候选中提取必要的参数
     */
    private fun ResolutionCandidate.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
        resolutionCallbacks: CangJieResolutionCallbacks,
    ) {
        runCompletion(resolvedCall, completionMode, diagnosticHolder, getSystem(), resolutionCallbacks)
    }

    /**
     * 将解析候选转换为调用解析结果
     *
     * 根据候选的状态和补全模式，生成相应类型的解析结果：
     *
     * ## 结果类型
     *
     * 1. **ErrorCallResolutionResult**: 候选是错误候选（类型不匹配、找不到函数等）
     * 2. **CompletedCallResolutionResult**: 完全补全成功，所有类型都已确定
     * 3. **PartialCallResolutionResult**: 部分补全，留待推断会话继续处理
     *
     * ## 示例
     *
     * ```kotlin
     * // 错误候选
     * fun foo(x: Int) {}
     * foo("hello")  // -> ErrorCallResolutionResult
     *
     * // 完全补全
     * fun bar(x: Int): String = x.toString()
     * val s: String = bar(42)  // -> CompletedCallResolutionResult
     *
     * // 部分补全
     * fun <T> baz(x: T): T = x
     * baz(42)  // T 可能需要推断会话 -> PartialCallResolutionResult
     * ```
     *
     * @param type 补全模式
     * @param diagnosticsHolder 诊断信息持有者
     * @param forwardToInferenceSession 是否转发到推断会话
     * @return 相应类型的解析结果
     */
    fun ResolutionCandidate.asCallResolutionResult(
        type: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder.SimpleHolder,
        forwardToInferenceSession: Boolean = false
    ): CallResolutionResult {
        val constraintSystem = getSystem()
        val allDiagnostics = diagnosticsHolder.getDiagnostics() + diagnostics

        // 如果是错误候选，直接返回错误结果
        if (isErrorCandidate()) {
            return ErrorCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem)
        }

        // 根据补全模式返回相应的结果
        return if (type == ConstraintSystemCompletionMode.FULL) {
            CompletedCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem)
        } else {
            PartialCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem, forwardToInferenceSession)
        }
    }

    /**
     * 向解析候选添加期望类型约束
     *
     * 期望类型来自调用的上下文，用于引导类型推断：
     *
     * ## 期望类型来源
     *
     * 1. **变量声明**:
     *    ```kotlin
     *    val x: String = foo()  // 期望类型: String
     *    ```
     *
     * 2. **函数参数**:
     *    ```kotlin
     *    fun bar(s: String) {}
     *    bar(foo())  // 期望类型: String
     *    ```
     *
     * 3. **返回语句**:
     *    ```kotlin
     *    fun baz(): String {
     *        return foo()  // 期望类型: String
     *    }
     *    ```
     *
     * ## 约束类型
     *
     * - **子类型约束**: `返回类型 <: 期望类型`（通常情况）
     * - **相等约束**: `返回类型 = Unit`（当期望 Unit 时）
     * - **无约束**: 当没有不固定的类型变量时（避免重复错误）
     *
     * @param returnType 函数返回类型
     * @param expectedType 期望类型
     */
    private fun ResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?, expectedType: UnwrappedType?
    ) {
        // 如果返回类型为空，无需添加约束
        if (returnType == null) return

        // 如果期望类型为空或为 "无期望类型" 标记（且不是 UNIT_EXPECTED_TYPE），无需添加约束
        if (expectedType == null ||
            (TypeUtils.noExpectedType(expectedType) && expectedType !== TypeUtils.UNIT_EXPECTED_TYPE)) return

        val csBuilder = getSystem().getBuilder()

        // 根据情况决定添加何种约束
        when {
            // 情况 1: 没有不固定的类型变量
            csBuilder.currentStorage().notFixedTypeVariables.isEmpty() -> {
                // 不添加约束，避免在后续类型检查时产生多个重复错误
                // 这与旧的类型推断保持一致
            }

            // 情况 2: 期望类型为 Unit
            expectedType === TypeUtils.UNIT_EXPECTED_TYPE -> {
                // 添加相等约束: 返回类型 = Unit
                csBuilder.addEqualityConstraintIfCompatible(
                    returnType,
                    csBuilder.builtIns.unitType,
                    ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
                )
            }

            // 情况 3: 其他情况
            else -> {
                // 添加子类型约束: 返回类型 <: 期望类型
                csBuilder.addSubtypeConstraint(
                    returnType,
                    expectedType,
                    ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
                )
            }
        }
    }

    /**
     * 从类型转换表达式中添加期望类型约束
     *
     * 类型转换（as 表达式）提供了额外的类型信息：
     *
     * ## 示例
     *
     * ```kotlin
     * val x = foo() as String  // 从 "as String" 推断 foo() 的期望类型
     * ```
     *
     * 在某些情况下，类型转换的目标类型可以用作期望类型，
     * 帮助类型推断系统更好地推断泛型参数。
     *
     * ## 使用场景
     *
     * ```kotlin
     * fun <T> create(): T = TODO()
     *
     * val obj = create() as MyClass
     * // "as MyClass" 提示 T 应该是 MyClass
     * ```
     *
     * @param returnType 返回类型
     * @param resolutionCallbacks 解析回调（提供获取期望类型的方法）
     */
    private fun ResolutionCandidate.addExpectedTypeFromCastConstraint(
        returnType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ) {
        if (returnType == null) return

        // 从 as 表达式获取期望类型
        val expectedType = resolutionCallbacks.getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedCall)
            ?: return

        val csBuilder = getSystem().getBuilder()

        // 添加子类型约束
        csBuilder.addSubtypeConstraint(
            returnType,
            expectedType,
            ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
        )
    }

    /**
     * 执行补全操作并返回解析结果（主入口方法）
     *
     * 这是 CangJieCallCompleter 的主要公共接口，协调整个补全流程：
     *
     * ## 完整流程
     *
     * 1. **前置检查**: 验证候选集合（空、单个、多个）
     * 2. **准备候选**: 绑定描述符，准备补全
     * 3. **计算返回类型**:
     *    - 普通函数：替换后的返回类型
     *    - 可调用引用：反射类型
     * 4. **添加约束**:
     *    - 期望类型约束
     *    - 类型转换约束
     * 5. **确定补全模式**:
     *    - FULL: 完全补全
     *    - PARTIAL: 部分补全
     * 6. **执行补全**: 运行约束求解和参数分析
     * 7. **生成结果**: 返回相应类型的解析结果
     *
     * ## 示例
     *
     * ```kotlin
     * // 场景 1: 简单调用
     * fun foo(x: Int): String = x.toString()
     * val s: String = foo(42)
     * // -> CompletedCallResolutionResult
     *
     * // 场景 2: 带 lambda
     * fun <T> bar(x: T, f: (T) -> Unit) {}
     * bar(42) { println(it) }
     * // -> 可能是 PartialCallResolutionResult（需要分析 lambda）
     *
     * // 场景 3: 重载歧义
     * fun baz(x: Int) {}
     * fun baz(x: String) {}
     * baz(1.0)  // 类型不匹配
     * // -> 多个候选，添加 ManyCandidatesCallDiagnostic
     * ```
     *
     * @param factory 候选工厂
     * @param candidates 候选集合
     * @param expectedType 期望类型
     * @param resolutionCallbacks 解析回调
     * @return 调用解析结果
     */
    fun runCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: MutableCollection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticHolder = CangJieDiagnosticsHolder.SimpleHolder()

        // 步骤 1: 检查候选数量并添加相应诊断
        when {
            candidates.isEmpty() ->
                diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic())

            candidates.size > 1 ->
                diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(candidates.toList()))
        }

        // 步骤 2: 准备候选（或创建错误候选）
        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)

        // 步骤 3: 计算返回类型并添加约束
        val resultType = when (candidate) {
            is SimpleResolutionCandidate -> {
                // 普通函数调用
                candidate.checkSamWithVararg(diagnosticHolder)
                candidate.substitutedReturnType().also {
                    // 添加期望类型约束
                    candidate.addExpectedTypeConstraint(it, expectedType)
                    // 添加类型转换约束
                    candidate.addExpectedTypeFromCastConstraint(it, resolutionCallbacks)
                }
            }

            is CallableReferenceResolutionCandidate -> {
                // 可调用引用
                candidate.substitutedReflectionType()
            }
        }

        // 步骤 4: 计算补全模式
        val completionMode = CompletionModeCalculator.computeCompletionMode(
            candidate,
            expectedType,
            resultType,
            trivialConstraintTypeInferenceOracle,
            resolutionCallbacks.inferenceSession
        )

        // 步骤 5: 根据补全模式执行补全并返回结果
        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate)) {
                    // 运行完全补全
                    candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
                    candidate.asCallResolutionResult(completionMode, diagnosticHolder)
                } else {
                    // 转发到推断会话
                    candidate.asCallResolutionResult(
                        ConstraintSystemCompletionMode.PARTIAL,
                        diagnosticHolder,
                        forwardToInferenceSession = true
                    )
                }
            }

            ConstraintSystemCompletionMode.PARTIAL -> {
                // 运行部分补全
                candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
                candidate.asCallResolutionResult(completionMode, diagnosticHolder)
            }

            ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL ->
                error("PCLA might be only run for K2")

            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA ->
                throw IllegalStateException("Should not be here")
        }
    }
}

/**
 * 判断候选是否为错误候选
 *
 * 错误候选的情况：
 * 1. 候选描述符是错误描述符（函数不存在、名称错误等）
 * 2. 约束系统有矛盾（类型约束无法同时满足）
 *
 * @return true 如果是错误候选
 */
internal fun ResolutionCandidate.isErrorCandidate(): Boolean {
    return ErrorUtils.isError(resolvedCall.candidateDescriptor) || hasContradiction
}