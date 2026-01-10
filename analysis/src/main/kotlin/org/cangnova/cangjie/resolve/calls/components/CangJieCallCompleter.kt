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
import org.cangnova.cangjie.resolve.calls.inference.components.TypeSubstitutorByConstructorMap
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.cangnova.cangjie.resolve.calls.inference.model.ExpectedTypeConstraintPositionImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tower.CandidateFactory
import org.cangnova.cangjie.resolve.calls.tower.forceResolution
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.error.ErrorTypeKind

internal val ConstraintSystem.builtIns: CangJieBuiltIns get() = ((this as ConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns

/**
 * CangJieCallCompleter 类负责处理函数调用的补全逻辑。
 *
 * 此类通过分析解析候选者并绑定必要的描述符，来支持函数调用的类型推断和补全。
 * 它提供了多种方法来处理候选者的预期类型约束、执行补全操作以及生成解析结果。
 *
 * @property trivialConstraintTypeInferenceOracle 用于推断简单约束类型的工具。
 * @property cangjieConstraintSystemCompleter 处理约束系统补全的工具。
 * @property postponedArgumentsAnalyzer 处理延迟参数分析的工具。
 */
class CangJieCallCompleter(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,

    ) {
    /**
     * 准备候选者以进行补全，通过分析提供的候选者并绑定必要的描述符。
     *
     * 此函数检查提供的候选集合中是否存在唯一的候选者。如果找到候选者，
     * 它会验证解析调用是否需要绑定描述符，特别是在处理 lambda 表达式的上下文中。
     * 如果需要，它会绑定解析调用并在适用时禁用合约。
     *
     * @param factory 用于创建错误候选者的工厂，如果未找到有效候选者。
     * @param candidates 要分析的解析候选者集合。
     * @param resolutionCallbacks 处理与解析相关的操作的回调，例如绑定解析调用和禁用合约。
     * @return 准备好的解析候选者，如果未找到有效候选者，则可能是错误候选者。
     */
    private fun prepareCandidateForCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: Collection<ResolutionCandidate>,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): ResolutionCandidate {
        val candidate = candidates.singleOrNull()

        // this is needed at least for non-local return checker, because when we analyze lambda we should already bind descriptor for outer call
        candidate?.resolvedCall?.let {
            val mayNeedDescriptor = it.argumentToCandidateParameter.keys.any { arg ->
                arg is LambdaCangJieCallArgument
            }
            if (mayNeedDescriptor) {
                resolutionCallbacks.bindStubResolvedCallForCandidate(it)
            }
            resolutionCallbacks.disableContractsIfNecessary(it)
        }

        return candidate ?: factory.createErrorCandidate().forceResolution()
    }

    /**
     * 创建所有候选者的解析结果。
     *
     * 此方法接受一组解析候选者，并为每个候选者添加预期类型约束。然后，它执行补全操作并收集每个候选者的诊断信息。
     * 最后，返回一个包含所有候选者及其诊断信息的解析结果。
     *
     * @param candidates 要处理的解析候选者集合。
     * @param expectedType 预期的类型，用于约束候选者的返回类型。
     * @param resolutionCallbacks 处理解析相关操作的回调。
     * @return 包含所有候选者及其诊断信息的解析结果。
     */
    fun createAllCandidatesResult(
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult {
        val completedCandidates = candidates.map { candidate ->
            val diagnosticsHolder = CangJieDiagnosticsHolder.SimpleHolder()

            candidate.addExpectedTypeConstraint(
                candidate.substitutedReturnType(), expectedType
            )

            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true
            )

            CandidateWithDiagnostics(candidate, diagnosticsHolder.getDiagnostics() + candidate.diagnostics)
        }
        return AllCandidatesResolutionResult(completedCandidates, resolutionCallbacks.createEmptyConstraintSystem())
    }

    /**
     * 检查是否为带有可变参数的SAM（单抽象方法）转换，并在适用情况下添加诊断信息
     *
     * @param diagnosticHolder 用于存储诊断信息的容器
     */
    private fun ResolutionCandidate.checkSamWithVararg(diagnosticHolder: CangJieDiagnosticsHolder.SimpleHolder) {
//        val samConversionPerArgumentWithWarningsForVarargAfterSam =
//            callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
//                    !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

//        val descriptor = resolvedCall.descriptor
//        if (/*samConversionPerArgumentWithWarningsForVarargAfterSam &&*/ descriptor is SyntheticMemberDescriptor<*>) {
//            val declarationDescriptor = descriptor.baseDescriptorForSynthetic as? FunctionDescriptor ?: return
//
//            if (declarationDescriptor.valueParameters.lastOrNull()?.isVararg == true) {
//                diagnosticHolder.addDiagnostic(
//                    ResolvedToSamWithVarargDiagnostic(resolvedCall.atom.argumentsInParenthesis.lastOrNull() ?: return)
//                )
//            }
//        }
    }

    /**
     * 计算替代后的返回类型
     *
     * 此函数用于计算经过泛型替代后的 resolvedCall 的返回类型它首先获取候选描述符的返回类型，
     * 然后应用新鲜变量替代器进行泛型参数的替代最后，如果可能的话，再应用新类型替代器
     * 由构造函数映射进行最终的替代如果任何一步替代失败或不可行，则返回原始的替代返回类型
     *
     * @return 替代后的返回类型，如果无法进行替代则返回 null
     */
    private fun ResolutionCandidate.substitutedReturnType(): UnwrappedType? {
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null
        val substitutedReturnTypeWithVariables = resolvedCall.freshVariablesSubstitutor.safeSubstitute(returnType)
        return (getResultingSubstitutor() as? TypeSubstitutorByConstructorMap)?.safeSubstitute(
            substitutedReturnTypeWithVariables
        ) ?: substitutedReturnTypeWithVariables
    }

    /**
     * 计算替代后的反射类型。
     *
     * 此方法使用新鲜变量替代器对反射候选者的类型进行替代。
     * 它返回替代后的类型，适用于反射调用的上下文。
     *
     * @return 替代后的反射类型。
     */
    private fun CallableReferenceResolutionCandidate.substitutedReflectionType(): UnwrappedType {
        return resolvedCall.freshVariablesSubstitutor.safeSubstitute(this.reflectionCandidateType)
    }

    /**
     * 执行补全操作以解析给定的调用原子。
     *
     * 此方法根据提供的参数运行补全过程。它首先确定返回类型，如果没有新鲜的返回类型，则使用默认的单元类型。
     * 然后，它调用 `cangjieConstraintSystemCompleter` 来执行补全，传入相关的上下文和参数。
     * 根据 `collectAllCandidatesMode` 的值，决定是设置空的分析结果还是分析延迟参数。
     * 最后，处理约束系统中的错误，并在遇到递归类型时添加相应的诊断信息。
     *
     * @param resolvedCallAtom 解析后的调用原子，包含有关调用的信息。
     * @param completionMode 补全模式，指示补全的方式。
     * @param diagnosticsHolder 用于收集诊断信息的持有者。
     * @param constraintSystem 当前的约束系统，用于执行补全。
     * @param resolutionCallbacks 处理解析相关操作的回调。
     * @param collectAllCandidatesMode 是否收集所有候选者的模式，默认为 false。
     */
    private fun runCompletion(
        resolvedCallAtom: ResolvedCallAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        constraintSystem: ConstraintSystem,
        resolutionCallbacks: CangJieResolutionCallbacks,
        collectAllCandidatesMode: Boolean = false
    ) {

        val returnType = resolvedCallAtom.freshReturnType ?: constraintSystem.builtIns.unitType
        cangjieConstraintSystemCompleter.runCompletion(
            constraintSystem.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(resolvedCallAtom),
            returnType,
            diagnosticsHolder
        ) {
            if (collectAllCandidatesMode) {
                it.setEmptyAnalyzedResults()
            } else {
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    resolutionCallbacks,
                    it,
                    completionMode,
                    diagnosticsHolder
                )
            }
        }

        constraintSystem.errors.forEach(diagnosticsHolder::addError)

        if (returnType is ErrorType && returnType.kind == ErrorTypeKind.RECURSIVE_TYPE) {
            diagnosticsHolder.addDiagnostic(TypeCheckerHasRanIntoRecursion)
        }
    }

    private fun ResolutionCandidate.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
        resolutionCallbacks: CangJieResolutionCallbacks,
    ) {
        runCompletion(resolvedCall, completionMode, diagnosticHolder, getSystem(), resolutionCallbacks)
    }

    /**
     * 将解析候选者转换为解析结果。
     *
     * 根据候选者的状态和类型，生成相应的解析结果。
     * 如果候选者是错误候选者，则返回错误解析结果。
     * 否则，根据指定的补全模式返回完整或部分的解析结果。
     *
     * @param type 指定的补全模式，决定返回的解析结果类型。
     * @param diagnosticsHolder 用于存储诊断信息的容器。
     * @param forwardToInferenceSession 是否将结果转发到推断会话，默认为 false。
     * @return 生成的解析结果，可能是完整的或部分的解析结果，或错误解析结果。
     */
    fun ResolutionCandidate.asCallResolutionResult(
        type: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder.SimpleHolder,
        forwardToInferenceSession: Boolean = false
    ): CallResolutionResult {
        val constraintSystem = getSystem()
        val allDiagnostics = diagnosticsHolder.getDiagnostics() + diagnostics

        if (isErrorCandidate()) {
            return ErrorCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem)
        }

        return if (type == ConstraintSystemCompletionMode.FULL) {
            CompletedCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem)
        } else {
            PartialCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem, forwardToInferenceSession)
        }
    }

    /**
     * 向解析候选者添加预期类型约束
     *
     * 根据给定的返回类型和预期类型，向类型系统的约束构建器添加类型约束。
     *
     * @param returnType 结果类型
     * @param expectedType 预期类型
     */
    private fun ResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?, expectedType: UnwrappedType?
    ) {
        // 如果结果类型为空，则无需添加约束，直接返回
        if (returnType == null) return
        // 如果预期类型为空，或者预期类型为无效类型（且不为UNIT_EXPECTED_TYPE），则无需添加约束
        if (expectedType == null || (TypeUtils.noExpectedType(expectedType) && expectedType !== TypeUtils.UNIT_EXPECTED_TYPE)) return

        // 获取当前类型的系统构建器
        val csBuilder = getSystem().getBuilder()

        // 根据当前上下文的不固定类型变量情况，决定添加何种类型的约束
        when {
            csBuilder.currentStorage().notFixedTypeVariables.isEmpty() -> {
                // 如果没有不固定的类型变量，则不添加任何约束
                // 这是为了避免在后续将结果类型与预期类型进行类型检查时发生多个不匹配错误所必需的。
                // 此外，它有助于IDE测试，在这些测试中，拥有特定的诊断信息非常重要。
                // 请注意，这与旧的推理是一致的，参见CallCompleter.completeResolvedCallAndArguments。
            }

            expectedType === TypeUtils.UNIT_EXPECTED_TYPE ->
                // 如果预期类型为UNIT_EXPECTED_TYPE，则添加等式约束
                csBuilder.addEqualityConstraintIfCompatible(
                    returnType, csBuilder.builtIns.unitType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
                )

            else ->
                // 对于其他情况，添加子类型约束
                csBuilder.addSubtypeConstraint(
                    returnType, expectedType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
                )
        }
    }

    /**
     * 从类型转换约束中添加预期类型。
     *
     * 此方法首先检查当前语言版本设置是否支持从类型转换中获取预期类型的特性。
     * 如果不支持或返回类型为 null，则直接返回。
     * 然后，它通过回调获取预期类型，并在跟踪中记录该信息。
     * 最后，使用类型系统的构建器添加子类型约束，将返回类型与预期类型关联起来。
     *
     * @param returnType 方法或表达式的返回类型。
     * @param resolutionCallbacks 处理解析相关操作的回调，包括获取预期类型。
     */
    private fun ResolutionCandidate.addExpectedTypeFromCastConstraint(
        returnType: UnwrappedType?, resolutionCallbacks: CangJieResolutionCallbacks
    ) {
        if (returnType == null) return

        val expectedType = resolutionCallbacks.getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedCall) ?: return
        val csBuilder = getSystem().getBuilder()

        csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom))
    }

    /**
     * 执行补全操作并返回解析结果。
     *
     * 此方法接受一组解析候选者，并根据提供的预期类型和回调执行补全逻辑。
     * 它首先检查候选者集合的状态，记录诊断信息，然后准备候选者进行补全。
     * 根据候选者的类型，计算返回类型，并确定补全模式。
     * 最后，根据补全模式执行补全并返回相应的解析结果。
     *
     * @param factory 用于创建候选者的工厂。
     * @param candidates 要处理的解析候选者集合。
     * @param expectedType 预期的类型，用于约束候选者的返回类型。
     * @param resolutionCallbacks 处理解析相关操作的回调。
     * @return 解析结果，可能是完整的或部分的解析结果。
     */
    fun runCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: MutableCollection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult {

        val diagnosticHolder = CangJieDiagnosticsHolder.SimpleHolder()


        when {


            candidates.isEmpty() -> diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic())
            candidates.size > 1 -> diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(candidates.toList()))

        }


        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
        val resultType = when (candidate) {
            is SimpleResolutionCandidate -> {
                candidate.checkSamWithVararg(diagnosticHolder)
                candidate.substitutedReturnType().also {
                    candidate.addExpectedTypeConstraint(it, expectedType)
                    candidate.addExpectedTypeFromCastConstraint(it, resolutionCallbacks)
                }

            }

            is CallableReferenceResolutionCandidate -> candidate.substitutedReflectionType()
        }

        val completionMode = CompletionModeCalculator.computeCompletionMode(
            candidate,
            expectedType,
            resultType,
            trivialConstraintTypeInferenceOracle,
            resolutionCallbacks.inferenceSession
        )


        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate)) {
                    candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
                    candidate.asCallResolutionResult(completionMode, diagnosticHolder)
                } else {
                    candidate.asCallResolutionResult(
                        ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder, forwardToInferenceSession = true
                    )
                }
            }

            ConstraintSystemCompletionMode.PARTIAL -> {
                candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
                candidate.asCallResolutionResult(completionMode, diagnosticHolder)
            }

            ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> error("PCLA might be only run for K2")
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> throw IllegalStateException("Should not be here")

        }

    }

}

internal fun ResolutionCandidate.isErrorCandidate(): Boolean {
    return ErrorUtils.isError(resolvedCall.candidateDescriptor) || hasContradiction
}
