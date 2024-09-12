package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.extensions.internal.CandidateInterceptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.TemporaryBindingTrace
import com.huawei.cangjie.resolve.calls.CallResolver
import com.huawei.cangjie.resolve.calls.CandidateResolver
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.context.CallCandidateResolutionContext
import com.huawei.cangjie.resolve.calls.context.CandidateResolveMode
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.inference.BuilderInferenceSupport
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall
import com.huawei.cangjie.resolve.calls.model.ResolvedCallImpl
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import com.huawei.cangjie.resolve.calls.results.ResolutionResultsHandler
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.tasks.OldResolutionCandidate
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.scopes.HierarchicalScope
import com.huawei.cangjie.resolve.scopes.SyntheticScopes
import com.huawei.cangjie.resolve.scopes.canBeResolvedWithoutDeprecation
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.resolve.scopes.receivers.SuperCallReceiverValue
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.utils.compactIfPossible

class NewResolutionOldInference(
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
//        abstract internal fun createTowerProcessor(
//            outer: NewResolutionOldInference,
//            name: Name,
//            tracing: TracingStrategy,
//            scopeTower: ImplicitScopeTower,
//            explicitReceiver: DetailedReceiver?,
//            context: BasicCallResolutionContext
//        ): ScopeTowerProcessor<MyCandidate>

        object Function : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
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
        }

        object Variable : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                return createVariableAndObjectProcessor(scopeTower, name, variableFactory, explicitReceiver)
//            }
        }

        //
//        object CallableReference : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                return PrioritizedCompositeScopeTowerProcessor(
//                    createSimpleFunctionProcessor(scopeTower, name, functionFactory, explicitReceiver, classValueReceiver = false),
//                    createVariableProcessor(scopeTower, name, variableFactory, explicitReceiver, classValueReceiver = false)
//                )
//            }
//        }
//
        object Invoke : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                val functionFactory = outer.CandidateFactoryImpl(name, context, tracing)
//                // todo
//                val call = (context.call as? CallTransformer.CallForImplicitInvoke).sure {
//                    "Call should be CallForImplicitInvoke, but it is: ${context.call}"
//                }
//                return createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
//                    createCallTowerProcessorForExplicitInvoke(
//                        scopeTower,
//                        functionFactory,
//                        context.transformToReceiverWithSmartCastInfo(call.dispatchReceiver),
//                        it
//                    )
//                }
//            }

        }

        class GivenCandidates : ResolutionKind() {
//            override fun createTowerProcessor(
//                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
//                scopeTower: ImplicitScopeTower, explicitReceiver: DetailedReceiver?, context: BasicCallResolutionContext
//            ): ScopeTowerProcessor<MyCandidate> {
//                throw IllegalStateException("Should be not called")
//            }
        }
    }


    fun <D : CallableDescriptor> runResolution(
        context: BasicCallResolutionContext,
        name: Name,
        kind: ResolutionKind,
        tracing: TracingStrategy
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
            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val resolvedCall =
                ResolvedCallImpl.create(candidate, candidateTrace, tracing, basicCallContext.dataFlowInfoForArguments)

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
                resolvedCall, basicCallContext, candidateTrace, tracing, basicCallContext.call,
                CandidateResolveMode.EXIT_ON_FIRST_ERROR
            )
            candidateResolver.performResolutionForCandidateCall(
                callCandidateResolutionContext,
                basicCallContext.checkArguments
            ) // todo

            val diagnostics = listOfNotNull(createPreviousResolveError(resolvedCall.status))
            MyCandidate(diagnostics, resolvedCall) {
                resolvedCall.performRemainingTasks()
                listOfNotNull(createPreviousResolveError(resolvedCall.status))
            }
        }
        if (basicCallContext.collectAllCandidates) {
            val allCandidates = towerResolver.runWithEmptyTowerData(
                KnownResultProcessor(resolvedCandidates),
                TowerResolver.AllCandidatesCollector(), useOrder = false
            )
            return allCandidatesResult(allCandidates)
        }

        val processedCandidates = towerResolver.runWithEmptyTowerData(
            KnownResultProcessor(resolvedCandidates),
            TowerResolver.SuccessfulResultCollector(), useOrder = true
        )

        return convertToOverloadResults(processedCandidates, tracing, basicCallContext)
    }

    private fun <D : CallableDescriptor> convertToOverloadResults(
        candidates: Collection<MyCandidate>,
        tracing: TracingStrategy,
        basicCallContext: BasicCallResolutionContext
    ): OverloadResolutionResultsImpl<D> {
        val resolvedCalls = candidates.map {
            val (diagnostics, resolvedCall) = it
            if (resolvedCall is VariableAsFunctionResolvedCallImpl) {
                // todo hacks
                tracing.bindReference(resolvedCall.variableCall.trace, resolvedCall.variableCall)
                tracing.bindResolvedCall(resolvedCall.variableCall.trace, resolvedCall)

                resolvedCall.variableCall.trace.addOwnDataTo(resolvedCall.functionCall.trace)

                resolvedCall.functionCall.tracingStrategy.bindReference(resolvedCall.functionCall.trace, resolvedCall.functionCall)
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
                                resolvedCall.call, resolvedCall.candidateDescriptor,
                                resolvedCall.resultingDescriptor, error, resolvedCall.trace
                            )
                        }
                    }
                }
            }

            @Suppress("UNCHECKED_CAST")
            resolvedCall as MutableResolvedCall<D>
        }

        return resolutionResultsHandler.computeResultAndReportErrors(basicCallContext, tracing, resolvedCalls, languageVersionSettings)
    }
    private fun <D : CallableDescriptor> allCandidatesResult(allCandidates: Collection<MyCandidate>) =
        OverloadResolutionResultsImpl.nameNotFound<D>().apply {
            this.allCandidates = allCandidates.map {
                @Suppress("UNCHECKED_CAST")
                it.resolvedCall as MutableResolvedCall<D>
            }
        }

}


