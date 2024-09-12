package com.huawei.cangjie.diagnostics

import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.util.containers.Interner
import com.intellij.util.containers.MultiMap

class DiagnosticsElementsCache(val diagnostics: Diagnostics, val filter: (Diagnostic) -> Boolean) {

    companion object {

        private fun buildElementToDiagnosticCache(
            diagnostics: Diagnostics,
            filter: (Diagnostic) -> Boolean
        ): MultiMap<PsiElement, Diagnostic> {
            val elementToDiagnostic: MultiMap<PsiElement, Diagnostic> =
                createConcurrentMultiMap()
            for (diagnostic in diagnostics) {
                if (filter.invoke(diagnostic)) {
                    elementToDiagnostic.putValue(diagnostic.psiElement, diagnostic)
                }
            }

            return elementToDiagnostic
        }
    }

    private val elementToDiagnostic: NotNullLazyValue<MultiMap<PsiElement, Diagnostic>> = NotNullLazyValue.atomicLazy {
        buildElementToDiagnosticCache(
            this.diagnostics,
            this.filter
        )
    }


    fun getDiagnostics(psiElement: PsiElement): MutableCollection<Diagnostic> {
        return elementToDiagnostic.value.get(psiElement)
    }


}

fun createStringInterner(): Interner<String> =
    Interner.createStringInterner()

fun <K, V> createConcurrentMultiMap(): MultiMap<K, V> =
    MultiMap.createConcurrent<K, V>()
