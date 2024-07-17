package com.huawei.cangjie.builtins

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name

enum class PrimitiveType(typeName: String) {
    BOOLEAN("Boolean"),
    CHAR("Char"),


    INT64("Int64"),
    INT32("Int32"),
    INT16("Int16"),
    INT8("Int8"),
    UINT64("Int64"),
    UINT32("Int32"),
    UINT16("Int16"),
    UINT8("Int8"),


    FLOAT("Float"),

    ;

    val typeName: Name = Name.identifier(typeName)

    val arrayTypeName: Name = Name.identifier("${typeName}Array")

    val typeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME.child(
            this.typeName
        )
    }

    val arrayTypeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StandardNames.BUILT_INS_PACKAGE_FQ_NAME.child(
            arrayTypeName
        )
    }

    companion object {
        @JvmField
        val NUMBER_TYPES = setOf(CHAR, INT64, INT32, INT16, INT8, FLOAT)

        @JvmStatic
        fun getByShortName(name: String): PrimitiveType? = when (name) {
            "Boolean" -> BOOLEAN
            "Char" -> CHAR


            "Int64" -> INT64

            "Int32" -> INT32

            "Int16" -> INT16

            "Int8" -> INT8
            "Float" -> FLOAT

            else -> null
        }

//        @JvmStatic
//        fun getByShortArrayName(name: String): PrimitiveType? = when (name) {
//            "BooleanArray" -> BOOLEAN
//            "CharArray" -> CHAR
//
//            "IntArray" -> INT64
//            "FloatArray" -> FLOAT
//
//            else -> null
//        }
    }
}
