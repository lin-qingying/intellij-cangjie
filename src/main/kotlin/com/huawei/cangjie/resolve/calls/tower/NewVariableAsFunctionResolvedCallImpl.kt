package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.ResolvedCallAtom
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.types.CangJieType

class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: NewAbstractResolvedCall<VariableDescriptor>,
    override val functionCall: NewAbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, NewAbstractResolvedCall<FunctionDescriptor>() {
    override fun getCandidateDescriptor() = functionCall.candidateDescriptor


    override fun getStatus() = functionCall.status


    override fun getDispatchReceiver() = functionCall.dispatchReceiver

    override fun getResultingDescriptor() = functionCall.resultingDescriptor


    override val psiCangJieCall: PSICangJieCall = functionCall.psiCangJieCall
    override val cangjieCall: CangJieCall? = functionCall.cangjieCall
    override val resolvedCallAtom: ResolvedCallAtom? = functionCall.resolvedCallAtom
    override val diagnostics: Collection<CangJieCallDiagnostic> = functionCall.diagnostics
    override fun updateExtensionReceiverType(newType: CangJieType)= functionCall.updateExtensionReceiverType(newType)
    override fun setResultingSubstitutor(substitutor: NewTypeSubstitutor?) {
        functionCall.setResultingSubstitutor(substitutor)
        variableCall.setResultingSubstitutor(substitutor)
    }

    override fun updateDispatchReceiverType(newType: CangJieType)  = functionCall.updateDispatchReceiverType(newType)

}
