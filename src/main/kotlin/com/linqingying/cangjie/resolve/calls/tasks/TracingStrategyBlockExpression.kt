package com.linqingying.cangjie.resolve.calls.tasks

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.CjBlockExpression
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall

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
