package com.linqingying.cangjie.diagnostics

import com.intellij.psi.PsiElement
import java.util.*


class DiagnosticWithParameters2<E : PsiElement , A:Any, B :Any>(
    psiElement: E,
    override val a: A,
    override val b: B,
    factory: DiagnosticFactory2<E, A, B>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters2Marker<A, B> {
    override val factory: DiagnosticFactory2<E, A, B>
        get() = super.factory as DiagnosticFactory2<E, A, B>

    override fun toString(): String {
        return "$factory(a = $a, b = $b)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters2<*, *, *>
        return a == that.a &&
                b == that.b
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b)
    }
}
