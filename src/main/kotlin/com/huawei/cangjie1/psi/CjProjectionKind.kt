package com.huawei.cangjie1.psi

import com.huawei.cangjie1.lexer.CjSingleValueToken
import com.huawei.cangjie1.lexer.CjTokens


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

