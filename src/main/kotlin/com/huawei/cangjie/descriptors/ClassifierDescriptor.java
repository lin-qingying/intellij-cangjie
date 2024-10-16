package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.mpp.ClassifierSymbolMarker;

import com.huawei.cangjie.types.SimpleType;
import com.huawei.cangjie.types.TypeConstructor;
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
