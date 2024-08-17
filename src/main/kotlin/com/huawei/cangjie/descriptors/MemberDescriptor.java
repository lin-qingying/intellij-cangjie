package com.huawei.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface MemberDescriptor extends DeclarationDescriptorNonRoot, DeclarationDescriptorWithVisibility {
    @NotNull
    Modality getModality();

    @Override
    @NotNull
    DescriptorVisibility getVisibility();

  default   boolean isExpect(){
      return false;
  };
//
//    bool isActual();
//
//    bool isExternal();
}
