package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.Diagnostic;
import com.huawei.cangjie.descriptors.MutableDiagnosticsWithSuppression;
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.types.util.TypeUtils;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.slicedMap.MutableSlicedMap;
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BindingContextUtils {

    @Nullable
    public static CangJieType updateRecordedType(
            @Nullable CangJieType type,
            @NotNull CjExpression expression,
            @NotNull BindingTrace trace,
            boolean shouldBeMadeNullable
    ) {
        if (type == null) return null;
        if (shouldBeMadeNullable) {
            type = TypeUtils.makeNullable(type);

        }
        trace.recordType(expression, type);
        return type;
    }

    @NotNull
    public static <K, V> V getNotNull(
            @NotNull BindingContext bindingContext,
            @NotNull ReadOnlySlice<K, V> slice,
            @NotNull K key
    ) {
        return getNotNull(bindingContext, slice, key, "Value at " + slice + " must not be null for " + key);
    }

    @NotNull
    public static <K, V> V getNotNull(
            @NotNull BindingContext bindingContext,
            @NotNull ReadOnlySlice<K, V> slice,
            @NotNull K key,
            @NotNull String messageIfNull
    ) {
        V value = bindingContext.get(slice, key);
        if (value == null) {
            throw new IllegalStateException(messageIfNull);
        }
        return value;
    }

    @Nullable
    public static CangJieTypeInfo getRecordedTypeInfo(@NotNull CjExpression expression, @NotNull BindingContext context) {
        // noinspection ConstantConditions
        if (context.get(BindingContext.PROCESSED, expression) != Boolean.TRUE) return null;
        // NB: should never return null if expression is already processed
        CangJieTypeInfo result = context.get(BindingContext.EXPRESSION_TYPE_INFO, expression);
        return result != null ? result : TypeInfoFactoryKt.noTypeInfo(DataFlowInfoFactory.EMPTY);
    }

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

    public static void recordFunctionDeclarationToDescriptor(@NotNull BindingTrace trace,
                                                             @NotNull PsiElement psiElement, @NotNull SimpleFunctionDescriptor function) {
        trace.record(BindingContext.FUNCTION, psiElement, function);
    }
}
