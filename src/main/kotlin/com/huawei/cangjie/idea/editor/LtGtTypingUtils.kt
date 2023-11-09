package com.huawei.cangjie.idea.editor

import com.huawei.cangjie.lexer.CjToken
import com.huawei.cangjie.lexer.CjTokens
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
        return isAfterClassIdentifier(offset, editor) || isAfterToken(
            offset,
            editor,
            CjTokens.FUNC_KEYWORD
        )
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
}

