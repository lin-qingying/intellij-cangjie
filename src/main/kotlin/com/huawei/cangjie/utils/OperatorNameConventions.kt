package com.huawei.cangjie.utils

import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.OperatorNameConventions.asOperatorString


object OperatorNameConventions {

    @JvmField
    val INVOKE = Name.identifier("*operator_invoke")

    @JvmField

    val GET = Name.identifier("*operator_get")

    @JvmField
    val NOT = Name.identifier("*operator_not") //!

    @JvmField

    val NOT_EQUALS = Name.identifier("*operator_not_equals") //!=

    @JvmField

    val EXPONENTIATION = Name.identifier("*operator_exponentiation") //**

    @JvmField

    val EQUALS = Name.identifier("*operator_equals") //==

    @JvmField

    val TIMES = Name.identifier("*operator_times")//*

    @JvmField

    val DIV = Name.identifier("*operator_div")// /

    @JvmField

    val REM = Name.identifier("*operator_rem")//%

    @JvmField

    val MINUS = Name.identifier("*operator_minus")//-

    @JvmField

    val PLUS = Name.identifier("*operator_plus")//+

    @JvmField

    val LEFT_SHIFT = Name.identifier("*operator_left_shift")//<<

    @JvmField

    val RIGHT_SHIFT = Name.identifier("*operator_right_shift")//>>

    @JvmField

    val COMPARE_GT = Name.identifier("*operator_compare_gt")//>

    @JvmField

    val COMPARE_LTEQ = Name.identifier("*operator_compare_lteq")//<=

    @JvmField

    val COMPARE_LT = Name.identifier("*operator_compare_lt")//<

    @JvmField

    val COMPARE_GTEQ = Name.identifier("*operator_compare_gteq")//>=

    @JvmField

    val AND = Name.identifier("*operator_and")//&

    @JvmField
    val XOR = Name.identifier("*operator_xor")//^
    @JvmField
    val OR = Name.identifier("*operator_or")//|



    @JvmField val ANDAND = Name.identifier("*operator_and2")
    @JvmField val OROR = Name.identifier("*operator_or2")

    fun Name.asOperatorString(): String {
        return when (this) {
            INVOKE -> "()"
            GET -> "[]"
            NOT -> "!"
            NOT_EQUALS -> "!="
            EXPONENTIATION -> "**"
            EQUALS -> "=="
            TIMES -> "*"
            DIV -> "/"
            REM -> "%"
            MINUS -> "-"
            PLUS -> "+"
            LEFT_SHIFT -> "<<"
            RIGHT_SHIFT -> ">>"
            COMPARE_GT -> ">"
            COMPARE_LTEQ -> "<="
            COMPARE_LT -> "<"
            COMPARE_GTEQ -> ">="
            AND -> "&"
            XOR -> "^"
            OR -> "|"
            else -> this.asString()
        }
    }

    fun String.asOperatorName(): Name {
        return when (this) {
            "()" -> INVOKE
            "[]" -> GET
            "!" -> NOT
            "!=" -> NOT_EQUALS
            "**" -> EXPONENTIATION
            "==" -> EQUALS
            "*" -> TIMES
            "/" -> DIV
            "%" -> REM
            "-" -> MINUS
            "+" -> PLUS
            "<<" -> LEFT_SHIFT
            ">>" -> RIGHT_SHIFT
            ">" -> COMPARE_GT
            "<=" -> COMPARE_LTEQ
            "<" -> COMPARE_LT
            ">=" -> COMPARE_GTEQ
            "&" -> AND
            "^" -> XOR
            "|" -> OR
            else -> Name.identifier(this)
        }
    }

}
