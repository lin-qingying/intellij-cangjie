package com.linqingying.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface MemberDescriptor extends DeclarationDescriptorNonRoot, DeclarationDescriptorWithVisibility {
    @NotNull
    Modality getModality();

   default void setModality(@NotNull Modality modality){

   }

    @Override
    @NotNull
    DescriptorVisibility getVisibility();

    default boolean isExpect() {
        return false;
    }
    default boolean isUnsafe(){
        return false;
}
    //
//    bool isActual();
//
//    bool isExternal();
}
