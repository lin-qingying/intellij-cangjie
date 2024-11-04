package com.linqingying.cangjie.descriptors;


import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.linqingying.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ReceiverParameterDescriptor extends ParameterDescriptor {
    @NotNull
    ReceiverValue getValue();

    @Nullable
    @Override
    ReceiverParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    @NotNull
    ReceiverParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner);
}
