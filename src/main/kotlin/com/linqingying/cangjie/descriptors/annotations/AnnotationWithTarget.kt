package com.linqingying.cangjie.descriptors.annotations

import com.linqingying.cangjie.utils.toLowerCaseAsciiOnly

data class AnnotationWithTarget(val annotation: AnnotationDescriptor, val target: AnnotationUseSiteTarget)

