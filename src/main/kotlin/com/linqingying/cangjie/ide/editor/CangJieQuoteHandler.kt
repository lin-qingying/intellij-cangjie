package com.linqingying.cangjie.ide.editor

import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.codeInsight.editorActions.QuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator

class CangJieQuoteHandler: QuoteHandler {

    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val tokenType = iterator.tokenType

        if (tokenType == CjTokens.RUNE_LITERAL) {
            val start = iterator.start
            val end = iterator.end
            return end - start >= 1 && offset == end - 1
        } else if (tokenType == CjTokens.CLOSING_QUOTE) {
            return true
        }
        return false
    }

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        return true
    }
    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean {

        val tokenType = iterator.tokenType
        return tokenType == CjTokens.REGULAR_STRING_PART ||
                tokenType == CjTokens.OPEN_QUOTE ||
                tokenType == CjTokens.CLOSING_QUOTE ||
                tokenType == CjTokens.SHORT_TEMPLATE_ENTRY_START ||
                tokenType == CjTokens.LONG_TEMPLATE_ENTRY_END ||
                tokenType == CjTokens.LONG_TEMPLATE_ENTRY_START
    }
    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean {
        val tokenType = iterator.tokenType

        if (tokenType == CjTokens.OPEN_QUOTE || tokenType == CjTokens.RUNE_LITERAL) {
            val start = iterator.start
            return offset == start
        }
        return false
    }
}
