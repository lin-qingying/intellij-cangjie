package com.linqingying.cangjie.resolve.descriptorUtil

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.types.CangJieType

//fun CangJieType.hasExactAnnotation(): Boolean = annotations.hasAnnotation(EXACT_ANNOTATION_FQ_NAME)

fun TypeParameterDescriptor.hasOnlyInputTypesAnnotation(): Boolean = annotations.hasAnnotation(ONLY_INPUT_TYPES_FQ_NAME)
//fun AnnotationDescriptor.isExactAnnotation(): Boolean = this.fqName == EXACT_ANNOTATION_FQ_NAME
//fun CallableDescriptor.hasDynamicExtensionAnnotation(): Boolean = annotations.hasAnnotation(DYNAMIC_EXTENSION_FQ_NAME)
//fun CallableDescriptor.hasLowPriorityInOverloadResolution(): Boolean = annotations.hasAnnotation(LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_FQ_NAME)
