package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement


class DiagnosticFactory2<E : PsiElement , A:Any, B:Any> private constructor(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters2<E, A, B> >(severity, positioningStrategy) {
    fun on(element: E, a: A, b: B): ParametrizedDiagnostic<E> {
        return DiagnosticWithParameters2(element, a, b, this, severity)
    }

    companion object {
        @JvmStatic
        fun <T : PsiElement , A:Any, B:Any> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory2<T, A, B> {
            return DiagnosticFactory2(severity, positioningStrategy)
        }

        @JvmStatic
        fun <T : PsiElement , A:Any, B:Any> create(severity: Severity): DiagnosticFactory2<T, A, B> {
            return DiagnosticFactory2(severity, PositioningStrategies.DEFAULT)
        }
    }
}
