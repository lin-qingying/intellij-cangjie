package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConstructorDescriptor extends FunctionDescriptor{
    @NotNull
    @Override
    ConstructorDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            DescriptorVisibility visibility,
            Kind kind,
            boolean copyOverrides
    );


    @Nullable
    @Override
    ConstructorDescriptor substitute(@NotNull TypeSubstitutor substitutor);
    @NotNull
    @Override
    ConstructorDescriptor getOriginal();
    @NotNull
    @Override
    ClassifierDescriptorWithTypeParameters getContainingDeclaration();
}