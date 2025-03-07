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

package cn.cangnova.cangjie.resolve.calls.tower

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.config.LanguageFeature
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.diagnostics.Errors
import cn.cangnova.cangjie.diagnostics.Errors.UNSUPPORTED_FEATURE
import cn.cangnova.cangjie.diagnostics.reportDiagnosticOnce
import cn.cangnova.cangjie.psi.CjCallableReference
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.psi.ValueArgument
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.DoubleColonExpressionResolver
import cn.cangnova.cangjie.resolve.MissingSupertypesResolver
import cn.cangnova.cangjie.resolve.caches.MissingDependencySupertypeChecker
import cn.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import cn.cangnova.cangjie.resolve.calls.checkers.CallCheckerContext
import cn.cangnova.cangjie.resolve.calls.components.CallableReferenceAdaptation
import cn.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import cn.cangnova.cangjie.resolve.calls.components.isVararg
import cn.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import cn.cangnova.cangjie.resolve.calls.inference.ComposedSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.EmptySubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import cn.cangnova.cangjie.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import cn.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import cn.cangnova.cangjie.resolve.calls.model.*
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import cn.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import cn.cangnova.cangjie.resolve.calls.tasks.TracingStrategyImpl
import cn.cangnova.cangjie.resolve.calls.util.CallMaker
import cn.cangnova.cangjie.resolve.calls.util.extractCallableReferenceExpression
import cn.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import cn.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.expressions.CoercionStrategy

