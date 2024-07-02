package com.huawei.cangjie.descriptors



enum class ClassKind(val codeRepresentation: String?) {
    CLASS( "class"),
    INTERFACE("interface"),
    ENUM( "enum"),
    ENUM_ENTRY( null);

    val isSingleton: Boolean
        get() =   this == ENUM_ENTRY

}