package com.linqingying.cangjie.resolve.calls.tasks

import com.linqingying.cangjie.builtins.isNonExtensionFunctionType
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjReferenceExpression
import com.linqingying.cangjie.psi.CjSimpleNameExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.types.CangJieType
import com.intellij.psi.PsiElement


class TracingStrategyForInvoke(
    reference: CjExpression,
    call: Call,
    private val calleeType: CangJieType
) : AbstractTracingStrategy(reference, call) {
    override fun bindCall(trace: BindingTrace, call: Call) {
        // If reference is a simple name, it's 'variable as function call' case ('foo(a, b)' where 'foo' is a variable).
        // The outer call is bound ('foo(a, b)'), while 'invoke' call for this case is 'foo.invoke(a, b)' and shouldn't be bound.
        if (reference is CjSimpleNameExpression) return
        trace.record(BindingContext.CALL, reference, call)
    }

    override fun <D : CallableDescriptor> bindReference(
        trace: BindingTrace, resolvedCall: ResolvedCall<D>
    ) {
        val callElement: PsiElement = call.callElement
        if (callElement is CjReferenceExpression) {
            trace.record(BindingContext.REFERENCE_TARGET, callElement, resolvedCall.candidateDescriptor)
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(
        trace: BindingTrace, resolvedCall: ResolvedCall<D>
    ) {
        if (reference is CjSimpleNameExpression) return
        trace.record(BindingContext.RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        functionExpectedOrNoReceiverAllowed(trace)
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace, candidates: Collection<ResolvedCall<D>>
    ) {
        functionExpectedOrNoReceiverAllowed(trace)
    }


    private fun functionExpectedOrNoReceiverAllowed(trace: BindingTrace) {
        if (calleeType.isNonExtensionFunctionType) {
            trace.report(Errors.NO_RECEIVER_ALLOWED.on(reference))
        } else {
            trace.report(Errors.FUNCTION_EXPECTED.on(reference, reference, calleeType))
        }
    }
}
