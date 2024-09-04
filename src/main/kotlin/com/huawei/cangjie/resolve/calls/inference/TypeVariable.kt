package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.huawei.cangjie.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import com.huawei.cangjie.types.CangJieType
interface CallHandle {
    object NONE : CallHandle
}
