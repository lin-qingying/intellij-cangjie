package com.huawei.cangjie.ide.editor

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.lexer.CjTokens
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.psi.PsiFile

class CangJieStringTemplateBackspaceHandler : BackspaceHandlerDelegate()  {

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        if (c != '{' || file.fileType != CangJieFileType.INSTANCE || !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return

        val offset = editor.caretModel.offset

        val highlighter = (editor as EditorEx).highlighter
        val iterator = highlighter.createIterator(offset)
        if (iterator.tokenType != CjTokens.LONG_TEMPLATE_ENTRY_END) return
        iterator.retreat()
        if (iterator.tokenType != CjTokens.LONG_TEMPLATE_ENTRY_START) return
        editor.document.deleteString(offset, offset + 1)
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        return false
    }
}
