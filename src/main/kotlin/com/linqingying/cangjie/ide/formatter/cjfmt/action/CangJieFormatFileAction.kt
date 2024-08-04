package com.linqingying.cangjie.ide.formatter.cjfmt.action

import com.linqingying.cangjie.ide.formatter.cjfmt.CangJieFormatService
import com.linqingying.cangjie.ide.formatter.cjfmt.checkFile
import com.intellij.codeInsight.actions.FileInEditorProcessor
import com.intellij.codeInsight.actions.LayoutCodeDialog
import com.intellij.codeInsight.actions.ShowReformatFileDialog
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.annotations.NonNls

class CangJieFormatFileAction : ShowReformatFileDialog() {

    companion object {
        @NonNls
        private val HELP_ID = "editing.codeReformatting"

    }

    override fun actionPerformed(event: AnActionEvent) {
        val dataContext = event.dataContext
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val editor = CommonDataKeys.EDITOR.getData(dataContext)
        if (project != null && editor != null) {

            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (file != null && file.virtualFile != null) {
                val hasSelection = editor.selectionModel.hasSelection()
                if (checkFile(file.virtualFile)) {
                    FileDocumentManager.getInstance().saveAllDocuments()
                    val reformatCodeService = CangJieFormatService.getInstance(project)
                    reformatCodeService.reformatFileCode(file, hasSelection, project, editor)
                    ActionManager.getInstance().getAction("SynchronizeCurrentFile").actionPerformed(event)
                } else {
                    val dialog = LayoutCodeDialog(project, file, hasSelection, "editing.codeReformatting")
                    dialog.show()
                    if (dialog.isOK) {
                        FileInEditorProcessor(file, editor, dialog.runOptions).processCode()
                    }
                }
            }
        }
    }
}
