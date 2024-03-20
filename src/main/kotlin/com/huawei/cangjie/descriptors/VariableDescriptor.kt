package com.huawei.cangjie.descriptors

import com.huawei.cangjie.mpp.PropertySymbolMarker

interface VariableDescriptor:
    CallableMemberDescriptor, PropertySymbolMarker {


    override fun newCopyBuilder():  CallableMemberDescriptor.CopyBuilder<out  VariableDescriptor?>
    override val original: VariableDescriptor

    val isVar: Boolean
}