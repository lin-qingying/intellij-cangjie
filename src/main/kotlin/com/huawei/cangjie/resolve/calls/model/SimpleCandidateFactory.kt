package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.components.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.components.candidate.SimpleErrorResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.SimpleResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.addSubsystemFromArgument
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.inference.model.LowerPriorityToPreserveCompatibility
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.tower.CandidateFactory
import com.huawei.cangjie.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.resolve.calls.tower.isSynthesized
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.isDynamic

class SimpleCandidateFactory(
    val callComponents: CangJieCallComponents,
    val scopeTower: ImplicitScopeTower,
    val cangjieCall: CangJieCall,
    val resolutionCallbacks: CangJieResolutionCallbacks,
) : CandidateFactory<SimpleResolutionCandidate> {

    val inferenceSession: InferenceSession = resolutionCallbacks.inferenceSession

    val baseSystem: ConstraintStorage

    init {
        val baseSystem = NewConstraintSystemImpl(
            callComponents.constraintInjector, callComponents.builtIns,
            callComponents.cangjieTypeRefiner
           , callComponents.languageVersionSettings
        )
        if (!inferenceSession.resolveReceiverIndependently()) {
            baseSystem.addSubsystemFromArgument(cangjieCall.explicitReceiver)
            baseSystem.addSubsystemFromArgument(cangjieCall.dispatchReceiverForInvokeExtension)
        }
        for (argument in cangjieCall.argumentsInParenthesis) {
            baseSystem.addSubsystemFromArgument(argument)
        }
        baseSystem.addSubsystemFromArgument(cangjieCall.externalArgument)

        baseSystem.addOtherSystem(inferenceSession.currentConstraintSystem())

        this.baseSystem = baseSystem.asReadOnlyStorage()
    }


    // todo: try something else, because current method is ugly and unstable
    private fun createReceiverArgument(
        explicitReceiver: ReceiverCangJieCallArgument?,
        fromResolution: ReceiverValueWithSmartCastInfo?
    ): SimpleCangJieCallArgument? =
        explicitReceiver as? SimpleCangJieCallArgument ?: // qualifier receiver cannot be safe
        fromResolution?.let {
            ReceiverExpressionCangJieCallArgument(
                it,
                isSafeCall = false,
                isForImplicitInvoke = cangjieCall.isForImplicitInvoke
            )
        }

    override fun createErrorCandidate(): SimpleResolutionCandidate {
        val errorScope =
            ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_RESOLUTION_CANDIDATE, cangjieCall.toString())
        val errorDescriptor = if (cangjieCall.callKind == CangJieCallKind.VARIABLE) {
            errorScope.getContributedVariables(cangjieCall.name, scopeTower.location)
        } else {
            errorScope.getContributedFunctions(cangjieCall.name, scopeTower.location)
        }.first()

        val dispatchReceiver = createReceiverArgument(cangjieCall.explicitReceiver, fromResolution = null)
        val explicitReceiverKind =
            if (dispatchReceiver == null) ExplicitReceiverKind.NO_EXPLICIT_RECEIVER else ExplicitReceiverKind.DISPATCH_RECEIVER

        return createCandidate(
            errorDescriptor, explicitReceiverKind, dispatchReceiver, extensionArgumentReceiver = null,
            extensionArgumentReceiverCandidates = null, initialDiagnostics = listOf(), knownSubstitutor = null
        )
    }

    private fun CangJieCall.getExplicitDispatchReceiver(explicitReceiverKind: ExplicitReceiverKind) =
        when (explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> explicitReceiver
            ExplicitReceiverKind.BOTH_RECEIVERS -> dispatchReceiverForInvokeExtension
            else -> null
        }

    private fun CangJieCall.getExplicitExtensionReceiver(explicitReceiverKind: ExplicitReceiverKind) =
        when (explicitReceiverKind) {
            ExplicitReceiverKind.EXTENSION_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> explicitReceiver
            else -> null
        }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): SimpleResolutionCandidate {
        val dispatchArgumentReceiver = createReceiverArgument(
            cangjieCall.getExplicitDispatchReceiver(explicitReceiverKind),
            towerCandidate.dispatchReceiver
        )
        val extensionArgumentReceiver =
            createReceiverArgument(cangjieCall.getExplicitExtensionReceiver(explicitReceiverKind), extensionReceiver)
        val descriptor = towerCandidate.descriptor
        var diagnostics: List<CangJieCallDiagnostic> = towerCandidate.diagnostics
        if (descriptor is PropertyDescriptor && descriptor.isSyntheticEnumEntries()) {
            diagnostics = diagnostics + LowerPriorityToPreserveCompatibility(needToReportWarning = false).asDiagnostic()
        }

        return createCandidate(
            descriptor, explicitReceiverKind, dispatchArgumentReceiver,
            extensionArgumentReceiver, extensionArgumentReceiverCandidates = null, diagnostics, knownSubstitutor = null
        )
    }

    private fun createCandidate(
        descriptor: CallableDescriptor,
        explicitReceiverKind: ExplicitReceiverKind,
        dispatchArgumentReceiver: SimpleCangJieCallArgument?,
        extensionArgumentReceiver: SimpleCangJieCallArgument?,
        extensionArgumentReceiverCandidates: List<SimpleCangJieCallArgument>?,
        initialDiagnostics: Collection<CangJieCallDiagnostic>,
        knownSubstitutor: TypeSubstitutor?
    ): SimpleResolutionCandidate {
        val resolvedKtCall = MutableResolvedCallAtom(
            cangjieCall, descriptor, explicitReceiverKind,
            dispatchArgumentReceiver, extensionArgumentReceiver, extensionArgumentReceiverCandidates
        )

        if (ErrorUtils.isError(descriptor)) {
            return SimpleErrorResolutionCandidate(
                callComponents,
                resolutionCallbacks,
                scopeTower,
                baseSystem,
                resolvedKtCall
            )
        }

        val candidate =
            SimpleResolutionCandidate(
                callComponents,
                resolutionCallbacks,
                scopeTower,
                baseSystem,
                resolvedKtCall,
                knownSubstitutor
            )

        initialDiagnostics.forEach(candidate::addDiagnostic)

//        if (callComponents.statelessCallbacks.isHiddenInResolution(descriptor, cangjieCall, resolutionCallbacks)) {
//            candidate.addDiagnostic(HiddenDescriptor)
//        }

        if (extensionArgumentReceiver != null) {
            val parameterIsDynamic = descriptor.extensionReceiverParameter!!.value.type.isDynamic()
            val argumentIsDynamic = extensionArgumentReceiver.receiver.receiverValue.type.isDynamic()

//            if (parameterIsDynamic != argumentIsDynamic ||
//                (parameterIsDynamic && !descriptor.hasDynamicExtensionAnnotation())
//            ) {
//                candidate.addDiagnostic(HiddenExtensionRelatedToDynamicTypes)
//            }
        }

        return candidate
    }

    override fun createCandidate(
        towerCandidate: CandidateWithBoundDispatchReceiver,
        explicitReceiverKind: ExplicitReceiverKind,
        extensionReceiverCandidates: List<ReceiverValueWithSmartCastInfo>
    ): SimpleResolutionCandidate {
        val dispatchArgumentReceiver = createReceiverArgument(
            cangjieCall.getExplicitDispatchReceiver(explicitReceiverKind),
            towerCandidate.dispatchReceiver
        )
        val extensionArgumentReceiverCandidates = extensionReceiverCandidates.mapNotNull {
            createReceiverArgument(cangjieCall.getExplicitExtensionReceiver(explicitReceiverKind), it)
        }

        return createCandidate(
            towerCandidate.descriptor, explicitReceiverKind, dispatchArgumentReceiver,
            null, extensionArgumentReceiverCandidates, towerCandidate.diagnostics, knownSubstitutor = null
        )
    }
}

fun PropertyDescriptor.isSyntheticEnumEntries(): Boolean {
    return isSynthesized && dispatchReceiverParameter == null && extensionReceiverParameter == null &&
            (containingDeclaration as? ClassDescriptor)?.kind == ClassKind.ENUM
}


