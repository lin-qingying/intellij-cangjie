/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.editor


import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjNodeTypes
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
