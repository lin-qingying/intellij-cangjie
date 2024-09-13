package com.huawei.cangjie.diagnostics;


import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DiagnosticWithParameters2<E extends PsiElement, A, B> extends AbstractDiagnostic<E> implements DiagnosticWithParameters2Marker<A, B>{
    private final A a;
    private final B b;

    public DiagnosticWithParameters2(
            @NotNull E psiElement,
            @NotNull A a,
            @NotNull B b,
            @NotNull DiagnosticFactory2<E, A, B> factory,
            @NotNull Severity severity
    ) {
        super(psiElement, factory, severity);
        this.a = a;
        this.b = b;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DiagnosticFactory2<E, A, B> getFactory() {
        return (DiagnosticFactory2<E, A, B>) super.getFactory();
    }

    @NotNull
    @Override
    public A getA() {
        return a;
    }

    @NotNull
    @Override
    public B getB() {
        return b;
    }

    @Override
    public String toString() {
        return getFactory() + "(a = " + a + ", b = " + b + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DiagnosticWithParameters2<?, ?, ?> that = (DiagnosticWithParameters2<?, ?, ?>) o;
        return Objects.equals(a, that.a) &&
                Objects.equals(b, that.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), a, b);
    }
}
