package com.linqingying.lsp.impl.quickFix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class LspQuickFixWrapper(val quickFixSet: LspQuickFixSet, val index: Int ) : IntentionAction,
    Comparable<IntentionAction> {
    override fun startInWriteAction(): Boolean = false


    override fun getFamilyName(): String  =""

    override fun getText(): String = lspIntentionAction?.text ?: ""

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        lspIntentionAction?.isAvailable(project, editor, file) ?: false

    var lspIntentionAction: IntentionAction? =  null
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        lspIntentionAction?.invoke(project, editor, file)
    }

    override fun compareTo(other: IntentionAction): Int {
        return if (other is LspQuickFixWrapper) {
            this.index - other.index
        } else {
            0
        }
    }
}

