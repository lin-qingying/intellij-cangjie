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

package com.linqingying.cangjie.resolve.calls


import  com.linqingying.cangjie.resolve.calls.components.createCallableReferenceProcessor
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.descriptors.PropertyDescriptor
import com.linqingying.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.linqingying.cangjie.resolve.calls.components.CallableReferenceArgumentResolver
import com.linqingying.cangjie.resolve.calls.components.CangJieCallCompleter
import com.linqingying.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.linqingying.cangjie.resolve.calls.components.NewOverloadingConflictResolver
import com.linqingying.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.linqingying.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.linqingying.cangjie.resolve.calls.context.CheckArgumentTypesMode
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.resolve.calls.model.CangJieCallKind.*
import com.linqingying.cangjie.resolve.calls.tower.*
import com.linqingying.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.UnwrappedType

class CangJieCallResolver(
    private val towerResolver: TowerResolver,
    private val cangjieCallCompleter: CangJieCallCompleter,
    private val overloadingConflictResolver: NewOverloadingConflictResolver,
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val callComponents: CangJieCallComponents

) {
    fun resolveCallableReferenceArgument(
        argument: CallableReferenceCangJieCallArgument,
        expectedType: UnwrappedType?,
        baseSystem: ConstraintStorage,
        resolutionCallbacks: CangJieResolutionCallbacks
    ): Collection<CallableReferenceResolutionCandidate> {
        val scopeTower = callComponents.statelessCallbacks.getScopeTowerForCallableReferenceArgument(argument)
        val factory = createCallableReferenceCallFactory(scopeTower, argument.call, resolutionCallbacks, expectedType, argument, baseSystem)

        return resolveCall(scopeTower, resolutionCallbacks, argument.call, collectAllCandidates = false, factory)
    }

    fun resolveAndCompleteGivenCandidates(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        cangjieCall: CangJieCall,
        expectedType: UnwrappedType?,
        givenCandidates: Collection<GivenCandidate>,
        collectAllCandidates: Boolean
    ): CallResolutionResult {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        cangjieCall.checkCallInvariants()

        val candidateFactory = SimpleCandidateFactory(callComponents, scopeTower, cangjieCall, resolutionCallbacks)
        val resolutionCandidates = givenCandidates.map { candidateFactory.createCandidate(it).forceResolution() }

        if (collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(
                KnownResultProcessor(resolutionCandidates),
                TowerResolver.AllCandidatesCollector(),
                useOrder = false
            )
            return cangjieCallCompleter.createAllCandidatesResult(allCandidates, expectedType, resolutionCallbacks)

        }

        val candidates = towerResolver.runWithEmptyTowerData(
            KnownResultProcessor(resolutionCandidates),
            TowerResolver.SuccessfulResultCollector(),
            useOrder = true
        )
        val mostSpecificCandidates = choseMostSpecific(cangjieCall, resolutionCallbacks, candidates)

        return cangjieCallCompleter.runCompletion(candidateFactory, mostSpecificCandidates.toMutableSet(), expectedType, resolutionCallbacks)
    }
    private fun createCallableReferenceCallFactory(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        argument: CallableReferenceCangJieCallArgument? = null,
        baseSystem: ConstraintStorage? = null
    ): CandidateFactory<CallableReferenceResolutionCandidate> {
        val resolutionAtom = argument
            ?: CallableReferenceCangJieCall(
                cangjieCall,
                resolutionCallbacks.getLhsResult(cangjieCall),
                cangjieCall.name
            )

        return CallableReferencesCandidateFactory(
            resolutionAtom,
            callComponents,
            scopeTower,
            expectedType,
            baseSystem,
            resolutionCallbacks
        )
    }

    private fun createSimpleCallFactory(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
    ): CandidateFactory<ResolutionCandidate> =
        SimpleCandidateFactory(callComponents, scopeTower, cangjieCall, resolutionCallbacks)

    private fun createFactory(
        scopeTower: ImplicitScopeTower,
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?
    ): CandidateFactory<ResolutionCandidate> =
        when (cangjieCall.callKind) {
            CALLABLE_REFERENCE -> createCallableReferenceCallFactory(
                scopeTower,
                cangjieCall,
                resolutionCallbacks,
                expectedType
            )

            else -> createSimpleCallFactory(scopeTower, cangjieCall, resolutionCallbacks)
        }

    private fun <C : ResolutionCandidate> resolveCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        cangjieCall: CangJieCall,
        collectAllCandidates: Boolean,
        candidateFactory: CandidateFactory<C>,
    ): Collection<C> {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        cangjieCall.checkCallInvariants()

        val processor = when (cangjieCall.callKind) {
//            ENUM ->{
//                createEnumAndEntryProcessor(
//                    cangjieCall,
//                    scopeTower,
//
//                    candidateFactory,
//
//                )
//            }
            ENUM_ENTRY ->{
                createEnumEntryProcessor(
                    cangjieCall,
                    scopeTower,

                    candidateFactory,

                )
            }
            CASE_ENUM ->{
                createEnumAndEntryProcessor(
                    cangjieCall,
                    scopeTower,

                    candidateFactory,

                    )
            }

            VARIABLE -> {
                createVariableAndObjectProcessor(
                    scopeTower,
                    cangjieCall.name,
                    candidateFactory,
                    cangjieCall.explicitReceiver?.receiver
                )
            }

            FUNCTION -> {
                createFunctionProcessor(
                    scopeTower,
                    cangjieCall.name,
                    candidateFactory,
                    resolutionCallbacks.getCandidateFactoryForInvoke(scopeTower, cangjieCall),
                    cangjieCall.explicitReceiver?.receiver
                )
            }

            CALLABLE_REFERENCE -> {


                createCallableReferenceProcessor(candidateFactory as CallableReferencesCandidateFactory) as ScopeTowerProcessor<C>
            }

            INVOKE -> {
                createProcessorWithReceiverValueOrEmpty(cangjieCall.explicitReceiver?.receiver) {
                    createCallTowerProcessorForExplicitInvoke(
                        scopeTower,
                        candidateFactory,
                        cangjieCall.dispatchReceiverForInvokeExtension?.receiver as ReceiverValueWithSmartCastInfo,
                        it
                    )
                }
            }

            UNSUPPORTED -> throw UnsupportedOperationException()

        }

//        if (collectAllCandidates) {
//            return towerResolver.collectAllCandidates(scopeTower, processor, cangjieCall.name)
//        }

        val candidates = towerResolver.runResolve(
            scopeTower,
            processor,
            useOrder = cangjieCall.callKind != UNSUPPORTED,
            name = cangjieCall.name
        )

        @Suppress("UNCHECKED_CAST")
        return choseMostSpecific(cangjieCall, resolutionCallbacks, candidates) as Set<C>
    }

    private fun choseMostSpecific(
        cangjieCall: CangJieCall,
        resolutionCallbacks: CangJieResolutionCallbacks,
        candidates: Collection<ResolutionCandidate>
    ): Set<ResolutionCandidate> {
        var refinedCandidates = candidates

        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.RefinedSamAdaptersPriority) && cangjieCall.callKind != CALLABLE_REFERENCE) {
            val nonSynthesized = candidates.filter { !it.resolvedCall.candidateDescriptor.isSynthesized }
            if (nonSynthesized.isNotEmpty()) {
                refinedCandidates = nonSynthesized
            }
        }

        var maximallySpecificCandidates =
            if (cangjieCall.callKind == CALLABLE_REFERENCE) {
                @Suppress("UNCHECKED_CAST")
                callableReferenceArgumentResolver.callableReferenceOverloadConflictResolver.chooseMaximallySpecificCandidates(
                    refinedCandidates as Collection<CallableReferenceResolutionCandidate>,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    discriminateGenerics = false
                )
            } else {
                overloadingConflictResolver.chooseMaximallySpecificCandidates(
                    refinedCandidates,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    discriminateGenerics = true // todo
                )
            }

        if (maximallySpecificCandidates.size > 1) {
            if (maximallySpecificCandidates.size == 2) {
                val enumEntryCandidate = maximallySpecificCandidates.find {
                    val descriptor = it.resolvedCall.candidateDescriptor
                    descriptor is FakeCallableDescriptorForObject && descriptor.classDescriptor.kind == ClassKind.ENUM_ENTRY
                }
                if (enumEntryCandidate != null) {
                    val otherCandidate = maximallySpecificCandidates.find {
                        val candidateDescriptor = it.resolvedCall.candidateDescriptor
                        candidateDescriptor !is FakeCallableDescriptorForObject
                    }
                    if (otherCandidate != null) {
                        val propertyDescriptor = otherCandidate.resolvedCall.candidateDescriptor
                        if (propertyDescriptor is PropertyDescriptor) {
                            val enumEntryDescriptor =
                                (enumEntryCandidate.resolvedCall.candidateDescriptor as FakeCallableDescriptorForObject).classDescriptor
                            otherCandidate.addDiagnostic(
                                EnumEntryAmbiguityWarning(
                                    propertyDescriptor,
                                    enumEntryDescriptor
                                )
                            )
                            return setOf(otherCandidate)
                        }
                    }
                }
            }
//            if (
//                callComponents.languageVersionSettings.supportsFeature(LanguageFeature.OverloadResolutionByLambdaReturnType)   &&
//                cangjieCall.callKind != CangJieCallKind.CALLABLE_REFERENCE &&
//                candidates.all { it.isSuccessful } &&
//                candidates.all { resolutionCallbacks.inferenceSession.shouldRunCompletion(it) }
//            ) {
//                val candidatesWithAnnotation = candidates.filter {
//                    it.resolvedCall.candidateDescriptor.annotations.hasAnnotation(OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_FQ_NAME)
//                }.toSet()
//                val candidatesWithoutAnnotation = candidates - candidatesWithAnnotation
//                if (candidatesWithAnnotation.isNotEmpty()) {
//                    @Suppress("UNCHECKED_CAST")
//                    val newCandidates = cangjieCallCompleter.chooseCandidateRegardingOverloadResolutionByLambdaReturnType(
//                        maximallySpecificCandidates as Set<SimpleResolutionCandidate>,
//                        resolutionCallbacks
//                    )
//                    maximallySpecificCandidates = overloadingConflictResolver.chooseMaximallySpecificCandidates(
//                        newCandidates,
//                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
//                        discriminateGenerics = true
//                    )
//
//                    if (maximallySpecificCandidates.size > 1 && candidatesWithoutAnnotation.any { it in maximallySpecificCandidates }) {
//                        maximallySpecificCandidates = maximallySpecificCandidates.toMutableSet().apply { removeAll(candidatesWithAnnotation) }
//                        maximallySpecificCandidates.singleOrNull()?.addDiagnostic(CandidateChosenUsingOverloadResolutionByLambdaAnnotation())
//                    }
//                }
//            }
        }

        return maximallySpecificCandidates

    }

    fun resolveAndCompleteCall(
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        cangjieCall: CangJieCall,
        expectedType: UnwrappedType?,
        collectAllCandidates: Boolean,
    ): CallResolutionResult {
        val candidateFactory = createFactory(scopeTower, cangjieCall, resolutionCallbacks, expectedType)
        val candidates =   resolveCall(scopeTower, resolutionCallbacks, cangjieCall, collectAllCandidates, candidateFactory)

        if (collectAllCandidates) {
            return cangjieCallCompleter.createAllCandidatesResult(candidates, expectedType, resolutionCallbacks)
        }

        return cangjieCallCompleter.runCompletion(candidateFactory, candidates.toMutableSet(), expectedType, resolutionCallbacks)
    }
}
