package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.doc.lexer.CDocLexer
import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.lexer.LayeredLexer
import com.intellij.lexer.StringLiteralLexer
import com.intellij.psi.tree.IElementType

class CangJieHighlightingLexer : LayeredLexer(CangJieLexer()) {
    init {

        registerSelfStoppingLayer(
            CDocLexer(),
            arrayOf<IElementType>(CjTokens.DOC_COMMENT),
            IElementType.EMPTY_ARRAY
        )
        registerSelfStoppingLayer(
            StringLiteralLexer('r', CjTokens.RUNE_LITERAL),
            arrayOf<IElementType>(CjTokens.RUNE_LITERAL),
            IElementType.EMPTY_ARRAY
        )
    }
}

