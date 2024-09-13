package com.huawei.cangjie.diagnostics;


import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DiagnosticWithParameters3<E extends PsiElement, A, B, C> extends AbstractDiagnostic<E> implements DiagnosticWithParameters3Marker<A, B, C> {
    private final A a;
    private final B b;
    private final C c;

    public DiagnosticWithParameters3(
            @NotNull E psiElement,
            @NotNull A a,
            @NotNull B b,
            @NotNull C c,
            @NotNull DiagnosticFactory3<E, A, B, C> factory,
            @NotNull Severity severity
    ) {
        super(psiElement, factory, severity);
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DiagnosticFactory3<E, A, B, C> getFactory() {
        return (DiagnosticFactory3<E, A, B, C>) super.getFactory();
    }

    @NotNull
    @Override
    public B getB() {
        return b;
    }

    @NotNull
    @Override
    public A getA() {
        return a;
    }

    @NotNull
    @Override
    public C getC() {
        return c;
    }

    @Override
    public String toString() {
        return getFactory() + "(a = " + a + ", b = " + b + ", c = " + c + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DiagnosticWithParameters3<?, ?, ?, ?> that = (DiagnosticWithParameters3<?, ?, ?, ?>) o;
        return Objects.equals(a, that.a) &&
                Objects.equals(b, that.b) &&
                Objects.equals(c, that.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), a, b, c);
    }
}
