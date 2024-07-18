package com.huawei.cangjie.idea.highlighter

import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.idea.inspections.suppress.AnnotationHostKind
import com.huawei.cangjie.psi.CjElement
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.MultiMap



interface CangJieQuickFixProvider {
    companion object {
        fun getInstance(project: Project): CangJieQuickFixProvider = project.service()
    }

    fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    fun createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    fun createUnresolvedReferenceQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    /**
     * Produces fixes for diagnostics from different factories lazily to avoid creation of redundant quick fixes,
     * e.g. in case only the first suitable fix is required.
     */
    fun createUnresolvedReferenceQuickFixesForElement(element: CjElement): Map<PsiElement, Sequence<IntentionAction>>

    fun createSuppressFix(element: CjElement, suppressionKey: String, hostKind: AnnotationHostKind): SuppressIntentionAction
}

object RegisterQuickFixesLaterIntentionAction : IntentionAction {
    override fun getText(): String = ""

    override fun getFamilyName(): String = ""

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = Unit

    override fun startInWriteAction(): Boolean = false
}
