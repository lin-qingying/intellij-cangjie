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
    @JvmField
    val STD_PACKAGE_FQ_NAME = FqName("std")

    @JvmField
    val STD_CORE_PACKAGE_FQ_NAME = STD_PACKAGE_FQ_NAME.child(Name.identifier("core"))

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
        val any: FqNameUnsafe = fqNameUnsafe("Any")

        @JvmField
        val nothing: FqNameUnsafe = fqNameUnsafe("Nothing")

        @JvmField
        val cloneable: FqNameUnsafe = fqNameUnsafe("Cloneable")

        @JvmField
        val suppress: FqName = fqName("Suppress")

        @JvmField
        val unit: FqNameUnsafe = fqNameUnsafe("Unit")

        @JvmField
        val charSequence: FqNameUnsafe = fqNameUnsafe("CharSequence")

        @JvmField
        val string: FqNameUnsafe = fqNameUnsafe("String")

        @JvmField
        val array: FqNameUnsafe = fqNameUnsafe("Array")

        @JvmField
        val _boolean: FqNameUnsafe = fqNameUnsafe("Boolean")

        @JvmField
        val _char: FqNameUnsafe = fqNameUnsafe("Char")

        @JvmField
        val _int8: FqNameUnsafe = fqNameUnsafe("Int8")

        @JvmField
        val _int16: FqNameUnsafe = fqNameUnsafe("Int16")

        @JvmField
        val _int32: FqNameUnsafe = fqNameUnsafe("Int32")

        @JvmField
        val _int64: FqNameUnsafe = fqNameUnsafe("Int64")

        @JvmField
        val _float: FqNameUnsafe = fqNameUnsafe("Float")

        @JvmField
        val _double: FqNameUnsafe = fqNameUnsafe("Double")

        @JvmField
        val number: FqNameUnsafe = fqNameUnsafe("Number")

        @JvmField
        val _enum: FqNameUnsafe = fqNameUnsafe("Enum")

        @JvmField
        val functionSupertype: FqNameUnsafe = fqNameUnsafe("Function")

        @JvmField
        val throwable: FqName = fqName("Throwable")

        @JvmField
        val comparable: FqName = fqName("Comparable")

//        @JvmField
//        val intRange: FqNameUnsafe = rangesFqName("IntRange")
//        @JvmField
//        val longRange: FqNameUnsafe = rangesFqName("LongRange")

        @JvmField
        val deprecated: FqName = fqName("Deprecated")

        @JvmField
        val deprecatedSinceCangJie: FqName = fqName("DeprecatedSinceCangJie")

        @JvmField
        val deprecationLevel: FqName = fqName("DeprecationLevel")

        @JvmField
        val replaceWith: FqName = fqName("ReplaceWith")

        @JvmField
        val extensionFunctionType: FqName = fqName("ExtensionFunctionType")

        @JvmField
        val contextFunctionTypeParams: FqName = fqName("ContextFunctionTypeParams")

        @JvmField
        val parameterName: FqName = fqName("ParameterName")

        @JvmField
        val parameterNameClassId: ClassId = ClassId.topLevel(parameterName)

        @JvmField
        val annotation: FqName = fqName("Annotation")


        @JvmField
        val unsafeVariance: FqName = fqName("UnsafeVariance")

        @JvmField
        val publishedApi: FqName = fqName("PublishedApi")

        @JvmField
        val platformDependent: FqName = FqName("kotlin.internal.PlatformDependent")

        @JvmField
        val platformDependentClassId: ClassId = ClassId.topLevel(platformDependent)

//        @JvmField
//        val iterator: FqName = collectionsFqName("Iterator")
//        @JvmField
//        val iterable: FqName = collectionsFqName("Iterable")
//        @JvmField
//        val collection: FqName = collectionsFqName("Collection")
//        @JvmField
//        val list: FqName = collectionsFqName("List")
//        @JvmField
//        val listIterator: FqName = collectionsFqName("ListIterator")
//        @JvmField
//        val set: FqName = collectionsFqName("Set")
//        @JvmField
//        val map: FqName = collectionsFqName("Map")
//        @JvmField
//        val mapEntry: FqName = map.child(Name.identifier("Entry"))
//        @JvmField
        /*        val mutableIterator: FqName = collectionsFqName("MutableIterator")
                @JvmField
                val mutableIterable: FqName = collectionsFqName("MutableIterable")
                @JvmField
                val mutableCollection: FqName = collectionsFqName("MutableCollection")
                @JvmField
                val mutableList: FqName = collectionsFqName("MutableList")
                @JvmField
                val mutableListIterator: FqName = collectionsFqName("MutableListIterator")
                @JvmField
                val mutableSet: FqName = collectionsFqName("MutableSet")
                @JvmField
                val mutableMap: FqName = collectionsFqName("MutableMap")*/
//        @JvmField
//        val mutableMapEntry: FqName = mutableMap.child(Name.identifier("MutableEntry"))

