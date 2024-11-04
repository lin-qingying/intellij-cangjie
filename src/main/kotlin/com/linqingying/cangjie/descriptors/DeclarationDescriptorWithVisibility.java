package com.linqingying.cangjie.descriptors;


import org.jetbrains.annotations.NotNull;

public interface DeclarationDescriptorWithVisibility extends DeclarationDescriptor {
    @NotNull
    DescriptorVisibility getVisibility();
}
