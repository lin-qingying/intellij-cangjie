package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.calls.inference.model.NewTypeVariable
import com.linqingying.cangjie.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import com.linqingying.cangjie.types.CangJieType
interface CallHandle {
    object NONE : CallHandle
}
