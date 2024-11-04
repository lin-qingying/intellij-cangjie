package com.linqingying.cangjie.diagnostics

import com.intellij.psi.PsiElement
import java.util.*


class DiagnosticWithParameters3<E : PsiElement , A:Any, B:Any, C:Any>(
    psiElement: E,
    override val a: A,
    override val b: B,
    override val c: C,
    factory: DiagnosticFactory3<E, A, B, C>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters3Marker<A, B, C> {
    override val factory: DiagnosticFactory3<E, A, B, C>
        get() = super.factory as DiagnosticFactory3<E, A, B, C>

    override fun toString(): String {
        return "$factory(a = $a, b = $b, c = $c)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters3<*, *, *, *>
        return a == that.a &&
                b == that.b &&
                c == that.c
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b, c)
    }
}
