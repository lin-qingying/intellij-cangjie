package com.linqingying.lsp.impl.intention

import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

import com.intellij.psi.PsiFile
import com.linqingying.lsp.api.customization.LspIntentionAction
import com.linqingying.lsp.impl.LspBundle
import com.linqingying.lsp.impl.quickFix.LspIntentionActionWrapperBase


private class LspIntention0 : LspIntentionActionWrapper(0)
private class LspIntention1 : LspIntentionActionWrapper(1)
private class LspIntention2 : LspIntentionActionWrapper(2)
private class LspIntention3 : LspIntentionActionWrapper(3)
private class LspIntention4 : LspIntentionActionWrapper(4)
private class LspIntention5 : LspIntentionActionWrapper(5)
private class LspIntention6 : LspIntentionActionWrapper(6)
private class LspIntention7 : LspIntentionActionWrapper(7)
private class LspIntention8 : LspIntentionActionWrapper(8)
private class LspIntention9 : LspIntentionActionWrapper(9)
private class LspIntention10 : LspIntentionActionWrapper(10)
private class LspIntention11 : LspIntentionActionWrapper(11)
private class LspIntention12 : LspIntentionActionWrapper(12)
private class LspIntention13 : LspIntentionActionWrapper(13)
private class LspIntention14 : LspIntentionActionWrapper(14)


private open class LspIntentionActionWrapper(index: Int) :
    LspIntentionActionWrapperBase(index) {
    override var lspIntentionAction: LspIntentionAction? = null


    private fun updateLspIntentionAction(editor: Editor) {
        val project = editor.project ?: return
        val intentionActions = LspIntentionsService.getInstance(project).getIntentionActions(editor)
        lspIntentionAction = if (index < intentionActions.size) intentionActions[index] else null
    }

    override fun getFamilyName() = LspBundle.message("intention.family.name")

    override fun isAvailable(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ): Boolean {
        return if (editor is EditorWindow) {
            false

        } else {
            this.updateLspIntentionAction(editor)
            super.isAvailable(project, editor, psiFile)
        }
    }
}

