package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.doc.lexer.CDocTokens
import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjFile
import com.intellij.lexer.Lexer
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.search.IndexPatternBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

abstract class IndexPatternBuilderAdapter : IndexPatternBuilder {
    override fun getCommentStartDelta(tokenType: IElementType, tokenText: CharSequence): Int {
        return when (tokenType) {
            CjTokens.EOL_COMMENT -> 2
            CjTokens.BLOCK_COMMENT -> 2
            CjTokens.DOC_COMMENT -> 3
            else -> 0
        }
    }

    override fun getCharsAllowedInContinuationPrefix(tokenType: IElementType): String {
        return when (tokenType) {
            CjTokens.BLOCK_COMMENT -> "*"
            CjTokens.DOC_COMMENT -> "*"
            else -> ""
        }
    }
}


class CangJieIndexPatternBuilder : IndexPatternBuilderAdapter() {
    override fun getCommentTokenSet(file: PsiFile): TokenSet? {
        return if (file is CjFile) TODO_COMMENT_TOKENS else null
    }

    override fun getIndexingLexer(file: PsiFile): Lexer? {
        return if (file is CjFile) CangJieLexer() else null
    }

    override fun getCommentStartDelta(tokenType: IElementType?): Int = 0

    override fun getCommentEndDelta(tokenType: IElementType?): Int = when (tokenType) {
        CjTokens.BLOCK_COMMENT -> "*/".length
        else -> 0
    }
}

private val TODO_COMMENT_TOKENS: TokenSet = TokenSet.orSet(CjTokens.COMMENTS, TokenSet.create(CDocTokens.CDOC))
