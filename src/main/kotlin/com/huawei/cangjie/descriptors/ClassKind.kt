package com.huawei.cangjie.descriptors


enum class ClassKind(val codeRepresentation: String?) {
    STRUCT("struct"),
    CLASS("class"),
    INTERFACE("interface"),
    TUPLE("tuple"),
    ENUM("enum"),
    EXTEND("extend"),
    ENUM_ENTRY(null),
    BASIC(null),
    ANNOTATION_CLASS("annotation class");

    val isStruct: Boolean
        get() = this == STRUCT
    val isEnumEntry: Boolean
        get() = this == ENUM_ENTRY
    val isObject: Boolean
        get() =  isEnumEntry || isStruct
    val isEnum: Boolean
        get() = this == ENUM || this == ENUM_ENTRY

    val isSingleton: Boolean
        get() =  this == ENUM_ENTRY
}
inline val ClassKind.isInterface: Boolean
    get() = this == ClassKind.INTERFACE

inline val ClassKind.isClass: Boolean
    get() = this == ClassKind.CLASS
