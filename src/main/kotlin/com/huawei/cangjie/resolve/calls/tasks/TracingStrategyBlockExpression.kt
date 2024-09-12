package com.huawei.cangjie.resolve.calls.tasks

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjBlockExpression
import com.huawei.cangjie.resolve.calls.model.ResolvedCall

class TracingStrategyBlockExpression(
    private val blockExpression: CjBlockExpression,
    call: Call
) :AbstractTracingStrategy(blockExpression,call){
    override fun bindCall(trace: BindingTrace, call: Call) {

    }

    override fun <D : CallableDescriptor?> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {

    }

    override fun <D : CallableDescriptor?> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {

    }

    override fun unresolvedReference(trace: BindingTrace) {

    }

    override fun <D : CallableDescriptor?> unresolvedReferenceWrongReceiver(
        trace: BindingTrace,
        candidates: MutableCollection<out ResolvedCall<D>>
    ) {

    }
}
