package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.lexer.CjTokens


enum class CjProjectionKind(token: CjSingleValueToken?) {
//    IN(CjTokens.IN_KEYWORD),

//    STAR(CjTokens.MUL),
    NONE(null);

    private val token: CjSingleValueToken?

    init {
        this.token = token
    }

    fun getToken(): CjSingleValueToken? {
        return token
    }
}