import cn.cangnova.cangjie.types.expressions.ExpressionTypingServices
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.types.util.contains
import cn.cangnova.cangjie.types.util.isUnit
import cn.cangnova.cangjie.types.util.shouldBeUpdated

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
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val callComponents: CangJieCallComponents,

    ) {
    private val topLevelCallCheckerContext = CallCheckerContext(
        topLevelCallContext, deprecationResolver, moduleDescriptor,
        missingSupertypesResolver,
        callComponents,
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
        resolvedAtom.subResolvedAtoms?.forEach { subCjPrimitive ->
            completeAll(subCjPrimitive)
        }
        complete(resolvedAtom)
    }

    private fun complete(resolvedAtom: ResolvedAtom) {
        if (topLevelCallContext.inferenceSession.callCompleted(resolvedAtom)) {
            return
        }

        when (resolvedAtom) {
//                is ResolvedCollectionLiteralAtom -> completeCollectionLiteralCalls(resolvedAtom)
            is ResolvedCallableReferenceArgumentAtom -> completeCallableReferenceArgument(resolvedAtom)
//                is ResolvedLambdaAtom -> completeLambda(resolvedAtom)
            is ResolvedCallAtom -> completeResolvedCall(resolvedAtom, emptyList())
//                is ResolvedSubCallArgument -> completeSubCallArgument(resolvedAtom)
//                is ResolvedExpressionAtom -> completeExpression(resolvedAtom)
            else -> {}
        }
    }

    fun completeCallableReferenceArgument(resolvedAtom: ResolvedCallableReferenceArgumentAtom): CangJieType? {
        if (resolvedAtom.completed) return null

        val psiCallArgument = resolvedAtom.atom.psiCallArgument as CallableReferenceCangJieCallArgumentImpl
        val callableReferenceCallCandidate = resolvedAtom.candidate ?: return null
        val descriptor = when (callableReferenceCallCandidate.candidate) {
            is FunctionDescriptor -> topLevelCallContext.trace.get(
                BindingContext.FUNCTION,
                psiCallArgument.cjCallableReferenceExpression
            )
//
//            is VariableCallableDescriptor -> topLevelCallContext.trace.get(
//                BindingContext.VARIABLE,
//                psiCallArgument.cjCallableReferenceExpression
//            )

            else -> null
        }
        val dataFlowInfo = resolvedAtom.atom.psiCallArgument.dataFlowInfoAfterThisArgument
        val resolvedCall = NewCallableReferenceResolvedCall<CallableDescriptor>(
            resolvedAtom,
            typeApproximator,
            expressionTypingServices.languageVersionSettings
        )

        return completeCallableReference(callableReferenceCallCandidate, descriptor, resolvedCall, dataFlowInfo)
            .also { resolvedAtom.completed = true }
    }

    private data class CallableReferenceResultTypeInfo(
        val dispatchReceiver: ReceiverValue?,
        val extensionReceiver: ReceiverValue?,
        val explicitReceiver: ReceiverValue?,
        val substitutor: NewTypeSubstitutor,
        val resultType: CangJieType
    )

    private fun extractCallableReferenceResultTypeInfoFromDescriptor(
        callableCandidate: CallableReferenceResolutionCandidate,
        recordedDescriptor: CallableDescriptor
    ): CallableReferenceResultTypeInfo {
        val dispatchReceiver = recordedDescriptor.dispatchReceiverParameter?.value
            ?: callableCandidate.dispatchReceiver?.receiver?.receiverValue
        val extensionReceiver = recordedDescriptor.extensionReceiverParameter?.value
            ?: callableCandidate.extensionReceiver?.receiver?.receiverValue
        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> extensionReceiver
            else -> null
        }

        return CallableReferenceResultTypeInfo(
            dispatchReceiver,
            extensionReceiver,
            explicitCallableReceiver,
            EmptySubstitutor,
            callableCandidate.reflectionCandidateType.replaceFunctionTypeArgumentsByDescriptor(recordedDescriptor)
        )
    }

    private fun CangJieType.replaceFunctionTypeArgumentsByDescriptor(descriptor: CallableDescriptor) =
        when (descriptor) {
            is CallableMemberDescriptor -> {
                val newArgumentTypes = buildList {
                    descriptor.extensionReceiverParameter?.let { add(it.type) }
                    addAll(descriptor.valueParameters.map { it.type })
                    add(descriptor.returnType)
                }
                if (newArgumentTypes.size == arguments.size) {
                    replace(arguments.mapIndexed { i, type ->
                        newArgumentTypes[i]?.let { type.replaceType(it) } ?: type
                    })
                } else this
            }

            is ValueDescriptor -> replace(descriptor.type.arguments)
            else -> this
        }

    private fun recordArgumentAdaptationForCallableReference(
        resolvedCall: NewAbstractResolvedCall<*>,
        callableReferenceAdaptation: CallableReferenceAdaptation?
    ) {
        if (callableReferenceAdaptation == null) return

        val callElement = resolvedCall.call.callElement
        val isUnboundReference = resolvedCall.dispatchReceiver is TransientReceiver

        fun makeFakeValueArgument(callArgument: CangJieCallArgument): ValueArgument {
            val fakeCallArgument = callArgument as? FakeCangJieCallArgumentForCallableReference
                ?: throw AssertionError("FakeCangJieCallArgumentForCallableReference expected: $callArgument")
            return FakePositionalValueArgumentForCallableReferenceImpl(
                callElement,
                if (isUnboundReference) fakeCallArgument.index + 1 else fakeCallArgument.index
            )
        }

        // We should record argument mapping only if callable reference requires adaptation:
        // - argument mapping is non-trivial: any of the arguments were mapped as defaults or vararg elements;
        // - result should be coerced.
        var hasNonTrivialMapping = false
        val mappedArguments = ArrayList<Pair<ValueParameterDescriptor, ResolvedValueArgument>>()
        for ((valueParameter, resolvedCallArgument) in callableReferenceAdaptation.mappedArguments) {
            val resolvedValueArgument = when (resolvedCallArgument) {
                ResolvedCallArgument.DefaultArgument -> {
                    hasNonTrivialMapping = true
                    DefaultValueArgument.DEFAULT
                }

                is ResolvedCallArgument.SimpleArgument -> {
                    val valueArgument = makeFakeValueArgument(resolvedCallArgument.callArgument)
                    if (valueParameter.isVararg)
                        VarargValueArgument(
                            listOf(
                                FakeImplicitSpreadValueArgumentForCallableReferenceImpl(callElement, valueArgument)
                            )
                        )
                    else
                        ExpressionValueArgument(valueArgument)
                }

                is ResolvedCallArgument.VarargArgument -> {
                    hasNonTrivialMapping = true
                    VarargValueArgument(
                        resolvedCallArgument.arguments.map {
                            makeFakeValueArgument(it)
                        }
                    )
                }
            }
            mappedArguments.add(valueParameter to resolvedValueArgument)
        }
        if (hasNonTrivialMapping || isCallableReferenceWithImplicitConversion(
                resolvedCall,
                callableReferenceAdaptation
            )
        ) {
            resolvedCall.updateValueArguments(mappedArguments.toMap())
        }
    }

    private fun isCallableReferenceWithImplicitConversion(
        resolvedCall: NewAbstractResolvedCall<*>,
        callableReferenceAdaptation: CallableReferenceAdaptation
    ): Boolean {
        val resultingDescriptor = resolvedCall.resultingDescriptor

        // TODO drop return type check - see noCoercionToUnitIfFunctionAlreadyReturnsUnit.kt
        if (callableReferenceAdaptation.coercionStrategy == CoercionStrategy.COERCION_TO_UNIT && !resultingDescriptor.returnType!!.isUnit())
            return true

//        if (callableReferenceAdaptation.suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION)
//            return true

        return false
    }

    private fun completeCallableReference(
        callableCandidate: CallableReferenceResolutionCandidate,
        recordedDescriptor: CallableDescriptor?,
        resolvedCall: NewAbstractResolvedCall<*>,
        additionalDataFlowInfo: DataFlowInfo? = null,
    ): CangJieType? {
        val rawExtensionReceiver = callableCandidate.extensionReceiver
//        val unrestrictedBuilderInferenceSupported =
//            topLevelCallContext.languageVersionSettings.supportsFeature(LanguageFeature.UnrestrictedBuilderInference)
        val callableReferenceExpression =
            callableCandidate.resolvedCall.atom.psiCangJieCall.extractCallableReferenceExpression() ?: return null

        if (rawExtensionReceiver != null /* && !unrestrictedBuilderInferenceSupported*/ && rawExtensionReceiver.receiver.receiverValue.type.contains { it is StubTypeForBuilderInference }) {
            topLevelTrace.reportDiagnosticOnce(
                Errors.TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE.on(
                    callableReferenceExpression
                )
            )
            return null
        }

        val resultTypeInfo = if (recordedDescriptor != null) {
            extractCallableReferenceResultTypeInfoFromDescriptor(callableCandidate, recordedDescriptor)
        } else {
            updateCallableReferenceResultType(callableCandidate)
        }

        if (resultTypeInfo == null) return null

        resolvedCall.apply {
            if (resultTypeInfo.dispatchReceiver != null) {
                updateDispatchReceiverType(resultTypeInfo.dispatchReceiver.type)
            }
            if (resultTypeInfo.extensionReceiver != null) {
                updateExtensionReceiverType(resultTypeInfo.extensionReceiver.type)
            }
            setResultingSubstitutor(resultTypeInfo.substitutor)
        }

        recordArgumentAdaptationForCallableReference(resolvedCall, callableCandidate.callableReferenceAdaptation)

        val psiCall = CallMaker.makeCall(
            callableReferenceExpression.callableReference,
            resultTypeInfo.explicitReceiver,
            null,
            callableReferenceExpression.callableReference,
            emptyList()
        )
        val tracing = TracingStrategyImpl.create(callableReferenceExpression.callableReference, psiCall)

        tracing.bindCall(topLevelTrace, psiCall)
        tracing.bindReference(topLevelTrace, resolvedCall)
        tracing.bindResolvedCall(topLevelTrace, resolvedCall)

        // TODO: probably we should also record key 'DATA_FLOW_INFO_BEFORE', see ExpressionTypingVisitorDispatcher.getTypeInfo
        val typeInfo = if (additionalDataFlowInfo != null) {
            createTypeInfo(resultTypeInfo.resultType, additionalDataFlowInfo)
        } else {
            createTypeInfo(resultTypeInfo.resultType)
        }

        topLevelTrace.record(BindingContext.EXPRESSION_TYPE_INFO, callableReferenceExpression, typeInfo)
        topLevelTrace.record(BindingContext.PROCESSED, callableReferenceExpression)

        cangjieToResolvedCallTransformer.runCallCheckers(resolvedCall, topLevelCallCheckerContext)

        return resultTypeInfo.resultType
    }

    fun CallableDescriptor.isSupportedForCallableReference() = this is PropertyDescriptor || this is FunctionDescriptor

    private fun ReceiverValue.updateReceiverValue(substitutor: NewTypeSubstitutor): ReceiverValue {
        val newType = substitutor.safeSubstitute(type.unwrap()).let {
            typeApproximator.approximateToSuperType(
                it,
                TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            ) ?: it
        }
        return if (type != newType) replaceType(newType as CangJieType) else this
    }

    private fun updateCallableReferenceResultType(callableCandidate: CallableReferenceResolutionCandidate): CallableReferenceResultTypeInfo? {
        val callableReferenceExpression =
            callableCandidate.resolvedCall.atom.psiCangJieCall.psiCall.callElement  as? CjCallableReference
                ?: return null
        val freshSubstitutor = callableCandidate.freshVariablesSubstitutor ?: return null
        val resultTypeParameters =
            freshSubstitutor.freshVariables.map { resultSubstitutor.safeSubstitute(it.defaultType) }
        val typeParametersSubstitutor = NewTypeSubstitutorByConstructorMap(
            callableCandidate.candidate.typeParameters.map { it.typeConstructor }.zip(resultTypeParameters).toMap()
        )
        val resultSubstitutor = if (callableCandidate.candidate.isSupportedForCallableReference()) {
            ComposedSubstitutor(typeParametersSubstitutor, resultSubstitutor)
        } else EmptySubstitutor

        // write down type for callable reference expression
        val resultType = resultSubstitutor.safeSubstitute(callableCandidate.reflectionCandidateType)

        argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(
            topLevelTrace, expressionTypingServices.statementFilter, resultType, callableReferenceExpression
        )

        val dispatchReceiver =
            callableCandidate.dispatchReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)
        val extensionReceiver =
            callableCandidate.extensionReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

        when (callableCandidate.candidate) {
            is FunctionDescriptor -> doubleColonExpressionResolver.bindFunctionReference(
                callableReferenceExpression,
                resultType,
                topLevelCallContext,
                callableCandidate.candidate
            )

//            is PropertyDescriptor -> doubleColonExpressionResolver.bindPropertyReference(
//                callableReferenceExpression,
//                resultType,
//                topLevelCallContext
//            )
        }

