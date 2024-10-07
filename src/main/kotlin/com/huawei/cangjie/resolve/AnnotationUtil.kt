package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.resolve.constants.ErrorValue


fun AnnotationDescriptor.argumentValue(parameterName: String): ConstantValue<*>? {
    return allValueArguments[Name.identifier(parameterName)].takeUnless { it is ErrorValue }
}
