package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.reportDiagnosticOnce
import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.MissingSupertypesResolver
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.checkers.CallCheckerContext
import com.huawei.cangjie.resolve.calls.components.CallableReferenceAdaptation
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategyImpl
import com.huawei.cangjie.resolve.calls.util.CallMaker
import com.huawei.cangjie.resolve.calls.util.extractCallableReferenceExpression
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.scopes.receivers.TransientReceiver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.DoubleColonExpressionResolver
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo

class ResolvedAtomCompleter(
    private val resultSubstitutor: NewTypeSubstitutor,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val builtIns: CangJieBuiltIns,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val dataFlowValueFactory: DataFlowValueFactory,
//    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val callComponents: CangJieCallComponents,

    ) {
    private val topLevelCallCheckerContext = CallCheckerContext(
        topLevelCallContext, deprecationResolver, moduleDescriptor,
//        missingSupertypesResolver,
//        callComponents,
    )
    private val topLevelTrace = topLevelCallCheckerContext.trace
    private fun extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom: ResolvedCallAtom): Set<CangJieCallDiagnostic> {
        val psiCall = CangJieToResolvedCallTransformer.keyForPartiallyResolvedCall(resolvedCallAtom)
        val partialCallContainer = topLevelTrace[BindingContext.ONLY_RESOLVED_CALL, psiCall]

        return partialCallContainer?.result?.diagnostics.orEmpty().toSet()
    }
    fun completeAll(resolvedAtom: ResolvedAtom) {
        if (!resolvedAtom.analyzed)
            return
        resolvedAtom.subResolvedAtoms?.forEach { subKtPrimitive ->
            completeAll(subKtPrimitive)
        }
        complete(resolvedAtom)
    }
    private fun complete(resolvedAtom: ResolvedAtom) {
        if (topLevelCallContext.inferenceSession.callCompleted(resolvedAtom)) {
            return
        }

        when (resolvedAtom) {
//                is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
//            is ResolvedCallableReferenceArgumentAtom -> completeCallableReferenceArgument(resolvedAtom)
//                is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom, emptyList())
//                is ResolvedSubCallArgument -> completeSubCallArgument(resolvedAtom)
//                is ResolvedExpressionAtom -> completeExpression(resolvedAtom)
            else -> {}
        }
    }

//    fun completeCallableReferenceArgument(resolvedAtom: ResolvedCallableReferenceArgumentAtom): CangJieType? {
//        if (resolvedAtom.completed) return null
//
//        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceCangJieCallArgumentImpl
//        val callableReferenceCallCandidate = resolvedAtom.candidate ?: return null
//        val descriptor = when (callableReferenceCallCandidate.candidate) {
//            is FunctionDescriptor -> topLevelCallContext.trace.get(
//                BindingContext.FUNCTION,
//                psiCallArgument.cjCallableReferenceExpression
//            )
//
//            is VariableDescriptor -> topLevelCallContext.trace.get(
//                BindingContext.VARIABLE,
//                psiCallArgument.cjCallableReferenceExpression
//            )
//
//            else -> null
//        }
//        val dataFlowInfo = resolvedAtom.atom.psiCallArgument.dataFlowInfoAfterThisArgument
//        val resolvedCall = NewCallableReferenceResolvedCall<CallableDescriptor>(
//            resolvedAtom,
////            typeApproximator,
////            expressionTypingServices.languageVersionSettings
//        )
//
//        return completeCallableReference(callableReferenceCallCandidate, descriptor, resolvedCall, dataFlowInfo)
//            .also { resolvedAtom.completed = true }
//    }

