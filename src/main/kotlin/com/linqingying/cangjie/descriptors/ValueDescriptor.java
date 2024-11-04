package com.linqingying.cangjie.descriptors;


import com.linqingying.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;

public interface ValueDescriptor extends CallableDescriptor {
    @NotNull
    CangJieType getType();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}
