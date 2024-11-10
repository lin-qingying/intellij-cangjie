package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.builtins.isBuiltinTupleType
import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.CjTypeReference
import com.linqingying.cangjie.resolve.caches.type
import com.linqingying.cangjie.resolve.constants.*
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.deccriptorClass
import com.linqingying.cangjie.types.util.isBoolean
import com.linqingying.cangjie.types.util.isEnum
import com.linqingying.cangjie.types.util.isUnit
import com.linqingying.cangjie.resolve.constants.ConstantValue as CV
import  com.linqingying.cangjie.types.util.substitute as Usubstitute

fun List<CjTypeReference>.types(): List<CangJieType> {
    return mapNotNull {
        it.type
    }

}

//this 是 类型参数中的类型 type的上层类型
private fun CangJieType.substitute(type: CangJieType): CangJieType {
    val thisName = this.constructor.declarationDescriptor?.name ?: return this

//查找泛型参数的index
    var index: Int? = null
    type.deccriptorClass?.declaredTypeParameters?.forEachIndexed { index1, typeParameterDescriptor ->
        if (typeParameterDescriptor.name == thisName) {
            index = index1
        }
    }
    if (index != null) {
        return this.Usubstitute(type.arguments[index!!].type)
    }
    return this
}

sealed class Constructor {

    fun arity(type: CangJieType): Int {

        return when {
            type.isEnum() && this is Enum -> {
                entry.typeReferences.size
            }
            type.isBuiltinTupleType && this is Single ->{
                type.arguments.size
            }

            else -> 0
        }

    }

    fun subTypes(type: CangJieType): List<CangJieType> {

        return when {

            this is Enum -> {
                entry.typeReferences.types().map {
                    it.substitute(type)
                }
            }

            this is Single -> {
                if (type.isBuiltinTupleType) {
                    type.arguments.map { it.type }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }

    }

    open fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = false

    /** Enum variants */
    data class Enum(val entry: CjEnumEntry) : Constructor()
    data class Type(val type: CangJieType) : Constructor()

    /** 不因构造函数而变化的所有模式的构造函数，例如结构模式和固定长度数组 */
    data object Single : Constructor() {
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
                ty.isBoolean() -> sequenceOf(true, false).map { ConstantValue(BoolValue(it)) }
                ty.isUnit() -> sequenceOf(true, false).map { ConstantValue(UnitValue) }
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
        this is UnitValue && other is UnitValue -> 0
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
