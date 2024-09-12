package com.huawei.cangjie.builtins

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.newHashMapWithExpectedSize
import com.huawei.cangjie.utils.newHashSetWithExpectedSize
import com.squareup.wire.internal.JvmField

@Suppress("Reformat")
object StandardNames {
    @kotlin.jvm.JvmField
    val CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME = Name.identifier("count")

    @kotlin.jvm.JvmField
    val NAME = Name.identifier("name")

    val rangeOfName = Name.identifier("rangeOf")
    val arrayOfName = Name.identifier("arrayOf")
    val returnOfName = Name.identifier("returnOf")

    @JvmStatic
    fun getFunctionName(parameterCount: Int): String {
        return "Function$parameterCount"
    }


    val STD_PACKAGE_NAME = Name.identifier("std")
    val CORE_PACKAGE_NAME = Name.identifier("core")


    val COMPRESS_PACKAGE_NAME = Name.identifier("compress")

    val CRYPTO_PACKAGE_NAME = Name.identifier("crypto")

    val ENCODING_PACKAGE_NAME = Name.identifier("encoding")

    val FUZZ_PACKAGE_NAME = Name.identifier("fuzz")

    val NET_PACKAGE_NAME = Name.identifier("net")

    val SERIALIZATION_PACKAGE_NAME = Name.identifier("serialization")


    @JvmField
    val STD_PACKAGE_FQ_NAME = FqName.topLevel(STD_PACKAGE_NAME)

    @JvmField
    val COMPRESS_PACKAGE_FQ_NAME = FqName.topLevel(COMPRESS_PACKAGE_NAME)

    @JvmField
    val CRYPTO_PACKAGE_FQ_NAME = FqName.topLevel(CRYPTO_PACKAGE_NAME)

    @JvmField
    val ENCODING_PACKAGE_FQ_NAME = FqName.topLevel(ENCODING_PACKAGE_NAME)

    @JvmField
    val FUZZ_PACKAGE_FQ_NAME = FqName.topLevel(FUZZ_PACKAGE_NAME)

    @JvmField
    val NET_PACKAGE_FQ_NAME = FqName.topLevel(NET_PACKAGE_NAME)

    @JvmField
    val SERIALIZATION_PACKAGE_FQ_NAME = FqName.topLevel(SERIALIZATION_PACKAGE_NAME)


    @JvmField
    val STD_CORE_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(Name.identifier("core"))


    @JvmField
    val NOTHING = Name.identifier("Nothing")

    @JvmField
    val RUNE = Name.identifier("Rune")

    @JvmField
    val UNIT = Name.identifier("Unit")

    @JvmField
    val INT8 = Name.identifier("Int8")

    @JvmField
    val INT16 = Name.identifier("Int16")

    @JvmField
    val INT32 = Name.identifier("Int32")

    @JvmField
    val INT64 = Name.identifier("Int64")

    @JvmField
    val INT_NATIVE = Name.identifier("IntNative")

    @JvmField
    val UINT8 = Name.identifier("UInt8")

    @JvmField
    val UINT16 = Name.identifier("UInt16")

    @JvmField
    val UINT32 = Name.identifier("UInt32")

    @JvmField
    val UINT64 = Name.identifier("UInt64")

    @JvmField
    val UINT_NATIVE = Name.identifier("UIntNative")

    @JvmField
    val FLOAT16 = Name.identifier("Float16")

    @JvmField
    val FLOAT32 = Name.identifier("Float32")

    @JvmField
    val FLOAT64 = Name.identifier("Float64")

    @JvmField
    val BOOL = Name.identifier("Bool")
    @JvmField
    val EXCEPTION = Name.identifier("Exception")

    @JvmField
    val OBJECT = Name.identifier("Object")

    @JvmField
    val ANY = Name.identifier("Any")

    @JvmField
    val ARRAY = Name.identifier("Array")
    @JvmField
    val RANGE = Name.identifier("Range")
    @JvmField
    val COUNTABLE = Name.identifier("Countable")
    @JvmField
    val EQUATABLE = Name.identifier("Equatable")
    @JvmField
    val COMPARABLE = Name.identifier("Comparable")

    @JvmField
    val STRING = Name.identifier("String")

    @JvmField
    val OPTION = Name.identifier("Option")


    @JvmField
    val CPOINTER = Name.identifier("CPointer")

    @JvmField
    val CSTRING = Name.identifier("CString")

    @JvmField
    val CTYPE = Name.identifier("CType")

    @JvmField
    val TOSTRING = Name.identifier("ToString")

    @JvmField
    val BUILT_INS_PACKAGE_FQ_NAME = FqName("")

