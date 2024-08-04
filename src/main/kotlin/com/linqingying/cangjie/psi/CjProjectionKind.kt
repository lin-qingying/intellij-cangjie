package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lexer.CjSingleValueToken
import com.linqingying.cangjie.lexer.CjTokens


enum class CjProjectionKind(token: CjSingleValueToken?) {
    IN(CjTokens.IN_KEYWORD),

    STAR(CjTokens.MUL),
    NONE(null);

    private val token: CjSingleValueToken?

    init {
        this.token = token
    }

    fun getToken(): CjSingleValueToken? {
        return token
    }
}

