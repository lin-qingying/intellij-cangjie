package com.huawei.cangjie.descriptors.annotations

import com.huawei.cangjie.utils.toLowerCaseAsciiOnly

data class AnnotationWithTarget(val annotation: AnnotationDescriptor, val target: AnnotationUseSiteTarget)

enum class AnnotationUseSiteTarget(renderName: String? = null) {
    FIELD,
    FILE,
    PROPERTY,
    PROPERTY_GETTER("get"),
    PROPERTY_SETTER("set"),
    RECEIVER,
    CONSTRUCTOR_PARAMETER("param"),
    SETTER_PARAMETER("setparam"),
    PROPERTY_DELEGATE_FIELD("delegate");

    val renderName: String = renderName ?: name.toLowerCaseAsciiOnly()
}
