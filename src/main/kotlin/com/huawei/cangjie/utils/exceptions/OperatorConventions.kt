package com.huawei.cangjie.utils.exceptions

import com.google.common.collect.ImmutableBiMap
import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name

object OperatorConventions {



@JvmField
    val BINARY_OPERATION_NAMES: ImmutableBiMap<CjSingleValueToken, Name> =
        ImmutableBiMap.builder<CjSingleValueToken,  Name>()
//            .put(CjTokens.MUL, TIMES)
//            .put(CjTokens.PLUS, PLUS)
//            .put(CjTokens.MINUS, MINUS)
//            .put(CjTokens.DIV, DIV)
//            .put(CjTokens.PERC, REM)
//            .put(CjTokens.RANGE, RANGE_TO)

            .build()

}
