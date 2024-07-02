package com.huawei.cangjie.diagnostics;

import com.huawei.cangjie.descriptors.PositioningStrategies;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class DiagnosticFactory4<E extends PsiElement, A, B, C, D> extends DiagnosticFactoryWithPsiElement<E, DiagnosticWithParameters4<E, A, B, C, D>> {

    protected DiagnosticFactory4(Severity severity, PositioningStrategy<? super E> positioningStrategy) {
        super(severity, positioningStrategy);
    }

    @NotNull
    public static <T extends PsiElement, A, B, C, D> DiagnosticFactory4<T, A, B, C, D> create(Severity severity) {
        return create(severity, PositioningStrategies.DEFAULT);
    }

    @NotNull
    public static <T extends PsiElement, A, B, C, D> DiagnosticFactory4<T, A, B, C, D> create(Severity severity, PositioningStrategy<? super T> positioningStrategy) {
        return new DiagnosticFactory4<>(severity, positioningStrategy);
    }

    @NotNull
    public ParametrizedDiagnostic<E> on(@NotNull E element, @NotNull A a, @NotNull B b, @NotNull C c, @NotNull D d) {
        return new DiagnosticWithParameters4<>(element, a, b, c, d,this, getSeverity());
    }
}
