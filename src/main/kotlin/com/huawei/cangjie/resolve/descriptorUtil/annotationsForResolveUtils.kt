package com.huawei.cangjie.resolve.descriptorUtil

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.types.CangJieType

//fun CangJieType.hasExactAnnotation(): Boolean = annotations.hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

fun TypeParameterDescriptor.hasOnlyInputTypesAnnotation(): Boolean = annotations.hasAnnotation(ONLY_INPUT_TYPES_FQ_NAME)
//fun AnnotationDescriptor.isExactAnnotation(): Boolean = this.fqName == EXACT_ANNOTATION_FQ_NAME
//fun CallableDescriptor.hasDynamicExtensionAnnotation(): Boolean = annotations.hasAnnotation(DYNAMIC_EXTENSION_FQ_NAME)
//fun CallableDescriptor.hasLowPriorityInOverloadResolution(): Boolean = annotations.hasAnnotation(LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME)