//        doubleColonExpressionResolver.checkReferenceIsToAllowedMember(
//            callableCandidate.candidate,
//            topLevelCallContext.trace,
//            callableReferenceExpression
//        )

        val explicitCallableReceiver = when (callableCandidate.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> callableCandidate.dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> callableCandidate.extensionReceiver
            else -> null
        }
        val explicitReceiver = explicitCallableReceiver?.receiver?.receiverValue?.updateReceiverValue(resultSubstitutor)

        return CallableReferenceResultTypeInfo(
            dispatchReceiver,
            extensionReceiver,
            explicitReceiver,
            resultSubstitutor,
            resultType
        )
    }
    internal fun checkReferenceIsToAllowedMember(
        descriptor: CallableDescriptor, trace: BindingTrace, expression: CjCallableReference
    ) {
//        val simpleName = expression.callableReference
//        if (!languageVersionSettings.supportsFeature(LanguageFeature.CallableReferencesToClassMembersWithEmptyLHS)) {
//            if (expression.isEmptyLHS &&
//                (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)) {
//                trace.report(
//                    UNSUPPORTED_FEATURE.on(
//                        simpleName, LanguageFeature.CallableReferencesToClassMembersWithEmptyLHS to languageVersionSettings
//                    )
//                )
//            }
//        }
//        if (descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(descriptor.containingDeclaration)) {
//            trace.report(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR.on(simpleName))
//        }
//        if (descriptor is CallableMemberDescriptor && isMemberExtension(descriptor)) {
//            trace.report(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED.on(simpleName, descriptor))
//        }
//        if (descriptor is VariableDescriptor && descriptor !is PropertyDescriptor) {
//            trace.report(UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS.on(simpleName))
//        }
    }
    fun completeResolvedCall(
        resolvedCallAtom: ResolvedCallAtom,
        diagnostics: Collection<CangJieCallDiagnostic>
    ): NewAbstractResolvedCall<*>? {
        val diagnosticsFromPartiallyResolvedCall = extractDiagnosticsFromPartiallyResolvedCall(resolvedCallAtom)

//        clearPartiallyResolvedCall(resolvedCallAtom)

        val atom = resolvedCallAtom.atom
        if (atom.psiCangJieCall is PSICangJieCallForVariable) return null

        val allDiagnostics = diagnostics + diagnosticsFromPartiallyResolvedCall

        val resolvedCall = cangjieToResolvedCallTransformer.transformToResolvedCall<CallableDescriptor>(
            resolvedCallAtom,
            topLevelTrace,
            resultSubstitutor,
            allDiagnostics
        )

        //
        val lastCall = if (resolvedCall is VariableAsFunctionResolvedCall) {
            resolvedCall.functionCall as NewAbstractResolvedCall<*>
        } else resolvedCall
        if (ErrorUtils.isError(resolvedCall.candidateDescriptor)) {
            cangjieToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
            checkMissingReceiverSupertypes(resolvedCall, missingSupertypesResolver, topLevelTrace)
            return resolvedCall
        }
//
        val psiCallForResolutionContext = when (atom) {
            // PARTIAL_CALL_RESOLUTION_CONTEXT has been written for the baseCall
            is PSICangJieCallForInvoke -> atom.baseCall.psiCall
            else -> atom.psiCangJieCall.psiCall
        }
//
        val callElement = psiCallForResolutionContext.callElement
        if (callElement is CjExpression) {
            val recordedType = topLevelCallContext.trace.getType(callElement)
            if (recordedType != null && recordedType.shouldBeUpdated() && resolvedCall.resultingDescriptor.returnType != null) {
                topLevelCallContext.trace.recordType(callElement, resolvedCall.resultingDescriptor.returnType)
            }
        }


        val resolutionContextForPartialCall =
            topLevelCallContext.trace[BindingContext.PARTIAL_CALL_RESOLUTION_CONTEXT, psiCallForResolutionContext]

        val callCheckerContext = if (resolutionContextForPartialCall != null)
            CallCheckerContext(
                resolutionContextForPartialCall.replaceBindingTrace(topLevelTrace),
                deprecationResolver,
                moduleDescriptor,
                missingSupertypesResolver,
                callComponents,
            )
        else
            topLevelCallCheckerContext

        cangjieToResolvedCallTransformer.bind(topLevelTrace, resolvedCall)
//
        cangjieToResolvedCallTransformer.runArgumentsChecks(topLevelCallContext, lastCall)
        cangjieToResolvedCallTransformer.runCallCheckers(resolvedCall, callCheckerContext)
//        cangjieToResolvedCallTransformer.runAdditionalReceiversCheckers(resolvedCall, topLevelCallContext)
//
        cangjieToResolvedCallTransformer.reportDiagnostics(
            topLevelCallContext,
            topLevelTrace,
            resolvedCall,
            allDiagnostics
        )

        return resolvedCall
    }

    private fun checkMissingReceiverSupertypes(
        resolvedCall: ResolvedCall<CallableDescriptor>,
        missingSupertypesResolver: MissingSupertypesResolver,
        trace: BindingTrace
    ) {
        val receiverValue = resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver
        receiverValue?.type?.let { receiverType ->
            MissingDependencySupertypeChecker.checkSupertypes(
                receiverType,
                resolvedCall.call.callElement,
                trace,
                missingSupertypesResolver
            )
        }
    }
}
