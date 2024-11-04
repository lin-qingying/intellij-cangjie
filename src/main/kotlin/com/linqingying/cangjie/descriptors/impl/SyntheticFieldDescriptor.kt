package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.PropertyDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name

class SyntheticFieldDescriptor   constructor(
    val propertyDescriptor: PropertyDescriptor,
    accessorDescriptor: PropertyAccessorDescriptor,
    sourceElement: SourceElement
) : LocalVariableDescriptor(
    accessorDescriptor, Annotations.EMPTY, SyntheticFieldDescriptor.NAME,
    propertyDescriptor.type, propertyDescriptor.isVar,
    sourceElement
)  {
    constructor(
        accessorDescriptor: PropertyAccessorDescriptor,
        sourceElement: SourceElement
    ) : this(accessorDescriptor.correspondingProperty, accessorDescriptor, sourceElement)

    companion object {
        @JvmField
        val NAME = Name.identifier("field")
    }
}
