package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.psi.CjCodeFragment
import com.intellij.codeInsight.intention.IntentionAction


abstract class CangJieIntentionActionsFactory : QuickFixFactory {
    protected open fun isApplicableForCodeFragment(): Boolean = false

    protected abstract fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>

    open fun areActionsAvailable(diagnostic: Diagnostic): Boolean = createActions(diagnostic).isNotEmpty()

    protected open fun doCreateActionsForAllProblems(
        sameTypeDiagnostics: Collection<Diagnostic>
    ): List<IntentionAction> = emptyList()

    fun createActions(diagnostic: Diagnostic): List<IntentionAction> = createActions(listOf(diagnostic), false)

    fun createActionsForAllProblems(sameTypeDiagnostics: Collection<Diagnostic>): List<IntentionAction> =
        createActions(sameTypeDiagnostics, true)

    private fun createActions(sameTypeDiagnostics: Collection<Diagnostic>, createForAll: Boolean): List<IntentionAction> {
        if (sameTypeDiagnostics.isEmpty()) return emptyList()
        val first = sameTypeDiagnostics.first()

        if (first.psiElement.containingFile is CjCodeFragment && !isApplicableForCodeFragment()) {
            return emptyList()
        }

        if (sameTypeDiagnostics.size > 1 && createForAll) {
            assert(sameTypeDiagnostics.all { it.psiElement == first.psiElement && it.factory == first.factory }) {
                "It's expected to be the list of diagnostics of same type and for same element"
            }

            return doCreateActionsForAllProblems(sameTypeDiagnostics)
        }

        return sameTypeDiagnostics.flatMapTo(arrayListOf()) { doCreateActions(it) }
    }
}
