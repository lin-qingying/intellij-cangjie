package com.huawei.cangjie.lang.core.lexer

import com.intellij.psi.tree.IElementType

fun String.getCangJieLexerTokenType(): IElementType? {
    val lexer = CangJieLexer()
    lexer.start(this)
    return if (lexer.tokenEnd == length) lexer.tokenType else null
}
