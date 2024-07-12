package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConstructorDescriptor extends FunctionDescriptor{
    boolean isPrimary();


    @NotNull
    ClassDescriptor getConstructedClass();

    @NotNull
    @Override
    ConstructorDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            DescriptorVisibility visibility,
            Kind kind,
            boolean copyOverrides
    );
//    @NotNull
//    @Override
//    CangJieType getReturnType();

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
