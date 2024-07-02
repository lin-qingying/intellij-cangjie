package com.huawei.cangjie.resolve.calls.tasks;


import com.huawei.cangjie.builtins.FunctionTypesKt;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.psi.CjReferenceExpression;
import com.huawei.cangjie.psi.CjSimpleNameExpression;
import com.huawei.cangjie.resolve.BindingContext;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.types.CangJieType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.huawei.cangjie.descriptors.Errors.FUNCTION_EXPECTED;
import static com.huawei.cangjie.descriptors.Errors.NO_RECEIVER_ALLOWED;
import static com.huawei.cangjie.resolve.BindingContext.CALL;
import static com.huawei.cangjie.resolve.BindingContext.RESOLVED_CALL;

public class TracingStrategyForInvoke extends AbstractTracingStrategy {
    private final CangJieType calleeType;

    public TracingStrategyForInvoke(
            @NotNull CjExpression reference,
            @NotNull Call call,
            @NotNull CangJieType calleeType
    ) {
        super(reference, call);
        this.calleeType = calleeType;
    }

    @Override
    public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
        // If reference is a simple name, it's 'variable as function call' case ('foo(a, b)' where 'foo' is a variable).
        // The outer call is bound ('foo(a, b)'), while 'invoke' call for this case is 'foo.invoke(a, b)' and shouldn't be bound.
        if (reference instanceof CjSimpleNameExpression) return;
        trace.record(CALL, reference, call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(
            @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
    ) {
        PsiElement callElement = call.getCallElement();
        if (callElement instanceof CjReferenceExpression) {
            trace.record(BindingContext.REFERENCE_TARGET, (CjReferenceExpression) callElement, resolvedCall.getCandidateDescriptor());
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(
            @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
    ) {
        if (reference instanceof CjSimpleNameExpression) return;
        trace.record(RESOLVED_CALL, call, resolvedCall);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        functionExpectedOrNoReceiverAllowed(trace);
    }

    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
            @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
    ) {
        functionExpectedOrNoReceiverAllowed(trace);
    }

    private void functionExpectedOrNoReceiverAllowed(BindingTrace trace) {
        if (FunctionTypesKt.isNonExtensionFunctionType(calleeType)) {
            trace.report(NO_RECEIVER_ALLOWED.on(reference));
        }
        else {
            trace.report(FUNCTION_EXPECTED.on(reference, reference, calleeType));
        }
    }
}
