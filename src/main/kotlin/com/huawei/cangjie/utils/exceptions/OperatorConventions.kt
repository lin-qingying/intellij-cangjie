package com.huawei.cangjie.utils.exceptions

import com.google.common.collect.ImmutableBiMap
import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.name.Name

object OperatorConventions {
    fun getNameForOperationSymbol(token: CjToken): Name? {
        return getNameForOperationSymbol(token, true, true)
    }

    fun getNameForOperationSymbol(
        token: CjToken,
        unaryOperations: Boolean,
        binaryOperations: Boolean
    ): Name? {
//        var name: Name
//
//        if (binaryOperations) {
//            name = BINARY_OPERATION_NAMES.get(token)
//            if (name != null) return name
//        }
//
//        if (unaryOperations) {
//            name = OperatorConventions.UNARY_OPERATION_NAMES.get(token)
//            if (name != null) return name
//        }
//
//        name = ASSIGNMENT_OPERATIONS.get(token)
//        if (name != null) return name
//        if (OperatorConventions.COMPARISON_OPERATIONS.contains(token)) return COMPARE_TO
//        if (OperatorConventions.EQUALS_OPERATIONS.contains(token)) return EQUALS
//        if (OperatorConventions.IN_OPERATIONS.contains(token)) return CONTAINS
        return null
    }

    @JvmField
    val ASSIGNMENT_OPERATIONS: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
//            .put(CjTokens.MULTEQ, TIMES_ASSIGN)
//            .put(CjTokens.DIVEQ, DIV_ASSIGN)
//            .put(CjTokens.PERCEQ, REM_ASSIGN)
//            .put(CjTokens.PLUSEQ, PLUS_ASSIGN)
//            .put(CjTokens.MINUSEQ, MINUS_ASSIGN)
            .build()


    @JvmField
    val BINARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken, Name>()
//            .put(CjTokens.MUL, TIMES)
//            .put(CjTokens.PLUS, PLUS)
//            .put(CjTokens.MINUS, MINUS)
//            .put(CjTokens.DIV, DIV)
//            .put(CjTokens.PERC, REM)
//            .put(CjTokens.RANGE, RANGE_TO)

            .build()

}
