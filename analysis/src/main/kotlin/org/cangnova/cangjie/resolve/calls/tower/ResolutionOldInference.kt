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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import org.cangnova.cangjie.extensions.internal.CandidateInterceptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.psiUtil.sure
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.TemporaryBindingTrace
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.CallTransformer
import org.cangnova.cangjie.resolve.calls.CandidateResolver
import org.cangnova.cangjie.resolve.calls.context.*
import org.cangnova.cangjie.resolve.calls.inference.BuilderInferenceSupport
import org.cangnova.cangjie.resolve.calls.model.CangJieCallDiagnostic
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import org.cangnova.cangjie.resolve.calls.results.ResolutionResultsHandler
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tasks.OldResolutionCandidate
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategyForInvoke
import org.cangnova.cangjie.resolve.calls.util.isConventionCall
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.SyntheticScopes
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.DeferredType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeApproximator
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.compactIfPossible

class  ResolutionOldInference(
    private val candidateResolver: CandidateResolver,
    private val towerResolver: TowerResolver,
    private val resolutionResultsHandler: ResolutionResultsHandler,
//    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val syntheticScopes: SyntheticScopes,
    private val languageVersionSettings: LanguageVersionSettings,
    private val builderInferenceSupport: BuilderInferenceSupport,
    private val deprecationResolver: DeprecationResolver,
    private val typeApproximator: TypeApproximator,
    private val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
    private val callResolver: CallResolver,
    private val candidateInterceptor: CandidateInterceptor
) {

    class MyCandidate(
        // Diagnostics that are already computed
        // if resultingApplicability is successful they must be the same as `diagnostics`,
        // otherwise they might be a bit different but result remains unsuccessful
        val eagerDiagnostics: List<CangJieCallDiagnostic>,
        val resolvedCall: MutableResolvedCall<*>,
        finalDiagnosticsComputation: (() -> List<CangJieCallDiagnostic>)? = null
    ) : Candidate {
        val diagnostics: List<CangJieCallDiagnostic> by lazy(LazyThreadSafetyMode.NONE) {
            finalDiagnosticsComputation?.invoke() ?: eagerDiagnostics
        }

        operator fun component1() = diagnostics
        operator fun component2() = resolvedCall

        override val resultingApplicability: CandidateApplicability by lazy(LazyThreadSafetyMode.NONE) {
            getResultApplicability(diagnostics)
        }

        override fun addCompatibilityWarning(other: Candidate) {
            // Only applicable for new inference
        }

        override val isSuccessful = getResultApplicability(eagerDiagnostics).isSuccess
    }

    sealed class ResolutionKind {
        internal abstract fun createTowerProcessor(
            outer: ResolutionOldInference,
            name: Name,
            tracing: TracingStrategy,
            scopeTower: ImplicitScopeTower,
            explicitReceiver: DetailedReceiver?,
            context: BasicCallResolutionContext
        ): ScopeTowerProcessor<MyCandidate>

        object EnumConstructor : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference,
                name: Name,
                tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower,
                explicitReceiver: DetailedReceiver?,
                context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createEnumConstructorProcessor(
                    scopeTower,
                    name,
                    functionFactory,
                    outer.CandidateFactoryProviderForInvokeImpl(functionFactory),
                    explicitReceiver
                )
            }
        }
