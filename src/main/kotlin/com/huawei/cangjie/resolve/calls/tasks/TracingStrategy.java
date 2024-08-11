package com.huawei.cangjie.resolve.calls.tasks;


import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface TracingStrategy {
    void bindCall(@NotNull BindingTrace trace, @NotNull Call call);
    <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall);


    <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall);

    void unresolvedReference(@NotNull BindingTrace trace);

    <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates);

    <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates);

    <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls);

    <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors);

    <D extends CallableDescriptor> void cannotCompleteResolve(
            @NotNull BindingTrace trace,
            @NotNull Collection<? extends ResolvedCall<D>> descriptors
    );
    void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor);

    void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter);

}
