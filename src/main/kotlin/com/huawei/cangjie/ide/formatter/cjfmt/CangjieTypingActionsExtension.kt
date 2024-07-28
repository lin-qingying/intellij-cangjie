package com.huawei.cangjie.ide.formatter.cjfmt

import com.intellij.codeInsight.editorActions.TypingActionsExtension
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

class CangjieTypingActionsExtension: TypingActionsExtension {


    override fun isSuitableContext(project: Project, editor: Editor): Boolean {
        val document = editor.document
        val documentManager = PsiDocumentManager.getInstance(project)
        val file = documentManager.getPsiFile(document)
        return file != null &&  checkFile(file.virtualFile)
    }


}
