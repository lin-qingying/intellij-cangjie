/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.parsing

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.resolve.constants.Float32
import com.linqingying.cangjie.resolve.constants.Float64

import com.intellij.openapi.util.text.CharFilter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType


private val FP_LITERAL_PARTS = "([_\\d]*)\\.?([_\\d]*)e?[+-]?([_\\d]*)[f]?".toRegex()
private val UNDERSCORES_FILTER = CharFilter { ch: Char -> ch != '_' }

fun hasIllegalUnderscore(text: String, elementType: IElementType): Boolean {
    val parts: List<String?> = if (elementType === CjNodeTypes.INTEGER_CONSTANT) {
        var start = 0
        var end: Int = text.length
        if (text.startsWith("0x", ignoreCase = true) || text.startsWith("0b", ignoreCase = true)) start += 2
        if (text.endsWith('l', ignoreCase = true)) --end
        listOf(text.substring(start, end))
    } else {
        FP_LITERAL_PARTS.findAll(text).flatMap { it.groupValues }.toList()
    }

    return parts.any { it != null && (it.startsWith("_") || it.endsWith("_")) }
}

fun hasInt64Suffix(text: String) = text.endsWith("i64") /*|| text.endsWith('L')*/
fun hasIntegerSuffix(text: String) =
    text.endsWith("i8") || text.endsWith("i16") || text.endsWith("i32") || text.endsWith("i64")
            || hasUnsignedSuffix(text)

fun hasFloatSuffix(text: String) = text.endsWith("f32") || text.endsWith("f64") || text.endsWith("f16")

//是否是无符号数
fun hasUnsignedSuffix(text: String) = text.endsWith("u8") || text.endsWith("u16") || text.endsWith("u32")
        || text.endsWith("u64")

fun hasUnsignedInt64Suffix(text: String) =
    text.endsWith("u64") /*|| text.endsWith("uL") ||
            text.endsWith("Ul") || text.endsWith("UL")*/

//将文本转为数值类型
fun parseNumericLiteral(text: String, type: IElementType): Number? {
    val canonicalText = removeUnderscores(text)
    return when (type) {
        CjNodeTypes.INTEGER_CONSTANT -> parseIntegerLiteral(canonicalText)
        CjNodeTypes.FLOAT_CONSTANT -> parseFloatingLiteral(canonicalText)
        else -> null
    }
}

private fun parseIntegerLiteral(text: String): Number? {
    if(text.endsWith("i8") || text.endsWith("u8")){
        return parseInt8(text)
    }else   if(text.endsWith("i16") || text.endsWith("u16")){
        return parseInt16(text)
    }else   if(text.endsWith("i32") || text.endsWith("u32")){
        return parseInt32(text)
    }
    return parseInt64(text)
}

private fun parseInt32(text: String): Int? {


    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
            text.endsWith("u32")-> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(3)
            }

            text.endsWith("i32")-> {

                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(3)
            }

            else -> {
                isUnsigned = false
                numberWithoutSuffix = text
            }
        }

        val (number, radix) = extractRadix(numberWithoutSuffix)

        if (isUnsigned) {


            java.lang.Integer.toUnsignedLong(number.toInt() ).toInt()
        } else {
            java.lang.Integer.parseInt(number, radix)
        }
    } catch (e: NumberFormatException) {
        null
    }
}
fun String.removeSuffix(i: Int): String = this.substring(0, this.length - i)

private fun parseInt16(text: String): Short? {


    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
            text.endsWith("u16")-> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(3)
            }

            text.endsWith("i16")-> {

                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(3)
            }

            else -> {
                isUnsigned = false
                numberWithoutSuffix = text
            }
        }

        val (number, radix) = extractRadix(numberWithoutSuffix)

        if (isUnsigned) {


            java.lang.Short.toUnsignedInt(number.toShort()).toShort()
        } else {
            java.lang.Short.parseShort(number, radix)
        }
    } catch (e: NumberFormatException) {
        null
    }
}
private fun parseInt8(text: String): Byte? {


    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
           text.endsWith("u8")-> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(2)
            }

            text.endsWith("i8")-> {

                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(2)
            }

            else -> {
                isUnsigned = false
                numberWithoutSuffix = text
            }
        }

        val (number, radix) = extractRadix(numberWithoutSuffix)

        if (isUnsigned) {


            java.lang.Byte.toUnsignedInt(number.toByte()).toByte()
        } else {
            java.lang.Byte.parseByte(number, radix)
        }
    } catch (e: NumberFormatException) {
        null
    }
}
private fun parseInt64(text: String): Long? {


    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
//            hasUnsignedInt64Suffix(text) -> {
//                isUnsigned = true
//                numberWithoutSuffix = text.removeSuffix(3)
//            }

            hasUnsignedSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(if (text.endsWith("u8")) 2 else 3)
            }

            text.endsWith("i64") -> {
                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(3)
            }

            text.endsWith("i8") -> {
                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(2)
            }

            text.endsWith("i32") -> {
                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(3)
            }

            text.endsWith("i16") -> {
                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(3)
            }

            else -> {
                isUnsigned = false
                numberWithoutSuffix = text
            }
        }

        val (number, radix) = extractRadix(numberWithoutSuffix)

        if (isUnsigned) {
            java.lang.Long.parseUnsignedLong(number, radix)
        } else {
            try {
                java.lang.Long.parseLong(number, radix)
            } catch (e: NumberFormatException) {
                java.lang.Long.parseUnsignedLong(number, radix)
            }
        }
    } catch (e: NumberFormatException) {
//        重新处理为无符号数


        null
    }
}
data class Float16(val raw: Short) : Number(), Comparable<Float16> {
    // 判断 Float16 是否表示无穷大
    fun isInfinite(): Boolean {
        val exponent = (raw.toInt() shr 10) and 0x1F  // 指数位
        val mantissa = raw.toInt() and 0x3FF  // 尾数位

        // 如果指数是全1（11111），且尾数为0，则表示无穷大（正无穷大或负无穷大）
        return exponent == 31 && mantissa == 0
    }

