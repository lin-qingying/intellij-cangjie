package com.linqingying.cangjie.descriptors;

import com.linqingying.cangjie.mpp.ClassifierSymbolMarker;

import com.linqingying.cangjie.types.SimpleType;
import com.linqingying.cangjie.types.TypeConstructor;
import org.jetbrains.annotations.NotNull;

public interface ClassifierDescriptor extends DeclarationDescriptorNonRoot, ClassifierSymbolMarker  {
    @NotNull
    TypeConstructor getTypeConstructor();

    @NotNull
    SimpleType getDefaultType();

    @NotNull
    @Override
    ClassifierDescriptor getOriginal();
}
