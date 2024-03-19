package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.mpp.FunctionSymbolMarker;
import org.jetbrains.annotations.NotNull;

public interface FunctionDescriptor extends CallableMemberDescriptor, FunctionSymbolMarker {
    @NotNull
    @Override
    FunctionDescriptor copy(DeclarationDescriptor newOwner, Modality modality, DescriptorVisibility visibility, Kind kind, boolean copyOverrides);

}