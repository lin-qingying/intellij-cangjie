package com.linqingying.cangjie.diagnostics

import com.linqingying.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement


class DiagnosticFactory3<E : PsiElement, A:Any, B:Any, C:Any> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters3<E, A, B, C> >(severity, positioningStrategy) {
    fun on(element: E, a: A, b: B, c: C): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters3(element, a, b, c, this, severity)
    }

    companion object {
        @JvmStatic
        fun <T : PsiElement, A:Any, B:Any, C:Any> create(severity: Severity): DiagnosticFactory3<T, A, B, C> {
            return create(severity, PositioningStrategies.DEFAULT)
        }
        @JvmStatic
        fun <T : PsiElement, A:Any, B:Any, C:Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory3<T, A, B, C> {
            return DiagnosticFactory3(severity, positioningStrategy)
        }
    }
}
