package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.CjTypeReference
import com.linqingying.cangjie.resolve.caches.type
import com.linqingying.cangjie.resolve.constants.*
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.deccriptorClass
import com.linqingying.cangjie.types.util.isEnum
import com.linqingying.cangjie.types.util.isStruct
import com.linqingying.cangjie.types.util.substitute
import com.linqingying.cangjie.resolve.constants.ConstantValue as CV

fun List<CjTypeReference>.types(): List<CangJieType> {
    return mapNotNull {
        it.type
    }

}

sealed class Constructor {

    fun arity(type: CangJieType): Int  {

        return when {
            type.isEnum() && this is Enum ->{
                entry.typeReferences.size
            }
            else -> 0
        }

    }

    fun subTypes(type: CangJieType): List<CangJieType> {

        return when {
//            this is Single && type.source is RsFieldsOwner -> {
//                type.item.fieldTypes.map { it.substitute(type.typeParameterValues) }
//            }
            this is Enum -> {
                entry.typeReferences.types().mapIndexed { index, it ->
                    it.substitute(type.arguments[index].type)
                }
            }

            else -> emptyList()
        }
        return emptyList()
    }

    open fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = false

    /** Enum variants */
    data class Enum(val entry: CjEnumEntry) : Constructor()

    /** The constructor of all patterns that don't vary by constructor, e.g. struct patterns and fixed-length arrays */
    object Single : Constructor() {
        override fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = true
    }

    /** Literal values */
    data class ConstantValue(val value: CV<*>) : Constructor() {
        override fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean =
            if (included) {
                value >= from && value <= to
            } else {
                value >= from && value < to
            }
    }

    companion object {

        private fun allConstructorsLazy(ty: CangJieType): Sequence<Constructor> =
            when {


                ty.isEnum() ->
                    (ty.deccriptorClass?.source?.getPsi() as? CjEnum)?.entry?.asSequence()?.map { Enum(it) }
                        ?: emptySequence()

                else -> sequenceOf(Single)
            }

        fun allConstructors(ty: CangJieType): List<Constructor> = allConstructorsLazy(ty).toList()

    }
}

private operator fun CV<*>.compareTo(other: CV<*>): Int {
    return when {
        this is BoolValue && other is BoolValue -> value.compareTo(other.value)
        this is Int64Value && other is Int64Value -> value.compareTo(other.value)
        this is Int32Value && other is Int32Value -> value.compareTo(other.value)
        this is Int16Value && other is Int16Value -> value.compareTo(other.value)
        this is Int8Value && other is Int8Value -> value.compareTo(other.value)
        this is UInt64Value && other is UInt64Value -> value.compareTo(other.value)
        this is UInt32Value && other is UInt32Value -> value.compareTo(other.value)
        this is UInt16Value && other is UInt16Value -> value.compareTo(other.value)
        this is UInt8Value && other is UInt8Value -> value.compareTo(other.value)

        this is Float64Value && other is Float64Value -> value.compareTo(other.value)

        this is Float32Value && other is Float32Value -> value.compareTo(other.value)
        this is Float16Value && other is Float16Value -> value.compareTo(other.value)


        this is StringValue && other is StringValue -> value.compareTo(other.value)
        this is RuneValue && other is RuneValue -> value.compareTo(other.value)
        else -> throw CheckMatchException("Comparison of incompatible types: $javaClass and ${other.javaClass}")
    }
}