    object FqNames {
        @JvmField
        val fqNameToPrimitiveType: Map<FqNameUnsafe, PrimitiveType> =
            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.entries.size).apply {
                for (primitiveType in PrimitiveType.entries) {
                    this[fqNameUnsafe(primitiveType.typeName.asString())] = primitiveType
                }
            }

        @JvmField
        val primitiveArrayTypeShortNames: Set<Name> =
            newHashSetWithExpectedSize<Name>(PrimitiveType.entries.size).apply {
                PrimitiveType.entries.mapTo(this) { it.arrayTypeName }
            }

        @JvmField
        val primitiveTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.entries.size).apply {
            PrimitiveType.entries.mapTo(this) { it.typeName }
        }

        @JvmField
        val arrayClassFqNameToPrimitiveType: MutableMap<FqNameUnsafe, PrimitiveType> =
            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.entries.size).apply {
                for (primitiveType in PrimitiveType.entries) {
                    this[fqNameUnsafe(primitiveType.arrayTypeName.asString())] = primitiveType
                }
            }

        @JvmField
        val core: FqName = FqName.topLevel(STD_PACKAGE_NAME).child(CORE_PACKAGE_NAME)

        @JvmField
        val anyFqName: FqName = core.child(ANY)

        @JvmField
        val anyUFqName: FqNameUnsafe = anyFqName.toUnsafe()
        @JvmField
        val exceptionFqName: FqName = core.child(  EXCEPTION)

        @JvmField
        val objectFqName: FqName = core.child(OBJECT)

        @JvmField
        val objectUFqName: FqNameUnsafe = objectFqName.toUnsafe()

        @JvmField
        val optionFqName: FqName = core.child(OPTION)

        @JvmField
        val optionUFqName: FqNameUnsafe = optionFqName.toUnsafe()



        @JvmField
        val countableFqName: FqName = core.child(COUNTABLE)
        @JvmField
        val equatableFqName: FqName = core.child(EQUATABLE)

        @JvmField
        val comparableFqName: FqName = core.child(COMPARABLE)

        @JvmField
        val rangeFqName: FqName = core.child(RANGE)

        @JvmField
        val stringFqName: FqName = core.child(STRING)

        @JvmField
        val stringUFqName: FqNameUnsafe = stringFqName.toUnsafe()

        @JvmField
        val arrayFqName: FqName = core.child(ARRAY)

        @JvmField
        val arrayUFqName: FqNameUnsafe = arrayFqName.toUnsafe()

        @JvmField
        val nothingFqName: FqName = fqName(NOTHING)
        @JvmField
        val nothingUFqName: FqNameUnsafe = nothingFqName.toUnsafe()


        @JvmField
        val toStringFqName: FqName = fqName(TOSTRING)

        @JvmField
        val toStringUFqName: FqNameUnsafe = nothingFqName.toUnsafe()


        @JvmField
        val runeFqName: FqName = fqName(RUNE)

        @JvmField
        val runeUFqName: FqNameUnsafe = runeFqName.toUnsafe()

        @JvmField
        val unitFqName = fqName(UNIT)

        @JvmField
        val unitUFqName = unitFqName.toUnsafe()

        /***************************Int***************************/
        @JvmField
        val int8FqName: FqName = fqName(INT8)

        @JvmField
        val int8UFqName: FqNameUnsafe = int8FqName.toUnsafe()

        @JvmField
        val int16FqName: FqName = fqName(INT16)

        @JvmField
        val int16UFqName: FqNameUnsafe = int16FqName.toUnsafe()

        @JvmField
        val int32FqName: FqName = fqName(INT32)

        @JvmField
        val int32UFqName: FqNameUnsafe = int32FqName.toUnsafe()

        @JvmField
        val int64FqName: FqName = fqName(INT64)

        @JvmField
        val int64UFqName: FqNameUnsafe = int64FqName.toUnsafe()

        @JvmField
        val int_nativeFqName: FqName = fqName(INT_NATIVE)

        @JvmField
        val int_nativeUFqName: FqNameUnsafe = int_nativeFqName.toUnsafe()

        /***************************UInt***************************/
        @JvmField
        val uint8FqName: FqName = fqName(UINT8)

        @JvmField
        val uint8UFqName: FqNameUnsafe = uint8FqName.toUnsafe()

        @JvmField
        val uint16FqName: FqName = fqName(UINT16)

        @JvmField
        val uint16UFqName: FqNameUnsafe = uint16FqName.toUnsafe()

        @JvmField
        val uint32FqName: FqName = fqName(UINT32)

        @JvmField
        val uint32UFqName: FqNameUnsafe = uint32FqName.toUnsafe()

        @JvmField
        val uint64FqName: FqName = fqName(UINT64)

        @JvmField
        val uint64UFqName: FqNameUnsafe = uint64FqName.toUnsafe()

        @JvmField
        val uint_nativeFqName: FqName = fqName(UINT_NATIVE)

        @JvmField
        val uint_nativeUFqName: FqNameUnsafe = uint_nativeFqName.toUnsafe()


        /***************************Bool***************************/
        @JvmField
        val boolFqName: FqName = fqName(BOOL)

        @JvmField
        val boolUFqName: FqNameUnsafe = boolFqName.toUnsafe()


        /***************************Float***************************/

        @JvmField
        val float16FqName: FqName = fqName(FLOAT16)

        @JvmField
        val float16UFqName: FqNameUnsafe = float16FqName.toUnsafe()

        @JvmField
        val float32FqName: FqName = fqName(FLOAT32)

        @JvmField
        val float32UFqName: FqNameUnsafe = float32FqName.toUnsafe()

        @JvmField
        val float64FqName: FqName = fqName(FLOAT64)

        @JvmField
        val float64UFqName: FqNameUnsafe = float64FqName.toUnsafe()


        /***************************内置类型***************************/
        @JvmField
        val cpointerFqName = core.child(CPOINTER)

        @JvmField
        val cpointerUFqName: FqNameUnsafe = cpointerFqName.toUnsafe()


        @JvmField
        val cstringFqName: FqName = core.child(CSTRING)

        @JvmField
        val cstringUFqName: FqNameUnsafe = cstringFqName.toUnsafe()

        @JvmField
        val ctypeFqName: FqName = core.child(CTYPE)

        @JvmField
        val ctypeUFqName: FqNameUnsafe = ctypeFqName.toUnsafe()


        @JvmField
        val uInt8ClassId: ClassId = ClassId.topLevel(uint8FqName)

        @JvmField
        val uInt16ClassId: ClassId = ClassId.topLevel(uint16FqName)

        @JvmField
        val uInt32ClassId: ClassId = ClassId.topLevel(uint32FqName)

        @JvmField
        val uInt64ClassId: ClassId = ClassId.topLevel(uint64FqName)


        @JvmField
        val extensionFunctionType: FqName = fqName("ExtensionFunctionType")

        @JvmField
        val contextFunctionTypeParams: FqName = fqName("ContextFunctionTypeParams")

        @JvmField
        val parameterName: FqName = fqName("ParameterName")


        @JvmField
        val annotation: FqName = fqName("Annotation")


