package com.huawei.cangjie.builtins

import com.huawei.cangjie.builtins.StandardNames.FqNames.unit
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

    @JvmStatic
    fun getFunctionName(parameterCount: Int): String {
        return "Function$parameterCount"
    }


    val STD_PACKAGE_NAME = Name.identifier("std")
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
    val OBJECT = STD_CORE_PACKAGE_FQ_NAME.child(Name.identifier("Object"))

    @JvmField
    val ANY = STD_CORE_PACKAGE_FQ_NAME.child(Name.identifier("Any"))
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
        val primitiveTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.entries.size).apply {
            PrimitiveType.entries.mapTo(this) { it.typeName }
        }


        @JvmField
        val core: FqName = FqName.topLevel(Name.identifier("std")).child(Name.identifier("core"))

        @JvmField
        val any: FqNameUnsafe = fqNameUnsafe("Any")


        @JvmField
        val string: FqNameUnsafe = fqNameUnsafe("String")

        @JvmField
        val array: FqNameUnsafe = fqNameUnsafe("Array")

        @JvmField
        val arrayFqName: FqName = fqName("Array")


        /***************************Nothing***************************/

        @JvmField
        val nothing: FqNameUnsafe = fqNameUnsafe("Nothing")

        @JvmField
        val nothingFqName: FqName = fqName("Nothing")


        /***************************Char***************************/


        @JvmField
        val rune: FqNameUnsafe = fqNameUnsafe("Rune")

        @JvmField
        val runeFqName: FqName = fqName("Rune")


        /***************************Unit***************************/

        @JvmField
        val unit = fqNameUnsafe("Unit")

        @JvmField
        val unitFqName = fqName("Unit")

        /***************************Int***************************/
        @JvmField
        val int8: FqNameUnsafe = fqNameUnsafe("Int8")

        @JvmField
        val int16: FqNameUnsafe = fqNameUnsafe("Int16")

        @JvmField
        val int32: FqNameUnsafe = fqNameUnsafe("Int32")

        @JvmField
        val int64: FqNameUnsafe = fqNameUnsafe("Int64")

        @JvmField
        val int_native: FqNameUnsafe = fqNameUnsafe("IntNative")

        /***************************UInt***************************/

        @JvmField
        val uint8: FqNameUnsafe = fqNameUnsafe("UInt8")

        @JvmField
        val uint16: FqNameUnsafe = fqNameUnsafe("UInt16")

        @JvmField
        val uint32: FqNameUnsafe = fqNameUnsafe("UInt32")

        @JvmField
        val uint64: FqNameUnsafe = fqNameUnsafe("UInt64")

        @JvmField
        val uint_native: FqNameUnsafe = fqNameUnsafe("UIntNative")

        @JvmField
        val int_nativeFqName: FqName = fqName("IntNative")

        @JvmField
        val uint_nativeFqName: FqName = fqName("UIntNative")

        @JvmField
        val int8FqName: FqName = fqName("Int8")

        @JvmField
        val int16FqName: FqName = fqName("Int16")

        @JvmField
        val int32FqName: FqName = fqName("Int32")

        @JvmField
        val int64FqName: FqName = fqName("Int64")

        @JvmField
        val uInt8FqName: FqName = fqName("UInt8")

        @JvmField
        val uInt16FqName: FqName = fqName("UInt16")

        @JvmField
        val uInt32FqName: FqName = fqName("UInt32")

        @JvmField
        val uInt64FqName: FqName = fqName("UInt64")

        @JvmField
        val uInt8ClassId: ClassId = ClassId.topLevel(uInt8FqName)

        @JvmField
        val uInt16ClassId: ClassId = ClassId.topLevel(uInt16FqName)

        @JvmField
        val uInt32ClassId: ClassId = ClassId.topLevel(uInt32FqName)

        @JvmField
        val uInt64ClassId: ClassId = ClassId.topLevel(uInt64FqName)


        /***************************Float***************************/
        @JvmField
        val float16: FqNameUnsafe = fqNameUnsafe("Float16")

        @JvmField
        val float32: FqNameUnsafe = fqNameUnsafe("Float32")

        @JvmField
        val float64: FqNameUnsafe = fqNameUnsafe("Float64")


        @JvmField
        val float16FqName: FqName = fqName("Float16")

        @JvmField
        val float32FqName: FqName = fqName("Float32")

        @JvmField
        val float64FqName: FqName = fqName("Float64")

        /***************************Bool***************************/
        @JvmField
        val bool: FqNameUnsafe = fqNameUnsafe("Bool")

        @JvmField

        val boolFqName: FqName = fqName("Bool")

        @JvmField
        val enum: FqNameUnsafe = fqNameUnsafe("Enum")


        @JvmField
        val throwable: FqName = fqName("Throwable")


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

        private fun fqName(simpleName: String): FqName {
            return BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
        }

        fun fromByName(name: Name): FqName =
            when (name) {
                uInt8FqName.shortName() -> uInt8FqName
                uInt16FqName.shortName() -> uInt16FqName
                uInt32FqName.shortName() -> uInt32FqName
                uInt64FqName.shortName() -> uInt64FqName

                int8FqName.shortName() -> int8FqName
                int16FqName.shortName() -> int16FqName
                int32FqName.shortName() -> int32FqName
                int64FqName.shortName() -> int64FqName

                float16FqName.shortName() -> float16FqName
                float32FqName.shortName() -> float32FqName
                float64FqName.shortName() -> float64FqName

                boolFqName.shortName() -> boolFqName

                runeFqName.shortName() -> runeFqName

                arrayFqName.shortName() -> arrayFqName
                unitFqName.shortName() -> unitFqName
                else -> throw IllegalArgumentException("Unknown name: $name")
            }

    }


    @JvmField
    val BASIC_TYPE_NAMES = setOf(
        unit
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
