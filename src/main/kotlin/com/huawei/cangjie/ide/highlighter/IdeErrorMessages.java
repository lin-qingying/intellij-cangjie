package com.huawei.cangjie.ide.highlighter;


import com.huawei.cangjie.diagnostics.UnboundDiagnostic;
import com.huawei.cangjie.diagnostics.rendering.DefaultErrorMessages;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import com.huawei.cangjie.diagnostics.rendering.DiagnosticRenderer;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public final class IdeErrorMessages {
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("IDE");

    // TODO: i18n
    @NlsSafe
    @NotNull
    public static String render(@NotNull UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = MAP.get(diagnostic.getFactory());

        if (renderer != null) {
            //noinspection unchecked
            return renderer.render(diagnostic);
        }

        return DefaultErrorMessages.render(diagnostic);
    }

    @TestOnly
    public static boolean hasIdeSpecificMessage(@NotNull UnboundDiagnostic diagnostic) {
        return MAP.get(diagnostic.getFactory()) != null;
    }

//    static {

    private IdeErrorMessages() {}
}
