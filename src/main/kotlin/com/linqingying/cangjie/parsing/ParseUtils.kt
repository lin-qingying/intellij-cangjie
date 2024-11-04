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
//    if(text.endsWith("i8") || text.endsWith("u8")){
//        return parseInt8(text)
//    }else   if(text.endsWith("i16") || text.endsWith("u16")){
//        return parseInt16(text)
//    }else   if(text.endsWith("i32") || text.endsWith("u32")){
//        return parseInt32(text)
//    }
    return parseInt64(text)
}

fun String.removeSuffix(i: Int): String = this.substring(0, this.length - i)

//private fun parseInt8(text: String): Byte? {
//
//
//    return try {
//        val isUnsigned: Boolean
//        val numberWithoutSuffix: String
//        when {
//           text.endsWith("u8")-> {
//                isUnsigned = true
//                numberWithoutSuffix = text.removeSuffix(2)
//            }
//
//            text.endsWith("i8")-> {
//
//                isUnsigned = false
//                numberWithoutSuffix = text.removeSuffix(2)
//            }
//
//            else -> {
//                isUnsigned = false
//                numberWithoutSuffix = text
//            }
//        }
//
//        val (number, radix) = extractRadix(numberWithoutSuffix)
//
//        if (isUnsigned) {
//
//
//            java.lang.Byte.toUnsignedInt(number.toByte()).toByte()
//        } else {
//            java.lang.Byte.parseByte(number, radix)
//        }
//    } catch (e: NumberFormatException) {
//        null
//    }
//}
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


// Float16 类定义，继承自 Number
//半精度浮点数使用 16 位表示，通常包括 1 位符号位、5 位指数位和 10 位尾数位
//class Float16(private var bits: Int) {
//
//    companion object {
//        private const val EXPONENT_MASK = 0x7C00
//        private const val SIGN_MASK = 0x8000
//        private const val MANTISSA_MASK = 0x03FF
//
//        fun fromFloat(value: Float): Float16 {
//            val intBits = java.lang.Float.floatToIntBits(value)
//            val sign = (intBits and SIGN_MASK) shr 31 // 修正：获取符号位
//            val exponent = (intBits and EXPONENT_MASK) shr 23
//            val mantissa = (intBits and MANTISSA_MASK)
//
//            // Convert to Float16 representation
//            // ... (conversion logic here)
//
//            return Float16((sign shl 15) or (exponent shl 10) or mantissa)
//        }
//
//        fun toFloat(float16: Float16): Float {
//            // Convert Float16 back to Float
//            // ... (conversion logic here)
//
//            return 0.0f // Placeholder
//        }
//    }
//
//    override fun toString(): String {
//        return "Float16(bits=$bits)"
//    }
//}

private fun parseFloatingLiteral(text: String): Number? {
    if (text.endsWith("f16")) {
//        return parseFloat16(text)
        return parseFloat(text.removeSuffix("f16"))

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
