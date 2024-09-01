package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface VariableDescriptor extends CallableMemberDescriptor, VariableDescriptorBase {
    @Override
    CallableMemberDescriptor.@NotNull CopyBuilder<? extends VariableDescriptor> newCopyBuilder();

    @NotNull
    @Override
    VariableDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    @Override
    void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors);

    @Override
    @NotNull
    VariableDescriptor getOriginal();

    @Override
    @NotNull
    Collection<? extends VariableDescriptor> getOverriddenDescriptors();
}
