package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor

import com.huawei.cangjie.resolve.DelegatingBindingTrace
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus

interface VariableAsFunctionResolvedCall {
    val functionCall: ResolvedCall<FunctionDescriptor>
    val variableCall: ResolvedCall<VariableDescriptor>
}

interface VariableAsFunctionMutableResolvedCall : VariableAsFunctionResolvedCall {
    override val functionCall: MutableResolvedCall<FunctionDescriptor>
    override val variableCall: MutableResolvedCall<VariableDescriptor>
}
class VariableAsFunctionResolvedCallImpl(
    override val functionCall: MutableResolvedCall<FunctionDescriptor>,
    override val variableCall: MutableResolvedCall<VariableDescriptor>
) : VariableAsFunctionMutableResolvedCall, MutableResolvedCall<FunctionDescriptor> by functionCall {

    override fun markCallAsCompleted() {
        functionCall.markCallAsCompleted()
        variableCall.markCallAsCompleted()
    }

    override fun isCompleted(): Boolean = functionCall.isCompleted && variableCall.isCompleted

    override fun getStatus(): ResolutionStatus = variableCall.status.combine(functionCall.status)

    override fun getTrace(): DelegatingBindingTrace {
        return functionCall.trace
    }

}

