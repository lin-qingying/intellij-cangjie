package com.linqingying.cangjie.descriptors.annotations

import com.linqingying.cangjie.utils.toLowerCaseAsciiOnly

enum class AnnotationUseSiteTarget(renderName: String? = null) {
    FIELD,
    FILE,
    PROPERTY,
    PROPERTY_GETTER("get"),
    PROPERTY_SETTER("set"),
    RECEIVER,
    CONSTRUCTOR_PARAMETER("param"),
    SETTER_PARAMETER("setparam");


    val renderName: String = renderName ?: name.toLowerCaseAsciiOnly()
}
