package com.linqingying.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface DeclarationDescriptorWithSource extends DeclarationDescriptor {
    //    @NotNull
//    default SourceElement getSource() {
//        return SourceElement.NO_SOURCE;
//    }
    SourceElement getSource();

    @Override
    @NotNull
    DeclarationDescriptorWithSource getOriginal();
}
