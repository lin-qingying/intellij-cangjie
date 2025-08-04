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

package cn.cangnova.cangjie.resolve.constants

import cn.cangnova.cangjie.parsing.extractRadix
import cn.cangnova.cangjie.psi.CjNodeTypes
import com.intellij.openapi.util.text.CharFilter
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType
import cn.cangnova.cangjie.resolve.constants.Float32
import cn.cangnova.cangjie.resolve.constants.Float64
import java.util.*


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
    if (text.endsWith("i8") || text.endsWith("u8")) {
        return parseInt8(text)
    } else if (text.endsWith("i16") || text.endsWith("u16")) {
        return parseInt16(text)
    } else if (text.endsWith("i32") || text.endsWith("u32")) {
        return parseInt32(text)
    }
    return parseInt64(text)
}

private fun parseInt32(text: String): Int? {


    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
            text.endsWith("u32") -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(3)
            }

            text.endsWith("i32") -> {

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


            Integer.toUnsignedLong(number.toInt()).toInt()
        } else {
            Integer.parseInt(number, radix)
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
            text.endsWith("u16") -> {
                isUnsigned = true
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
            text.endsWith("u8") -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(2)
            }

            text.endsWith("i8") -> {

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


//data class Float16(private val bitSet: BitSet) : Number(), Comparable<Float16> {
//
//    constructor(value: Short) : this(shortToBitSet(value)) // 支持从 Short 构造
//    constructor(value: Int) : this(intToBitSet(value)) // 支持从 Short 构造
//
//    override fun toShort(): Short {
//        return bitSet.toShort() // 将内部 BitSet 转换为 Short
//    }
//
//    // 判断 Float16 是否表示无穷大
//    fun isInfinite(): Boolean {
//        // 提取指数部分（第 10 到 14 位）
//        var exponent = 0
//        for (i in 10..14) {
//            if (bitSet[i]) {
//                exponent = exponent or (1 shl (i - 10))
//            }
//        }
//
//        // 检查指数是否全为 1
//        if (exponent != 31) return false
//
//        // 检查尾数部分是否全为 0
//        for (i in 0..9) {
//            if (bitSet[i]) return false
//        }
//
//        return true
//    }
//
//    // 转换为标准的 Float 类型
//    override fun toFloat(): Float {
//        val sign = if (bitSet.get(15)) 1 else 0
//        val exponent = (0 until 5).fold(0) { exp, i -> exp or (if (bitSet.get(10 + i)) 1 shl (4 - i) else 0) }
//        val fraction = (0 until 10).fold(0) { frac, i -> frac or (if (bitSet.get(i)) 1 shl (9 - i) else 0) }
//
//        // 判断是否是 Infinity 或 NaN
//        if (exponent == 0x1F) { // Infinity or NaN
//            return if (fraction == 0) {
//                if (sign == 0) Float.POSITIVE_INFINITY else Float.NEGATIVE_INFINITY
//            } else {
//                Float.NaN
//            }
//        }
//
//        val newExp: Int
//        val newFrac: Int
//
//        if (exponent == 0) { // Subnormal numbers
//            newExp = 0
//            newFrac = fraction
//        } else {
//            newExp = (exponent + EXPONENT_BIAS) and 0xFF
//            newFrac = fraction shl 13
//        }
//
//        val newBits = (sign shl 31) or (newExp shl 23) or newFrac
//        return java.lang.Float.intBitsToFloat(newBits)
//    }
//
//    // 转换为 Double 类型
//    override fun toDouble(): Double = toFloat().toDouble()
//
//    // 转换为 Int 类型
//    override fun toInt(): Int = toFloat().toInt()
//
//    // 转换为 Long 类型
//    override fun toLong(): Long = toFloat().toLong()
//
//
//    // 转换为 Byte 类型
//    override fun toByte(): Byte = toFloat().toInt().toByte()
//
//    // 比较两个 Float16 的值
//    override fun compareTo(other: Float16): Int = toFloat().compareTo(other.toFloat())
//
//    // 重写 equals 方法
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is Float16 && other !is Float && other !is Double) return false
//        return when (other) {
//            is Float16 -> this.toFloat() == other.toFloat()
//            is Float -> this.toFloat() == other
//            is Double -> this.toDouble() == other
//            else -> false
//        }
//    }
//
//
//    // 重写 hashCode 方法
//    override fun hashCode(): Int = toFloat().hashCode()
//
//    // 提供更友好的字符串表示
//    override fun toString(): String = toFloat().toString()
//
//    companion object {
//        // 将 Short 转换为 BitSet
//        private fun shortToBitSet(value: Short): BitSet {
//            val bitSet = BitSet(16)
//            for (i in 0 until 16) {
//                if ((value.toInt() shr i) and 1 == 1) {
//                    bitSet.set(i)
//                }
//            }
//            return bitSet
//        }
//
//        private fun intToBitSet(value: Int): BitSet {
//            val bitSet = BitSet(32)  // 创建一个 32 位的 BitSet
//
//            for (i in 0 until 32) {
//                // 使用位运算检查每一位
//                if ((value and (1 shl i)) != 0) {
//                    bitSet.set(i)  // 如果该位为 1，则设置 BitSet 中对应的位为 true
//                }
//            }
//
//            return bitSet
//        }
//
//        // 常量定义
//        private const val SIGN_MASK = 0x8000
//        private const val EXPONENT_MASK = 0x7C00
//        private const val FRACTION_MASK = 0x03FF
//        private const val EXPONENT_BIAS = 15
//
//        // 常用的浮点常数
//        val NaN = Float16(0x7E00)  // NaN
//        val INF = Float16(0x7C00)  // Infinity
//        val ZERO = Float16(0x0000)  // Zero
//        val NEG_ZERO = Float16(0x8000)  // Negative Zero
//
//        // 从标准浮点数转换到 Float16
//        fun fromFloat1(f: Float): Float16 {
//            val intBits = java.lang.Float.floatToIntBits(f)
//            val sign = (intBits shr 31) and 0x1
//            val exponent = (intBits shr 23) and 0xFF
//            val fraction = intBits and 0x7FFFFF
//
//            if (exponent == 0xFF) { // Infinity or NaN
//                return if (fraction == 0) {
//                    if (sign == 0) INF else NEG_ZERO
//                } else {
//                    NaN
//                }
//            }
//
//            val newExp: Int
//            val newFrac: Int
//
//            // Convert to Float16
//            val bits = BitSet(16)
//
//            if (exponent == 0) { // Subnormal numbers
//                newExp = 0
//                newFrac = fraction shr 13
//            } else {
//                newExp = (exponent - 112) and 0x1F
//                newFrac = (fraction shr 13) and 0x3FF
//            }
//
//            // Set the bits for sign, exponent, and fraction
//            bits.set(15, sign == 1)
//            for (i in 0..4) bits.set(10 + i, (newExp shr (4 - i) and 1) == 1)
//            for (i in 0..9) bits.set(i, (newFrac shr (9 - i) and 1) == 1)
//
//            return Float16(bits)
//        }
//
//        fun fromFloat (value: Float): Float16 {
//            if (value.isNaN()) return Float16(0x7e00.toShort())  // NaN
//            if (value == Float.POSITIVE_INFINITY) return Float16(0x7c00.toShort())  // 正无穷
//            if (value == Float.NEGATIVE_INFINITY) return Float16(0xfc00.toShort())  // 负无穷
//            if (value == 0f) return Float16(0.toShort())  // 正零或负零
//
//            // 初始化一个 16 位的 BitSet，用于存储 Float16 的二进制表示
//            val bitSet = BitSet(16)
//
//            // 提取符号位
//            val sign = if (value < 0) 1 else 0
//            if (sign == 1) bitSet.set(15)  // 第 15 位是符号位
//
//            val absValue = Math.abs(value)
//
//            // 计算指数和尾数
//            val exponent = Math.floor(Math.log(absValue.toDouble()) / Math.log(2.0)).toInt()
//            var mantissa = absValue / Math.pow(2.0, exponent.toDouble()) - 1.0
//
//            // 处理 Float16 的指数范围 [-14, +15]
//            val normalizedExponent = exponent + 15
//
//            if (normalizedExponent >= 31) {
//                // 超过最大表示范围，返回无穷
//                bitSet.set(10, 15) // 指数部分全为 1
//                return Float16(bitSet)
//            } else if (normalizedExponent <= 0) {
//                // 处理次正规化数（指数过小）
//                val subnormalMantissa = (absValue / Math.pow(2.0, -14.0) * 1024).toInt()
//                for (i in 0..9) {
//                    if ((subnormalMantissa shr i) and 1 == 1) {
//                        bitSet.set(i)
//                    }
//                }
//                return Float16(bitSet)
//            }
//
//            // 设置指数部分
//            for (i in 10..14) {
//                if ((normalizedExponent shr (i - 10)) and 1 == 1) {
//                    bitSet.set(i)
//                }
//            }
//
//            // 设置尾数部分，尾数应乘以 1024
//            val scaledMantissa = (mantissa * 1024).toInt()
//            for (i in 0..9) {
//                if ((scaledMantissa shr i) and 1 == 1) {
//                    bitSet.set(i)
//                }
//            }
//
//            return Float16(bitSet)
//        }
//
//
//        // 将 Double 转换为 Float16 类型
//        fun fromDouble(value: Double): Float16 = fromFloat(value.toFloat())
//    }
//}

// 16-bit Float (Half Precision) implementation
data class Float16(private val bitSet: BitSet) : Number(), Comparable<Float16> {
    // Check if the Float16 is Infinity
    fun isInfinite(): Boolean {
        // Check exponent (bits 10-14) and mantissa (bits 0-9)
        val exponent = (0 until 5).fold(0) { acc, i ->
            acc or (if (bitSet[14 - i]) 1 shl i else 0)
        }
        val mantissa = (0 until 10).fold(0) { acc, i ->
            acc or (if (bitSet[i]) 1 shl i else 0)
        }
        // Infinity occurs when exponent is all 1s and mantissa is 0
        return exponent == 31 && mantissa == 0
    }

    // Get the float value from the Float16 representation
    override fun toFloat(): Float {
        val sign = if (bitSet[15]) -1 else 1
        var exponent = 0
        var mantissa = 0
        for (i in 10..14) {
            if (bitSet[i]) exponent += (1 shl (i - 10))
        }
        for (i in 0..9) {
            if (bitSet[i]) mantissa += (1 shl (9 - i))
        }

        // Check for special cases (NaN, Infinity, Zero)
        if (exponent == 31) {
            if (mantissa == 0) return sign * Float.POSITIVE_INFINITY
            else return Float.NaN
        } else if (exponent == 0) {
            if (mantissa == 0) return sign * 0.0f
            else {
                // Denormalized number
                var value = 0.0f
                var factor = 1.0f / (1 shl 10)
                for (i in 9 downTo 0) {
                    if ((mantissa and (1 shl i)) != 0) {
                        value += factor
                    }
                    factor /= 2
                }
                return sign * value * Math.pow(2.0, -14.0).toFloat()
            }
        } else {
            var value = 1.0f
            var factor = 1.0f / (1 shl 10)
            for (i in 9 downTo 0) {
                if ((mantissa and (1 shl i)) != 0) {
                    value += factor
                }
                factor /= 2
            }
            return sign * value * Math.pow(2.0, (exponent - 15).toDouble()).toFloat()
        }
    }

    // Override Number methods
    override fun toByte(): Byte = toFloat().toInt().toByte()
    override fun toShort(): Short = toFloat().toInt().toShort()
    override fun toInt(): Int = toFloat().toInt()
    override fun toLong(): Long = toFloat().toLong()

    override fun toDouble(): Double = toFloat().toDouble()
    override fun compareTo(other: Float16): Int {
        return toFloat().compareTo(other.toFloat())
    }


    operator fun unaryMinus(): Float16 {
        return this
    }

    operator fun unaryPlus(): Float16 {  return this
    }


    // Convert a Float to Float16 representation
    companion object {
        fun fromFloat(value: Float): Float16 {
            val bitSet = BitSet(16)

            val bits = java.lang.Float.floatToIntBits(value)
            val sign = (bits shr 31) and 0x1
            var exponent = (bits shr 23) and 0xFF
            var mantissa = bits and 0x7FFFFF

            if (exponent == 0xFF) {
                // NaN or Infinity
                bitSet.set(15, sign == 1)
                bitSet.set(14, true)
                for (i in 0..9) {
                    bitSet.set(i, (mantissa and (1 shl (22 - i))) != 0)
                }
            } else if (exponent == 0) {
                // Denormalized numbers
                bitSet.set(15, sign == 1)
                for (i in 0..9) {
                    bitSet.set(i, (mantissa and (1 shl (22 - i))) != 0)
                }
            } else {
                // Normalized numbers
                exponent -= 127
                exponent += 15
                bitSet.set(15, sign == 1)
                for (i in 0..4) {
                    bitSet.set(10 + i, (exponent and (1 shl (4 - i))) != 0)
                }
                for (i in 0..9) {
                    bitSet.set(i, (mantissa and (1 shl (22 - i))) != 0)
                }
            }
            return Float16(bitSet)
        }
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
        return parseFloat(text.removeSuffix("f16"))?.let {
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
