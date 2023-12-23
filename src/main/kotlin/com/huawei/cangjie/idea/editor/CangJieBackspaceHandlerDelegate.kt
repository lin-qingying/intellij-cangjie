package com.huawei.cangjie.idea.editor

import com.huawei.cangjie.idea.editor.LtGtTypingUtils.isAfterFunckeyword
import com.huawei.cangjie.idea.editor.LtGtTypingUtils.isAfterToken
import com.huawei.cangjie.lexer.CjTokens.IDENTIFIER
import com.huawei.cangjie.psi.CjFile
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class CangJieBackspaceHandlerDelegate : BackspaceHandlerDelegate() {
    private var deleteGt = false
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {

        val offset = editor.caretModel.offset - 1
        deleteGt = c == '<' && file is CjFile && (isAfterFunckeyword(
            offset,
            editor
        ) || isAfterToken(offset, editor, IDENTIFIER))

    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        val offset = editor.caretModel.offset
        val chars = editor.document.charsSequence
        if (editor.document.textLength <= offset) return false //文件结束后的虚拟空间


        val c1 = chars[offset]
        if (c == '<' && deleteGt) {
            if (c1 == '>') {
                LtGtTypingUtils.handleCangJieLTDeletion(editor, offset)
            }
            return true
        }

        return false
    }
}