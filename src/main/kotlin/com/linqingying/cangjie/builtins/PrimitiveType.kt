package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name

enum class PrimitiveType(typeName: String) {
    BOOL("Bool"),
    Rune("Rune"),


    INT64("Int64"),
    INT32("Int32"),
    INT16("Int16"),
    INT8("Int8"),
    INTNATIVE("IntNative"),
    UINT64("Int64"),
    UINT32("Int32"),
    UINT16("Int16"),
    UINT8("Int8"),
    UINTNATIVE("UIntNative"),


    FLOAT64("Float64"),
    FLOAT32("Float32"),
    FLOAT16("Float16"),

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
        val NUMBER_TYPES = setOf(Rune, INT64, INT32, INT16, INT8, FLOAT64,FLOAT32,FLOAT16)

        @JvmStatic
        fun getByShortName(name: String): PrimitiveType? = when (name) {
            "Bool" -> BOOL
            "Rune" -> Rune


            "Int64" -> INT64

            "Int32" -> INT32

            "Int16" -> INT16

            "Int8" -> INT8
            "Float64" -> FLOAT64
            "Float32" -> FLOAT32
            "Float16" -> FLOAT16

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
