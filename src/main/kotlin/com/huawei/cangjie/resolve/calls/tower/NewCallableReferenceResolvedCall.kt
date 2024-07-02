package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.util.toResolutionStatus
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.UnwrappedType


class NewCallableReferenceResolvedCall<D : CallableDescriptor>(
    val resolvedAtom: ResolvedCallableReferenceAtom,
//    override val typeApproximator: TypeApproximator,
//    override val languageVersionSettings: LanguageVersionSettings,
    substitutor: NewTypeSubstitutor? = null,
) : NewAbstractResolvedCall<D>() {
//    override val positionDependentApproximation: Boolean = true
//    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument> = emptyMap()
    override val diagnostics: Collection<CangJieCallDiagnostic> = emptyList()
    override fun updateExtensionReceiverType(newType: CangJieType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    override val resolvedCallAtom: ResolvedCallableReferenceCallAtom?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.resolvedCall

        }

    override val psiCangJieCall: PSICangJieCall =
        when (resolvedAtom) {
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom.psiCangJieCall
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.atom.call.psiCangJieCall
        }

//    override val freshSubstitutor: FreshVariableNewTypeSubstitutor?
//        get() = when (resolvedAtom) {
//            is ResolvedCallableReferenceCallAtom -> resolvedAtom.freshVariablesSubstitutor
//            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.freshVariablesSubstitutor
//        }

    override val cangjieCall: CangJieCall?
        get() = when (resolvedAtom) {
            is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.cangjieCall?.call
            is ResolvedCallableReferenceCallAtom -> resolvedAtom.atom
        }

    private lateinit var resultingDescriptor: D
    private lateinit var typeArguments: List<UnwrappedType>

    private var extensionReceiver: ReceiverValue? = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.extensionReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.extensionReceiver?.receiver?.receiverValue
        else -> {
            error("Unexpected resolved atom: $resolvedAtom")}
    }

    private var dispatchReceiver = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.dispatchReceiverArgument?.receiverValue
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.dispatchReceiver?.receiver?.receiverValue
//        else -> {
//            error("Unexpected resolved atom: $resolvedAtom")}
    }

//    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver
    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
//    override fun getContextReceivers() = emptyList<ReceiverValue>()
//
//    override fun updateDispatchReceiverType(newType: CangJieType) {
//        if (dispatchReceiver?.type == newType) return
//        dispatchReceiver = dispatchReceiver?.replaceType(newType)
//    }
//
//    override fun updateExtensionReceiverType(newType: CangJieType) {
//        if (extensionReceiver?.type == newType) return
//        extensionReceiver = extensionReceiver?.replaceType(newType)
//    }

//    override fun updateContextReceiverTypes(newTypes: List<CangJieType>) {
//        // TODO: Update context receivers
//        return
//    }

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = when (resolvedAtom) {
        is ResolvedCallableReferenceCallAtom -> resolvedAtom.candidateDescriptor as D
        is ResolvedCallableReferenceArgumentAtom -> resolvedAtom.candidate?.candidate as D
        else -> {
            error("Unexpected resolved atom: $resolvedAtom")
        }
    }

    override fun getResultingDescriptor(): D = resultingDescriptor
//    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping = ArgumentUnmapped
//
//    override fun getTypeArguments(): Map<TypeParameterDescriptor, CangJieType> {
//        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
//        return typeParameters.zip(typeArguments).toMap()
//    }

    override fun getStatus() = CandidateApplicability.RESOLVED.toResolutionStatus()

//    override fun getExplicitReceiverKind() = when (resolvedAtom) {
//        is ResolvedCallableReferenceArgumentAtom ->
//            resolvedAtom.candidate?.explicitReceiverKind ?: ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
//        is ResolvedCallableReferenceCallAtom -> resolvedAtom.explicitReceiverKind
//    }
//
//    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments =
//        MutableDataFlowInfoForArguments.WithoutArgumentsCheck(DataFlowInfo.EMPTY)
//
//    override fun getSmartCastDispatchReceiverType(): CangJieType? = null

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
//        substituteReceivers(substitutor)
//
//        @Suppress("UNCHECKED_CAST")
//        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D
//
//        freshSubstitutor?.let { freshSubstitutor ->
//            typeArguments = freshSubstitutor.freshVariables.map {
//                val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
//                typeApproximator.approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
//                    ?: substituted
//            }
//        }
    }

    override fun updateDispatchReceiverType(newType: CangJieType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }

//    override fun containsOnlyOnlyInputTypesErrors(): Boolean = false
//
//    override fun argumentToParameterMap(
//        resultingDescriptor: CallableDescriptor,
//        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
//    ): Map<ValueArgument, ArgumentMatchImpl> = emptyMap()

    init {
        setResultingSubstitutor(substitutor)
    }
}
