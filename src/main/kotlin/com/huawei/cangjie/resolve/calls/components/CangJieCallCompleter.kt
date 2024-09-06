package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import com.huawei.cangjie.resolve.calls.inference.components.CangJieConstraintSystemCompleter
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import com.huawei.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import com.huawei.cangjie.resolve.calls.inference.model.ExpectedTypeConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.CandidateFactory
import com.huawei.cangjie.resolve.calls.tower.forceResolution
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.util.TypeUtils

internal val NewConstraintSystem.builtIns: CangJieBuiltIns get() = ((this as NewConstraintSystemImpl).typeSystemContext as BuiltInsProvider).builtIns

class CangJieCallCompleter(
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,

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

    private fun ResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?,
        expectedType: UnwrappedType?
    ) {
        if (returnType == null) return
        if (expectedType == null || (TypeUtils.noExpectedType(expectedType) && expectedType !== TypeUtils.UNIT_EXPECTED_TYPE)) return

        val csBuilder = getSystem().getBuilder()

        when {
            csBuilder.currentStorage().notFixedTypeVariables.isEmpty() -> {
                // This is needed to avoid multiple mismatch errors as we type check resulting type against expected one later
                // Plus, it helps with IDE-tests where it's important to have particular diagnostics.
                // Note that it aligns with the old inference, see CallCompleter.completeResolvedCallAndArguments

                // Another point is to avoid adding constraint from expected type for constant expressions like `1 + 1` because of
                // type coercion for numbers:
                // val a: Long = 1 + 1, result type of "1 + 1" will be Int and adding constraint with Long will produce type mismatch
                return
            }

            expectedType === TypeUtils.UNIT_EXPECTED_TYPE ->
                csBuilder.addEqualityConstraintIfCompatible(
                        returnType, csBuilder.builtIns.unitType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
                )

            else ->
                csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom))
        }
    }
    private fun ResolutionCandidate.addExpectedTypeFromCastConstraint(
        returnType: UnwrappedType?,
        resolutionCallbacks: CangJieResolutionCallbacks
    ) {
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ExpectedTypeFromCast)) return
        if (returnType == null) return

        val expectedType = resolutionCallbacks.getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedCall) ?: return
        val csBuilder = getSystem().getBuilder()

        csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom))
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
