package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.CangJieSuppressCache
import com.huawei.cangjie.descriptors.SimpleDiagnostics
import com.intellij.psi.PsiElement

class DiagnosticsWithSuppression(val suppressCache: CangJieSuppressCache, val diagnostics: Collection<Diagnostic>

) :
    Diagnostics {
    val elementsCache =  DiagnosticsElementsCache(this, suppressCache.filter)

    //    val elementsCache = DiagnosticsElementsCache(this, suppressCache.filter)
    override fun all(): Collection<Diagnostic> {
//        return diagnostics.filter(suppressCache.filter)
        return diagnostics.filter(suppressCache.filter)



    }

    override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
        return elementsCache.getDiagnostics(psiElement)


    }

    override fun noSuppression(): Diagnostics {
        return SimpleDiagnostics(diagnostics)

    }

}
