package com.huawei.cangjie.resolve.descriptorUtil

import com.huawei.cangjie.descriptors.TypeParameterDescriptor

fun TypeParameterDescriptor.hasOnlyInputTypesAnnotation(): Boolean = annotations.hasAnnotation(ONLY_INPUT_TYPES_FQ_NAME)
