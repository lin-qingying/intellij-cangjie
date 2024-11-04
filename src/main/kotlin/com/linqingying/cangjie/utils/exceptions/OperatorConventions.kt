package com.linqingying.cangjie.utils.exceptions

import com.google.common.collect.ImmutableBiMap
import com.google.common.collect.ImmutableSet
import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.utils.OperatorNameConventions.AND
import com.linqingying.cangjie.utils.OperatorNameConventions.ANDAND
import com.linqingying.cangjie.utils.OperatorNameConventions.ANDANDEQ_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.ANDEQ_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_GT
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_GTEQ
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_LT
import com.linqingying.cangjie.utils.OperatorNameConventions.COMPARE_LTEQ
import com.linqingying.cangjie.utils.OperatorNameConventions.DEC
import com.linqingying.cangjie.utils.OperatorNameConventions.DIV
import com.linqingying.cangjie.utils.OperatorNameConventions.DIV_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.EQUALS
import com.linqingying.cangjie.utils.OperatorNameConventions.EXPONENTIATION
import com.linqingying.cangjie.utils.OperatorNameConventions.EXPONENTIATION_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.GET
import com.linqingying.cangjie.utils.OperatorNameConventions.GTGTEQ_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.INC
import com.linqingying.cangjie.utils.OperatorNameConventions.INVOKE
import com.linqingying.cangjie.utils.OperatorNameConventions.LEFT_SHIFT
import com.linqingying.cangjie.utils.OperatorNameConventions.LTLTEQ_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.MINUS
import com.linqingying.cangjie.utils.OperatorNameConventions.MINUS_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.NOT
import com.linqingying.cangjie.utils.OperatorNameConventions.NOT_EQUALS
import com.linqingying.cangjie.utils.OperatorNameConventions.OR
import com.linqingying.cangjie.utils.OperatorNameConventions.OREQ_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.OROR
import com.linqingying.cangjie.utils.OperatorNameConventions.OROREQ_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.PLUS
import com.linqingying.cangjie.utils.OperatorNameConventions.PLUS_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.REM
import com.linqingying.cangjie.utils.OperatorNameConventions.REM_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.RIGHT_SHIFT
import com.linqingying.cangjie.utils.OperatorNameConventions.TIMES
import com.linqingying.cangjie.utils.OperatorNameConventions.TIMES_ASSIGN
import com.linqingying.cangjie.utils.OperatorNameConventions.UNARY_MINUS
import com.linqingying.cangjie.utils.OperatorNameConventions.XOR
import com.linqingying.cangjie.utils.OperatorNameConventions.XOREQ_ASSIGN
import com.intellij.psi.tree.IElementType


object OperatorConventions {

    val DOUBLE: Name = Name.identifier("toDouble")

    val FLOAT: Name = Name.identifier("toFloat")

    val LONG: Name = Name.identifier("toLong")

    val INT: Name = Name.identifier("toInt")

    val CHAR: Name = Name.identifier("toChar")

    val SHORT: Name = Name.identifier("toShort")

    val BYTE: Name = Name.identifier("toByte")

    fun getOperationSymbolForName(name: Name): CjToken? {
        if (!isConventionName(name)) return null
        var token =
            BINARY_OPERATION_NAMES.inverse().get(name)
        if (token != null) return token
        token = UNARY_OPERATION_NAMES.inverse().get(name)
        if (token != null) return token
        token = ASSIGNMENT_OPERATIONS.inverse().get(name)
        if (token != null) return token
        return null
    }

    val NUMBER_CONVERSIONS: ImmutableSet<Name> =
        ImmutableSet.of<Name>(
//           OperatorConventions.DOUBLE,
//           OperatorConventions.FLOAT,
            LONG,
//           OperatorConventions.INT,
//           OperatorConventions.SHORT,
//           OperatorConventions.BYTE,
//           OperatorConventions.CHAR
        )

    fun isConventionName(name: Name): Boolean {
        return CONVENTION_NAMES.contains(name)
    }

    @JvmField
    val ASSIGNMENT_OPERATION_COUNTERPARTS: ImmutableBiMap<CjSingleValueToken, CjSingleValueToken> =
        ImmutableBiMap.builder<CjSingleValueToken, CjSingleValueToken>()
            .put(CjTokens.MULTEQ, CjTokens.MUL)
            .put(CjTokens.DIVEQ, CjTokens.DIV)
            .put(CjTokens.PERCEQ, CjTokens.PERC)
            .put(CjTokens.PLUSEQ, CjTokens.PLUS)
            .put(CjTokens.MINUSEQ, CjTokens.MINUS)