//        @JvmField val kClass: FqNameUnsafe = reflect("KClass")
//        @JvmField val kType: FqNameUnsafe = reflect("KType")
//        @JvmField val kCallable: FqNameUnsafe = reflect("KCallable")
//        @JvmField val kProperty0: FqNameUnsafe = reflect("KProperty0")
//        @JvmField val kProperty1: FqNameUnsafe = reflect("KProperty1")
//        @JvmField val kProperty2: FqNameUnsafe = reflect("KProperty2")
//        @JvmField val kMutableProperty0: FqNameUnsafe = reflect("KMutableProperty0")
//        @JvmField val kMutableProperty1: FqNameUnsafe = reflect("KMutableProperty1")
//        @JvmField val kMutableProperty2: FqNameUnsafe = reflect("KMutableProperty2")
//        @JvmField val kPropertyFqName: FqNameUnsafe = reflect("KProperty")
//        @JvmField val kMutablePropertyFqName: FqNameUnsafe = reflect("KMutableProperty")
//        @JvmField val kProperty: ClassId = ClassId.topLevel(kPropertyFqName.toSafe())
//        @JvmField val kDeclarationContainer: FqNameUnsafe = reflect("KDeclarationContainer")
//        @JvmField val findAssociatedObject: FqNameUnsafe = reflect("findAssociatedObject")

        @JvmField
        val uInt8FqName: FqName = fqName("UInt8")

        @JvmField
        val uInt16FqName: FqName = fqName("UInt16")

        @JvmField
        val uInt32FqName: FqName = fqName("UInt32")

        @JvmField
        val uInt64FqName: FqName = fqName("UInt64")

        @JvmField
        val uInt8: ClassId = ClassId.topLevel(uInt8FqName)

        @JvmField
        val uInt16: ClassId = ClassId.topLevel(uInt16FqName)

        @JvmField
        val uInt32: ClassId = ClassId.topLevel(uInt32FqName)

        @JvmField
        val uInt64: ClassId = ClassId.topLevel(uInt64FqName)

        @JvmField
        val uByteArrayFqName: FqName = fqName("UByteArray")

        @JvmField
        val uShortArrayFqName: FqName = fqName("UShortArray")

        @JvmField
        val uIntArrayFqName: FqName = fqName("UIntArray")

        @JvmField
        val uLongArrayFqName: FqName = fqName("ULongArray")

//        @JvmField val primitiveTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.values().size).apply {
//            PrimitiveType.values().mapTo(this) { it.typeName }
//        }
//
//        @JvmField val primitiveArrayTypeShortNames: Set<Name> = newHashSetWithExpectedSize<Name>(PrimitiveType.values().size).apply {
//            PrimitiveType.values().mapTo(this) { it.arrayTypeName }
//        }
//
//        @JvmField val fqNameToPrimitiveType: Map<FqNameUnsafe, PrimitiveType> =
//            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.values().size).apply {
//                for (primitiveType in PrimitiveType.values()) {
//                    this[fqNameUnsafe(primitiveType.typeName.asString())] = primitiveType
//                }
//            }
//
//        @JvmField val arrayClassFqNameToPrimitiveType: MutableMap<FqNameUnsafe, PrimitiveType> =
//            newHashMapWithExpectedSize<FqNameUnsafe, PrimitiveType>(PrimitiveType.values().size).apply {
//                for (primitiveType in PrimitiveType.values()) {
//                    this[fqNameUnsafe(primitiveType.arrayTypeName.asString())] = primitiveType
//                }
//            }


        private fun fqNameUnsafe(simpleName: String): FqNameUnsafe {
            return fqName(simpleName).toUnsafe()
        }

        private fun fqName(simpleName: String): FqName {
            return BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
        }

//        private fun collectionsFqName(simpleName: String): FqName {
//            return COLLECTIONS_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
//        }
//
//        private fun rangesFqName(simpleName: String): FqNameUnsafe {
//            return RANGES_PACKAGE_FQ_NAME.child(Name.identifier(simpleName)).toUnsafe()
//        }

//        @JvmStatic
//        fun reflect(simpleName: String): FqNameUnsafe {
//            return CANGJIE_REFLECT_FQ_NAME.child(Name.identifier(simpleName)).toUnsafe()
//        }

//        private fun annotationName(simpleName: String): FqName {
//            return ANNOTATION_PACKAGE_FQ_NAME.child(Name.identifier(simpleName))
//        }
//
//        private fun internalName(simpleName: String): FqName {
//            return CANGJIE_INTERNAL_FQ_NAME.child(Name.identifier(simpleName))
//        }
    }

//    @JvmField
//    val CANGJIE_INTERNAL_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("internal"))
//
//    @JvmField
//    val RANGES_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("ranges"))
//
//    @JvmField
//    val ANNOTATION_PACKAGE_FQ_NAME = BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier("annotation"))

    @JvmField
    val BASIC_TYPES = setOf(
        unit
    )


    @JvmField
    val BUILT_INS_PACKAGE_FQ_NAMES = setOf(
        BUILT_INS_PACKAGE_FQ_NAME,
//        STD_PACKAGE_FQ_NAME
    )
}
