package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.types.TypeSubstitutor;
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
