package com.huawei.cangjie.diagnostics.rendering;

import com.huawei.cangjie.config.LanguageVersion;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.diagnostics.*;
import com.intellij.psi.PsiElement;
import kotlin. jvm. functions. Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class DiagnosticFactoryToRendererMap {
    private final Map<DiagnosticFactory<?>, DiagnosticRenderer<?>> map = new HashMap<>();
    // TO catch EA-75872
    private final String name;
    private boolean immutable = false;

    public DiagnosticFactoryToRendererMap(String name) {
        this.name = name;
    }

    public DiagnosticFactoryToRendererMap() {
        this("<unnamed>");
    }

    private static String deprecationMessage(DiagnosticFactoryForDeprecation<?, ?, ?> factory, String errorMessage) {
        LanguageVersion sinceVersion = factory.getDeprecatingFeature().getSinceVersion();
        StringBuilder builder = new StringBuilder();
        builder.append(errorMessage).append(". This will become an error");
        if (sinceVersion != null) {
            builder.append(" in Kotlin ").append(sinceVersion.getVersionString());
        } else {
            builder.append(" in a future release");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "DiagnosticFactory#" + name;
    }

    private void checkMutability() {
        if (immutable) {
            throw new IllegalStateException("factory to renderer map is already immutable");
        }
    }

    public <E extends PsiElement> void put(@NotNull DiagnosticFactory0<E> factory, @NotNull String message) {
        checkMutability();
        map.put(factory, new SimpleDiagnosticRenderer(message));
    }
    public <E extends PsiElement> void put(@NotNull DiagnosticFactory0<E> factory, @NotNull Function0<String> message) {
        checkMutability();
        map.put(factory, new SimpleDiagnosticRendererByFunction(message));
    }
    public <E extends PsiElement> void put(@NotNull DiagnosticFactory0<E> factory, @NotNull AstMsgData message) {
        checkMutability();
        map.put(factory, new SimpleDiagnosticRendererByAstMsgData(message));
    }
    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactory1<E, A> factory, @NotNull String message, @Nullable DiagnosticParameterRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters1Renderer<A>(message, rendererA));
    }

    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactory1<E, A> factory, @NotNull String message, @NotNull MultiRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory, new DiagnosticWithParametersMultiRenderer<A>(message, rendererA));
    }

    public <E extends PsiElement, A, B> void put(@NotNull DiagnosticFactory2<E, A, B> factory,
                                                 @NotNull String message,
                                                 @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                 @Nullable DiagnosticParameterRenderer<? super B> rendererB) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters2Renderer<A, B>(message, rendererA, rendererB));
    }

    public <E extends PsiElement, A, B, C> void put(@NotNull DiagnosticFactory3<E, A, B, C> factory,
                                                    @NotNull String message,
                                                    @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                    @Nullable DiagnosticParameterRenderer<? super B> rendererB,
                                                    @Nullable DiagnosticParameterRenderer<? super C> rendererC) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters3Renderer<A, B, C>(message, rendererA, rendererB, rendererC));
    }

    public <E extends PsiElement, A, B, C, D> void put(@NotNull DiagnosticFactory4<E, A, B, C, D> factory,
                                                       @NotNull String message,
                                                       @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                       @Nullable DiagnosticParameterRenderer<? super B> rendererB,
                                                       @Nullable DiagnosticParameterRenderer<? super C> rendererC,
                                                       @Nullable DiagnosticParameterRenderer<? super D> rendererD) {
        checkMutability();
        map.put(factory, new DiagnosticWithParameters4Renderer<A, B, C, D>(message, rendererA, rendererB, rendererC, rendererD));
    }

    public void put(@NotNull DiagnosticFactory<?> factory, @NotNull DiagnosticRenderer<?> renderer) {
        checkMutability();
        map.put(factory, renderer);
    }

    public <E extends PsiElement> void put(@NotNull DiagnosticFactoryForDeprecation0<E> factory, @NotNull String message) {
        checkMutability();
        map.put(factory.getErrorFactory(), new SimpleDiagnosticRenderer(message));
        map.put(factory.getWarningFactory(), new SimpleDiagnosticRenderer(deprecationMessage(factory, message)));
    }

    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactoryForDeprecation1<E, A> factory, @NotNull String message, @Nullable DiagnosticParameterRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory.getErrorFactory(), new DiagnosticWithParameters1Renderer<A>(message, rendererA));
        map.put(factory.getWarningFactory(), new DiagnosticWithParameters1Renderer<A>(deprecationMessage(factory, message), rendererA));
    }

    public <E extends PsiElement, A> void put(@NotNull DiagnosticFactoryForDeprecation1<E, A> factory, @NotNull String message, @NotNull MultiRenderer<? super A> rendererA) {
        checkMutability();
        map.put(factory.getErrorFactory(), new DiagnosticWithParametersMultiRenderer<A>(message, rendererA));
        map.put(factory.getWarningFactory(), new DiagnosticWithParametersMultiRenderer<A>(deprecationMessage(factory, message), rendererA));
    }

    public <E extends PsiElement, A, B> void put(@NotNull DiagnosticFactoryForDeprecation2<E, A, B> factory,
                                                 @NotNull String message,
                                                 @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                 @Nullable DiagnosticParameterRenderer<? super B> rendererB) {
        checkMutability();
        map.put(factory.getErrorFactory(), new DiagnosticWithParameters2Renderer<A, B>(message, rendererA, rendererB));
        map.put(factory.getWarningFactory(), new DiagnosticWithParameters2Renderer<A, B>(deprecationMessage(factory, message), rendererA, rendererB));
    }

    public <E extends PsiElement, A, B, C> void put(@NotNull DiagnosticFactoryForDeprecation3<E, A, B, C> factory,
                                                    @NotNull String message,
                                                    @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                    @Nullable DiagnosticParameterRenderer<? super B> rendererB,
                                                    @Nullable DiagnosticParameterRenderer<? super C> rendererC) {
        checkMutability();
        map.put(factory.getErrorFactory(), new DiagnosticWithParameters3Renderer<A, B, C>(message, rendererA, rendererB, rendererC));
        map.put(factory.getWarningFactory(), new DiagnosticWithParameters3Renderer<A, B, C>(deprecationMessage(factory, message), rendererA, rendererB, rendererC));
    }

    public <E extends PsiElement, A, B, C, D> void put(@NotNull DiagnosticFactoryForDeprecation4<E, A, B, C, D> factory,
                                                       @NotNull String message,
                                                       @Nullable DiagnosticParameterRenderer<? super A> rendererA,
                                                       @Nullable DiagnosticParameterRenderer<? super B> rendererB,
                                                       @Nullable DiagnosticParameterRenderer<? super C> rendererC,
                                                       @Nullable DiagnosticParameterRenderer<? super D> rendererD) {
        checkMutability();
        map.put(factory.getErrorFactory(), new DiagnosticWithParameters4Renderer<A, B, C, D>(message, rendererA, rendererB, rendererC, rendererD));
        map.put(factory.getWarningFactory(), new DiagnosticWithParameters4Renderer<A, B, C, D>(deprecationMessage(factory, message), rendererA, rendererB, rendererC, rendererD));
    }

    @Nullable
    public DiagnosticRenderer<?> get(@NotNull DiagnosticFactory<?> factory) {
        return map.get(factory);
    }

    public void setImmutable() {
        immutable = false;
    }
}
