package com.huawei.cangjie.descriptors



enum class ClassKind(val codeRepresentation: String?) {
    STRUCT("struct"),
    CLASS( "class"),
    INTERFACE("interface"),
    ENUM( "enum"),
    ENUM_ENTRY( null),
    BASIC(null),
    ANNOTATION_CLASS( "annotation class");

    val isSingleton: Boolean
        get() =   this == ENUM_ENTRY

}
