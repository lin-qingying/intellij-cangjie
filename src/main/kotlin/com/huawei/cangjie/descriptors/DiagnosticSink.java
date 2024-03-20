package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.descriptors.rendering.DefaultErrorMessages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DiagnosticSink {
    DiagnosticSink DO_NOTHING = new DiagnosticSink() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
        }

        @Override
        public boolean wantsDiagnostics() {
            return false;
        }
    };

    DiagnosticSink THROW_EXCEPTION = new DiagnosticSink() {
        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                PsiFile psiFile = diagnostic.getPsiFile();
                List<TextRange> textRanges = diagnostic.getTextRanges();
                String diagnosticText = DefaultErrorMessages.render(diagnostic);
                throw new IllegalStateException(diagnostic.getFactory().getName() + ": " + diagnosticText + " " + PsiDiagnosticUtils
                        .atLocation(psiFile, textRanges.get(0)));
            }
        }

        @Override
        public boolean wantsDiagnostics() {
            return true;
        }
    };
    interface DiagnosticsCallback {
        void callback(Diagnostic diagnostic);
    }

    void report(@NotNull Diagnostic diagnostic);

    /**
     * use {@link #setCallbackIfNotSet(DiagnosticsCallback)} instead
     * @param callback
     */
    @Deprecated
    default void setCallback(@NotNull DiagnosticsCallback callback) {
        setCallbackIfNotSet(callback);
    }

    default boolean setCallbackIfNotSet(@NotNull DiagnosticsCallback callback) {
        return false;
    }

    default void resetCallback() { }

    boolean wantsDiagnostics();
}
