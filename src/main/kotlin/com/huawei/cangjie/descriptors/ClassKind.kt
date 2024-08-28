package com.huawei.cangjie.descriptors


enum class ClassKind(val codeRepresentation: String?) {
    STRUCT("struct"),
    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum"),
    EXTEND("extend"),
    ENUM_ENTRY(null),
    BASIC(null),
    ANNOTATION_CLASS("annotation class");

    val isStruct: Boolean
        get() = this == STRUCT
    val isEnum: Boolean
        get() = this == ENUM || this == ENUM_ENTRY
}
