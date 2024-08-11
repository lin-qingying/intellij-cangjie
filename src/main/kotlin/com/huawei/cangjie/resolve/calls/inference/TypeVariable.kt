package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import com.huawei.cangjie.types.CangJieType
interface CallHandle {
    object NONE : CallHandle
}
class TypeVariable(
    val call: CallHandle,
    internal val freshTypeParameter: TypeParameterDescriptor,
    val originalTypeParameter: TypeParameterDescriptor,
    val isExternal: Boolean
) {
    val name: Name get() = originalTypeParameter.name

    val type: CangJieType get() = freshTypeParameter.defaultType

    fun hasOnlyInputTypesAnnotation(): Boolean =
        originalTypeParameter.hasOnlyInputTypesAnnotation()
}
