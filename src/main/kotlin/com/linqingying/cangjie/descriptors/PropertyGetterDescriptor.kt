package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptor


interface   PropertyGetterDescriptor : PropertyAccessorDescriptor {
    override val original: PropertyGetterDescriptor
    override fun getOverriddenDescriptors(): Collection<PropertyGetterDescriptor>

}

interface PropertySetterDescriptor : PropertyAccessorDescriptor {
    override val original: PropertySetterDescriptor

    override fun getOverriddenDescriptors(): Collection<PropertySetterDescriptor>
}
