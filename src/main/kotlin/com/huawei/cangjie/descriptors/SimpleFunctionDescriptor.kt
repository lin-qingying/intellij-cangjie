package com.huawei.cangjie.descriptors

import com.huawei.cangjie.mpp.SimpleFunctionSymbolMarker

interface SimpleFunctionDescriptor : FunctionDescriptor, SimpleFunctionSymbolMarker {
    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out SimpleFunctionDescriptor>
}
