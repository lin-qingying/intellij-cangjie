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