//        object Enum : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: ResolutionOldInference,
//                name: Name,
//                tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower,
//                explicitReceiver: DetailedReceiver?,
//                context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                return createFunctionProcessor(
//                    scopeTower,
//                    name,
//                    functionFactory,
//                    outer.CandidateFactoryProviderForInvokeImpl(functionFactory),
//                    explicitReceiver
//                )
//            }
//        }

        object CaseEnum : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createFunctionProcessor(
                    scopeTower,
                    name,
                    functionFactory,
                    outer.CandidateFactoryProviderForInvokeImpl(functionFactory),
                    explicitReceiver
                )
            }
        }

        object Function : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createFunctionProcessor(
                    scopeTower,
                    name,
                    functionFactory,
                    outer.CandidateFactoryProviderForInvokeImpl(functionFactory),
                    explicitReceiver
                )
            }
        }

        object Variable : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference,
                name: Name,
                tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower,
                explicitReceiver: DetailedReceiver?,
                context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createVariableProcessor(scopeTower, name, variableFactory, explicitReceiver)
            }
        }


        object CallableReference : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference,
                name: Name,
                tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower,
                explicitReceiver: DetailedReceiver?,
                context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return PrioritizedCompositeScopeTowerProcessor(
                    createSimpleFunctionProcessor(
                        scopeTower,
                        name,
                        functionFactory,
                        explicitReceiver
                    ),
                    createVariableProcessor(
                        scopeTower,
                        name,
                        variableFactory,
                        explicitReceiver
                    )
                )
            }
        }

        //
        object Invoke : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
                // todo
                val call = (context.call as? CallTransformer.CallForImplicitInvoke).sure {
                    "Call should be CallForImplicitInvoke, but it is: ${context.call}"
                }
                return createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
                    createCallTowerProcessorForExplicitInvoke(
                        scopeTower,
                        functionFactory,
                        context.transformToReceiverWithSmartCastInfo(call.dispatchReceiver!!),
                        it
                    )
                }
            }

        }

        class GivenCandidates : ResolutionKind() {
            override fun createTowerProcessor(
                outer: ResolutionOldInference, name: Name, tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                throw IllegalStateException("Should be not called")
            }
        }
    }


    fun <D : CallableDescriptor> runResolution(
        context: BasicCallResolutionContext, name: Name, kind: ResolutionKind, tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {
//        TODO runResolution
        return OverloadResolutionResultsImpl.nameNotFound()
    }

    private val BasicCallResolutionContext.isSuperCall: Boolean get() = call.isCallWithSuperReceiver()
    internal fun Call.isCallWithSuperReceiver(): Boolean = explicitReceiver is SuperCallReceiverValue


    fun <D : CallableDescriptor> runResolutionForGivenCandidates(
        basicCallContext: BasicCallResolutionContext,
        tracing: TracingStrategy,
        candidates: Collection<OldResolutionCandidate<D>>
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCandidates = candidates.map { candidate ->
            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolveName candidate")
            val resolvedCall =
                MutableResolvedCallImpl.create(candidate, candidateTrace, tracing, basicCallContext.dataFlowInfoForArguments)

            if (deprecationResolver.isHiddenInResolution(
                    candidate.descriptor,
                    basicCallContext.call,
                    basicCallContext.trace.bindingContext,
                    basicCallContext.isSuperCall
                )
            ) {
                return@map MyCandidate(listOf(HiddenDescriptor), resolvedCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                resolvedCall,
                basicCallContext,
                candidateTrace,
                tracing,
                basicCallContext.call,
                CandidateResolveMode.EXIT_ON_FIRST_ERROR
            )
            candidateResolver.performResolutionForCandidateCall(
                callCandidateResolutionContext, basicCallContext.checkArguments
            ) // todo

            val diagnostics = listOfNotNull(createPreviousResolveError(resolvedCall.status))
            MyCandidate(diagnostics, resolvedCall) {
                resolvedCall.performRemainingTasks()
                listOfNotNull(createPreviousResolveError(resolvedCall.status))
            }
        }
        if (basicCallContext.collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(
                KnownResultProcessor(resolvedCandidates), TowerResolver.AllCandidatesCollector(), useOrder = false
            )
            return allCandidatesResult(allCandidates)
        }

        val processedCandidates = towerResolver.runWithEmptyTowerData(
            KnownResultProcessor(resolvedCandidates), TowerResolver.SuccessfulResultCollector(), useOrder = true
        )

        return convertToOverloadResults(processedCandidates, tracing, basicCallContext)
    }

    private fun <D : CallableDescriptor> convertToOverloadResults(
        candidates: Collection<MyCandidate>, tracing: TracingStrategy, basicCallContext: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCalls = candidates.map {
            val (diagnostics, resolvedCall) = it
            if (resolvedCall is VariableAsFunctionResolvedCallImpl) {
                // todo hacks
                tracing.bindReference(resolvedCall.variableCall.trace, resolvedCall.variableCall)
                tracing.bindResolvedCall(resolvedCall.variableCall.trace, resolvedCall)

                resolvedCall.variableCall.trace.addOwnDataTo(resolvedCall.functionCall.trace)

                resolvedCall.functionCall.tracingStrategy.bindReference(
                    resolvedCall.functionCall.trace,
                    resolvedCall.functionCall
                )
                //                resolvedCall.hackInvokeTracing.bindResolvedCall(resolvedCall.functionCall.trace, resolvedCall)
            } else {
                tracing.bindReference(resolvedCall.trace, resolvedCall)
                tracing.bindResolvedCall(resolvedCall.trace, resolvedCall)
            }

            if (resolvedCall.status.possibleTransformToSuccess()) {
                for (error in diagnostics) {
                    when (error) {
//
//                        is NestedClassViaInstanceReference -> tracing.nestedClassAccessViaInstanceReference(
//                            resolvedCall.trace,
//                            error.classDescriptor,
//                            resolvedCall.explicitReceiverKind
//                        )

                        is ErrorDescriptorDiagnostic -> {
                            // todo
                            //  return@map null
                        }

                        is ResolvedUsingDeprecatedVisibility -> {
                            reportResolvedUsingDeprecatedVisibility(
                                resolvedCall.call,
                                resolvedCall.candidateDescriptor,
                                resolvedCall.resultingDescriptor,
                                error,
                                resolvedCall.trace
                            )
                        }
                    }
                }
            }

            @Suppress("UNCHECKED_CAST") resolvedCall as MutableResolvedCall<D>
        }

        return resolutionResultsHandler.computeResultAndReportErrors(
            basicCallContext,
            tracing,
            resolvedCalls,
            languageVersionSettings
        )
    }

    private fun <D : CallableDescriptor> allCandidatesResult(allCandidates: Collection<MyCandidate>) =
        OverloadResolutionResultsImpl.nameNotFound<D>().apply {
            this.allCandidates = allCandidates.map {
                @Suppress("UNCHECKED_CAST") it.resolvedCall as MutableResolvedCall<D>
            }
        }

    private inner class CandidateFactoryProviderForInvokeImpl(
        val functionContext: CandidateFactoryImpl
    ) : CandidateFactoryProviderForInvoke<MyCandidate> {

        override fun transformCandidate(
            variable: MyCandidate,
            invoke: MyCandidate
        ): MyCandidate {
            @Suppress("UNCHECKED_CAST") val resolvedCallImpl = VariableAsFunctionResolvedCallImpl(
                invoke.resolvedCall as MutableResolvedCall<FunctionDescriptor>,
                variable.resolvedCall as MutableResolvedCall<VariableDescriptor>
            )
            assert(variable.resultingApplicability.isSuccess) {
                "Variable call must be success: $variable"
            }

            return MyCandidate(variable.eagerDiagnostics + invoke.eagerDiagnostics, resolvedCallImpl) {
                variable.diagnostics + invoke.diagnostics
            }
        }

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<MyCandidate> {
            val newCall = CallTransformer.stripCallArguments(functionContext.basicCallContext.call).let {
                if (stripExplicitReceiver) CallTransformer.stripReceiver(it) else it
            }
            return CandidateFactoryImpl(
                functionContext.name,
                functionContext.basicCallContext.replaceCall(newCall),
                functionContext.tracing
            )
        }

        override fun factoryForInvoke(
            variable: MyCandidate,
            useExplicitReceiver: Boolean
        ): Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<MyCandidate>>? {
            assert(variable.resolvedCall.status.possibleTransformToSuccess()) {
                "Incorrect status: ${variable.resolvedCall.status} for variable call: ${variable.resolvedCall} " +
                        "and descriptor: ${variable.resolvedCall.candidateDescriptor}"
            }
            val calleeExpression = variable.resolvedCall.call.calleeExpression
            val variableDescriptor = variable.resolvedCall.resultingDescriptor as VariableDescriptor
            assert(variable.resolvedCall.status.possibleTransformToSuccess() && calleeExpression != null) {
                "Unexpected variable candidate: $variable"
            }
            val variableType = variableDescriptor.type

            if (variableType is DeferredType && variableType.isComputing) {
                return null // todo: create special check that there is no invoke on variable
            }
            val basicCallContext = functionContext.basicCallContext
            val variableReceiver = ExpressionReceiver.create(
                calleeExpression!!,
                variableType,
                basicCallContext.trace.bindingContext
            )
            // used for smartCasts, see: DataFlowValueFactory.getIdForSimpleNameExpression
            functionContext.tracing.bindReference(variable.resolvedCall.trace, variable.resolvedCall)
            // todo hacks
            val functionCall = CallTransformer.CallForImplicitInvoke(
                basicCallContext.call.explicitReceiver?.takeIf { useExplicitReceiver },
                variableReceiver, basicCallContext.call, true
            )
            val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, functionCall, variableReceiver.type)
            val basicCallResolutionContext = basicCallContext.replaceBindingTrace(variable.resolvedCall.trace)
                .replaceCall(functionCall)
                .replaceContextDependency(ContextDependency.DEPENDENT) // todo

            val newContext =
                CandidateFactoryImpl(OperatorNameConventions.INVOKE, basicCallResolutionContext, tracingForInvoke)

            return basicCallResolutionContext.transformToReceiverWithSmartCastInfo(variableReceiver) to newContext
        }

    }

    private inner class CandidateFactoryImpl(
        val name: Name, val basicCallContext: BasicCallResolutionContext, val tracing: TracingStrategy
    ) : CandidateFactory<MyCandidate> {
        override fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            explicitReceiverKind: ExplicitReceiverKind
        ): MyCandidate {

            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolveName candidate")
            val candidateCall = MutableResolvedCallImpl(
                basicCallContext.call,
                towerCandidate.descriptor,
                towerCandidate.dispatchReceiver?.receiverValue,
                explicitReceiverKind,
                null,
                candidateTrace,
                tracing,
                basicCallContext.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )



            if (deprecationResolver.isHiddenInResolution(
                    towerCandidate.descriptor,
                    basicCallContext.call,
                    basicCallContext.trace.bindingContext,
                    basicCallContext.isSuperCall
                )
            ) {
                return MyCandidate(listOf(HiddenDescriptor), candidateCall)
            }

            val callCandidateResolutionContext = CallCandidateResolutionContext.create(
                candidateCall,
                basicCallContext,
                candidateTrace,
                tracing,
                basicCallContext.call,
                CandidateResolveMode.EXIT_ON_FIRST_ERROR
            )
            candidateResolver.performResolutionForCandidateCall(
                callCandidateResolutionContext,
                basicCallContext.checkArguments
            ) // todo

            val diagnostics = createDiagnosticsForCandidate(towerCandidate, candidateCall)
            return MyCandidate(diagnostics, candidateCall) {
                candidateCall.performRemainingTasks()
                createDiagnosticsForCandidate(towerCandidate, candidateCall)
            }
        }

        override fun createErrorCandidate(): MyCandidate {
            throw IllegalStateException("Not supported creating error candidate for the old type inference candidate factory")
        }

        private fun createDiagnosticsForCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver, candidateCall: MutableResolvedCallImpl<CallableDescriptor>
        ): List<ResolutionDiagnostic> = mutableListOf<ResolutionDiagnostic>().apply {
            addAll(towerCandidate.diagnostics)
            addAll(checkInfixAndOperator(basicCallContext.call, towerCandidate.descriptor))
            addIfNotNull(createPreviousResolveError(candidateCall.status))
        }

        private fun checkInfixAndOperator(call: Call, descriptor: CallableDescriptor): List<ResolutionDiagnostic> {
            if (descriptor !is FunctionDescriptor || ErrorUtils.isError(descriptor)) return emptyList()
            if (descriptor.name != name && (name == OperatorNameConventions.UNARY_PLUS || name == OperatorNameConventions.UNARY_MINUS)) {
                return listOf(DeprecatedUnaryPlusAsPlus)
            }

            val conventionError =
                if (isConventionCall(call) && !descriptor.isOperator) InvokeConventionCallNoOperatorModifier else null
//            val infixError = if (isInfixCall(call) && !descriptor.isInfix) InfixCallNoInfixModifier else null
            return listOfNotNull(conventionError/*, infixError*/)
        }

    }
}


