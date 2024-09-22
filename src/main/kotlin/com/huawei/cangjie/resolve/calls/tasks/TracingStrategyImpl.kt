package com.huawei.cangjie.resolve.calls.tasks

import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.diagnostics.Errors
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjReferenceExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.resolve.calls.util.isInvokeCallOnVariable
import com.huawei.cangjie.types.ErrorUtils.isError

class TracingStrategyImpl private constructor(override val reference: CjReferenceExpression, call: Call) :
    AbstractTracingStrategy(reference, call) {
    override fun bindCall(trace: BindingTrace, call: Call) {
        trace.record(BindingContext.CALL, call.calleeExpression, call)
    }

    override fun <D : CallableDescriptor> bindReference(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        val descriptor: DeclarationDescriptor = resolvedCall.candidateDescriptor
        //        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
//            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
//        }
//        if (descriptor instanceof FakeCallableDescriptorForObject) {
//            FakeCallableDescriptorForObject fakeCallableDescriptorForObject = (FakeCallableDescriptorForObject) descriptor;
//            descriptor = fakeCallableDescriptorForObject.getReferencedDescriptor();
//            if (fakeCallableDescriptorForObject.getClassDescriptor().getCompanionObjectDescriptor() != null) {
//                trace.record(SHORT_REFERENCE_TO_COMPANION_OBJECT, reference, fakeCallableDescriptorForObject.getClassDescriptor());
//            }
//        }
        val storedReference = trace.get(
            BindingContext.REFERENCE_TARGET,
            reference
        )
        if (storedReference == null || !isError(descriptor)) {
            trace.record(
                BindingContext.REFERENCE_TARGET,
                reference, descriptor
            )
        }
    }

    override fun <D : CallableDescriptor> bindResolvedCall(trace: BindingTrace, resolvedCall: ResolvedCall<D>) {
        trace.record(BindingContext.RESOLVED_CALL, call, resolvedCall)
    }

    override fun unresolvedReference(trace: BindingTrace) {
        trace.report(Errors.UNRESOLVED_REFERENCE.on(reference, reference))
    }

    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace,
        candidates: Collection<ResolvedCall<D>>
    ) {
        val variableDescriptor = isFunctionExpectedError<D>(candidates)
        if (variableDescriptor != null) {
            trace.report(
                Errors.FUNCTION_EXPECTED.on(
                    reference,
                    reference, variableDescriptor.type
                )
            )
        } else {
            trace.report(Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates))
        }
    } //    @Override
    //    public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
    //        DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
    //        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
    //            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
    //        }
    //        if (descriptor instanceof FakeCallableDescriptorForObject) {
    //            FakeCallableDescriptorForObject fakeCallableDescriptorForObject = (FakeCallableDescriptorForObject) descriptor;
    //            descriptor = fakeCallableDescriptorForObject.getReferencedDescriptor();
    //            if (fakeCallableDescriptorForObject.getClassDescriptor().getCompanionObjectDescriptor() != null) {
    //                trace.record(SHORT_REFERENCE_TO_COMPANION_OBJECT, reference, fakeCallableDescriptorForObject.getClassDescriptor());
    //            }
    //        }
    //        DeclarationDescriptor storedReference = trace.get(REFERENCE_TARGET, reference);
    //        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
    //            trace.record(REFERENCE_TARGET, reference, descriptor);
    //        }
    //    }
    //
    //    @Override
    //    public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
    //        trace.record(RESOLVED_CALL, call, resolvedCall);
    //    }
    //
    //    @Override
    //    public void unresolvedReference(@NotNull BindingTrace trace) {
    //        trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
    //    }
    //
    //    @Override
    //    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
    //        VariableCallableDescriptor variableDescriptor = isFunctionExpectedError(candidates);
    //        if (variableDescriptor != null) {
    //            trace.report(Errors.FUNCTION_EXPECTED.on(reference, reference, variableDescriptor.getType()));
    //        }
    //        else {
    //            trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates));
    //        }
    //    }
    //
    //    @Nullable
    //    private static <D extends CallableDescriptor> VariableCallableDescriptor isFunctionExpectedError(
    //            @NotNull Collection<? extends ResolvedCall<D>> candidates
    //    ) {
    //        List<VariableCallableDescriptor> variables = CollectionsKt.map(candidates, TracingStrategyImpl::variableIfFunctionExpectedError);
    //        List<VariableCallableDescriptor> distinctVariables = CollectionsKt.distinct(variables);
    //        return CollectionsKt.singleOrNull(distinctVariables);
    //    }
    //
    //    @Nullable
    //    private static <D extends CallableDescriptor> VariableCallableDescriptor variableIfFunctionExpectedError(
    //            @NotNull ResolvedCall<D> candidate
    //    ) {
    //        if (!(candidate instanceof VariableAsFunctionResolvedCall)) return null;
    //
    //        ResolvedCall<VariableCallableDescriptor> variableCall = ((VariableAsFunctionResolvedCall) candidate).getVariableCall();
    //        ResolvedCall<FunctionDescriptor> functionCall = ((VariableAsFunctionResolvedCall) candidate).getFunctionCall();
    //
    //        CangJieType type = variableCall.getCandidateDescriptor().getType();
    //
    //        bool nonFunctionalVar = variableCall.getStatus().isSuccess() && !FunctionTypesCj.isFunctionType(type);
    //        Call functionPsiCall = functionCall.getCall();
    //        if (nonFunctionalVar && CallResolverUtilCj.isInvokeCallOnVariable(functionPsiCall) && functionPsiCall.getValueArguments().isEmpty()) {
    //            return variableCall.getCandidateDescriptor();
    //        }
    //
    //        return null;
    //    }


    companion object {
        //
        @JvmStatic
        fun create(reference: CjReferenceExpression, call: Call): TracingStrategy {
            return TracingStrategyImpl(reference, call)
        }


        private fun <D : CallableDescriptor> isFunctionExpectedError(
            candidates: Collection<ResolvedCall<D>>
        ): VariableDescriptor? {
            val variables =
                candidates.map { candidate: ResolvedCall<D> -> variableIfFunctionExpectedError(candidate)!! }
            val distinctVariables = variables.distinct()
            return distinctVariables.singleOrNull()
        }

        private fun <D : CallableDescriptor> variableIfFunctionExpectedError(
            candidate: ResolvedCall<D>
        ): VariableDescriptor? {
            if (candidate !is VariableAsFunctionResolvedCall) return null

            val variableCall = (candidate as VariableAsFunctionResolvedCall).variableCall
            val functionCall = (candidate as VariableAsFunctionResolvedCall).functionCall

            val type = variableCall.candidateDescriptor.type

            val nonFunctionalVar = variableCall.status.isSuccess && !type.isFunctionType
            val functionPsiCall = functionCall.call
            if (nonFunctionalVar && isInvokeCallOnVariable(functionPsiCall) && functionPsiCall.valueArguments.isEmpty()) {
                return variableCall.candidateDescriptor
            }

            return null
        }
    }
}
