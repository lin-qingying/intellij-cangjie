package com.huawei.cangjie.descriptors

import com.intellij.psi.PsiElement

open class DiagnosticFactory1<E : PsiElement, A> protected constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters1<E, A>>(
        severity, positioningStrategy
    ) {
    fun on(element: E, argument: A): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters1(element, argument, this, severity)
    }

    companion object {
        fun <T : PsiElement, A> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory1<T, A> {
            return DiagnosticFactory1(severity, positioningStrategy)
        }

        fun <T : PsiElement, A> create(severity: Severity): DiagnosticFactory1<T, A> {
            return create<T, A>(severity, PositioningStrategies.DEFAULT)
        }
    }
}