fun ResolutionContext<*>.transformToReceiverWithSmartCastInfo(receiver: ReceiverValue) =
    transformToReceiverWithSmartCastInfo(
        scope.ownerDescriptor,
        trace.bindingContext,
        dataFlowInfo,
        receiver,
        languageVersionSettings,
        dataFlowValueFactory
    )

fun transformToReceiverWithSmartCastInfo(
    containingDescriptor: DeclarationDescriptor,
    bindingContext: BindingContext,
    dataFlowInfo: DataFlowInfo,
    receiver: ReceiverValue,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): ReceiverValueWithSmartCastInfo {
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiver, bindingContext, containingDescriptor)
    return ReceiverValueWithSmartCastInfo(
        receiver,
        dataFlowInfo.getCollectedTypes(dataFlowValue/*, languageVersionSettings*/).compactIfPossible(),
        dataFlowValue.isStable
    )
}

internal fun reportResolvedUsingDeprecatedVisibility(
    call: Call,
    candidateDescriptor: CallableDescriptor,
    resultingDescriptor: CallableDescriptor,
    diagnostic: ResolvedUsingDeprecatedVisibility,
    trace: BindingTrace
) {
    call.calleeExpression?.let{
        trace.record(
            BindingContext.DEPRECATED_SHORT_NAME_ACCESS, it
        )
    }


    val descriptorToLookup: DeclarationDescriptor = when (candidateDescriptor) {
        is ClassConstructorDescriptor -> candidateDescriptor.containingDeclaration
        // 枚举构造器现在直接通过 EnumConstructorDescriptor 暴露
        is EnumConstructorDescriptor -> candidateDescriptor.containingDeclaration
        is SyntheticMemberDescriptor<*> -> candidateDescriptor.baseDescriptorForSynthetic
        is PropertyDescriptor, is FunctionDescriptor -> candidateDescriptor
        else -> error(
            "Unexpected candidate descriptor of resolved call with " + "ResolvedUsingDeprecatedVisibility-diagnostic: $candidateDescriptor\n" + "Call context: ${call.callElement.parent?.text}"
        )
    }

    // If this descriptor was resolved from HierarchicalScope, then there can be another, non-deprecated path
    // in parents of base scope
//    val sourceScope = diagnostic.baseSourceScope
//    val canBeResolvedWithoutDeprecation = if (sourceScope is HierarchicalScope) {
//        descriptorToLookup.canBeResolvedWithoutDeprecation(
//            sourceScope,
//            diagnostic.lookupLocation
//        )
//    } else {
//        // Normally, that should be unreachable, but instead of asserting that, we will report diagnostic
//        false
//    }

//    if (!canBeResolvedWithoutDeprecation) {
//        trace.report(
//            Errors.DEPRECATED_ACCESS_BY_SHORT_NAME.on(call.callElement, resultingDescriptor)
//        )
//    }

}

internal class PreviousResolutionError(candidateLevel: CandidateApplicability) : ResolutionDiagnostic(candidateLevel)

internal fun createPreviousResolveError(status: ResolutionStatus): PreviousResolutionError? {
    val level = when (status) {
        ResolutionStatus.SUCCESS, ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> return null
        ResolutionStatus.UNSAFE_CALL_ERROR -> CandidateApplicability.UNSAFE_CALL
        ResolutionStatus.ARGUMENTS_MAPPING_ERROR -> CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
        ResolutionStatus.RECEIVER_TYPE_ERROR -> CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
        else -> CandidateApplicability.INAPPLICABLE
    }
    return PreviousResolutionError(level)
}
