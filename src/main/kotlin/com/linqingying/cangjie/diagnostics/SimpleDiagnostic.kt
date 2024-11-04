package com.linqingying.cangjie.diagnostics

import com.intellij.psi.PsiElement


class SimpleDiagnostic<E : PsiElement >(
    psiElement: E,
    factory: DiagnosticFactory0<E>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity) {
    override val factory: DiagnosticFactory0<E>
        get() = super.factory as DiagnosticFactory0<E>
}
