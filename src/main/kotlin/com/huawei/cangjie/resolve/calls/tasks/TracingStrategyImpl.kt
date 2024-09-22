package com.huawei.cangjie.resolve.calls.tasks;

import com.huawei.cangjie.builtins.FunctionTypesKt;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.diagnostics.Errors;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.CjReferenceExpression;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;

import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall;
import com.huawei.cangjie.resolve.calls.util.CallResolverUtilKt;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.ErrorUtils;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.huawei.cangjie.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static com.huawei.cangjie.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER;
import static com.huawei.cangjie.resolve.BindingContext.*;

public class TracingStrategyImpl extends AbstractTracingStrategy {
    private final CjReferenceExpression reference;

    private TracingStrategyImpl(@NotNull CjReferenceExpression reference, @NotNull Call call) {
        super(reference, call);
        this.reference = reference;
    }
//
    @NotNull
    public static TracingStrategy create(@NotNull CjReferenceExpression reference, @NotNull Call call) {
        return new TracingStrategyImpl(reference, call);
    }


    @Override
    public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
        trace.record(CALL, call.getCalleeExpression(), call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
        DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
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
        DeclarationDescriptor storedReference = trace.get(REFERENCE_TARGET, reference);
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, reference, descriptor);
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
        trace.record(RESOLVED_CALL, call, resolvedCall);

    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        trace.report(UNRESOLVED_REFERENCE.on(reference, reference));

    }

    @Nullable
    private static <D extends CallableDescriptor> VariableDescriptor isFunctionExpectedError(
            @NotNull Collection<? extends ResolvedCall<D>> candidates
    ) {
        List<VariableDescriptor> variables = CollectionsKt.map(candidates, TracingStrategyImpl::variableIfFunctionExpectedError);
        List<VariableDescriptor> distinctVariables = CollectionsKt.distinct(variables);
        return CollectionsKt.singleOrNull(distinctVariables);
    }

    @Nullable
    private static <D extends CallableDescriptor> VariableDescriptor variableIfFunctionExpectedError(
            @NotNull ResolvedCall<D> candidate
    ) {
        if (!(candidate instanceof VariableAsFunctionResolvedCall)) return null;

        ResolvedCall<VariableDescriptor> variableCall = ((VariableAsFunctionResolvedCall) candidate).getVariableCall();
        ResolvedCall<FunctionDescriptor> functionCall = ((VariableAsFunctionResolvedCall) candidate).getFunctionCall();

        CangJieType type = variableCall.getCandidateDescriptor().getType();

        boolean nonFunctionalVar = variableCall.getStatus().isSuccess() && !FunctionTypesKt.isFunctionType(type);
        Call functionPsiCall = functionCall.getCall();
        if (nonFunctionalVar && CallResolverUtilKt.isInvokeCallOnVariable(functionPsiCall) && functionPsiCall.getValueArguments().isEmpty()) {
            return variableCall.getCandidateDescriptor();
        }

        return null;
    }




    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
        VariableDescriptor variableDescriptor = isFunctionExpectedError(candidates);
        if (variableDescriptor != null) {
            trace.report(Errors.FUNCTION_EXPECTED.on(reference, reference, variableDescriptor.getType()));
        }
        else {
            trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates));
        }
    }







//    @Override
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
}
