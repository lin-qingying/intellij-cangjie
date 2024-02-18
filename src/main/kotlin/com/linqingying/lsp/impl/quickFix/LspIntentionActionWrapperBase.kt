package com.linqingying.lsp.impl.quickFix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

import com.intellij.psi.PsiFile
import com.linqingying.lsp.api.customization.LspIntentionAction


abstract class LspIntentionActionWrapperBase(protected val index: Int) :
    IntentionAction,
    Comparable<IntentionAction> {


    abstract var lspIntentionAction: LspIntentionAction?

    override operator fun compareTo(other: IntentionAction): Int {
        if (other is LspIntentionActionWrapperBase) {
            return index - other.index
        }
        return 0
    }

    override fun generatePreview(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ): IntentionPreviewInfo {
        val lspIntentionAction = lspIntentionAction
        if (lspIntentionAction != null) {
            val previewInfo = lspIntentionAction.generatePreview(project, editor, psiFile)
            return previewInfo
        }
        return IntentionPreviewInfo.EMPTY
    }

    override fun getFamilyName(): String = ""

    override fun getText(): String {

        return lspIntentionAction?.text ?: ""
    }

    override operator fun invoke(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ) {

        lspIntentionAction?.invoke(project, editor, psiFile)
    }

    override fun isAvailable(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ): Boolean {

        return lspIntentionAction?.isAvailable(project, editor, psiFile) ?: false
    }

    override fun startInWriteAction(): Boolean = false
}

