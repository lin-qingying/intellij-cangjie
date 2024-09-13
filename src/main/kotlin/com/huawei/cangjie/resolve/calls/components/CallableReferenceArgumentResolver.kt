package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.VisibilityError
import com.huawei.cangjie.resolve.calls.tower.VisibilityErrorOnArgument
import com.huawei.cangjie.resolve.calls.tower.isInapplicable

class CallableReferenceArgumentResolver(val callableReferenceOverloadConflictResolver: CallableReferenceOverloadConflictResolver)
{

    fun processCallableReferenceArgument(
        csBuilder: ConstraintSystemBuilder,
        resolvedAtom: ResolvedCallableReferenceArgumentAtom,
        diagnosticsHolder: CangJieDiagnosticsHolder,
        resolutionCallbacks: CangJieResolutionCallbacks
    ) {
        val argument = resolvedAtom.atom
        val expectedType = resolvedAtom.expectedType?.let { (csBuilder.buildCurrentSubstitutor() as NewTypeSubstitutor).safeSubstitute(it) }
        val candidates = resolutionCallbacks.resolveCallableReferenceArgument(resolvedAtom.atom, expectedType, csBuilder.currentStorage())

        if (candidates.size > 1 && resolvedAtom is EagerCallableReferenceAtom) {
            if (candidates.all { it.resultingApplicability.isInapplicable }) {
                diagnosticsHolder.addDiagnostic(CallableReferenceCallCandidatesAmbiguity(argument, candidates))
            }

            resolvedAtom.setAnalyzedResults(
                candidate = null,
                subResolvedAtoms = listOf(resolvedAtom.transformToPostponed())
            )
            return
        }

        val chosenCandidate = candidates.singleOrNull()
        if (chosenCandidate != null) {
            val toFreshSubstitutor = CreateFreshVariablesSubstitutor.createToFreshVariableSubstitutorAndAddInitialConstraints(
                chosenCandidate.candidate,
                resolvedAtom.atom.call,
                csBuilder
            )
            chosenCandidate.addConstraints(csBuilder, toFreshSubstitutor, callableReference = argument)
            chosenCandidate.diagnostics.forEach {
                val transformedDiagnostic = when (it) {
                    is CompatibilityWarning -> CompatibilityWarningOnArgument(argument, it.candidate)
                    is VisibilityError -> VisibilityErrorOnArgument(argument, it.invisibleMember)
                    else -> it
                }
                diagnosticsHolder.addDiagnostic(transformedDiagnostic)
            }
            chosenCandidate.freshVariablesSubstitutor = toFreshSubstitutor
        } else {
            if (candidates.isEmpty()) {
                diagnosticsHolder.addDiagnostic(NoneCallableReferenceCallCandidates(argument))
            } else {
                diagnosticsHolder.addDiagnostic(CallableReferenceCallCandidatesAmbiguity(argument, candidates))
            }
        }

        // todo -- create this inside CallableReferencesCandidateFactory
        val subCjArguments = listOfNotNull(buildResolvedCjArgument(argument.lhsResult))

        resolvedAtom.setAnalyzedResults(chosenCandidate, subCjArguments)
    }

    private fun buildResolvedCjArgument(lhsResult: LHSResult): ResolvedAtom? {
        if (lhsResult !is LHSResult.Expression) return null
        return when (val lshCallArgument = lhsResult.lshCallArgument) {
            is SubCangJieCallArgument -> lshCallArgument.callResult
            is ExpressionCangJieCallArgument -> ResolvedExpressionAtom(lshCallArgument)
            else -> unexpectedArgument(lshCallArgument)
        }
    }
}
