package com.huawei.cangjie.diagnostics.rendering;

import com.huawei.cangjie.descriptors.DiagnosticFactory;
import com.huawei.cangjie.diagnostics.DiagnosticFactory0;
import com.huawei.cangjie.diagnostics.DiagnosticFactory1;
import com.huawei.cangjie.diagnostics.DiagnosticFactory2;
import com.huawei.cangjie.diagnostics.DiagnosticFactory3;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DiagnosticFactoryToRendererMap {

    private final String name;
    public DiagnosticFactoryToRendererMap(String name) {
        this.name = name;
    }
    public DiagnosticFactoryToRendererMap() {
        this("<unnamed>");
    }
    @Override
    public String toString() {
        return "DiagnosticFactory#" + name;
    }
    private final Map<DiagnosticFactory<?>, DiagnosticRenderer<?>> map = new HashMap<>();

    private boolean immutable = false;
    @Nullable
    public DiagnosticRenderer<?> get(@NotNull DiagnosticFactory<?> factory) {
        return map.get(factory);
    }

    private void checkMutability() {
        if (immutable) {
            throw new IllegalStateException("factory to renderer map is already immutable");
        }
    }


    public <E extends PsiElement, A, B, C> void put(@NotNull DiagnosticFactory3<E, A, B, C> factory,
                                                    @NotNull String message,
                                                    @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                    @Nullable DiagnosticParameterRenderer<? super B> rendererB,
                                                    @Nullable  DiagnosticParameterRenderer<? super C> rendererC) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters3Renderer<A, B, C>(message, rendererA, rendererB, rendererC));
    }
    public <E extends PsiElement> void put(@NotNull DiagnosticFactory0<E> factory, @NotNull String message) {
        checkMutability();
        map.put(factory, new SimpleDiagnosticRenderer(message));
    }
    public <E extends PsiElement, A, B> void put(@NotNull DiagnosticFactory2<E, A, B> factory,
                                                 @NotNull String message,
                                                 @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                 @Nullable DiagnosticParameterRenderer<? super B> rendererB) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters2Renderer<A, B>(message, rendererA, rendererB));
    }
    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactory1<E, A> factory, @NotNull String message, @Nullable DiagnosticParameterRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters1Renderer<A>(message, rendererA));
    }
}
