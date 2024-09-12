package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.GenericDiagnostics
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement

interface Diagnostics: GenericDiagnostics<Diagnostic> {
    val modificationTracker: ModificationTracker
        get() = throw IllegalStateException("Trying to obtain modification tracker for Diagnostics object of class ${this::class.java}")

    override fun all(): Collection<Diagnostic>

    override fun isEmpty(): Boolean = all().isEmpty()

    override fun iterator(): Iterator<Diagnostic> = all().iterator()

    fun forElement(psiElement: PsiElement): Collection<Diagnostic>

    fun noSuppression(): Diagnostics

    fun setCallback(callback: DiagnosticSink.DiagnosticsCallback) {
        setCallbackIfNotSet(callback)
    }

    fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean = false

    fun resetCallback() {}

    companion object {
        val EMPTY: Diagnostics = object : Diagnostics {
            override fun noSuppression(): Diagnostics = this
            override val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
            override fun all() = listOf<Diagnostic>()
            override fun forElement(psiElement: PsiElement) = listOf<Diagnostic>()
        }
    }
}
