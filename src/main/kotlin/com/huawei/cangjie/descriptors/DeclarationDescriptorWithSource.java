package com.huawei.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface DeclarationDescriptorWithSource extends DeclarationDescriptor {
    @NotNull
    SourceElement getSource();

    @Override
    @NotNull
    DeclarationDescriptorWithSource getOriginal();
}
