package com.huawei.cangjie.diagnostics.rendering;

import com.huawei.cangjie.diagnostics.Diagnostic;
import org.jetbrains.annotations.NotNull;

public class SimpleDiagnosticRenderer implements DiagnosticRenderer<Diagnostic> {
    private final String message;

    public SimpleDiagnosticRenderer(@NotNull String message) {
        this.message = message;
    }

    @NotNull
    @Override
    public String render(@NotNull Diagnostic diagnostic) {
        return message;
    }

    @NotNull
    @Override
    public Object[] renderParameters(@NotNull Diagnostic diagnostic) {
        return new Object[0];
    }
}
