/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.calls.components

import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.resolve.calls.tower.VisibilityError
import cn.cangnova.cangjie.resolve.calls.tower.VisibilityErrorOnArgument
import cn.cangnova.cangjie.resolve.calls.tower.isInapplicable

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