            .put(CjTokens.ANDEQ, CjTokens.AND)
            .put(CjTokens.XOREQ, CjTokens.XOR)
            .put(CjTokens.OREQ, CjTokens.OR)
            .put(CjTokens.LTLTEQ, CjTokens.LTLT)
            .put(CjTokens.GTGTEQ, CjTokens.GTGT)
            .put(CjTokens.MULMULEQ, CjTokens.MULMUL)
            .put(CjTokens.ANDANDEQ, CjTokens.ANDAND)
            .put(CjTokens.OROREQ, CjTokens.OROR)


            .build()

    @JvmStatic
    fun isConventionType(type: IElementType): Boolean {
        return COMPARISON_OPERATIONS_NAMES.containsKey(type) ||
                UNARY_OPERATION_NAMES.containsKey(type) ||
                BINARY_OPERATION_NAMES.containsKey(type) ||
                COMPARISON_OPERATIONS_NAMES.containsKey(
                    type
                )
    }

    @JvmStatic
    fun getNameForOperationSymbol(token: CjToken?): Name? {
        token ?: return null
        return getNameForOperationSymbol(token, true, true)
    }

    @JvmField

    val COMPARISON_OPERATIONS_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.GTEQ, COMPARE_GTEQ)
            .put(CjTokens.LTEQ, COMPARE_LTEQ)
            .put(CjTokens.GT, COMPARE_GT)
            .put(CjTokens.LT, COMPARE_LT)
            .put(CjTokens.EXCLEQ, NOT_EQUALS)
            .put(CjTokens.EQEQ, EQUALS)
            .build()

    @JvmField
    val BOOLEAN_OPERATIONS_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.ANDAND, ANDAND)
            .put(CjTokens.OROR, OROR)
            .build()

    fun getNameForOperationSymbol(
        token: CjToken,
        unaryOperations: Boolean,
        binaryOperations: Boolean
    ): Name? {

        var name: Name?

        if (binaryOperations) {
            name = BINARY_OPERATION_NAMES[token]
            if (name != null) return name
        }

        if (unaryOperations) {
            name = UNARY_OPERATION_NAMES[token]
            if (name != null) return name
        }

        name = ASSIGNMENT_OPERATIONS[token]
        if (name != null) return name
        name = COMPARISON_OPERATIONS_NAMES[token]
        if (name != null) return name
        name = BOOLEAN_OPERATIONS_NAMES[token]
        if (name != null) return name
//        if (OperatorConventions.EQUALS_OPERATIONS.contains(token)) return EQUALS
//        if (OperatorConventions.IN_OPERATIONS.contains(token)) return CONTAINS
        return null
    }

    @JvmField
    val UNARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.EXCL, NOT)
            .put(CjTokens.PLUSPLUS, INC)
            .put(CjTokens.MINUSMINUS, DEC)
            .put(CjTokens.MINUS, UNARY_MINUS)

            .build()

    @JvmField
    val ASSIGNMENT_OPERATIONS: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.MULTEQ, TIMES_ASSIGN)
            .put(CjTokens.DIVEQ, DIV_ASSIGN)
            .put(CjTokens.PERCEQ, REM_ASSIGN)
            .put(CjTokens.PLUSEQ, PLUS_ASSIGN)
            .put(CjTokens.MINUSEQ, MINUS_ASSIGN)
            .put(CjTokens.MULMULEQ, EXPONENTIATION_ASSIGN)


            .put(CjTokens.OROREQ, OROREQ_ASSIGN)
            .put(CjTokens.ANDANDEQ, ANDANDEQ_ASSIGN)
            .put(CjTokens.OREQ, OREQ_ASSIGN)
            .put(CjTokens.ANDEQ, ANDEQ_ASSIGN)
            .put(CjTokens.XOREQ, XOREQ_ASSIGN)
            .put(CjTokens.GTGTEQ, GTGTEQ_ASSIGN)
            .put(CjTokens.LTLTEQ, LTLTEQ_ASSIGN)

            .build()


    @JvmField
    val BINARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
            .put(CjTokens.MUL, TIMES)
            .put(CjTokens.MULMUL, EXPONENTIATION)
            .put(CjTokens.PLUS, PLUS)
            .put(CjTokens.MINUS, MINUS)
            .put(CjTokens.DIV, DIV)
            .put(CjTokens.PERC, REM)

            .put(CjTokens.GTGT, RIGHT_SHIFT)
            .put(CjTokens.LTLT, LEFT_SHIFT)

            .put(CjTokens.AND, AND)
            .put(CjTokens.XOR, XOR)
            .put(CjTokens.OR, OR)


//            .put(CjTokens.RANGE, RANGE_TO)

            .build()
    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well

    @JvmField
    val CONVENTION_NAMES: ImmutableSet<Name> =
        ImmutableSet.builder<Name>()
            .add(INVOKE)

            .add(GET)
            .addAll(UNARY_OPERATION_NAMES.values)
            .addAll(COMPARISON_OPERATIONS_NAMES.values)
            .addAll(BINARY_OPERATION_NAMES.values)
//            .addAll(ASSIGNMENT_OPERATIONS.values)
            .build()

}
