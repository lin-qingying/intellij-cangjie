/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.constants

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.types.CangJieType

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
fun Number.toFloat16():Float16{
return Float16.fromFloat( toFloat())
}
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


            is Float16 -> Float16Value(value)

            is Float32 -> Float32Value(value)
            is Float64 -> Float64Value(value)
            is Bool -> BoolValue(value)
            is String -> StringValue(value)

            is Unit -> UnitValue

            else -> null
        }
    }

    fun createFloatConstantValue(
        value: Double,
        expectedType: CangJieType,

        ): ConstantValue<*>? {
        fun  toFloat16(): Float16 {
            return Float16.fromFloat(value.toFloat())
        }

        return when {
            CangJieBuiltIns.isFloat64(expectedType) -> Float64Value(value)
            CangJieBuiltIns.isFloat32(expectedType) && value == value.toFloat()
                .toDouble() -> Float32Value(value.toFloat())

            CangJieBuiltIns.isFloat16(expectedType) /*&& value == toFloat16().toDouble()*/
                -> Float16Value(toFloat16())


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
                    .fromUInt8ToLong() -> UInt8Value(value.toByte())

                CangJieBuiltIns.isUInt16(expectedType) && value == value.toShort().fromUInt16ToLong() -> UInt16Value(
                    value.toShort()
                )

                CangJieBuiltIns.isUInt32(expectedType) && value == value.toInt()
                    .fromUInt32ToLong() -> UInt32Value(value.toInt())

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


fun Byte.fromUInt8ToLong(): Long = this.toLong() and 0xFF
fun Short.fromUInt16ToLong(): Long = this.toLong() and 0xFFFF
fun Int.fromUInt32ToLong(): Long = this.toLong() and 0xFFFF_FFFF
