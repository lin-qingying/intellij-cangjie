package com.huawei.cangjie.highlighter

import com.huawei.cangjie.doc.lexer.CDocTokens
import com.huawei.cangjie.doc.lexer.CDocTokens.CDOC_HIGHLIGHT_TOKENS
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

 import com.huawei.cangjie.lexer.CjTokens


class CangJieHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return CangJieHighlightingLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(
            keys1[tokenType],
            keys2[tokenType]
        )
    }

    companion object {
        private val keys1: MutableMap<IElementType, TextAttributesKey> = HashMap()
        private val keys2: MutableMap<IElementType, TextAttributesKey> = HashMap()



        init {

            fillMap(keys1,CjTokens.KEYWORDS, CangJieHighlightingColors.KEYWORD)


            fillMap(keys1,CjTokens.BASICTYPES, CangJieHighlightingColors.KEYWORD)

            keys1[CjTokens.LET_KEYWORD] =
                CangJieHighlightingColors.LET_KEYWORD
            keys1[CjTokens.VAR_KEYWORD] =
                CangJieHighlightingColors.VAR_KEYWORD
            keys1[CjTokens.CONST_KEYWORD] =
                CangJieHighlightingColors.CONST_KEYWORD




            keys1[CjTokens.INTEGER_LITERAL] =
                CangJieHighlightingColors.NUMBER
            keys1[CjTokens.FLOAT_LITERAL] =
                CangJieHighlightingColors.NUMBER
            fillMap(
                keys1,
                TokenSet.andNot(
                   CjTokens.OPERATIONS,
                    TokenSet.orSet(
                        TokenSet.create(
                           CjTokens.IDENTIFIER,
                           CjTokens.AT
                        ),CjTokens.KEYWORDS
                    )
                ),
                CangJieHighlightingColors.OPERATOR_SIGN
            )
            keys1[CjTokens.LPAR] = CangJieHighlightingColors.PARENTHESIS
            keys1[CjTokens.RPAR] = CangJieHighlightingColors.PARENTHESIS
            keys1[CjTokens.LBRACE] = CangJieHighlightingColors.BRACES
            keys1[CjTokens.RBRACE] = CangJieHighlightingColors.BRACES
            keys1[CjTokens.LBRACKET] = CangJieHighlightingColors.BRACKETS
            keys1[CjTokens.RBRACKET] = CangJieHighlightingColors.BRACKETS
            keys1[CjTokens.COMMA] = CangJieHighlightingColors.COMMA
            keys1[CjTokens.SEMICOLON] = CangJieHighlightingColors.SEMICOLON
            keys1[CjTokens.COLON] = CangJieHighlightingColors.COLON

            keys1[CjTokens.QUEST] = CangJieHighlightingColors.QUEST
            keys1[CjTokens.DOT] = CangJieHighlightingColors.DOT
            keys1[CjTokens.ARROW] = CangJieHighlightingColors.ARROW
            keys1[CjTokens.OPEN_QUOTE] = CangJieHighlightingColors.STRING
            keys1[CjTokens.CLOSING_QUOTE] =
                CangJieHighlightingColors.STRING
            keys1[CjTokens.REGULAR_STRING_PART] =
                CangJieHighlightingColors.STRING
            keys1[CjTokens.LONG_TEMPLATE_ENTRY_END] =
                CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.LONG_TEMPLATE_ENTRY_START] =
                CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.SHORT_TEMPLATE_ENTRY_START] =
                CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.ESCAPE_SEQUENCE] =
                CangJieHighlightingColors.STRING_ESCAPE
            keys1[CjTokens.RUNE_LITERAL] =
                CangJieHighlightingColors.STRING
            keys1[CjTokens.EOL_COMMENT] =
                CangJieHighlightingColors.LINE_COMMENT
            keys1[CjTokens.SHEBANG_COMMENT] =
                CangJieHighlightingColors.LINE_COMMENT
            keys1[CjTokens.BLOCK_COMMENT] =
                CangJieHighlightingColors.BLOCK_COMMENT
            keys1[CjTokens.DOC_COMMENT] =
                CangJieHighlightingColors.DOC_COMMENT
            fillMap(
                keys1,
                CDOC_HIGHLIGHT_TOKENS,
                CangJieHighlightingColors.DOC_COMMENT
            )
            keys1[CDocTokens.TAG_NAME] =
                CangJieHighlightingColors.DOC_COMMENT
            keys2[CDocTokens.TAG_NAME] =
                CangJieHighlightingColors.CDOC_TAG
            keys1[TokenType.BAD_CHARACTER] = CangJieHighlightingColors.BAD_CHARACTER
        }
    }
}

