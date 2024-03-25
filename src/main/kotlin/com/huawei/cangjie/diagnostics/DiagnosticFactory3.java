package com.huawei.cangjie.diagnostics;


import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class DiagnosticFactory3<E extends PsiElement, A, B, C> extends DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters3<E, A, B, C>> {

    protected DiagnosticFactory3(Severity severity, PositioningStrategy<? super E> positioningStrategy) {
        super(severity, positioningStrategy);
    }

    @NotNull
    public static <T extends PsiElement, A, B, C> DiagnosticFactory3<T, A, B, C> create(Severity severity) {
        return create(severity, PositioningStrategies.DEFAULT);
    }

    @NotNull
    public static <T extends PsiElement, A, B, C> DiagnosticFactory3<T, A, B, C> create(Severity severity, PositioningStrategy<? super T> positioningStrategy) {
        return new DiagnosticFactory3<>(severity, positioningStrategy);
    }

    @NotNull
    public ParametrizedDiagnostic<E> on(@NotNull E element, @NotNull A a, @NotNull B b, @NotNull C c) {
        return new DiagnosticWithParameters3<>(element, a, b, c, this, getSeverity());
    }
}
