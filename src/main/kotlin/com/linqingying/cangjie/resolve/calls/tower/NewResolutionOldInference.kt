package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.linqingying.cangjie.extensions.internal.CandidateInterceptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.psiUtil.sure
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.TemporaryBindingTrace
import com.linqingying.cangjie.resolve.calls.CallResolver
import com.linqingying.cangjie.resolve.calls.CallTransformer
import com.linqingying.cangjie.resolve.calls.CandidateResolver
import com.linqingying.cangjie.resolve.calls.context.*
import com.linqingying.cangjie.resolve.calls.inference.BuilderInferenceSupport
import com.linqingying.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.linqingying.cangjie.resolve.calls.model.MutableResolvedCall
import com.linqingying.cangjie.resolve.calls.model.ResolvedCallImpl
import com.linqingying.cangjie.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import com.linqingying.cangjie.resolve.calls.results.ResolutionResultsHandler
import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.linqingying.cangjie.resolve.calls.tasks.OldResolutionCandidate
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategyForInvoke
import com.linqingying.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.linqingying.cangjie.resolve.calls.util.isConventionCall
import com.linqingying.cangjie.resolve.deprecation.DeprecationResolver
import com.linqingying.cangjie.resolve.hasDynamicExtensionAnnotation
import com.linqingying.cangjie.resolve.scopes.SyntheticScopes
import com.linqingying.cangjie.resolve.scopes.receivers.*
import com.linqingying.cangjie.types.DeferredType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.TypeApproximator
import com.linqingying.cangjie.types.isDynamic
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.addIfNotNull
import com.linqingying.cangjie.utils.compactIfPossible

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
        internal abstract fun createTowerProcessor(
            outer: NewResolutionOldInference,
            name: Name,
            tracing: TracingStrategy,
            scopeTower: ImplicitScopeTower,
            explicitReceiver: DetailedReceiver?,
            context: BasicCallResolutionContext
        ): ScopeTowerProcessor<MyCandidate>

        object Enum : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference,
                name: Name,
                tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower,
                explicitReceiver: DetailedReceiver?,
                context: BasicCallResolutionContext
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

        object CaseEnum : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
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
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
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
                outer: NewResolutionOldInference,
                name: Name,
                tracing: TracingStrategy,
                scopeTower: ImplicitScopeTower,
                explicitReceiver: DetailedReceiver?,
                context: BasicCallResolutionContext
            ): ScopeTowerProcessor<MyCandidate> {
                val variableFactory = outer.CandidateFactoryImpl(name, context, tracing)
                return createVariableAndObjectProcessor(scopeTower, name, variableFactory, explicitReceiver)
            }
        }


        object CallableReference : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference,
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
                        explicitReceiver,
                        classValueReceiver = false
                    ),
                    createVariableProcessor(
                        scopeTower,
                        name,
                        variableFactory,
                        explicitReceiver,
                        classValueReceiver = false
                    )
                )
            }
        }

        //
        object Invoke : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
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
                        context.transformToReceiverWithSmartCastInfo(call.dispatchReceiver),
                        it
                    )
                }
            }

        }

        class GivenCandidates : ResolutionKind() {
            override fun createTowerProcessor(
                outer: NewResolutionOldInference, name: Name, tracing: TracingStrategy,
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
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiver: ReceiverValueWithSmartCastInfo?
        ): MyCandidate {

            val candidateTrace = TemporaryBindingTrace.create(basicCallContext.trace, "Context for resolve candidate")
            val candidateCall = ResolvedCallImpl(
                basicCallContext.call,
                towerCandidate.descriptor,
                towerCandidate.dispatchReceiver?.receiverValue,
                extensionReceiver?.receiverValue,
                explicitReceiverKind,
                null,
                candidateTrace,
                tracing,
                basicCallContext.dataFlowInfoForArguments // todo may be we should create new mutable info for arguments
            )

            /**
             * See https://jetbrains.quip.com/qcTDAFcgFLEM
             *
             * For now we have only 2 functions with dynamic receivers: iterator() and unsafeCast()
             * Both this function are marked via @kotlin.internal.DynamicExtension.
             */
            if (extensionReceiver != null) {
                val parameterIsDynamic = towerCandidate.descriptor.extensionReceiverParameter!!.value.type.isDynamic()
                val argumentIsDynamic = extensionReceiver.receiverValue.type.isDynamic()

                if (parameterIsDynamic != argumentIsDynamic || (parameterIsDynamic && !towerCandidate.descriptor.hasDynamicExtensionAnnotation())) {
                    return MyCandidate(listOf(HiddenExtensionRelatedToDynamicTypes), candidateCall)
                }
            }

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

        /**
         * The function is called only inside [NoExplicitReceiverScopeTowerProcessor] with [TowerData.BothTowerLevelAndContextReceiversGroup].
         * This case involves only [SimpleCandidateFactory].
         */
        override fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver,
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
        ): MyCandidate =
            error("${this::class.simpleName} doesn't support candidates with multiple extension receiver candidates")

        override fun createErrorCandidate(): MyCandidate {
            throw IllegalStateException("Not supported creating error candidate for the old type inference candidate factory")
        }

        private fun createDiagnosticsForCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver, candidateCall: ResolvedCallImpl<CallableDescriptor>
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
    trace.record(
        BindingContext.DEPRECATED_SHORT_NAME_ACCESS, call.calleeExpression
    )

    val descriptorToLookup: DeclarationDescriptor = when (candidateDescriptor) {
        is ClassConstructorDescriptor -> candidateDescriptor.containingDeclaration
        is FakeCallableDescriptorForObject -> candidateDescriptor.classDescriptor
        is SyntheticMemberDescriptor<*> -> candidateDescriptor.baseDescriptorForSynthetic
        is PropertyDescriptor, is FunctionDescriptor -> candidateDescriptor
        is EnumClassCallableDescriptor -> candidateDescriptor.type
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
