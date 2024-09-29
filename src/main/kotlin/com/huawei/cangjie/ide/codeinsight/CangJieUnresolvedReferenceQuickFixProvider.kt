package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.highlighter.CangJieQuickFixProvider
import com.huawei.cangjie.psi.CjElement
import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.psi.PsiReference

class CangJieUnresolvedReferenceQuickFixProvider : UnresolvedReferenceQuickFixProvider<PsiReference>() {
    override fun registerFixes(reference: PsiReference, registrar: QuickFixActionRegistrar) {
        val element = reference.element as? CjElement ?: return

        val quickFixProvider =  CangJieQuickFixProvider.getInstance(element.project)
        val documentWindow = (element.containingFile.virtualFile as? VirtualFileWindow)?.documentWindow

        quickFixProvider.createUnresolvedReferenceQuickFixesForElement(element)
            .forEach { (diagnosticElement, quickFixes) ->
                val textRange = diagnosticElement.textRange
                val textRangeInHost = documentWindow?.injectedToHost(textRange) ?: textRange
                for (quickFix in quickFixes) {
                    registrar.register(textRangeInHost, quickFix, null)
                }
            }
    }

    override fun getReferenceClass(): Class<PsiReference> = PsiReference::class.java

}
