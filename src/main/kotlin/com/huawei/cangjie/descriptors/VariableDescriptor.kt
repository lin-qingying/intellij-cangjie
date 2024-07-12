package com.huawei.cangjie.descriptors

interface VariableDescriptor: ValueDescriptor
  /*  CallableMemberDescriptor, PropertySymbolMarker*/ {


//    override fun newCopyBuilder():  CallableMemberDescriptor.CopyBuilder<out  VariableDescriptor?>
//    override val original: VariableDescriptor

    val isVar: Boolean
}
