package com.linqingying.cangjie.diagnostics

import com.linqingying.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement

class DiagnosticFactory4<E : PsiElement , A:Any, B:Any, C:Any, D:Any> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters4<E, A, B, C, D> >(severity, positioningStrategy) {
    fun on(element: E, a: A, b: B, c: C, d: D): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters4(element, a, b, c, d, this, severity)
    }

    companion object {
        @JvmStatic
        fun <T : PsiElement , A:Any, B:Any, C:Any, D:Any> create(severity: Severity): DiagnosticFactory4<T, A, B, C, D> {
            return create(severity, PositioningStrategies.DEFAULT)
        }
        @JvmStatic
        fun <T : PsiElement , A:Any, B:Any, C:Any, D:Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory4<T, A, B, C, D> {
            return DiagnosticFactory4(severity, positioningStrategy)
        }
    }
}
