package com.linqingying.cangjie.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


abstract class DiagnosticFactoryWithPsiElement<E : PsiElement, D : Diagnostic>(
    severity: Severity,
    val  positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactory<D>(severity) {


    fun getTextRanges(diagnostic: ParametrizedDiagnostic<E>): List<TextRange> {
        // TODO: it's strange that java requires cast here, because ParametrizedDiagnostic<E> inherits DiagnosticMarker
        return positioningStrategy.markDiagnostic(diagnostic)
    }

    fun isValid(diagnostic: ParametrizedDiagnostic<E>): Boolean {
        return positioningStrategy.isValid(diagnostic.psiElement)
    }




    fun cast(d: Diagnostic): D {
        return super.cast(d)
    }
}
