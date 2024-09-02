package com.huawei.cangjie.resolve.constants

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.types.CangJieType

typealias Float64 = Double
typealias Float32 = Float
typealias Int64 = Long
typealias Int32 = Int
typealias Int16 = Short
typealias Int8 = Byte
typealias UInt64 = ULong
typealias UInt32 = UInt
typealias UInt16 = UShort
typealias UInt8 = UByte
typealias Rune = Char

typealias Bool = Boolean

object ConstantValueFactory {

    fun createConstantValue(value: Any?, module: ModuleDescriptor? = null): ConstantValue<*>? {
        return when (value) {
            is Int8 -> Int8Value(value)
            is Int16 -> Int16Value(value)
            is Int32 -> Int32Value(value)
            is Int64 -> Int64Value(value)


//            is UInt8 -> UInt8Value(value)
//            is UInt16 -> UInt16Value(value)
//            is UInt32 -> UInt32Value(value)
//            is UInt64 ->UInt64Value(value)
            is Rune -> RuneValue(value)


//            FLoat16
            is Float32 -> Float32Value(value)
            is Float64 -> Float64Value(value)
            is Bool -> BoolValue(value)
            is String -> StringValue(value)

            is Unit -> UnitValue
//            is ByteArray -> createArrayValue(value.toList(), module, PrimitiveType.BYTE)
//            is ShortArray -> createArrayValue(value.toList(), module, PrimitiveType.SHORT)
//            is IntArray -> createArrayValue(value.toList(), module, PrimitiveType.INT)
//            is LongArray -> createArrayValue(value.toList(), module, PrimitiveType.LONG)
//            is CharArray -> createArrayValue(value.toList(), module, PrimitiveType.CHAR)
//            is FloatArray -> createArrayValue(value.toList(), module, PrimitiveType.FLOAT)
//            is DoubleArray -> createArrayValue(value.toList(), module, PrimitiveType.DOUBLE)
//            is BooleanArray -> createArrayValue(value.toList(), module, PrimitiveType.BOOLEAN)
//            null -> NullValue()
            else -> null
        }
    }

    fun createIntegerConstantValue(
        value: Long,
        expectedType: CangJieType,
        isUnsigned: Boolean
    ): ConstantValue<*>? {

        return if (isUnsigned) {
            when {
                CangJieBuiltIns.isUInt8(expectedType) && value == value.toByte()
                    .fromUByteToLong() -> UInt8Value(value.toByte())

                CangJieBuiltIns.isUInt16(expectedType) && value == value.toShort().fromUShortToLong() -> UInt16Value(
                    value.toShort()
                )

                CangJieBuiltIns.isUInt32(expectedType) && value == value.toInt()
                    .fromUIntToLong() -> UInt32Value(value.toInt())

                CangJieBuiltIns.isUInt64(expectedType) -> UInt64Value(value)
                else -> null
            }
        } else {
            when {
                CangJieBuiltIns.isInt64(expectedType) -> Int64Value(value)
                CangJieBuiltIns.isInt32(expectedType) && value == value.toInt().toLong() -> Int32Value(value.toInt())
                CangJieBuiltIns.isInt16(expectedType) && value == value.toShort()
                    .toLong() -> Int16Value(value.toShort())

                CangJieBuiltIns.isInt8(expectedType) && value == value.toByte().toLong() -> Int8Value(value.toByte())
                CangJieBuiltIns.isRune(expectedType) -> Int32Value(value.toInt())
                else -> null
            }
        }
    }
}

fun Byte.fromUByteToLong(): Long = this.toLong() and 0xFF
fun Short.fromUShortToLong(): Long = this.toLong() and 0xFFFF
fun Int.fromUIntToLong(): Long = this.toLong() and 0xFFFF_FFFF
