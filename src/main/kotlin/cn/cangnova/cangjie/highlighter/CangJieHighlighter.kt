/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens
import cn.cangnova.cangjie.psi.cdoc.lexer.CDocTokens.CDOC_HIGHLIGHT_TOKENS
import cn.cangnova.cangjie.lexer.CjTokens
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class CangJieHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer {
        return CangJieHighlightingLexer()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(
            keys1[tokenType],
            keys2[tokenType],
        )
    }

    companion object {
        private val keys1: MutableMap<IElementType, TextAttributesKey> = HashMap()
        private val keys2: MutableMap<IElementType, TextAttributesKey> = HashMap()

        init {

            fillMap(keys1, CjTokens.KEYWORDS, CangJieHighlightingColors.KEYWORD)

            fillMap(keys1, CjTokens.BASICTYPES, CangJieHighlightingColors.KEYWORD)

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
                            CjTokens.AT,
                        ),
                        CjTokens.KEYWORDS,
                    ),
                ),
                CangJieHighlightingColors.OPERATOR_SIGN,
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
                CangJieHighlightingColors.DOC_COMMENT,
            )
            keys1[CDocTokens.TAG_NAME] =
                CangJieHighlightingColors.DOC_COMMENT
            keys2[CDocTokens.TAG_NAME] =
                CangJieHighlightingColors.CDOC_TAG
            keys1[TokenType.BAD_CHARACTER] = CangJieHighlightingColors.BAD_CHARACTER
        }
    }
}
