package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.mpp.CallableSymbolMarker;

public interface CallableDescriptor extends DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot,
        Substitutable<CallableDescriptor>, CallableSymbolMarker {

}