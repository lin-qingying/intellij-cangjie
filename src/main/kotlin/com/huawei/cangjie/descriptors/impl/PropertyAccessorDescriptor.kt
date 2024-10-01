package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*

interface PropertyAccessorDescriptor : FunctionDescriptor {
    val isDefault: Boolean

    override val original: PropertyAccessorDescriptor

    override fun getOverriddenDescriptors(): Collection<PropertyAccessorDescriptor >

    val correspondingProperty: PropertyDescriptor

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): PropertyAccessorDescriptor
}