//    private fun extractCallableReferenceResultTypeInfoFromDescriptor(
//        callableCandidate: CallableReferenceResolutionCandidate,
//        recordedDescriptor: CallableDescriptor
//    ): CallableReferenceResultTypeInfo {
//        val dispatchReceiver = recordedDescriptor.dispatchReceiverParameter?.value
//            ?: callableCandidate.dispatchReceiver?.receiver?.receiverValue
//        val extensionReceiver = recordedDescriptor.extensionReceiverParameter?.value
//            ?: callableCandidate.extensionReceiver?.receiver?.receiverValue
//        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
//            ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiver
//            ExplicitReceiverKind.EXTENSION_RECEIVER -> extensionReceiver
//            else -> null
//        }
//
//        return CallableReferenceResultTypeInfo(
//            dispatchReceiver,
//            extensionReceiver,
//            explicitCallableReceiver,
//            EmptySubstitutor,
//            callableCandidate.reflectionCandidateType.replaceFunctionTypeArgumentsByDescriptor(recordedDescriptor)
//        )
//    }
//
//    private fun updateCallableReferenceResultType(callableCandidate: CallableReferenceResolutionCandidate): CallableReferenceResultTypeInfo? {
//        val callableReferenceExpression =
//            callableCandidate.resolvedCall.atom.psiCangJieCall.psiCall.callElement.parent as? KtCallableReferenceExpression
//                ?: return null
//        val freshSubstitutor = callableCandidate.freshVariablesSubstitutor ?: return null
//        val resultTypeParameters =
//            freshSubstitutor.freshVariables.map { resultSubstitutor.safeSubstitute(it.defaultType) }
//        val typeParametersSubstitutor = NewTypeSubstitutorByConstructorMap(
//            callableCandidate.candidate.typeParameters.map { it.typeConstructor }.zip(resultTypeParameters).toMap()
//        )
//        val resultSubstitutor = if (callableCandidate.candidate.isSupportedForCallableReference()) {
//            ComposedSubstitutor(typeParametersSubstitutor, resultSubstitutor)
//        } else EmptySubstitutor
//
//        // write down type for callable reference expression
//        val resultType = resultSubstitutor.safeSubstitute(callableCandidate.reflectionCandidateType)
//
//        argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
//            topLevelTrace, expressionTypingServices.statementFilter, resultType, callableReferenceExpression
//        )
//
//        val dispatchReceiver =
//            callableCandidate.dispatchReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
//        val extensionReceiver =
//            callableCandidate.extensionReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
//
//        when (callableCandidate.candidate) {
//            is FunctionDescriptor -> doubleColonExpressionResolver.bindFunctionReference(
//                callableReferenceExpression,
//                resultType,
//                topLevelCallContext,
//                callableCandidate.candidate
//            )
//
//            is PropertyDescriptor -> doubleColonExpressionResolver.bindPropertyReference(
//                callableReferenceExpression,
//                resultType,
//                topLevelCallContext
//            )
//        }
//
//        doubleColonExpressionResolver.checkReferenceIsToAllowedMember(
//            callableCandidate.candidate,
//            topLevelCallContext.trace,
//            callableReferenceExpression
//        )
//
//        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
//            ExplicitReceiverKind.DISPATCH_RECEIVER -> callableCandidate.dispatchReceiver
//            ExplicitReceiverKind.EXTENSION_RECEIVER -> callableCandidate.extensionReceiver
//            else -> null
//        }
//        val explicitReceiver = explicitCallableReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
//
//        return CallableReferenceResultTypeInfo(
//            dispatchReceiver,
//            extensionReceiver,
//            explicitReceiver,
//            resultSubstitutor,
//            resultType
//        )
//    }
//
//
//    private fun recordArgumentAdaptationForCallableReference(
//        resolvedCall: NewAbstractResolvedCall<*>,
//        callableReferenceAdaptation: CallableReferenceAdaptation?
//    ) {
//        if (callableReferenceAdaptation == null) return
//
//        val callElement = resolvedCall.call.callElement
//        val isUnboundReference = resolvedCall.dispatchReceiver is TransientReceiver
//
//        fun makeFakeValueArgument(callArgument: CangJieCallArgument): ValueArgument {
//            val fakeCallArgument = callArgument as? FakeCangJieCallArgumentForCallableReference
//                ?: throw AssertionError("FakeCangJieCallArgumentForCallableReference expected: $callArgument")
//            return FakePositionalValueArgumentForCallableReferenceImpl(
//                callElement,
//                if (isUnboundReference) fakeCallArgument.index + 1 else fakeCallArgument.index
//            )
//        }
//
//        // We should record argument mapping only if callable reference requires adaptation:
//        // - argument mapping is non-trivial: any of the arguments were mapped as defaults or vararg elements;
//        // - result should be coerced.
//        var hasNonTrivialMapping = false
//        val mappedArguments = ArrayList<Pair<ValueParameterDescriptor, ResolvedValueArgument>>()
//        for ((valueParameter, resolvedCallArgument) in callableReferenceAdaptation.mappedArguments) {
//            val resolvedValueArgument = when (resolvedCallArgument) {
//                ResolvedCallArgument.DefaultArgument -> {
//                    hasNonTrivialMapping = true
//                    DefaultValueArgument.DEFAULT
//                }
//
//                is ResolvedCallArgument.SimpleArgument -> {
//                    val valueArgument = makeFakeValueArgument(resolvedCallArgument.callArgument)
//                    if (valueParameter.isVararg)
//                        VarargValueArgument(
//                            listOf(
//                                FakeImplicitSpreadValueArgumentForCallableReferenceImpl(callElement, valueArgument)
//                            )
//                        )
//                    else
//                        ExpressionValueArgument(valueArgument)
//                }
//
//                is ResolvedCallArgument.VarargArgument -> {
//                    hasNonTrivialMapping = true
//                    VarargValueArgument(
//                        resolvedCallArgument.arguments.map {
//                            makeFakeValueArgument(it)
//                        }
//                    )
//                }
//            }
//            mappedArguments.add(valueParameter to resolvedValueArgument)
//        }
//        if (hasNonTrivialMapping || isCallableReferenceWithImplicitConversion(
//                resolvedCall,
//                callableReferenceAdaptation
//            )
//        ) {
//            resolvedCall.updateValueArguments(mappedArguments.toMap())
//        }
//    }
//
//    private fun completeCallableReference(
//        callableCandidate: CallableReferenceResolutionCandidate,
//        recordedDescriptor: CallableDescriptor?,
//        resolvedCall: NewAbstractResolvedCall<*>,
//        additionalDataFlowInfo: DataFlowInfo? = null,
//    ): CangJieType? {
//        val rawExtensionReceiver = callableCandidate.extensionReceiver
////        val unrestrictedBuilderInferenceSupported =
////            topLevelCallContext.languageVersionSettings.supportsFeature(LanguageFeature.UnrestrictedBuilderInference)
//        val callableReferenceExpression =
//            callableCandidate.resolvedCall.atom.psiCangJieCall.extractCallableReferenceExpression() ?: return null
//
//        if (rawExtensionReceiver != null /* && !unrestrictedBuilderInferenceSupported*/ && rawExtensionReceiver.receiver.receiverValue.type.contains { it is StubTypeForBuilderInference }) {
//            topLevelTrace.reportDiagnosticOnce(
//                Errors.TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE.on(
//                    callableReferenceExpression
//                )
//            )
//            return null
//        }
//
//        val resultTypeInfo = if (recordedDescriptor != null) {
//            extractCallableReferenceResultTypeInfoFromDescriptor(callableCandidate, recordedDescriptor)
//        } else {
//            updateCallableReferenceResultType(callableCandidate)
//        }
//
//        if (resultTypeInfo == null) return null
//
//        resolvedCall.apply {
//            if (resultTypeInfo.dispatchReceiver != null) {
//                updateDispatchReceiverType(resultTypeInfo.dispatchReceiver.type)
//            }
//            if (resultTypeInfo.extensionReceiver != null) {
//                updateExtensionReceiverType(resultTypeInfo.extensionReceiver.type)
//            }
//            setResultingSubstitutor(resultTypeInfo.substitutor)
//        }
//
//        recordArgumentAdaptationForCallableReference(resolvedCall, callableCandidate.callableReferenceAdaptation)
//
//        val psiCall = CallMaker.makeCall(
//            callableReferenceExpression.callableReference,
//            resultTypeInfo.explicitReceiver,
//            null,
//            callableReferenceExpression.callableReference,
//            emptyList()
//        )
//        val tracing = TracingStrategyImpl.create(callableReferenceExpression.callableReference, psiCall)
//
//        tracing.bindCall(topLevelTrace, psiCall)
//        tracing.bindReference(topLevelTrace, resolvedCall)
//        tracing.bindResolvedCall(topLevelTrace, resolvedCall)
//
//        // TODO: probably we should also record key 'DATA_FLOW_INFO_BEFORE', see ExpressionTypingVisitorDispatcher.getTypeInfo
//        val typeInfo = if (additionalDataFlowInfo != null) {
//            createTypeInfo(resultTypeInfo.resultType, additionalDataFlowInfo)
//        } else {
//            createTypeInfo(resultTypeInfo.resultType)
//        }
//
//        topLevelTrace.record(BindingContext.EXPRESSION_TYPE_INFO, callableReferenceExpression, typeInfo)
//        topLevelTrace.record(BindingContext.PROCESSED, callableReferenceExpression)
//
//        cangjieToResolvedCallTransformer.runCallCheckers(resolvedCall, topLevelCallCheckerContext)
//
//        return resultTypeInfo.resultType
//    }

    fun completeResolvedCall(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): NewAbstractResolvedCall<*> {
        val diagnosticsFromPartiallyResolvedCall = extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom)

