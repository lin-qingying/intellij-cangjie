package com.huawei.cangjie.diagnostics

import com.intellij.psi.PsiElement
import java.util.*


class DiagnosticWithParameters4<E : PsiElement , A:Any, B:Any, C:Any, D:Any>(
    psiElement: E,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    factory: DiagnosticFactory4<E, A, B, C, D>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters4Marker<A, B, C, D> {
    override val factory: DiagnosticFactory4<E, A, B, C, D>
        get() = super.factory as DiagnosticFactory4<E, A, B, C, D>

    override fun toString(): String {
        return "$factory(a = $a, b = $b, c = $c, d = $d)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters4<*, *, *, *, *>
        return a == that.a &&
                b == that.b &&
                c == that.c &&
                d == that.d
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a, b, c, d)
    }
}