fun ResolutionContext<*>.transformToReceiverWithSmartCastInfo(receiver: ReceiverValue) =
    transformToReceiverWithSmartCastInfo(
        scope.ownerDescriptor,
        trace.bindingContext,
        dataFlowInfo,
        receiver, /*languageVersionSettings,*/
        dataFlowValueFactory
    )

fun transformToReceiverWithSmartCastInfo(
    containingDescriptor: DeclarationDescriptor,
    bindingContext: BindingContext,
    dataFlowInfo: DataFlowInfo,
    receiver: ReceiverValue,
//    languageVersionSettings: LanguageVersionSettings,
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
    trace.record(
        BindingContext.DEPRECATED_SHORT_NAME_ACCESS,
        call.calleeExpression
    )

    val descriptorToLookup: DeclarationDescriptor = when (candidateDescriptor) {
        is ClassConstructorDescriptor -> candidateDescriptor.containingDeclaration
        is FakeCallableDescriptorForObject -> candidateDescriptor.classDescriptor
        is SyntheticMemberDescriptor<*> -> candidateDescriptor.baseDescriptorForSynthetic
        is PropertyDescriptor, is FunctionDescriptor -> candidateDescriptor
        else -> error(
            "Unexpected candidate descriptor of resolved call with " +
                    "ResolvedUsingDeprecatedVisibility-diagnostic: $candidateDescriptor\n" +
                    "Call context: ${call.callElement.parent?.text}"
        )
    }

    // If this descriptor was resolved from HierarchicalScope, then there can be another, non-deprecated path
    // in parents of base scope
    val sourceScope = diagnostic.baseSourceScope
    val canBeResolvedWithoutDeprecation = if (sourceScope is HierarchicalScope) {
        descriptorToLookup.canBeResolvedWithoutDeprecation(
            sourceScope,
            diagnostic.lookupLocation
        )
    } else {
        // Normally, that should be unreachable, but instead of asserting that, we will report diagnostic
        false
    }

    if (!canBeResolvedWithoutDeprecation) {
        trace.report(
            Errors.DEPRECATED_ACCESS_BY_SHORT_NAME.on(call.callElement, resultingDescriptor)
        )
    }

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
