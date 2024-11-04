package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.descriptors.VariableDescriptor
import com.linqingying.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import com.linqingying.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.linqingying.cangjie.resolve.calls.model.ResolvedCallAtom
import com.linqingying.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeApproximator


class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewAbstractResolvedCall<VariableDescriptor>,
    override val functionCall: NewAbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, NewAbstractResolvedCall<FunctionDescriptor>() {
    val baseCall: PSICangJieCallImpl = (functionCall.psiCangJieCall as PSICangJieCallForInvoke).baseCall

    override val resolvedCallAtom: ResolvedCallAtom? = functionCall.resolvedCallAtom
    override val psiCangJieCall: PSICangJieCall = functionCall.psiCangJieCall
    override val typeApproximator: TypeApproximator = functionCall.typeApproximator
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor? = functionCall.freshSubstitutor

    override val cangjieCall = functionCall.cangjieCall
    override val languageVersionSettings = functionCall.languageVersionSettings
    override val argumentMappingByOriginal = functionCall.argumentMappingByOriginal

    override val diagnostics: Collection<CangJieCallDiagnostic> = functionCall.diagnostics

    override fun getStatus() = functionCall.status
    override fun getContextReceivers() = functionCall.contextReceivers

    override fun getTypeArguments() = functionCall.typeArguments


    override fun getCandidateDescriptor() = functionCall.candidateDescriptor
    override fun getSmartCastDispatchReceiverType() = functionCall.smartCastDispatchReceiverType

    override fun getExplicitReceiverKind() = functionCall.explicitReceiverKind


    override fun getExtensionReceiver() = functionCall.extensionReceiver


    override fun getResultingDescriptor() = functionCall.resultingDescriptor

    override fun getDispatchReceiver() = functionCall.dispatchReceiver

    override fun updateDispatchReceiverType(newType: CangJieType) = functionCall.updateDispatchReceiverType(newType)
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ) = functionCall.argumentToParameterMap(resultingDescriptor, valueArguments)

    override fun updateExtensionReceiverType(newType: CangJieType) = functionCall.updateExtensionReceiverType(newType)

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        functionCall.setResultingSubstitutor(substitutor)
        variableCall.setResultingSubstitutor(substitutor)
    }
}

