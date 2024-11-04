package com.linqingying.cangjie.resolve.calls.tasks

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.Errors.UNRESOLVED_REFERENCE
import com.linqingying.cangjie.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.CjConstructorDelegationCall
import com.linqingying.cangjie.resolve.BindingContext.*
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.util.reportOnElement
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils


class TracingStrategyForImplicitConstructorDelegationCall(
    val delegationCall: CjConstructorDelegationCall, call: Call
) : AbstractTracingStrategy(delegationCall.calleeExpression!!, call) {

    val calleeExpression = delegationCall.calleeExpression

    override fun bindCall(trace: BindingTrace, call: Call) {
        trace.record(CALL, call.calleeExpression, call)
    }

    override fun <D : CallableDescriptor> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        val descriptor = resolvedCall.candidateDescriptor
        val storedReference = trace.get(REFERENCE_TARGET, calleeExpression)
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, calleeExpression, descriptor)
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        trace.record(RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        trace.report(UNRESOLVED_REFERENCE.on(calleeExpression!!, calleeExpression))
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(trace: BindingTrace, candidates: Collection<ResolvedCall<D>>) {
        trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates))
    }


    override fun <D : CallableDescriptor> ambiguity(trace: BindingTrace, resolvedCalls: Collection<ResolvedCall<D>>) {
        reportError(trace)
    }
    override fun <D : CallableDescriptor> noneApplicable(
        trace: BindingTrace,
        descriptors: Collection<ResolvedCall<D>>
    ) {
        reportError(trace)
    }


    override fun noValueForParameter(trace: BindingTrace, valueParameter: ValueParameterDescriptor) {
        reportError(trace)
    }

    override fun unsafeCall(trace: BindingTrace, type: CangJieType, isCallForImplicitInvoke: Boolean) {
        unexpectedError("unsafeCall")

    }

    private fun reportError(trace: BindingTrace) {
        val reportOn = delegationCall.reportOnElement()
        if (!trace.bindingContext.diagnostics.forElement(reportOn).any { it.factory == Errors.EXPLICIT_DELEGATION_CALL_REQUIRED }) {
            trace.report(Errors.EXPLICIT_DELEGATION_CALL_REQUIRED.on(reportOn))
        }
    }

    // Underlying methods should not be called because such errors are impossible
    // when resolving delegation call


    override fun <D : CallableDescriptor> cannotCompleteResolve(
        trace: BindingTrace,
        descriptors: Collection<ResolvedCall<D>>
    ) {
        unexpectedError("cannotCompleteResolve")
    }
    override fun invisibleMember(trace: BindingTrace, descriptor: DeclarationDescriptorWithVisibility) {
        reportError(trace)

    }

    private fun unexpectedError(type: String) {
        throw AssertionError("Unexpected error type: $type")
    }
}
