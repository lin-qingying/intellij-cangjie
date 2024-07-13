package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.ResolvedCallAtom
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.util.toResolutionStatus
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeApproximator


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

    override fun updateExtensionReceiverType(newType: CangJieType) {
        if (extensionReceiver?.type == newType) return
        extensionReceiver = extensionReceiver?.replaceType(newType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCandidateDescriptor(): D = resolvedCallAtom.candidateDescriptor as D
    override fun getStatus(): ResolutionStatus = getResultApplicability(diagnostics).toResolutionStatus()


    override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver


    override fun getResultingDescriptor(): D = resultingDescriptor


    override val psiCangJieCall: PSICangJieCall = resolvedCallAtom.atom.psiCangJieCall
    override val cangjieCall: CangJieCall = resolvedCallAtom.atom
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor
        get() = resolvedCallAtom.freshVariablesSubstitutor

    fun updateDiagnostics(completedDiagnostics: Collection<CangJieCallDiagnostic>) {
        diagnostics = completedDiagnostics
    }

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        //clear cached values
//        updateArgumentsMapping(null)
//        updateValueArguments(null)

//        substituteReceivers(substitutor)

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
    init {
        setResultingSubstitutor(substitutor)
    }
}
