package com.linqingying.cangjie.resolve.calls.tasks;

import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.psi.Call;
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.linqingying.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OldResolutionCandidate<D extends CallableDescriptor> {
    private final Call call;
    private final D candidateDescriptor;
    private final TypeSubstitutor knownTypeParametersResultingSubstitutor;
    private ReceiverValue dispatchReceiver; // receiver object of a method
    private ExplicitReceiverKind explicitReceiverKind;

    private OldResolutionCandidate(
            @NotNull Call call, @NotNull D descriptor, @Nullable ReceiverValue dispatchReceiver,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @Nullable TypeSubstitutor knownTypeParametersResultingSubstitutor
    ) {
        this.call = call;
        this.candidateDescriptor = descriptor;
        this.dispatchReceiver = dispatchReceiver;
        this.explicitReceiverKind = explicitReceiverKind;
        this.knownTypeParametersResultingSubstitutor = knownTypeParametersResultingSubstitutor;
    }

    public static <D extends CallableDescriptor> OldResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor
    ) {
        return new OldResolutionCandidate<>(call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);
    }

    public static <D extends CallableDescriptor> OldResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor, @Nullable TypeSubstitutor knownTypeParametersResultingSubstitutor
    ) {
        return new OldResolutionCandidate<>(call, descriptor, null, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                knownTypeParametersResultingSubstitutor);
    }

    public static <D extends CallableDescriptor> OldResolutionCandidate<D> create(
            @NotNull Call call, @NotNull D descriptor, @Nullable ReceiverValue dispatchReceiver,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @Nullable TypeSubstitutor knownTypeParametersResultingSubstitutor
    ) {
        return new OldResolutionCandidate<>(call, descriptor, dispatchReceiver, explicitReceiverKind, knownTypeParametersResultingSubstitutor);
    }

    public void setDispatchReceiver(@Nullable ReceiverValue dispatchReceiver) {
        this.dispatchReceiver = dispatchReceiver;
    }

    public void setExplicitReceiverKind(@NotNull ExplicitReceiverKind explicitReceiverKind) {
        this.explicitReceiverKind = explicitReceiverKind;
    }

    @NotNull
    public Call getCall() {
        return call;
    }

    @NotNull
    public D getDescriptor() {
        return candidateDescriptor;
    }

    @Nullable
    public ReceiverValue getDispatchReceiver() {
        return dispatchReceiver;
    }

    @NotNull
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return explicitReceiverKind;
    }

    @Nullable
    public TypeSubstitutor getKnownTypeParametersResultingSubstitutor() {
        return knownTypeParametersResultingSubstitutor;
    }

    @Override
    public String toString() {
        return candidateDescriptor.toString();
    }
}
