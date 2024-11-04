package com.linqingying.cangjie.resolve.constants

object TypeConversionUtil {


    fun isFPZero(text: String): Boolean {
        for (element in text) {
            val c = element
            if (Character.isDigit(c) && c != '0') return false
            val d = c.uppercaseChar()
            if (d == 'E' || d == 'P') break
        }
        return true
    }
}
