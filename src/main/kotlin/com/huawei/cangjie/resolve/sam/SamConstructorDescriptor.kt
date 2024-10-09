package com.huawei.cangjie.resolve.sam

import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor
import com.huawei.cangjie.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import com.huawei.cangjie.resolve.scopes.DescriptorKindExclude
interface SamConstructorDescriptor : SimpleFunctionDescriptor, FunctionInterfaceConstructorDescriptor {
    fun getSingleAbstractMethod(): CallableMemberDescriptor
}

object SamConstructorDescriptorKindExclude : DescriptorKindExclude() {
    override fun excludes(descriptor: DeclarationDescriptor) = descriptor is SamConstructorDescriptor

    override val fullyExcludedDescriptorKinds: Int get() = 0
}
