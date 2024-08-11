package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.*

import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.calls.util.toResolutionStatus
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeApproximator
import com.huawei.cangjie.types.UnwrappedType


class NewResolvedCallImpl<D : CallableDescriptor>(
    override val resolvedCallAtom: ResolvedCallAtom,
    substitutor: NewTypeSubstitutor?,
    diagnostics: Collection<CangJieCallDiagnostic>,
    override val typeApproximator: TypeApproximator,
    override val languageVersionSettings: LanguageVersionSettings,
) : NewAbstractResolvedCall<D>() {
    private var dispatchReceiver = resolvedCallAtom.dispatchReceiverArgument?.receiver?.receiverValue
    private lateinit var resultingDescriptor: D
    override var diagnostics: Collection<CangJieCallDiagnostic> = diagnostics
        private set
    private var extensionReceiver = resolvedCallAtom.extensionReceiverArgument?.receiver?.receiverValue
    private var smartCastDispatchReceiverType: CangJieType? = null
    private var contextReceivers = resolvedCallAtom.contextReceiversArguments.map { it.receiver.receiverValue }

    override fun updateExtensionReceiverType(newType: CangJieType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }
    private lateinit var typeArguments: List<UnwrappedType>

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getSmartCastDispatchReceiverType(): CangJieType? = smartCastDispatchReceiverType


    override fun getExplicitReceiverKind(): ExplicitReceiverKind = resolvedCallAtom.explicitReceiverKind



    override fun getExtensionReceiver(): ReceiverValue? = extensionReceiver


    override fun getStatus(): ResolutionStatus = getResultApplicability(diagnostics).toResolutionStatus()
    override fun getContextReceivers(): List<ReceiverValue> = contextReceivers




    override fun getTypeArguments(): Map<TypeParameterDescriptor, CangJieType> {
        val typeParameters = candidateDescriptor.typeParameters.takeIf { it.isNotEmpty() } ?: return emptyMap()
        return typeParameters.zip(typeArguments).toMap()
    }

    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver


    override fun getResultingDescriptor(): D = resultingDescriptor


    override val psiCangJieCall: PSICangJieCall = resolvedCallAtom.atom.psiCangJieCall
    override val cangjieCall: CangJieCall = resolvedCallAtom.atom
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor
        get() = resolvedCallAtom.freshVariablesSubstitutor
    override val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
        get() = resolvedCallAtom.argumentMappingByOriginal

    fun updateDiagnostics(completedDiagnostics: Collection<CangJieCallDiagnostic>) {
        diagnostics = completedDiagnostics
    }

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
//        updateArgumentsMapping(null)
//        updateValueArguments(null)

        substituteReceivers(substitutor)

        @Suppress("UNCHECKED_CAST")
        resultingDescriptor = substitutedResultingDescriptor(substitutor) as D

//        typeArguments = freshSubstitutor.freshVariables.map {
//            val substituted = (substitutor ?: FreshVariableNewTypeSubstitutor.Empty).safeSubstitute(it.defaultType)
//            typeApproximator
//                .approximateToSuperType(substituted, TypeApproximatorConfiguration.IntegerLiteralsTypesApproximation)
//                ?: substituted
//        }
//
//        calculateExpectedTypeForSamConvertedArgumentMap(substitutor)
//        calculateExpectedTypeForSuspendConvertedArgumentMap(substitutor)
//        calculateExpectedTypeForUnitConvertedArgumentMap(substitutor)
//        calculateExpectedTypeForConstantConvertedArgumentMap()
    }

    override fun updateDispatchReceiverType(newType: CangJieType) {
        if (dispatchReceiver?.type == newType) return
        dispatchReceiver = dispatchReceiver?.replaceType(newType)
    }
    private fun collectErrorPositions(): Map<ValueArgument, List<CangJieCallDiagnostic>> {
        val result = mutableListOf<Pair<ValueArgument, CangJieCallDiagnostic>>()

        fun ConstraintPosition.originalPosition(): ConstraintPosition =
            if (this is IncorporationConstraintPosition) {
                from.originalPosition()
            } else {
                this
            }

        diagnostics.forEach {
            val position = when (val error = it.constraintSystemError) {
                is NewConstraintError -> error.position.originalPosition()
//                is CapturedTypeFromSubtyping -> error.position.originalPosition()
//                is ConstrainingTypeIsError -> error.position.originalPosition()
                else -> null
            } as? ArgumentConstraintPositionImpl ?: return@forEach

            val argument = (position.argument as? PSICangJieCallArgument)?.valueArgument ?: return@forEach
            result += argument to it
        }

        return result.groupBy({ it.first }) { it.second }
    }
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ): Map<ValueArgument, ArgumentMatchImpl> {
        val argumentErrors = collectErrorPositions()

        return LinkedHashMap<ValueArgument, ArgumentMatchImpl>().also { result ->
            for (parameter in resultingDescriptor.valueParameters) {
                val resolvedArgument = valueArguments[parameter] ?: continue
                for (argument in resolvedArgument.arguments) {
                    val status = argumentErrors[argument]?.let {
                        ArgumentMatchStatus.TYPE_MISMATCH
                    } ?: ArgumentMatchStatus.SUCCESS
                    result[argument] = ArgumentMatchImpl(parameter).apply { recordMatchStatus(status) }
                }
            }
        }
    }

    init {
        setResultingSubstitutor(substitutor)
    }
}
