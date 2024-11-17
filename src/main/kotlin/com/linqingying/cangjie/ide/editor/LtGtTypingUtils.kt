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

import com.linqingying.cangjie.lexer.CjToken
import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.codeInsight.editorActions.TypedHandlerUtil
import com.intellij.openapi.editor.Editor
import com.intellij.psi.tree.TokenSet




internal object LtGtTypingUtils {
    private val INVALID_INSIDE_REFERENCE = TokenSet.create(
        CjTokens.SEMICOLON,
        CjTokens.LBRACE,
        CjTokens.RBRACE
    )

    fun handleCangJieAutoCloseLT(editor: Editor?) {
        TypedHandlerUtil.handleAfterGenericLT(
            editor!!,
            CjTokens.LT,
            CjTokens.GT,
            INVALID_INSIDE_REFERENCE
        )
    }

    fun handleCangJieGTInsert(editor: Editor?): Boolean {
        return TypedHandlerUtil.handleGenericGT(
            editor!!,
            CjTokens.LT,
            CjTokens.GT,
            INVALID_INSIDE_REFERENCE
        )
    }

    fun handleCangJieLTDeletion(editor: Editor?, offset: Int) {
        TypedHandlerUtil.handleGenericLTDeletion(
            editor!!,
            offset,
            CjTokens.LT,
            CjTokens.GT,
            INVALID_INSIDE_REFERENCE
        )
    }

    fun shouldAutoCloseAngleBracket(offset: Int, editor: Editor): Boolean {
        return isAfterClassIdentifier(offset, editor) || isAfterFunckeyword(offset,editor)
    }

    fun isAfterFunckeyword(offset: Int, editor: Editor): Boolean {
        return isAfterTokenAndSeparatedByToken(offset , editor, CjTokens.FUNC_KEYWORD, CjTokens.IDENTIFIER)
    }

    private fun isAfterClassIdentifier(offset: Int, editor: Editor): Boolean {
        val iterator = editor.highlighter.createIterator(offset)
        if (iterator.atEnd()) {
            return false
        }
        if (iterator.start > 0) {
            iterator.retreat()
        }
        return TypedHandlerUtil.isClassLikeIdentifier(
            offset,
            editor,
            iterator,
            CjTokens.IDENTIFIER
        )
    }

    fun isAfterToken(offset: Int, editor: Editor, tokenType: CjToken): Boolean {
        val iterator = editor.highlighter.createIterator(offset)
        if (iterator.atEnd()) {
            return false
        }
        if (iterator.start > 0) {
            iterator.retreat()
        }
        if (iterator.tokenType === CjTokens.WHITE_SPACE && iterator.start > 0) {
            iterator.retreat()
        }
        return iterator.tokenType === tokenType
    }

    fun isAfterTokenAndSeparatedByToken(offset: Int, editor: Editor, tokenType: CjToken, separatedTokenType: CjToken):Boolean{

        val iterator = editor.highlighter.createIterator(offset)
        if (iterator.atEnd()) {
            return false
        }
        if (iterator.start > 0) {
            iterator.retreat()
        }
        while ((iterator.tokenType === CjTokens.WHITE_SPACE || iterator.tokenType === separatedTokenType) && iterator.start > 0) {
            iterator.retreat()
        }
        return iterator.tokenType === tokenType
    }
}

