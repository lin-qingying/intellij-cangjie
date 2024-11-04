package com.linqingying.cangjie.ide.editor

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjAbstractClassBody
import com.intellij.codeInsight.hint.DeclarationRangeUtil
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType

private val TYPE_TOKENS_INSIDE_ANGLE_BRACKETS = TokenSet.orSet(
    CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET,
    TokenSet.create(
        CjTokens.IDENTIFIER, CjTokens.COMMA,
        CjTokens.AT,
        CjTokens.RBRACKET, CjTokens.LBRACKET,

        CjTokens.COLON,  CjTokens.LPAR, CjTokens.RPAR,
        CjTokens.CLASS_KEYWORD, CjTokens.DOT
    )
)
class CangJiePairedBraceMatcher: PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> {
        return pairs
    }
    private val pairs = arrayOf(
        BracePair(CjTokens.LPAR, CjTokens.RPAR, false),
        BracePair(CjTokens.LONG_TEMPLATE_ENTRY_START, CjTokens.LONG_TEMPLATE_ENTRY_END, false),
        BracePair(CjTokens.LBRACE, CjTokens.RBRACE, true),
        BracePair(CjTokens.LBRACKET, CjTokens.RBRACKET, false),
        BracePair(CjTokens.LT, CjTokens.GT, false),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean {

        return if (lbraceType == CjTokens.LONG_TEMPLATE_ENTRY_START) {

            false
        } else CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(contextType)
                || contextType === CjTokens.COLON
                || contextType === CjTokens.SEMICOLON
                || contextType === CjTokens.COMMA
                || contextType === CjTokens.RPAR
                || contextType === CjTokens.RBRACKET
                || contextType === CjTokens.RBRACE
                || contextType === CjTokens.LBRACE
                || contextType === CjTokens.LONG_TEMPLATE_ENTRY_END

    }

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int {
        val element = file.findElementAt(openingBraceOffset)
        if (element == null || element is PsiFile) return openingBraceOffset
        val parent = element.parent
        return when  {
            parent is CjAbstractClassBody || parent.elementType == CjNodeTypes.BLOCK ->
                DeclarationRangeUtil.getPossibleDeclarationAtRange(parent.parent)?.startOffset ?: openingBraceOffset

            else -> openingBraceOffset
        }
    }

}
