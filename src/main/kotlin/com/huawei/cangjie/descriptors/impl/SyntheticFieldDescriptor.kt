package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.descriptors.SourceElement
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name

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
