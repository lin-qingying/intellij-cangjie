package com.huawei.cangjie.diagnostics;


import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class SimpleDiagnostic<E extends PsiElement> extends AbstractDiagnostic<E> {
    public SimpleDiagnostic(
            @NotNull E psiElement,
            @NotNull DiagnosticFactory0<E> factory,
            @NotNull Severity severity
    ) {
        super(psiElement, factory, severity);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DiagnosticFactory0<E> getFactory() {
        return (DiagnosticFactory0<E>) super.getFactory();
    }
}
