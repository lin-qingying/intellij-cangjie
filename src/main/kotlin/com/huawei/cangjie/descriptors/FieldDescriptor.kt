package com.huawei.cangjie.descriptors

import com.huawei.cangjie.descriptors.annotations.Annotated
//元数据
interface FieldDescriptor : Annotated {
    val correspondingVariableBase: VariableDescriptorBase
}
