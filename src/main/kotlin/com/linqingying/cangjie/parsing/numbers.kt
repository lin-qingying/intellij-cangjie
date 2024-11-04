package com.linqingying.cangjie.parsing

data class NumberWithRadix(val number: String, val radix: Int)

fun extractRadix(value: String): NumberWithRadix = when {
    value.startsWith("0x") || value.startsWith("0X") -> NumberWithRadix(value.substring(2), 16)
    value.startsWith("0b") || value.startsWith("0B") -> NumberWithRadix(value.substring(2), 2)
    else -> NumberWithRadix(value, 10)
}
