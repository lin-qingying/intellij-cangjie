package com.huawei.cangjie.name

object SpecialNames {
    @JvmField
    val NO_NAME_PROVIDED = Name.special("<no name provided>")

    @JvmField
    val ROOT_PACKAGE = Name.special("<root package>")
    private const val ANONYMOUS_PARAMETER_NAME_PREFIX = "anonymous parameter"

    @JvmField
    val DEFAULT_NAME_FOR_COMPANION_OBJECT = Name.identifier("Companion")

    @JvmStatic
    fun anonymousParameterName(index: Int): Name {
        return Name.special("<$ANONYMOUS_PARAMETER_NAME_PREFIX $index>")
    }

    @JvmField
    val SAFE_IDENTIFIER_FOR_NO_NAME = Name.identifier("no_name_in_PSI_3d19d79d_1ba9_4cd0_b7f5_b46aa3cd5d40")

    const val ANONYMOUS_STRING = "<anonymous>"

    @JvmField
    val ANONYMOUS = Name.special(ANONYMOUS_STRING)

    @JvmField
    val ANONYMOUS_FQ_NAME = FqName.topLevel(Name.special(ANONYMOUS_STRING))

    @JvmField
    val OPTION = Name.identifier("Option")

    @JvmField
    val UNARY = Name.special("<unary>")

    @JvmField
    val THIS = Name.special("<this>")
    @JvmField
    val END_INIT = Name.special("<~init>")

    @JvmField
    val INIT = Name.special("<init>")

    @JvmField
    val ITERATOR = Name.special("<iterator>")

    @JvmField
    val DESTRUCT = Name.special("<destruct>")

    @JvmField
    val LOCAL = Name.special("<local>")

    @JvmField
    val UNDERSCORE_FOR_UNUSED_VAR = Name.special("<unused var>")

    @JvmField
    val IMPLICIT_SET_PARAMETER = Name.special("<set-?>")

    @JvmField
    val ARRAY = Name.special("<array>")

    @JvmField
    val RECEIVER = Name.special("<receiver>")


    @JvmField
    val ENUM_GET_ENTRIES = Name.special("<get-entries>")


    @JvmStatic
    fun subscribeOperatorIndex(idx: Int): Name {
        require(idx >= 0) { "Index should be non-negative, but was $idx" }

        return Name.special("<index_$idx>")
    }

    @JvmStatic
    fun safeIdentifier(name: Name?): Name {
        return if (name != null && !name.isSpecial) name else SAFE_IDENTIFIER_FOR_NO_NAME
    }

    @JvmStatic
    fun safeIdentifier(name: String?): Name {
        return safeIdentifier(if (name == null) null else Name.identifier(name))
    }

    fun isSafeIdentifier(name: Name): Boolean {
        return name.asString().isNotEmpty() && !name.isSpecial
    }
}
