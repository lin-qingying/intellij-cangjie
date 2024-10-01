package com.huawei.cangjie.descriptors.annotations

import com.huawei.cangjie.utils.toLowerCaseAsciiOnly

data class AnnotationWithTarget(val annotation: AnnotationDescriptor, val target: AnnotationUseSiteTarget)

