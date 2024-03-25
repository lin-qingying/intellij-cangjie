package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.Diagnostic;
import com.huawei.cangjie.descriptors.MutableDiagnosticsWithSuppression;
import com.huawei.cangjie.utils.slicedMap.MutableSlicedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BindingContextUtils {

    @SuppressWarnings("unchecked")
    static void addOwnDataTo(
            @NotNull BindingTrace trace, @Nullable TraceEntryFilter filter, boolean commitDiagnostics,
            @NotNull MutableSlicedMap map, MutableDiagnosticsWithSuppression diagnostics
    ) {
        map.forEach((slice, key, value) -> {
            if (filter == null || filter.accept(slice, key)) {
                trace.record(slice, key, value);
            }

            return null;
        });

        if (!commitDiagnostics) return;

        for (Diagnostic diagnostic : diagnostics.getOwnDiagnostics()) {
            if (filter == null || filter.accept(null, diagnostic.getPsiElement())) {
                trace.report(diagnostic);
            }
        }

    }
}
