package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.ResolvedCallAtom
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeApproximator


class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewAbstractResolvedCall<VariableDescriptor>,
    override val functionCall: NewAbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, NewAbstractResolvedCall<FunctionDescriptor>() {
    val baseCall: PSICangJieCallImpl = (functionCall.psiCangJieCall as PSICangJieCallForInvoke).baseCall

    override val resolvedCallAtom: ResolvedCallAtom? = functionCall.resolvedCallAtom
    override val psiCangJieCall: PSICangJieCall = functionCall.psiCangJieCall
    override val typeApproximator: TypeApproximator = functionCall.typeApproximator
    override val freshSubstitutor: FreshVariableNewTypeSubstitutor? = functionCall.freshSubstitutor
//    override val argumentMappingByOriginal = functionCall.argumentMappingByOriginal
    override val cangjieCall = functionCall.cangjieCall
    override val languageVersionSettings = functionCall.languageVersionSettings
    override val diagnostics: Collection<CangJieCallDiagnostic> = functionCall.diagnostics

    override fun getStatus() = functionCall.status
    override fun getCandidateDescriptor() = functionCall.candidateDescriptor
    override fun getResultingDescriptor() = functionCall.resultingDescriptor
//    override fun getExtensionReceiver() = functionCall.extensionReceiver
//    override fun getContextReceivers() = functionCall.contextReceivers
    override fun getDispatchReceiver() = functionCall.dispatchReceiver
//    override fun getExplicitReceiverKind() = functionCall.explicitReceiverKind
//    override fun getTypeArguments() = functionCall.typeArguments
//    override fun getSmartCastDispatchReceiverType() = functionCall.smartCastDispatchReceiverType
//    override fun containsOnlyOnlyInputTypesErrors() = functionCall.containsOnlyOnlyInputTypesErrors()
    override fun updateDispatchReceiverType(newType: CangJieType) = functionCall.updateDispatchReceiverType(newType)
    override fun updateExtensionReceiverType(newType: CangJieType) = functionCall.updateExtensionReceiverType(newType)
//    override fun updateContextReceiverTypes(newTypes: List<CangJieType>) = functionCall.updateContextReceiverTypes(newTypes)
//    override fun argumentToParameterMap(
//        resultingDescriptor: CallableDescriptor,
//        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
//    ) = functionCall.argumentToParameterMap(resultingDescriptor, valueArguments)

    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        functionCall.setResultingSubstitutor(substitutor)
        variableCall.setResultingSubstitutor(substitutor)
    }
}

