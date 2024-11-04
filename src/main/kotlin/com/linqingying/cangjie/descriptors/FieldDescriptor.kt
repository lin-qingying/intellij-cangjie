package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.descriptors.annotations.Annotated
//元数据
interface FieldDescriptor : Annotated {
    val correspondingVariableBase: VariableDescriptor
}
