package com.huawei.cangjie.diagnostics;


import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class DiagnosticFactory0<E extends PsiElement> extends DiagnosticFactoryWithPsiElement<E, SimpleDiagnostic<E>> {

    protected DiagnosticFactory0(Severity severity, PositioningStrategy<? super E> positioningStrategy) {
        super(severity, positioningStrategy);
    }

    @NotNull
    public static <T extends PsiElement> DiagnosticFactory0<T> create(Severity severity) {
        return create(severity, PositioningStrategies.DEFAULT);
    }

    @NotNull
    public static <T extends PsiElement> DiagnosticFactory0<T> create(Severity severity, PositioningStrategy<? super T> positioningStrategy) {
        return new DiagnosticFactory0<>(severity, positioningStrategy);
    }

    @NotNull
    public SimpleDiagnostic<E> on(@NotNull E element) {
        return new SimpleDiagnostic<>(element, this, getSeverity());
    }
}
