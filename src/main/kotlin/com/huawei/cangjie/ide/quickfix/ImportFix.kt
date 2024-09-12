package com.huawei.cangjie.ide.quickfix

import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.psi.PsiElement

@OptIn(IntellijInternalApi::class)
internal class ImportFix(expression: CjSimpleNameExpression) : AbstractImportFix(expression, MyFactory) {
    override fun elementsToCheckDiagnostics(): Collection<PsiElement> {
        val expression = element ?: return emptyList()
        return listOfNotNull(expression, expression.parent?.takeIf { it is CjCallExpression })
    }

    companion object MyFactory : FactoryWithUnresolvedReferenceQuickFix() {
        override fun areActionsAvailable(diagnostic: Diagnostic): Boolean {
            val expression = expression(diagnostic)
            return expression != null && expression.references.isNotEmpty()
        }

        override fun createImportAction(diagnostic: Diagnostic): ImportFix? =
            expression(diagnostic)?.let(::ImportFix)

        private fun expression(diagnostic: Diagnostic): CjSimpleNameExpression? =
            when (val element = diagnostic.psiElement) {
                is CjSimpleNameExpression -> element
                is CjCallExpression -> element.calleeExpression
                else -> null
            } as? CjSimpleNameExpression
    }
}
