/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.resolve;

import com.google.common.collect.Lists;

import com.intellij.psi.PsiElement;
import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor;
import com.linqingying.cangjie.diagnostics.Diagnostic;
import com.linqingying.cangjie.psi.CjElement;
import com.linqingying.cangjie.psi.CjExpression;
import com.linqingying.cangjie.psi.CjQualifiedExpression;
import com.linqingying.cangjie.psi.CjSimpleNameExpression;
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory;
import com.linqingying.cangjie.resolve.calls.util.CallUtilKt;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.linqingying.cangjie.types.util.TypeUtils;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
import com.linqingying.cangjie.utils.slicedMap.MutableSlicedMap;
import com.linqingying.cangjie.utils.slicedMap.ReadOnlySlice;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.linqingying.cangjie.diagnostics.Errors.AMBIGUOUS_LABEL;
import static com.linqingying.cangjie.resolve.BindingContext.AMBIGUOUS_LABEL_TARGET;

public class BindingContextUtils {
    @Nullable
    public static VariableDescriptor extractVariableDescriptorFromReference(
            @NotNull BindingContext bindingContext,
            @Nullable CjElement element
    ) {
        if (element instanceof CjSimpleNameExpression) {
            return variableDescriptorForDeclaration(bindingContext.get(BindingContext.REFERENCE_TARGET, (CjSimpleNameExpression) element));
        } else if (element instanceof CjQualifiedExpression) {
            return extractVariableDescriptorFromReference(bindingContext, ((CjQualifiedExpression) element).getSelectorExpression());
        }
        return null;
    }

    public static void reportAmbiguousLabel(
            @NotNull BindingTrace trace,
            @NotNull CjSimpleNameExpression targetLabel,
            @NotNull Collection<DeclarationDescriptor> declarationsByLabel
    ) {
        Collection<PsiElement> targets = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : declarationsByLabel) {
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
            assert element != null : "Label can only point to something in the same lexical scope";
            targets.add(element);
        }
        if (!targets.isEmpty()) {
            trace.record(AMBIGUOUS_LABEL_TARGET, targetLabel, targets);
        }
        trace.report(AMBIGUOUS_LABEL.on(targetLabel));
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

    /**
     * 将自定义数据添加到给定的trace对象中
     * 此方法遍历一个映射，根据条件将数据记录到trace中，并可选择性地提交诊断信息
     *
     * @param trace             BindingTrace对象，用于记录数据和诊断信息
     * @param filter            TraceEntryFilter对象，用于过滤哪些数据应被记录如果为null，则不进行过滤
     * @param commitDiagnostics 指示是否应提交诊断信息的布尔值
     * @param map               MutableSlicedMap对象，包含要添加到trace的数据
     * @param diagnostics       MutableDiagnosticsWithSuppression对象，包含可能要提交的诊断信息
     */
    @SuppressWarnings("unchecked")
    static void addOwnDataTo(
            @NotNull BindingTrace trace, @Nullable TraceEntryFilter filter, boolean commitDiagnostics,
            @NotNull MutableSlicedMap map, MutableDiagnosticsWithSuppression diagnostics
    ) {
        // 遍历map中的每个条目，根据filter的条件将数据记录到trace中
        map.forEach((slice, key, value) -> {
            // 如果filter为null或当前条目通过filter的检验，则记录该条目
            if (filter == null || filter.accept(slice, key)) {
                trace.record(slice, key, value);
            }

            return null;
        });

        // 如果不提交诊断信息，则直接返回
        if (!commitDiagnostics) return;

        // 遍历诊断信息，根据filter的条件将诊断信息提交到trace中
        for (Diagnostic diagnostic : diagnostics.getOwnDiagnostics()) {
            // 如果filter为null或当前诊断信息通过filter的检验，则提交该诊断信息
            if (filter == null || filter.accept(null, diagnostic.getPsiElement())) {
                trace.report(diagnostic);
            }
        }
    }
    public static void recordMacroDeclarationToDescriptor(@NotNull BindingTrace trace,
                                                             @NotNull PsiElement psiElement, @NotNull MacroDescriptor macroDescriptor) {
        trace.record(BindingContext.MACRO, psiElement, macroDescriptor);
    }
    public static void recordFunctionDeclarationToDescriptor(@NotNull BindingTrace trace,
                                                             @NotNull PsiElement psiElement, @NotNull SimpleFunctionDescriptor function) {
        trace.record(BindingContext.FUNCTION, psiElement, function);
    }

    public static <K, V> void removeBySlice(ReadOnlySlice<K, V> slice, K key, @NotNull BindingTrace trace) {
        if (trace instanceof DelegatingBindingTrace) {
            ((DelegatingBindingTrace) trace).removeBySlice(slice, key);
        }
    }

    public static void remove(CjElement key, @NotNull BindingTrace trace) {
        if (trace instanceof DelegatingBindingTrace) {
            ((DelegatingBindingTrace) trace).remove(key);
        }
    }
}
