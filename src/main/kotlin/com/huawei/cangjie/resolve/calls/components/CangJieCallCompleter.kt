package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import com.huawei.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.CandidateFactory
import com.huawei.cangjie.resolve.calls.tower.forceResolution
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.UnwrappedType

class CangJieCallCompleter(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,

    ) {

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

    fun createAllCandidatesResult(
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult {
        val completedCandidates = candidates.map { candidate ->
            val diagnosticsHolder = CangJieDiagnosticsHolder.SimpleHolder()

//            candidate.addExpectedTypeConstraint(
//                candidate.substitutedReturnType(), expectedType
//            )

//            runCompletion(
//                candidate.resolvedCall,
//                ConstraintSystemCompletionMode.FULL,
//                diagnosticsHolder,
//                candidate.getSystem(),
//                resolutionCallbacks,
//                collectAllCandidatesMode = true
//            )

            CandidateWithDiagnostics(candidate, diagnosticsHolder.getDiagnostics() + candidate.diagnostics)
        }
        return AllCandidatesResolutionResult(completedCandidates, resolutionCallbacks.createEmptyConstraintSystem())
    }

    private fun ResolutionCandidate.checkSamWithVararg(diagnosticHolder: CangJieDiagnosticsHolder.SimpleHolder) {
//        val samConversionPerArgumentWithWarningsForVarargAfterSam =
//            callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
//                    !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

//        val candidateDescriptor = resolvedCall.candidateDescriptor
//        if (/*samConversionPerArgumentWithWarningsForVarargAfterSam &&*/ candidateDescriptor is SyntheticMemberDescriptor<*>) {
//            val declarationDescriptor = candidateDescriptor.baseDescriptorForSynthetic as? FunctionDescriptor ?: return
//
//            if (declarationDescriptor.valueParameters.lastOrNull()?.isVararg == true) {
//                diagnosticHolder.addDiagnostic(
//                    ResolvedToSamWithVarargDiagnostic(resolvedCall.atom.argumentsInParenthesis.lastOrNull() ?: return)
//                )
//            }
//        }
    }

    private fun ResolutionCandidate.substitutedReturnType(): UnwrappedType? {
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null
        val substitutedReturnTypeWithVariables = resolvedCall.freshVariablesSubstitutor.safeSubstitute(returnType)
        return (getResultingSubstitutor() as? NewTypeSubstitutorByConstructorMap)
            ?.safeSubstitute(substitutedReturnTypeWithVariables)
            ?: substitutedReturnTypeWithVariables
    }

    private fun CallableReferenceResolutionCandidate.substitutedReflectionType(): UnwrappedType {
        return resolvedCall.freshVariablesSubstitutor.safeSubstitute(this.reflectionCandidateType)
    }

    private fun runCompletion(
        resolvedCallAtom: ResolvedCallAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        constraintSystem: NewConstraintSystem,
        resolutionCallbacks: CangJieResolutionCallbacks,
        collectAllCandidatesMode: Boolean = false
    ) {

    }

    private fun ResolutionCandidate.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: CangJieDiagnosticsHolder,
        resolutionCallbacks: CangJieResolutionCallbacks,
    ) {
        runCompletion(resolvedCall, completionMode, diagnosticHolder, getSystem(), resolutionCallbacks)
    }

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

    fun runCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticHolder = CangJieDiagnosticsHolder.SimpleHolder()
        when {
            candidates.isEmpty() -> diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic())
            candidates.size > 1 -> diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(candidates))
        }

        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
        val resultType = when (candidate) {
            is SimpleResolutionCandidate -> {
//                candidate.checkSamWithVararg(diagnosticHolder)
                candidate.substitutedReturnType().also {
//                    candidate.addExpectedTypeConstraint(it, expectedType)
//                    candidate.addExpectedTypeFromCastConstraint(it, resolutionCallbacks)
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
//

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
