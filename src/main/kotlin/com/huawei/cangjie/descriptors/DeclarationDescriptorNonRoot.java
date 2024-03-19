package com.huawei.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface DeclarationDescriptorNonRoot extends DeclarationDescriptorWithSource {

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

}
