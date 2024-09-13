package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.descriptors.PositioningStrategies
import com.intellij.psi.PsiElement


class DiagnosticFactory0<E : PsiElement>(
    severity: Severity,
    positioningStrategy: PositioningStrategy<E>
) :
    DiagnosticFactoryWithPsiElement<E, SimpleDiagnostic<E>>(severity, positioningStrategy) {
    fun on(element: E): SimpleDiagnostic<E> {
        return SimpleDiagnostic(element, this, severity)
    }

    companion object {
        @JvmStatic
        fun <T : PsiElement> create(severity: Severity): DiagnosticFactory0<T> {
            return create(severity, PositioningStrategies.DEFAULT)
        }
        @JvmStatic
        fun <T : PsiElement> create(
            severity: Severity,
            positioningStrategy: PositioningStrategy<T>
        ): DiagnosticFactory0<T> {
            return DiagnosticFactory0(severity, positioningStrategy)
        }
    }
}
