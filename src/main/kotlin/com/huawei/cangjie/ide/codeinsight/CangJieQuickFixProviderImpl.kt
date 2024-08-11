package com.huawei.cangjie.ide.codeinsight

import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.ide.highlighter.CangJieQuickFixProvider
import com.huawei.cangjie.ide.inspections.suppress.AnnotationHostKind
import com.huawei.cangjie.ide.inspections.suppress.CangJieSuppressIntentionAction
import com.huawei.cangjie.psi.CjElement
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap

class CangJieQuickFixProviderImpl: CangJieQuickFixProvider {
    override fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {
//        TODO("Not yet implemented")
        return MultiMap.empty()
    }

    override fun createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {
//        TODO("Not yet implemented")
        return MultiMap.empty()

    }

    override fun createUnresolvedReferenceQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {
//        TODO("Not yet implemented")
        return MultiMap.empty()

    }

    override fun createUnresolvedReferenceQuickFixesForElement(element: CjElement): Map<PsiElement, Sequence<IntentionAction>> {
//        TODO("Not yet implemented")
        return emptyMap()

    }

    override fun createSuppressFix(
        element: CjElement,
        suppressionKey: String,
        hostKind: AnnotationHostKind
    ): SuppressIntentionAction {
        return CangJieSuppressIntentionAction(element, suppressionKey, hostKind)



    }
}
