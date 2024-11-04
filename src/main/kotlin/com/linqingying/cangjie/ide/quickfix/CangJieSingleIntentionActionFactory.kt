package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement


abstract class CangJieSingleIntentionActionFactory : CangJieIntentionActionsFactory() {
    protected abstract fun createAction(diagnostic: Diagnostic): IntentionAction?

    final override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> =
        listOfNotNull(createAction(diagnostic))

    companion object {
        inline fun <reified PSI : PsiElement> createFromQuickFixesPsiBasedFactory(
            psiBasedFactory: QuickFixesPsiBasedFactory<PSI>
        ): CangJieSingleIntentionActionFactory = object : CangJieSingleIntentionActionFactory() {
            override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                val factories = psiBasedFactory.createQuickFix(diagnostic.psiElement as PSI)
                return when (factories.size) {
                    0 -> null
                    1 -> factories.single()
                    else -> error("To convert QuickFixesPsiBasedFactory to CangJieSingleIntentionActionFactory, it should always return one or zero quickfixes")
                }
            }
        }
    }
}
