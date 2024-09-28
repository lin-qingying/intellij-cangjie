package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement

class   DiagnosticFactory1<E : PsiElement , A:Any>    (
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters1<E, A> >(severity, positioningStrategy) {
    fun on(element: E, argument: A): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters1(element, argument, this, severity)
    }

    companion object {
        @JvmStatic
        fun <T : PsiElement , A:Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory1<T, A> {
            return DiagnosticFactory1(severity, positioningStrategy)
        }

        @JvmStatic
        fun <T : PsiElement , A:Any> create(severity: Severity): DiagnosticFactory1<T, A> {
            return create(severity, PositioningStrategies.DEFAULT)
        }
    }
}
