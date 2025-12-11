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

package org.cangnova.cangjie.resolve.calls.results/*
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

//package org.cangnova.cangjie.resolve.calls.results
//
//import org.cangnova.cangjie.analyzer.createOverloadingConflictResolver
//import org.cangnova.cangjie.builtins.CangJieBuiltIns
//import org.cangnova.cangjie.config.LanguageFeature
//import org.cangnova.cangjie.config.LanguageVersionSettings
//import org.cangnova.cangjie.descriptors.BindingTrace
//import org.cangnova.cangjie.descriptors.CallableDescriptor
//import org.cangnova.cangjie.descriptors.ModuleDescriptor
//import org.cangnova.cangjie.resolve.calls.context.CallResolutionContext
//import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
//import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
//import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
//import org.cangnova.cangjie.resolve.calls.tower.isSynthesized
//import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
//import org.cangnova.cangjie.utils.CancellationChecker
//import java.util.stream.Collectors
//
//class ResolutionResultsHandler(
//    builtIns: CangJieBuiltIns,
//    module: ModuleDescriptor,
//    specificityComparator: TypeSpecificityComparator,
//    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
//    cancellationChecker: CancellationChecker,
//    cangjieTypeRefiner: CangJieTypeRefiner
//) {
//    private val overloadingConflictResolver = createOverloadingConflictResolver(
//        builtIns,
//        module,
//        specificityComparator,
//        platformOverloadsSpecificityComparator,
//        cancellationChecker,
//        cangjieTypeRefiner
//    )
//
//    companion object {
//        private fun <D : CallableDescriptor > recordFailedInfo(
//            tracing: TracingStrategy,
//            trace: BindingTrace,
//            candidates: Collection<MutableResolvedCall<D>>
//        ): OverloadResolutionResultsImpl<D> {
//            if (candidates.size == 1) {
//                val failed = candidates.iterator().next()
//                failed.getTrace().moveAllMyDataTo(trace)
//                return OverloadResolutionResultsImpl.singleFailedCandidate<D>(
//                    failed
//                )
//            }
//            tracing.noneApplicable<D>(trace, candidates)
//            tracing.recordAmbiguity<D>(trace, candidates)
//            return OverloadResolutionResultsImpl.manyFailedCandidates<D>(
//                candidates
//            )
//        }
//    }
//
//    fun <D : CallableDescriptor> computeResultAndReportErrors(
//        context: CallResolutionContext<*>,
//        tracing: TracingStrategy,
//        candidates: Collection<MutableResolvedCall<D>>,
//        languageVersionSettings: LanguageVersionSettings
//    ): OverloadResolutionResultsImpl<D> {
//        val successfulCandidates =
//            LinkedHashSet<MutableResolvedCall<D>>()
//        val failedCandidates =
//            LinkedHashSet<MutableResolvedCall<D>>()
//        val incompleteCandidates =
//            LinkedHashSet<MutableResolvedCall<D>>()
//        val candidatesWithWrongReceiver =
//            LinkedHashSet<MutableResolvedCall<D>>()
//
//
//        for (candidateCall in candidates) {
//            val status: ResolutionStatus = candidateCall.getStatus()
//            assert(status != ResolutionStatus.UNKNOWN_STATUS) { "No resolution for " + candidateCall.getCandidateDescriptor() }
//            if (status.isSuccess) {
//                successfulCandidates.add(candidateCall)
//            } else if (status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
//                incompleteCandidates.add(candidateCall)
//            } else if (candidateCall.getStatus() == ResolutionStatus.RECEIVER_TYPE_ERROR) {
//                candidatesWithWrongReceiver.add(candidateCall)
//            } else if (candidateCall.getStatus() != ResolutionStatus.RECEIVER_PRESENCE_ERROR) {
//                failedCandidates.add(candidateCall)
//            }
//        }
//
//        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific
//        if (successfulCandidates.isNotEmpty() || incompleteCandidates.isNotEmpty()) {
//            return computeSuccessfulResult<D>(
//                context,
//                tracing,
//                successfulCandidates,
//                incompleteCandidates,
//                context.checkArguments,
//                languageVersionSettings
//            )
//        } else if (failedCandidates.isNotEmpty()) {
//            return computeFailedResult<D>(
//                tracing,
//                context.trace,
//                failedCandidates,
//                context.checkArguments,
//                languageVersionSettings
//            )
//        }
//        if (candidatesWithWrongReceiver.isNotEmpty()) {
//            tracing.unresolvedReferenceWrongReceiver<D>(context.trace, candidatesWithWrongReceiver)
//            return OverloadResolutionResultsImpl.candidatesWithWrongReceiver<D>(
//                candidatesWithWrongReceiver
//            )
//        }
//        tracing.unresolvedReference(context.trace)
//        return OverloadResolutionResultsImpl.nameNotFound<D>()
//    }
//
//    private fun <D : CallableDescriptor> computeFailedResult(
//        tracing: TracingStrategy,
//        trace: BindingTrace,
//        failedCandidates: Set<MutableResolvedCall<D>>,
//        checkArgumentsMode: CheckArgumentTypesMode,
//        languageVersionSettings: LanguageVersionSettings
//    ): OverloadResolutionResultsImpl<D> {
//        if (failedCandidates.size == 1) {
//            return recordFailedInfo<D>(
//                tracing,
//                trace,
//                failedCandidates
//            )
//        }
//
//        for (severityLevel in ResolutionStatus.SEVERITY_LEVELS) {
//            val thisLevel  =  java.util.LinkedHashSet<MutableResolvedCall<D>>()
//            for (candidate in failedCandidates) {
//                if (severityLevel.contains(candidate.getStatus())) {
//                    thisLevel.add(candidate)
//                }
//            }
//            if (thisLevel.isNotEmpty()) {
//                if (severityLevel.contains(ResolutionStatus.ARGUMENTS_MAPPING_ERROR)) {
//                    val myResolver  =
//                        overloadingConflictResolver
//                    return recordFailedInfo<D>(
//                        tracing,
//                        trace,
//                        myResolver.filterOutEquivalentCalls(
//                            java.util.LinkedHashSet<MutableResolvedCall<D>>(
//                                thisLevel
//                            )
//                        )
//                    )
//                }
//                val results  =
//                    chooseAndReportMaximallySpecific(
//                        thisLevel, false, checkArgumentsMode, languageVersionSettings
//                    )
//                return recordFailedInfo(
//                    tracing,
//                    trace,
//                    results.getResultingCalls()
//                )
//            }
//        }
//
//        throw AssertionError("Should not be reachable, cause every status must belong to some level: $failedCandidates")
//    }
//
//    private fun <D : CallableDescriptor> chooseAndReportMaximallySpecific(
//        candidates: Set<MutableResolvedCall<D> >,
//        discriminateGenerics: Boolean,
//        checkArgumentsMode: CheckArgumentTypesMode,
//        languageVersionSettings: LanguageVersionSettings
//    ): OverloadResolutionResultsImpl<D> {
//        val myResolver  = overloadingConflictResolver
//
//        var refinedCandidates  = candidates
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority)) {
//            val nonSynthesized =
//                HashSet<MutableResolvedCall<D> >()
//            for (candidate in candidates) {
//                if (!candidate.getCandidateDescriptor().isSynthesized) {
//                    nonSynthesized.add(candidate)
//                }
//            }
//
//            if (nonSynthesized.isNotEmpty()) {
//                refinedCandidates = nonSynthesized
//            }
//        }
//
//        var specificCalls :   Set<MutableResolvedCall<D>> = myResolver.chooseMaximallySpecificCandidates(refinedCandidates, checkArgumentsMode, discriminateGenerics)
//
//        if (specificCalls.size > 1) {
//            specificCalls = specificCalls.stream()
//                .filter { call  ->
//                    true
////                    !call.getCandidateDescriptor().annotations.hasAnnotation(
////                        AnnotationsForResolve.getOVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME()
////                    )
//                }.collect(setOf())
//        }
//
//        return if (specificCalls.size == 1) {
//            OverloadResolutionResultsImpl.success(
//                specificCalls.iterator().next()
//            )
//        } else {
//            OverloadResolutionResultsImpl.ambiguity<D>(specificCalls)
//        }
//    }
//
//    private fun <D : CallableDescriptor> computeSuccessfulResult(
//        context: CallResolutionContext<*>,
//        tracing: TracingStrategy,
//        successfulCandidates: Set<MutableResolvedCall<D>>,
//        incompleteCandidates: Set<MutableResolvedCall<D>>,
//        checkArgumentsMode: CheckArgumentTypesMode,
//        languageVersionSettings: LanguageVersionSettings
//    ): OverloadResolutionResultsImpl<D> {
//        val successfulAndIncomplete: MutableSet<MutableResolvedCall<D>> =
//            java.util.LinkedHashSet<MutableResolvedCall<D>>()
//        successfulAndIncomplete.addAll(successfulCandidates)
//        successfulAndIncomplete.addAll(incompleteCandidates)
//        val results: OverloadResolutionResultsImpl<D> =
//            chooseAndReportMaximallySpecific<D>(
//                successfulAndIncomplete, true, checkArgumentsMode, languageVersionSettings
//            )
//        if (results.isSingleResult) {
//            val resultingCall: MutableResolvedCall<D> =
//                results.getResultingCall()
//            resultingCall.getTrace().moveAllMyDataTo(context.trace)
//            if (resultingCall.getStatus() == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
//                return OverloadResolutionResultsImpl.incompleteTypeInference<D>(
//                    resultingCall
//                )
//            }
//        }
//        if (results.isAmbiguity) {
//            tracing.recordAmbiguity<D>(context.trace, results.resultingCalls)
//            val allCandidatesIncomplete: Boolean =
//                ResolutionResultsHandler.allIncomplete<D>(results.resultingCalls)
//            // This check is needed for the following case:
//            //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
//            if (context.checkArguments != CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ||
//                !CallUtilCj.hasUnresolvedArguments(context.call, context)
//            ) {
//                if (allCandidatesIncomplete) {
//                    tracing.cannotCompleteResolve<D>(context.trace, results.resultingCalls)
//                } else {
//                    tracing.ambiguity<D>(context.trace, results.resultingCalls)
//                }
//            }
//            if (allCandidatesIncomplete) {
//                return OverloadResolutionResultsImpl.incompleteTypeInference<D>(
//                    results.resultingCalls
//                )
//            }
//        }
//        return results
//    }
//}
