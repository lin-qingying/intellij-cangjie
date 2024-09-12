package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.diagnostics.Diagnostic;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory;
import com.huawei.cangjie.resolve.calls.util.CallUtilKt;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.types.util.TypeUtils;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.huawei.cangjie.utils.slicedMap.MutableSlicedMap;
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BindingContextUtils {
    @Nullable
    public static VariableDescriptor extractVariableDescriptorFromReference(
            @NotNull BindingContext bindingContext,
            @Nullable CjElement element
    ) {
        if (element instanceof CjSimpleNameExpression) {
            return variableDescriptorForDeclaration(bindingContext.get(BindingContext.REFERENCE_TARGET, (CjSimpleNameExpression) element));
        }
        else if (element instanceof CjQualifiedExpression) {
            return extractVariableDescriptorFromReference(bindingContext, ((CjQualifiedExpression) element).getSelectorExpression());
        }
        return null;
    }

    @Nullable
    public static VariableDescriptor variableDescriptorForDeclaration(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof VariableDescriptor)
            return (VariableDescriptor) descriptor;
//        if (descriptor instanceof ClassDescriptor) {
//            return TowerLevelsKt.getFakeDescriptorForObject((ClassDescriptor) descriptor);
//        }
        return null;
    }
    @Nullable
    public static VariableDescriptor extractVariableFromResolvedCall(
            @NotNull BindingContext bindingContext,
            @Nullable CjElement callElement
    ) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilKt.getResolvedCall(callElement, bindingContext);
        if (resolvedCall == null || !(resolvedCall.getResultingDescriptor() instanceof VariableDescriptor)) return null;
        return (VariableDescriptor) resolvedCall.getResultingDescriptor();
    }
    @Nullable
    public static CangJieType updateRecordedType(
            @Nullable CangJieType type,
            @NotNull CjExpression expression,
            @NotNull BindingTrace trace,
            boolean shouldBeMadeNullable
    ) {
        if (type == null) return null;
        if (shouldBeMadeNullable) {
            type = TypeUtils.makeOptional(type);

        }
        trace.recordType(expression, type);
        return type;
    }
    @NotNull
    public static Pair<FunctionDescriptor, PsiElement> getContainingFunctionSkipFunctionLiterals(
            @Nullable DeclarationDescriptor startDescriptor,
            boolean strict
    ) {
        FunctionDescriptor containingFunctionDescriptor = DescriptorUtils.getParentOfType(startDescriptor, FunctionDescriptor.class, strict);
        PsiElement containingFunction = containingFunctionDescriptor != null ? DescriptorToSourceUtils.getSourceFromDescriptor(containingFunctionDescriptor) : null;
//        while (containingFunction instanceof CjFunctionLiteral) {
//            containingFunctionDescriptor = DescriptorUtils.getParentOfType(containingFunctionDescriptor, FunctionDescriptor.class);
//            containingFunction = containingFunctionDescriptor != null ? DescriptorToSourceUtils
//                    .getSourceFromDescriptor(containingFunctionDescriptor) : null;
//        }

        return new Pair<>(containingFunctionDescriptor, containingFunction);
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
