package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.CandidateFactory
import com.huawei.cangjie.resolve.calls.tower.forceResolution
import com.huawei.cangjie.types.UnwrappedType

class CangJieCallCompleter {

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
    fun runCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): CallResolutionResult{
        val diagnosticHolder = CangJieDiagnosticsHolder.SimpleHolder()
//        when {
//            candidates.isEmpty() -> diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic())
//            candidates.size > 1 -> diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(candidates))
//        }
//
//        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
//        val resultType = when (candidate) {
//            is SimpleResolutionCandidate -> {
//                candidate.checkSamWithVararg(diagnosticHolder)
//                candidate.substitutedReturnType().also {
//                    candidate.addExpectedTypeConstraint(it, expectedType)
//                    candidate.addExpectedTypeFromCastConstraint(it, resolutionCallbacks)
//                }
//            }
//            is CallableReferenceResolutionCandidate -> candidate.substitutedReflectionType()
//        }
//
//        val completionMode = CompletionModeCalculator.computeCompletionMode(
//            candidate, expectedType, resultType, trivialConstraintTypeInferenceOracle, resolutionCallbacks.inferenceSession
//        )
////
//
//        return when (completionMode) {
//            ConstraintSystemCompletionMode.FULL -> {
//                if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate)) {
//                    candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
//                    candidate.asCallResolutionResult(completionMode, diagnosticHolder)
//                } else {
//                    candidate.asCallResolutionResult(
//                        ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder, forwardToInferenceSession = true
//                    )
//                }
//            }
//            ConstraintSystemCompletionMode.PARTIAL -> {
//                candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
//                candidate.asCallResolutionResult(completionMode, diagnosticHolder)
//            }
//            ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL -> error("PCLA might be only run for K2")
//            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> throw IllegalStateException("Should not be here")
//
//        }
TODO()
    }

}
