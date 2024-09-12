package com.huawei.cangjie.descriptors

import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.diagnostics.Diagnostics
import com.huawei.cangjie.diagnostics.DiagnosticsElementsCache
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