    // 转换为标准的 Float 类型
    override fun toFloat(): Float {
        val sign = (raw.toInt() shr 15) and 0x1  // 符号位
        val exponent = (raw.toInt() shr 10) and 0x1F  // 指数位
        val mantissa = raw.toInt() and 0x3FF  // 尾数位

        return if (exponent == 0) {
            if (mantissa == 0) {
                0f
            } else {
                val result = mantissa.toFloat() * Math.pow(2.0, -24.0).toFloat()
                if (sign == 1) -result else result
            }
        } else if (exponent == 31) {
            if (mantissa == 0) {
                if (sign == 1) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
            } else {
                Float.NaN
            }
        } else {
            val value = (1.0f + mantissa.toFloat() / 1024.0f) * Math.pow(2.0, (exponent - 15).toDouble()).toFloat()
            if (sign == 1) -value else value
        }
    }

    // 转换为 Double 类型
    override fun toDouble(): Double = toFloat().toDouble()

    // 转换为 Int 类型
    override fun toInt(): Int = toFloat().toInt()

    // 转换为 Long 类型
    override fun toLong(): Long = toFloat().toLong()

    // 转换为 Short 类型
    override fun toShort(): Short = toFloat().toInt().toShort()

    // 转换为 Byte 类型
    override fun toByte(): Byte = toFloat().toInt().toByte()

    // 比较两个 Float16 的值
    override fun compareTo(other: Float16): Int = toFloat().compareTo(other.toFloat())

    // 重写 equals 方法
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Float16 && other !is Float && other !is Double) return false
        return when (other) {
            is Float16 -> this.toFloat() == other.toFloat()
            is Float -> this.toFloat() == other
            is Double -> this.toDouble() == other
            else -> false
        }
    }


    // 重写 hashCode 方法
    override fun hashCode(): Int = toFloat().hashCode()

    // 提供更友好的字符串表示
    override fun toString(): String = toFloat().toString()

    companion object {
        // 将 Float 转换为 Float16 类型
        fun fromFloat(value: Float): Float16 {
            if (value.isNaN()) return Float16(0x7e00.toShort())
            if (value == Float.POSITIVE_INFINITY) return Float16(0x7c00.toShort())
            if (value == Float.NEGATIVE_INFINITY) return Float16(0xfc00.toShort())
            if (value == 0f) return Float16(0.toShort())

            val sign = if (value < 0) 1 else 0
            val absValue = Math.abs(value)
            val exponent = (Math.log(absValue.toDouble()) / Math.log(2.0)).toInt()
            var mantissa = (absValue / Math.pow(2.0, exponent.toDouble()) - 1.0) * 1024

            val e = (exponent + 15)
            mantissa = mantissa.coerceIn(0.0, 1023.0)
            val m = mantissa.toInt()

            val raw = (sign shl 15) or ((e and 0x1F) shl 10) or (m and 0x3FF)
            return Float16(raw.toShort())
        }

        // 将 Double 转换为 Float16 类型
        fun fromDouble(value: Double): Float16 = fromFloat(value.toFloat())
    }
}


// Float16 类定义，继承自 Number
//半精度浮点数使用 16 位表示，通常包括 1 位符号位、5 位指数位和 10 位尾数位
//class Float16(private val value: Float) : Number() , Comparable<Float16>{
//    override fun toByte(): Byte {
//        return value.toInt().toByte();
//    }
//
//    override fun toDouble(): Double {
//       return value.toDouble()
//    }
//
//    override fun toFloat(): Float {
//        return value
//    }
//
//    override fun toInt(): Int {
//return value.toInt()
//    }
//
//    override fun toLong(): Long {
//      return value.toLong()
//    }
//
//    override fun toShort(): Short {
//      return value.toInt().toShort()
//    }
//
//    override fun compareTo(other: Float16): Int {
//       return value.compareTo(other.value)
//    }
//
//
//}

private fun parseFloatingLiteral(text: String): Number? {
    if (text.endsWith("f16")) {
//        return parseFloat16(text)
        return parseFloat(text.removeSuffix("f16"))?.let{
            Float16.fromFloat(it)
        }

    }
    if (text.endsWith("f32")) {
        return parseFloat(text.removeSuffix("f32"))
    }
    if (text.endsWith("f64")) {
        return parseDouble(text.removeSuffix("f64"))
    }
    return parseDouble(text)
}

//Float64 处理为Double
private fun parseDouble(text: String): Float64? {
    return try {
        java.lang.Double.parseDouble(text)
    } catch (e: NumberFormatException) {
        null
    }
}

//Float32 处理为Float
private fun parseFloat(text: String): Float32? {
    return try {
        java.lang.Float.parseFloat(text)
    } catch (e: NumberFormatException) {
        null
    }
}

fun Float.isFloat16(): Boolean {
    return this in -65504.0f..65504.0f
}
//private fun parseFloat16(text: String): Float16? {
//    return try {
//        // 移除 "f16" 后缀并解析为 Float
//        val floatValue = text.removeSuffix("f16").toFloat()
//        Float16(floatValue) // 创建 Float16 实例
//    } catch (e: NumberFormatException) {
//        null
//    }
//}

fun parseBoolean(text: String): Boolean {
    if ("true" == text) {
        return true
    } else if ("false" == text) {
        return false
    }

    throw IllegalStateException("Must not happen. A boolean literal has text: " + text)
}

fun removeUnderscores(text: String): String {
    return StringUtil.strip(text, UNDERSCORES_FILTER)
}
