package com.huawei.cangjie.descriptors

import com.intellij.psi.PsiElement
import java.util.*


class DiagnosticWithParameters1<E : PsiElement, A>(
    psiElement: E,
    override val a: A,
    factory: DiagnosticFactory1<E, A>,
    severity: Severity
) : AbstractDiagnostic<E>(psiElement, factory, severity),
    DiagnosticWithParameters1Marker<A> {
    override val factory: DiagnosticFactory1<E, A>
        get() = super.factory as DiagnosticFactory1<E, A>

    override fun toString(): String {
        return "$factory(a = $a)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as DiagnosticWithParameters1<*, *>
        return a == that.a
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), a)
    }
}
