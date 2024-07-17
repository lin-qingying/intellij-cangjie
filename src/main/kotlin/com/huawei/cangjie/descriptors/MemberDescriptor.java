package com.huawei.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface MemberDescriptor extends DeclarationDescriptorNonRoot, DeclarationDescriptorWithVisibility {
    @NotNull
    Modality getModality();

    @Override
    @NotNull
    DescriptorVisibility getVisibility();

//    bool isExpect();
//
//    bool isActual();
//
//    bool isExternal();
}
