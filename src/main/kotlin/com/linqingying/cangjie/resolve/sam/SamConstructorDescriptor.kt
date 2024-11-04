package com.linqingying.cangjie.resolve.sam

import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.SimpleFunctionDescriptor
import com.linqingying.cangjie.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindExclude
interface SamConstructorDescriptor : SimpleFunctionDescriptor, FunctionInterfaceConstructorDescriptor {
    fun getSingleAbstractMethod(): CallableMemberDescriptor
}

object SamConstructorDescriptorKindExclude : DescriptorKindExclude() {
    override fun excludes(descriptor: DeclarationDescriptor) = descriptor is SamConstructorDescriptor

    override val fullyExcludedDescriptorKinds: Int get() = 0
}
