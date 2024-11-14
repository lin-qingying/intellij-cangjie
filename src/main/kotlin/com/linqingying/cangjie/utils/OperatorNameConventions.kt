package com.linqingying.cangjie.utils

import com.linqingying.cangjie.name.Name


object OperatorNameConventions {
    @JvmField
    val CONTAINS = Name.identifier("contains")

    @JvmField
    val INVOKE = Name.identifier("*operator_invoke")

    @JvmField

    val GET = Name.identifier("*operator_get")

    @JvmField
    val SET = Name.identifier("*operator_set")

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


    //    不可被重载  只用于检查
    @JvmField
    val TIMES_ASSIGN = Name.identifier("*operator_timesAssign") //-=

    @JvmField
    val DIV_ASSIGN = Name.identifier("*operator_divAssign") // /=

    @JvmField
    val EXPONENTIATION_ASSIGN = Name.identifier("*operator_exponentiationAssign") // /=

    @JvmField
    val OROREQ_ASSIGN = Name.identifier("*operator_or2Assign") // /=

    @JvmField
    val ANDANDEQ_ASSIGN = Name.identifier("*operator_and2Assign") // /=

    @JvmField
    val OREQ_ASSIGN = Name.identifier("*operator_orAssign") // /=

    @JvmField
    val ANDEQ_ASSIGN = Name.identifier("*operator_andAssign") // /=

    @JvmField
    val XOREQ_ASSIGN = Name.identifier("*operator_xorAssign") // /=

    @JvmField
    val GTGTEQ_ASSIGN = Name.identifier("*operator_rightShiftAssign") // /=

    @JvmField
    val LTLTEQ_ASSIGN = Name.identifier("*operator_leftShiftAssign") // /=


    @JvmField

    val PIPELINE = Name.identifier("*operator_pipeline")//*

    @JvmField

    val COMPOSITION = Name.identifier("*operator_composition")//*


    @JvmField
    val REM_ASSIGN = Name.identifier("*operator_remAssign")

    @JvmField
    val PLUS_ASSIGN = Name.identifier("*operator_plusAssign")

    @JvmField
    val MINUS_ASSIGN = Name.identifier("*operator_minusAssign")

    @JvmField
    val ANDAND = Name.identifier("*operator_and2")

    @JvmField
    val OROR = Name.identifier("*operator_or2")


    @JvmField
    val INC = Name.identifier("*operator_inc")

    @JvmField
    val DEC = Name.identifier("*operator_dec")

    @JvmField
    val UNARY_MINUS = Name.identifier("*operator_unaryMinus")

    @JvmField
    val UNARY_PLUS = Name.identifier("*operator_unaryPlus")


    //迭代器对象中的方法
    @JvmField
    val ITERATOR = Name.identifier("iterator")

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

            TIMES_ASSIGN -> "*="
            DIV_ASSIGN -> "/="
            EXPONENTIATION_ASSIGN -> "**="
            REM_ASSIGN -> "%="
            PLUS_ASSIGN -> "+="
            MINUS_ASSIGN -> "-="
            ANDAND -> "&&"
            OROR -> "||"
            PIPELINE -> "|>"
            COMPOSITION -> "~>"
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

            "*=" -> TIMES_ASSIGN
            "/=" -> DIV_ASSIGN
            "**=" -> EXPONENTIATION_ASSIGN
            "%=" -> REM_ASSIGN
            "+=" -> PLUS_ASSIGN
            "-=" -> MINUS_ASSIGN
            "&&" -> ANDAND
            "||" -> OROR

            "|>" -> PIPELINE
            "~>" -> COMPOSITION

            else -> Name.identifier(this)
        }
    }

}
