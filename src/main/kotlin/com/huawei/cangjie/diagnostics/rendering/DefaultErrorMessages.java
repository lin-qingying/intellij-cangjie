package com.huawei.cangjie.diagnostics.rendering;

import com.huawei.cangjie.diagnostics.UnboundDiagnostic;
import com.huawei.cangjie.utils.AddToStdlibKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


import static com.huawei.cangjie.descriptors.Errors.*;
import static com.huawei.cangjie.diagnostics.rendering.Renderers.ELEMENT_TEXT;

public class DefaultErrorMessages {
    private static final List<DiagnosticFactoryToRendererMap> RENDERER_MAPS;

    @NotNull
    @SuppressWarnings("unchecked")
    public static String render(@NotNull UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = getRendererForDiagnostic(diagnostic);
        if (renderer != null) {
            return renderer.render(diagnostic);
        }
        return diagnostic + " (error: could not render message)";
    }

    @Nullable
    public static DiagnosticRenderer getRendererForDiagnostic(@NotNull UnboundDiagnostic diagnostic) {
        // firstNotNullOfOrNull from stdlib can not be used here because it is InlineOnly function and can not be accessed from Java
        @SuppressWarnings("deprecation")
        DiagnosticRenderer<?> renderer = AddToStdlibKt.firstNotNullResult(RENDERER_MAPS, map -> map.get(diagnostic.getFactory()));
        if (renderer != null)
            return renderer;
        else
            return diagnostic.getFactory().getDefaultRenderer();
    }

    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("Default");


    static {
        RENDERER_MAPS = List.of(MAP);

    }



    static {

        MAP.put(UNRESOLVED_REFERENCE(), "Unresolved reference: {0}", ELEMENT_TEXT);
    }
}
