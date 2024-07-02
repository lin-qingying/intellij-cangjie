package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.resolve.builtIns
import com.huawei.cangjie.resolve.calls.components.CallableReceiver
import com.huawei.cangjie.resolve.calls.components.CallableReferenceAdaptation
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tower.CandidateFactory
import com.huawei.cangjie.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.CoercionStrategy
import com.huawei.cangjie.utils.SmartList


class CallableReferencesCandidateFactory(
    val kotlinCall: CallableReferenceResolutionAtom,
    val callComponents: CangJieCallComponents,
    val scopeTower: ImplicitScopeTower,
    val expectedType: UnwrappedType?,
    private val baseSystem: ConstraintStorage?,
    private val resolutionCallbacks: CangJieResolutionCallbacks
) : CandidateFactory<CallableReferenceResolutionCandidate> {

    private fun toCallableReceiver(receiver: ReceiverValueWithSmartCastInfo, isExplicit: Boolean): CallableReceiver {
        if (!isExplicit) return CallableReceiver.ScopeReceiver(receiver)

        return when (val lhsResult = kotlinCall.lhsResult) {
            is LHSResult.Expression -> CallableReceiver.ExplicitValueReceiver(receiver)
            is LHSResult.Type -> {
                if (lhsResult.qualifier?.classValueReceiver?.type == receiver.receiverValue.type) {
                    CallableReceiver.BoundValueReference(receiver)
                } else {
                    CallableReceiver.UnboundReference(receiver)
                }
            }
            is LHSResult.Object -> CallableReceiver.BoundValueReference(receiver)
            else -> throw IllegalStateException("Unsupported kind of lhsResult: $lhsResult")
        }
    }
    private fun buildReflectionType(
        descriptor: CallableDescriptor,
        dispatchReceiver: CallableReceiver?,
        extensionReceiver: CallableReceiver?,
        expectedType: UnwrappedType?,
        builtins: CangJieBuiltIns,
    ): Pair<UnwrappedType, CallableReferenceAdaptation?> {
        val argumentsAndReceivers = ArrayList<CangJieType>(descriptor.valueParameters.size + 2 + descriptor.contextReceiverParameters.size)

        val contextReceiversTypes = descriptor.contextReceiverParameters.map { it.type }
        argumentsAndReceivers.addAll(contextReceiversTypes)
//
//        if (dispatchReceiver is CallableReceiver.UnboundReference) {
//            argumentsAndReceivers.add(dispatchReceiver.receiver.stableType)
//        }
//        if (extensionReceiver is CallableReceiver.UnboundReference) {
//            argumentsAndReceivers.add(extensionReceiver.receiver.stableType)
//        }

        val descriptorReturnType = descriptor.returnType
            ?: ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE, descriptor.toString())

        return when (descriptor) {
//            is VariableDescriptor -> {
//                val mutable = descriptor.isVar
////                        && run {
////                    val setter = descriptor.setter
////                    setter == null || DescriptorVisibilities.isVisible(
////                        dispatchReceiver?.asReceiverValueForVisibilityChecks, setter,
////                        scopeTower.lexicalScope.ownerDescriptor, false
////                    )
////                }
//
//                callComponents.reflectionTypes.getKPropertyType(
//                    Annotations.EMPTY,
//                    argumentsAndReceivers,
//                    descriptorReturnType,
//                    mutable
//                ) to null
//
//            }
//            is FunctionDescriptor -> {
//                val callableReferenceAdaptation = getCallableReferenceAdaptation(
//                    descriptor, expectedType,
//                    unboundReceiverCount = argumentsAndReceivers.size,
//                    builtins = builtins
//                )
//
//                // conversions aren't needed for top-level callable references
//                val buildTypeWithConversions = kotlinCall is CallableReferenceCangJieCallArgument
//
//                val returnType = if (callableReferenceAdaptation == null || !buildTypeWithConversions) {
//                    descriptor.valueParameters.mapTo(argumentsAndReceivers) { it.type }
//                    descriptorReturnType
//                } else {
//                    val arguments = callableReferenceAdaptation.argumentTypes
//                    val coercion = callableReferenceAdaptation.coercionStrategy
//                    argumentsAndReceivers.addAll(arguments)
//
//                    if (coercion == CoercionStrategy.COERCION_TO_UNIT)
//                        descriptor.builtIns.getUnitType()
//                    else
//                        descriptorReturnType
//                }
//
//                val suspendConversionStrategy = callableReferenceAdaptation?.suspendConversionStrategy
//                val isSuspend = descriptor.isSuspend ||
//                        (suspendConversionStrategy == SuspendConversionStrategy.SUSPEND_CONVERSION && buildTypeWithConversions)
//
//                callComponents.reflectionTypes.getKFunctionType(
//                    Annotations.EMPTY, null, emptyList(), argumentsAndReceivers, null,
//                    returnType, descriptor.builtIns, isSuspend
//                ) to callableReferenceAdaptation
//            }
            else -> {
//                assert(!descriptor.isSupportedForCallableReference()) { "${descriptor::class} isn't supported to use in callable references actually, but it's listed in `isSupportedForCallableReference` method" }
                ErrorUtils.createErrorType(ErrorTypeKind.UNSUPPORTED_CALLABLE_REFERENCE_TYPE, descriptor.toString()) to null
            }
        }
    }
    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): CallableReferenceResolutionCandidate {
        val dispatchCallableReceiver =
            towerCandidate.dispatchReceiver?.let { toCallableReceiver(it, explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER) }
        val extensionCallableReceiver = extensionReceiver?.let { toCallableReceiver(it, explicitReceiverKind == ExplicitReceiverKind.EXTENSION_RECEIVER) }
        val candidateDescriptor = towerCandidate.descriptor
        val diagnostics = SmartList<CangJieCallDiagnostic>()
//
        val (reflectionCandidateType, callableReferenceAdaptation) = buildReflectionType(
            candidateDescriptor,
            dispatchCallableReceiver,
            extensionCallableReceiver,
            expectedType,
            callComponents.builtIns,
        )

        fun createCallableReferenceCallCandidate(diagnostics: List<CangJieCallDiagnostic>) = CallableReferenceResolutionCandidate(
            candidateDescriptor, dispatchCallableReceiver, extensionCallableReceiver,
            explicitReceiverKind, reflectionCandidateType, callableReferenceAdaptation,
            kotlinCall, expectedType, callComponents, scopeTower, resolutionCallbacks, baseSystem
        ).also { diagnostics.forEach(it::addDiagnostic) }

//        if (callComponents.statelessCallbacks.isHiddenInResolution(candidateDescriptor, kotlinCall.call, resolutionCallbacks)) {
//            diagnostics.add(HiddenDescriptor)
//            return createCallableReferenceCallCandidate(diagnostics)
//        }
//
//        if (needCompatibilityResolveForCallableReference(callableReferenceAdaptation, candidateDescriptor)) {
//            markCandidateForCompatibilityResolve(diagnostics)
//        }
//
//        if (callableReferenceAdaptation != null && expectedType != null && hasNonTrivialAdaptation(callableReferenceAdaptation)) {
//            if (!expectedType.isFunctionType && !expectedType.isSuspendFunctionType) { // expectedType has some reflection type
//                diagnostics.add(AdaptedCallableReferenceIsUsedWithReflection(kotlinCall))
//            }
//        }
//
//        if (callableReferenceAdaptation != null &&
//            callableReferenceAdaptation.defaults != 0 &&
//            !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
//        ) {
//            diagnostics.add(CallableReferencesDefaultArgumentUsed(kotlinCall, candidateDescriptor, callableReferenceAdaptation.defaults))
//        }
//
//        if (candidateDescriptor !is CallableMemberDescriptor) {
//            return createCallableReferenceCallCandidate(listOf(NotCallableMemberReference(kotlinCall, candidateDescriptor)))
//        }
//
//        if (candidateDescriptor is PropertyDescriptor && candidateDescriptor.isSyntheticEnumEntries()) {
//            diagnostics.add(LowerPriorityToPreserveCompatibility(needToReportWarning = false).asDiagnostic())
//        }

        diagnostics.addAll(towerCandidate.diagnostics)
        // todo smartcast on receiver diagnostic and CheckInstantiationOfAbstractClass

        return createCallableReferenceCallCandidate(diagnostics)
    }

    override fun createErrorCandidate(): CallableReferenceResolutionCandidate {
        TODO("Not yet implemented")
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): CallableReferenceResolutionCandidate {
        TODO("Not yet implemented")
    }
}