//        clearPartiallyResolvedCall(resolvedCallAtom)

//        val atom = resolvedCallAtom.atom
//        if (atom.psiCangJieCall is PSICangJieCallForVariable) return null

        val allDiagnostics = diagnostics + diagnosticsFromPartiallyResolvedCall

        val resolvedCall = cangjieToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(
            resolvedCallAtom,
            topLevelTrace,
            resultSubstitutor,
            allDiagnostics
        )

        //
//        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) {
//            resolvedCall.functionCall
//        } else resolvedCall
//        if (ErrorUtils.isError(resolvedCall.candidateDescriptor)) {
//            cangjieToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
//            checkMissingReceiverSupertypes(resolvedCall, missingSupertypesResolver, topLevelTrace)
//            return resolvedCall
//        }
//
//        val psiCallForResolutionContext = when (atom) {
//            // PARTIAL_CALL_RESOLUTION_CONTEXT has been written for the baseCall
//            is PSICangJieCallForInvoke -> atom.baseCall.psiCall
//            else -> atom.psiCangJieCall.psiCall
//        }
//
//        val callElement = psiCallForResolutionContext.callElement
//        if (callElement is CjExpression) {
//            val recordedType = topLevelCallContext.trace.getType(callElement)
//            if (recordedType != null && recordedType.shouldBeUpdated() && resolvedCall.resultingDescriptor.returnType != null) {
//                topLevelCallContext.trace.recordType(callElement, resolvedCall.resultingDescriptor.returnType)
//            }
//        }



//        val resolutionContextForPartialCall =
//            topLevelCallContext.trace[BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCallForResolutionContext]
//
//        val callCheckerContext = if (resolutionContextForPartialCall != null)
//            CallCheckerContext(
//                resolutionContextForPartialCall.replaceBindingTrace(topLevelTrace),
//                deprecationResolver,
//                moduleDescriptor,
//                missingSupertypesResolver,
//                callComponents,
//            )
//        else
//            topLevelCallCheckerContext

        cangjieToResolvedCallTransformer.bind(topLevelTrace, resolvedCall)
//
//        cangjieToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
//        cangjieToResolvedCallTransformer.runCallCheckers(resolvedCall, callCheckerContext)
//        cangjieToResolvedCallTransformer.runAdditionalReceiversCheckers(resolvedCall, topLevelCallContext)
//
//        cangjieToResolvedCallTransformer.reportDiagnostics(
//            topLevelCallContext,
//            topLevelTrace,
//            resolvedCall,
//            allDiagnostics
//        )

        return resolvedCall
    }
}
