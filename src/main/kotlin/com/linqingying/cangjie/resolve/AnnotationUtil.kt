package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.resolve.constants.ErrorValue


fun AnnotationDescriptor.argumentValue(parameterName: String): ConstantValue<*>? {
    return allValueArguments[Name.identifier(parameterName)].takeUnless { it is ErrorValue }
}
