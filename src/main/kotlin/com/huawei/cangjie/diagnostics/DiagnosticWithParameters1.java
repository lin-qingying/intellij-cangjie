package com.huawei.cangjie.diagnostics;


import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DiagnosticWithParameters1<E extends PsiElement, A> extends AbstractDiagnostic<E> implements DiagnosticWithParameters1Marker<A> {
    private final A a;

    public DiagnosticWithParameters1(
            @NotNull E psiElement,
            @NotNull A a,
            @NotNull DiagnosticFactory1<E, A> factory,
            @NotNull Severity severity
    ) {
        super(psiElement, factory, severity);
        this.a = a;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public DiagnosticFactory1<E, A> getFactory() {
        return (DiagnosticFactory1<E, A>) super.getFactory();
    }

    @NotNull
    @Override
    public A getA() {
        return a;
    }

    @Override
    public String toString() {
        return getFactory() + "(a = " + a + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DiagnosticWithParameters1<?, ?> that = (DiagnosticWithParameters1<?, ?>) o;
        return Objects.equals(a, that.a);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), a);
    }
}
