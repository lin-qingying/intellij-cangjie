package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;

public interface ValueDescriptor extends CallableDescriptor {
    @NotNull
    CangJieType getType();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}
