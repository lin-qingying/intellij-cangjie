package com.huawei.cangjie.types;

import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.types.model.TypeConstructorMarker;
import org.jetbrains.annotations.Nullable;

public interface TypeConstructor extends TypeConstructorMarker {
    @Nullable
    ClassifierDescriptor getDeclarationDescriptor();
}