//        @kotlin.jvm.JvmField
//        val cCallable: FqNameUnsafe = reflect("KCallable")


        private fun fqNameUnsafe(simpleName: String): FqNameUnsafe {
            return fqName(simpleName).toUnsafe()
        }

        private fun fqName(name: Name): FqName {
            return BUILT_INS_PACKAGE_FQ_NAME.child(name)
        }

        private fun fqName(simpleName: String): FqName {
            return fqName(Name.identifier(simpleName))
        }

        fun fromByName(name: Name): FqName =
            when (name) {
                NOTHING -> nothingFqName


                UINT8 -> uint8FqName
                UINT16 -> uint16FqName
                UINT32 -> uint32FqName
                UINT64 -> uint64FqName
                UINT_NATIVE -> uint_nativeFqName

                INT8 -> int8FqName
                INT16 -> int16FqName
                INT32 -> int32FqName
                INT64 -> int64FqName
                INT_NATIVE -> int_nativeFqName

                FLOAT16 -> float16FqName
                FLOAT32 -> float32FqName
                FLOAT64 -> float64FqName

                BOOL -> boolFqName

                RUNE -> runeFqName

                ARRAY -> arrayFqName
                UNIT -> unitFqName


              CPOINTER -> cpointerFqName
               CSTRING -> cstringFqName

                else -> throw IllegalArgumentException("Unknown name: $name")
            }

    }


    @JvmField
    val BASIC_TYPE_NAMES = setOf(
        UNIT
    )


    @JvmField
    val STDLIB_PACKAGE_FQ_NAMES = setOf(
        STD_PACKAGE_FQ_NAME,
        COMPRESS_PACKAGE_FQ_NAME,
        NET_PACKAGE_FQ_NAME,
        FUZZ_PACKAGE_FQ_NAME,
        ENCODING_PACKAGE_FQ_NAME,
        CRYPTO_PACKAGE_FQ_NAME,
        SERIALIZATION_PACKAGE_FQ_NAME
    )


    private fun namesToSetOf(): Set<FqName> {


        val set = mutableSetOf<FqName>()


        set.add(BUILT_INS_PACKAGE_FQ_NAME)
//        set.add(STD_CORE_PACKAGE_FQ_NAME)


//        STDLIB_PACKAGE_FQ_NAMES.map {
//            set.add(it)
//
//
//        }
        return set


    }

    @JvmField
    val ALL_NAMES = namesToSetOf()
}
