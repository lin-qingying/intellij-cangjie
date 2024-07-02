package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReceiverParameterDescriptorImpl extends AbstractReceiverParameterDescriptor{

    private final DeclarationDescriptor containingDeclaration;
    private ReceiverValue value;
    public ReceiverParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ReceiverValue value,
            @NotNull Annotations annotations
    ) {
        this(containingDeclaration, value, annotations, SpecialNames.THIS);
    }
    public ReceiverParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ReceiverValue value,
            @NotNull Annotations annotations,
            @NotNull Name name
    ) {
        super(annotations, name);
        this.containingDeclaration = containingDeclaration;
        this.value = value;
    }
    @Nullable
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;

    }

    @Override
    public @NotNull ReceiverValue getValue() {
        return value;

    }
//    public void setOutType(@NotNull CangJieType outType) {
//        assert TypeUtilsKt.shouldBeUpdated(this.value.getType());
//        this.value = value.replaceType(outType);
//    }
    @Override
    public @NotNull ReceiverParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner) {
        return new ReceiverParameterDescriptorImpl(newOwner, value, getAnnotations());

    }


}
