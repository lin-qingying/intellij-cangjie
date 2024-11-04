package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.Diagnostics
import com.linqingying.cangjie.diagnostics.DiagnosticsElementsCache
import com.intellij.psi.PsiElement

class SimpleDiagnostics(diagnostics: Collection<Diagnostic>) : SimpleGenericDiagnostics<Diagnostic>(diagnostics),
    Diagnostics {
    //copy to prevent external change
    private val diagnostics = ArrayList(diagnostics)

    @Suppress("UNCHECKED_CAST")
    private val elementsCache = DiagnosticsElementsCache(this) { true }

    override fun all() = diagnostics

    override fun forElement(psiElement: PsiElement): MutableCollection<Diagnostic> = elementsCache.getDiagnostics(psiElement)

    override fun noSuppression() = this
}
