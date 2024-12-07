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

package com.linqingying.cangjie.descriptors.impl;

import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;
import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.descriptors.annotations.AnnotationsKt;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.psi.CjFunction;
import com.linqingying.cangjie.psi.CjFunctionImpl;
import com.linqingying.cangjie.resolve.DescriptorFactory;
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor;
import com.linqingying.cangjie.resolve.scopes.receivers.ExtensionReceiver;
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitContextReceiver;
import com.linqingying.cangjie.resolve.source.CangJieSourceElement;
import com.linqingying.cangjie.resolve.source.PsiSourceElementKt;
import com.linqingying.cangjie.types.*;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class FunctionDescriptorImpl extends DeclarationDescriptorNonRootImpl implements FunctionDescriptor {
    private final FunctionDescriptor original;
    private final CallableMemberDescriptor.Kind kind;
    protected Map<CallableDescriptor.UserDataKey<?>, Object> userDataMap;
    private List<TypeParameterDescriptor> typeParameters;
    private List<ValueParameterDescriptor> unsubstitutedValueParameters = new ArrayList<>();
    private CangJieType unsubstitutedReturnType;
    private List<ReceiverParameterDescriptor> contextReceiverParameters;
    //    扩展接收器
    private ReceiverParameterDescriptor extensionReceiverParameter;
    private ReceiverParameterDescriptor dispatchReceiverParameter;
    private Modality modality = Modality.FINAL;
    private DescriptorVisibility visibility = DescriptorVisibilities.INTERNAL;
    private boolean isOperator;
    private boolean isStatic;
    private boolean isUnsafe;

    private boolean isExpect;
    private boolean isHiddenToOvercomeSignatureClash;
    private boolean isHiddenForResolutionEverywhereBesideSupercalls;
    private boolean hasStableParameterNames = true;
    private boolean hasSynthesizedParameterNames;

    private Collection<FunctionDescriptor> overriddenFunctions;
    private volatile Function0<Collection<FunctionDescriptor>> lazyOverriddenFunctionsTask;
    @Nullable
    private FunctionDescriptor initialSignatureDescriptor;

    protected FunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable FunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull CallableMemberDescriptor.Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, source);
        this.original = null == original ? this : original;
        this.kind = kind;
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(
            FunctionDescriptor substitutedDescriptor,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull TypeSubstitutor substitutor
    ) {
        return getSubstitutedValueParameters(substitutedDescriptor, unsubstitutedValueParameters, substitutor, false, false, null);
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(
            FunctionDescriptor substitutedDescriptor,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull TypeSubstitutor substitutor,
            boolean dropOriginal,
            boolean preserveSourceElement,
            @Nullable boolean[] wereChanges
    ) {
        List<ValueParameterDescriptor> result = new ArrayList<>(unsubstitutedValueParameters.size());
        for (ValueParameterDescriptor unsubstitutedValueParameter : unsubstitutedValueParameters) {
            // TODO : Lazy?
            CangJieType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType(), Variance.INVARIANT);
            CangJieType varargElementType = unsubstitutedValueParameter.getVarargElementType();
            CangJieType substituteVarargElementType =
                    null == varargElementType ? null : substitutor.substitute(varargElementType, Variance.INVARIANT);
            if (null == substitutedType) return null;
            if (substitutedType != unsubstitutedValueParameter.getType() || varargElementType != substituteVarargElementType) {
                if (null != wereChanges) {
                    wereChanges[0] = true;
                }
            }

            Function0<List<VariableDescriptor>> destructuringVariablesAction = getDestructuringVariablesAction(unsubstitutedValueParameter);

            result.add(
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                            substitutedDescriptor,
                            dropOriginal ? null : unsubstitutedValueParameter,
                            unsubstitutedValueParameter.getIndex(),
                            unsubstitutedValueParameter.getAnnotations(),
                            unsubstitutedValueParameter.getName(),
                            unsubstitutedValueParameter.isNamed(),
                            substitutedType,
                            unsubstitutedValueParameter.declaresDefaultValue(),

//                            substituteVarargElementType,
                            preserveSourceElement ? unsubstitutedValueParameter.getSource() : SourceElement.NO_SOURCE,
                            destructuringVariablesAction
                    )
            );
        }
        return result;
    }

    private static @Nullable Function0<List<VariableDescriptor>> getDestructuringVariablesAction(ValueParameterDescriptor unsubstitutedValueParameter) {
        Function0<List<VariableDescriptor>> destructuringVariablesAction = null;
        if (unsubstitutedValueParameter instanceof ValueParameterDescriptorImpl.WithDestructuringDeclaration) {
            List<VariableDescriptor> destructuringVariables =
                    ((ValueParameterDescriptorImpl.WithDestructuringDeclaration) unsubstitutedValueParameter)
                            .getDestructuringVariables();
            destructuringVariablesAction = () -> destructuringVariables;
        }
        return destructuringVariablesAction;
    }

    public void setExpect(boolean isExpect) {
        this.isExpect = isExpect;
    }

    public void setHasStableParameterNames(boolean hasStableParameterNames) {
        this.hasStableParameterNames = hasStableParameterNames;
    }

    @Override
    public boolean hasStableParameterNames() {
        return hasStableParameterNames;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getUserData(CallableDescriptor.UserDataKey<V> key) {
        if (null == userDataMap) return null;
        return (V) userDataMap.get(key);
    }

    public void setIsStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public void setIsOperator(boolean isOperator) {
        this.isOperator = isOperator;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getDispatchReceiverParameter() {
        return dispatchReceiverParameter;
    }

    @NotNull
    public FunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<ReceiverParameterDescriptor> contextReceiverParameters,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable CangJieType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull DescriptorVisibility visibility
    ) {
        this.typeParameters = CollectionsKt.toList(typeParameters);
        this.unsubstitutedValueParameters = CollectionsKt.toList(unsubstitutedValueParameters);


        this.unsubstitutedReturnType = unsubstitutedReturnType;
        this.modality = modality;
        this.visibility = visibility;
        this.extensionReceiverParameter = extensionReceiverParameter;
        this.dispatchReceiverParameter = dispatchReceiverParameter;
        this.contextReceiverParameters = contextReceiverParameters;

        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            if (typeParameterDescriptor.getIndex() != i) {
                throw new IllegalStateException(typeParameterDescriptor + " index is " + typeParameterDescriptor.getIndex() + " but position is " + i);
            }
        }

        for (int i = 0; i < unsubstitutedValueParameters.size(); ++i) {
            // TODO fill me
            final int firstValueParameterOffset = 0; // receiverParameter.exists() ? 1 : 0;
            ValueParameterDescriptor valueParameterDescriptor = unsubstitutedValueParameters.get(i);
            if (valueParameterDescriptor.getIndex() != i + firstValueParameterOffset) {
                throw new IllegalStateException(valueParameterDescriptor + "index is " + valueParameterDescriptor.getIndex() + " but position is " + i);
            }
        }

        return this;
    }

    @Override
    public boolean getIsExtend() {
        if (null == dispatchReceiverParameter) return false;
        return dispatchReceiverParameter.getContainingDeclaration() instanceof LazyExtendClassDescriptor;

    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getContextReceiverParameters() {
        return Objects.requireNonNullElse(contextReceiverParameters, Collections.emptyList());
    }


    @Nullable
    @Override
    public ReceiverParameterDescriptor getExtensionReceiverParameter() {
        return extensionReceiverParameter;
    }

    public void setExtensionReceiverParameter(@NotNull ReceiverParameterDescriptor extensionReceiverParameter) {
        this.extensionReceiverParameter = extensionReceiverParameter;
    }

    @NotNull
    @Override
    public Collection<? extends FunctionDescriptor> getOverriddenDescriptors() {
        performOverriddenLazyCalculationIfNeeded();
        return null != overriddenFunctions ? overriddenFunctions : Collections.emptyList();
    }


    /**
     * 重写规则  this 表示的该对象 (已经重写的对象，open修饰): 注意 这里的open
     *
     * @param overriddenDescriptors 被重写方法，也就是抽象方法
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {

        if (getContainingDeclaration() instanceof LazyExtendClassDescriptor && overriddenDescriptors.isEmpty()) {
            return;
        }
        overriddenFunctions = new ArrayList<>();

        for (FunctionDescriptor function : (Collection<? extends FunctionDescriptor>) overriddenDescriptors) {
            if (!function.getIsExtend()) {
                overriddenFunctions.add(function);
                if (function.isHiddenForResolutionEverywhereBesideSupercalls()) {
                    isHiddenForResolutionEverywhereBesideSupercalls = true;
                    break;
                }
            }

        }
    }

    private void performOverriddenLazyCalculationIfNeeded() {
        Function0<Collection<FunctionDescriptor>> overriddenTask = lazyOverriddenFunctionsTask;
        if (null != overriddenTask) {
            overriddenFunctions = overriddenTask.invoke();
            // Here it's important that this assignment is strictly after previous one
            // `lazyOverriddenFunctionsTask` is volatile, so when someone will see that it's null,
            // he can read consistent collection from `overriddenFunctions`,
            // because it's assignment happens-before of "lazyOverriddenFunctionsTask = null"
            lazyOverriddenFunctionsTask = null;
        }
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    public void setModality(@NotNull Modality modality) {
        this.modality = modality;
    }

    @NotNull
    @Override
    public DescriptorVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(@NotNull DescriptorVisibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public boolean isStatic() {


        PsiElement element = PsiSourceElementKt.getPsi(getSource());
        if (element instanceof CjFunction) {
            isStatic = ((CjFunction) element).isStatic();
        }
        return isStatic;

    }

    @Override
    public boolean isUnsafe() {
        PsiElement element = PsiSourceElementKt.getPsi(getSource());
        if (element instanceof CjFunction) {
            isUnsafe = ((CjFunction) element).isUnsafe();
        }
        return isUnsafe;
    }


    @Override
    public boolean isOperator() {
        if (isOperator) return true;

        for (FunctionDescriptor descriptor : getOriginal().getOverriddenDescriptors()) {
            if (descriptor.isOperator()) return true;
        }

        return false;
    }


    public void setOperator(boolean isOperator) {
        this.isOperator = isOperator;
    }


    @Override
    public boolean isHiddenToOvercomeSignatureClash() {
        return isHiddenToOvercomeSignatureClash;
    }


    private void setHiddenToOvercomeSignatureClash(boolean hiddenToOvercomeSignatureClash) {
        isHiddenToOvercomeSignatureClash = hiddenToOvercomeSignatureClash;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        return hasSynthesizedParameterNames;
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        List<TypeParameterDescriptor> parameters = typeParameters;
        // Diagnostics for EA-141456
        if (null == parameters) {
            throw new IllegalStateException("typeParameters == null for " + this);
        }
        return parameters;
    }

    @Override
    public @NotNull List<TypeParameterDescriptor> getTypeParametersNotExtend() {
        List<TypeParameterDescriptor> parameters = typeParameters;
        // Diagnostics for EA-141456
        if (null == parameters) {
            throw new IllegalStateException("typeParameters == null for " + this);
        }

        if (1 == parameters.size() && null != extensionReceiverParameter && null == ((CjFunctionImpl) ((CangJieSourceElement) getSource()).getPsi()).getOriginalTypeParameterList()) {
            return Collections.emptyList();
        }
        return parameters;
    }

    @Override
    public void validate() {
        getTypeParameters();
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return unsubstitutedValueParameters;
    }


    @Override
    public CangJieType getReturnType() {


        return unsubstitutedReturnType;
    }

    public void setReturnType(@NotNull CangJieType unsubstitutedReturnType) {
//        if (this.unsubstitutedReturnType != null) {
        // TODO: uncomment and fix tests
        //throw new IllegalStateException("returnType already set");
//        }
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @NotNull
    @Override
    public CallableMemberDescriptor.Kind getKind() {
        return kind;
    }

    @Override
    public FunctionDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }

        return newCopyBuilder(originalSubstitutor)
                .setOriginal(getOriginal())
                .setPreserveSourceElement()
                .setJustForTypeSubstitution(true)
                .build();
    }

    @Nullable
    private CangJieType getExtensionReceiverParameterType() {
        if (null == extensionReceiverParameter) return null;
        return extensionReceiverParameter.getType();
    }

    @Override
    public boolean isHiddenForResolutionEverywhereBesideSupercalls() {
        return isHiddenForResolutionEverywhereBesideSupercalls;
    }

    private void setHiddenForResolutionEverywhereBesideSupercalls(boolean hiddenForResolutionEverywhereBesideSupercalls) {
        isHiddenForResolutionEverywhereBesideSupercalls = hiddenForResolutionEverywhereBesideSupercalls;
    }

    @Override
    @NotNull
    public FunctionDescriptor.CopyBuilder<? extends FunctionDescriptor> newCopyBuilder() {
        return newCopyBuilder(TypeSubstitutor.EMPTY);
    }

    @NotNull
    protected CopyConfiguration newCopyBuilder(@NotNull TypeSubstitutor substitutor) {
        return new CopyConfiguration(
                substitutor.getSubstitution(),
                getContainingDeclaration(), modality, getVisibility(), kind, getValueParameters(), getContextReceiverParameters(),
                extensionReceiverParameter, getReturnType(), null);
    }

    /**
     * 替换函数描述符中的各种参数和类型。
     *
     * @param configuration 替换配置对象，包含新的扩展接收者参数、分发接收者参数、值参数描述符等。
     * @return 替换后的函数描述符，如果替换过程中出现错误则返回 null。
     */
    @Nullable
    protected FunctionDescriptor doSubstitute(@NotNull CopyConfiguration configuration) {
        boolean[] wereChanges = new boolean[1];

        // 合并原始注解和附加注解
        Annotations resultAnnotations =
                null != configuration.additionalAnnotations
                        ? AnnotationsKt.composeAnnotations(getAnnotations(), configuration.additionalAnnotations)
                        : getAnnotations();

        // 创建一个新的函数描述符副本
        FunctionDescriptorImpl substitutedDescriptor = createSubstitutedCopy(
                configuration.newOwner, configuration.original, configuration.kind, configuration.name, resultAnnotations,
                getSourceToUseForCopy(configuration.preserveSourceElement, configuration.original));

        // 获取未替换的类型参数
        List<TypeParameterDescriptor> unsubstitutedTypeParameters =
                null == configuration.newTypeParameters ? getTypeParameters() : configuration.newTypeParameters;

        wereChanges[0] |= !unsubstitutedTypeParameters.isEmpty();

        // 替换类型参数
        List<TypeParameterDescriptor> substitutedTypeParameters =
                new ArrayList<>(unsubstitutedTypeParameters.size());
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(
                unsubstitutedTypeParameters, configuration.substitution, substitutedDescriptor, substitutedTypeParameters, wereChanges
        );
        if (null == substitutor) return null;

        // 替换上下文接收者参数
        List<ReceiverParameterDescriptor> substitutedContextReceiverParameters = new ArrayList<>();
        if (!configuration.newContextReceiverParameters.isEmpty()) {
            int index = 0;
            for (ReceiverParameterDescriptor newContextReceiverParameter : configuration.newContextReceiverParameters) {
                CangJieType substitutedContextReceiverType =
                        substitutor.substitute(newContextReceiverParameter.getType(), Variance.INVARIANT);
                if (null == substitutedContextReceiverType) {
                    return null;
                }
                ReceiverParameterDescriptor substitutedContextReceiverParameter =
                        DescriptorFactory.createContextReceiverParameterForCallable(substitutedDescriptor, substitutedContextReceiverType,
                                ((ImplicitContextReceiver) newContextReceiverParameter.getValue()).getCustomLabelName(),
                                newContextReceiverParameter.getAnnotations(),
                                index);
                index++;
                substitutedContextReceiverParameters.add(substitutedContextReceiverParameter);

                wereChanges[0] |= substitutedContextReceiverType != newContextReceiverParameter.getType();
            }
        }

        // 替换扩展接收者参数
        ReceiverParameterDescriptor substitutedReceiverParameter = null;
        if (null != configuration.newExtensionReceiverParameter) {
            CangJieType substitutedExtensionReceiverType =
                    substitutor.substitute(configuration.newExtensionReceiverParameter.getType(), Variance.INVARIANT);
            if (null == substitutedExtensionReceiverType) {
                return null;
            }
            substitutedReceiverParameter = new ReceiverParameterDescriptorImpl(
                    substitutedDescriptor,
                    new ExtensionReceiver(
                            substitutedDescriptor, substitutedExtensionReceiverType, configuration.newExtensionReceiverParameter.getValue()
                    ),
                    configuration.newExtensionReceiverParameter.getAnnotations()
            );

            wereChanges[0] |= substitutedExtensionReceiverType != configuration.newExtensionReceiverParameter.getType();
        }

        // 替换分发接收者参数
        ReceiverParameterDescriptor substitutedExpectedThis = null;
        if (null != configuration.dispatchReceiverParameter) {
            // 当生成假覆盖成员时，其分发接收者参数类型应为基类类型，这是正确的。
            // 例如：
            // class Base { fun foo() }
            // class Derived <: Base
            // let x: Base
            // if (x is Derived) {
            //    // `x` 不应被标记为智能转换
            //    // 但如果假覆盖的 `foo` 有 `Derived` 作为其分发接收者参数类型，则会标记为智能转换
            //    x.foo()
            // }
            substitutedExpectedThis = configuration.dispatchReceiverParameter.substitute(substitutor);
            if (null == substitutedExpectedThis) {
                return null;
            }

            wereChanges[0] |= substitutedExpectedThis != configuration.dispatchReceiverParameter;
        }

        // 替换值参数
        List<ValueParameterDescriptor> substitutedValueParameters = getSubstitutedValueParameters(
                substitutedDescriptor, configuration.newValueParameterDescriptors, substitutor, configuration.dropOriginalInContainingParts,
                configuration.preserveSourceElement, wereChanges
        );
        if (null == substitutedValueParameters) {
            return null;
        }

        // 替换返回类型
        CangJieType substitutedReturnType = substitutor.substitute(configuration.newReturnType, Variance.INVARIANT);
        if (null == substitutedReturnType) {
            return null;
        }

        wereChanges[0] |= substitutedReturnType != configuration.newReturnType;

        // 如果没有变化且仅用于类型替换，则返回当前描述符
        if (!wereChanges[0] && configuration.justForTypeSubstitution) {
            return this;
        }

        // 初始化替换后的描述符
        substitutedDescriptor.initialize(
                substitutedReceiverParameter, substitutedExpectedThis, substitutedContextReceiverParameters,
                substitutedTypeParameters,
                substitutedValueParameters,
                substitutedReturnType,
                configuration.newModality,
                configuration.newVisibility
        );
        substitutedDescriptor.isOperator = isOperator;
        substitutedDescriptor.isExpect = isExpect;
        substitutedDescriptor.hasStableParameterNames = hasStableParameterNames;
        substitutedDescriptor.isHiddenToOvercomeSignatureClash = configuration.isHiddenToOvercomeSignatureClash;
        substitutedDescriptor.isHiddenForResolutionEverywhereBesideSupercalls = configuration.isHiddenForResolutionEverywhereBesideSupercalls;
        substitutedDescriptor.isStatic = isStatic;

        substitutedDescriptor.hasSynthesizedParameterNames = null != configuration.newHasSynthesizedParameterNames ? configuration.newHasSynthesizedParameterNames : hasSynthesizedParameterNames;

        // 处理用户数据映射
        if (!configuration.userDataMap.isEmpty() || null != userDataMap) {
            Map<CallableDescriptor.UserDataKey<?>, Object> newMap = configuration.userDataMap;

            if (null != userDataMap) {
                for (Map.Entry<CallableDescriptor.UserDataKey<?>, Object> entry : userDataMap.entrySet()) {
                    if (!newMap.containsKey(entry.getKey())) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (1 == newMap.size()) {
                substitutedDescriptor.userDataMap =
                        Collections.singletonMap(
                                newMap.keySet().iterator().next(), newMap.values().iterator().next());
            } else {
                substitutedDescriptor.userDataMap = newMap;
            }
        }

        // 处理签名更改或初始签名描述符
        if (configuration.signatureChange || null != initialSignatureDescriptor) {
            FunctionDescriptor initialSignature = (null != initialSignatureDescriptor ? initialSignatureDescriptor : this);
            FunctionDescriptor initialSignatureSubstituted = initialSignature.substitute(substitutor);
            substitutedDescriptor.initialSignatureDescriptor = initialSignatureSubstituted;
        }

        // 处理覆盖描述符
        if (configuration.copyOverrides && !getOriginal().getOverriddenDescriptors().isEmpty()) {
            if (configuration.substitution.isEmpty()) {
                Function0<Collection<FunctionDescriptor>> overriddenFunctionsTask = lazyOverriddenFunctionsTask;
                if (null != overriddenFunctionsTask) {
                    substitutedDescriptor.lazyOverriddenFunctionsTask = overriddenFunctionsTask;
                } else {
                    substitutedDescriptor.setOverriddenDescriptors(getOverriddenDescriptors());
                }
            } else {
                substitutedDescriptor.lazyOverriddenFunctionsTask = () -> {
                    Collection<FunctionDescriptor> result = new SmartList<>();
                    for (FunctionDescriptor overriddenFunction : getOverriddenDescriptors()) {
                        result.add(overriddenFunction.substitute(substitutor));
                    }
                    return result;
                };
            }
        }

        return substitutedDescriptor;
    }


    @NotNull
    @Override
    public FunctionDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            DescriptorVisibility visibility,
            CallableMemberDescriptor.Kind kind,
            boolean copyOverrides
    ) {
        return Objects.requireNonNull(newCopyBuilder()
                .setOwner(newOwner)
                .setModality(modality)
                .setVisibility(visibility)
                .setKind(kind)
                .setCopyOverrides(copyOverrides)
                .build());
    }

    public void setHasSynthesizedParameterNames(boolean hasSynthesizedParameterNames) {
        this.hasSynthesizedParameterNames = hasSynthesizedParameterNames;
    }

    @NotNull
    protected abstract FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull CallableMemberDescriptor.Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    );

    @NotNull
    private SourceElement getSourceToUseForCopy(boolean preserveSource, @Nullable FunctionDescriptor original) {
        return preserveSource
                ? (null != original ? original : getOriginal()).getSource()
                : SourceElement.NO_SOURCE;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }

    @Override
    @Nullable
    public FunctionDescriptor getInitialSignatureDescriptor() {
        return initialSignatureDescriptor;
    }

    private void setInitialSignatureDescriptor(@Nullable FunctionDescriptor initialSignatureDescriptor) {
        this.initialSignatureDescriptor = initialSignatureDescriptor;
    }

    // Don't use on published descriptors
    public <V> void putInUserDataMap(CallableDescriptor.UserDataKey<V> key, Object value) {
        if (null == userDataMap) {
            userDataMap = new LinkedHashMap<>();
        }
        userDataMap.put(key, value);
    }

    public class CopyConfiguration implements FunctionDescriptor.CopyBuilder<FunctionDescriptor> {
        private final Map<CallableDescriptor.UserDataKey<?>, Object> userDataMap = new LinkedHashMap<>();
        protected @NotNull TypeSubstitution substitution;
        protected @NotNull DeclarationDescriptor newOwner;
        protected @NotNull Modality newModality;
        protected @NotNull
        DescriptorVisibility newVisibility;
        protected @Nullable FunctionDescriptor original;
        protected @NotNull CallableMemberDescriptor.Kind kind;
        protected @NotNull List<ValueParameterDescriptor> newValueParameterDescriptors;
        protected @NotNull List<ReceiverParameterDescriptor> newContextReceiverParameters;
        protected @Nullable ReceiverParameterDescriptor newExtensionReceiverParameter;
        protected @Nullable ReceiverParameterDescriptor dispatchReceiverParameter = FunctionDescriptorImpl.this.dispatchReceiverParameter;
        protected @NotNull CangJieType newReturnType;
        protected @Nullable Name name;
        protected boolean copyOverrides = true;
        protected boolean signatureChange;
        protected boolean preserveSourceElement;
        protected boolean dropOriginalInContainingParts;
        protected boolean justForTypeSubstitution;
        private boolean isHiddenToOvercomeSignatureClash = isHiddenToOvercomeSignatureClash();
        private Annotations additionalAnnotations;
        private boolean isHiddenForResolutionEverywhereBesideSupercalls = isHiddenForResolutionEverywhereBesideSupercalls();
        private List<TypeParameterDescriptor> newTypeParameters;
        private Boolean newHasSynthesizedParameterNames;

        public CopyConfiguration(
                @NotNull TypeSubstitution substitution,
                @NotNull DeclarationDescriptor newOwner,
                @NotNull Modality newModality,
                @NotNull DescriptorVisibility newVisibility,
                @NotNull CallableMemberDescriptor.Kind kind,
                @NotNull List<ValueParameterDescriptor> newValueParameterDescriptors,
                @NotNull List<ReceiverParameterDescriptor> newContextReceiverParameters,
                @Nullable ReceiverParameterDescriptor newExtensionReceiverParameter,
                @NotNull CangJieType newReturnType,
                @Nullable Name name
        ) {
            this.substitution = substitution;
            this.newOwner = newOwner;
            this.newModality = newModality;
            this.newVisibility = newVisibility;
            this.kind = kind;
            this.newValueParameterDescriptors = newValueParameterDescriptors;
            this.newContextReceiverParameters = newContextReceiverParameters;
            this.newExtensionReceiverParameter = newExtensionReceiverParameter;
            this.newReturnType = newReturnType;
            this.name = name;
        }

        @Override
        @NotNull
        public CopyConfiguration setOwner(@NotNull DeclarationDescriptor owner) {
            newOwner = owner;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setModality(@NotNull Modality modality) {
            newModality = modality;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setVisibility(@NotNull DescriptorVisibility visibility) {
            newVisibility = visibility;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setKind(@NotNull CallableMemberDescriptor.Kind kind) {
            this.kind = kind;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setCopyOverrides(boolean copyOverrides) {
            this.copyOverrides = copyOverrides;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setName(@NotNull Name name) {
            this.name = name;
            return this;
        }


        @Override
        @NotNull
        public CopyConfiguration setValueParameters(@NotNull List<ValueParameterDescriptor> parameters) {
            newValueParameterDescriptors = parameters;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters) {
            newTypeParameters = parameters;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setReturnType(@NotNull CangJieType type) {
            newReturnType = type;
            return this;
        }


        @NotNull
        @Override
        public FunctionDescriptor.CopyBuilder<FunctionDescriptor> setContextReceiverParameters(@NotNull List<ReceiverParameterDescriptor> contextReceiverParameters) {
            newContextReceiverParameters = contextReceiverParameters;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setExtensionReceiverParameter(@Nullable ReceiverParameterDescriptor extensionReceiverParameter) {
            newExtensionReceiverParameter = extensionReceiverParameter;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter) {
            this.dispatchReceiverParameter = dispatchReceiverParameter;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setPreserveSourceElement() {
            preserveSourceElement = true;
            return this;
        }


        @Override
        @NotNull
        public CopyConfiguration setSignatureChange() {
            signatureChange = true;
            return this;
        }

        public CopyConfiguration setHasSynthesizedParameterNames(boolean value) {
            newHasSynthesizedParameterNames = value;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setDropOriginalInContainingParts() {
            dropOriginalInContainingParts = true;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setHiddenToOvercomeSignatureClash() {
            isHiddenToOvercomeSignatureClash = true;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setHiddenForResolutionEverywhereBesideSupercalls() {
            isHiddenForResolutionEverywhereBesideSupercalls = true;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setAdditionalAnnotations(@NotNull Annotations additionalAnnotations) {
            this.additionalAnnotations = additionalAnnotations;
            return this;
        }

        @Override
        @Nullable
        public FunctionDescriptor build() {
            return doSubstitute(this);
        }

        @Nullable
        public FunctionDescriptor getOriginal() {
            return original;
        }

        @Override
        @NotNull
        public CopyConfiguration setOriginal(@Nullable CallableMemberDescriptor original) {
            this.original = (FunctionDescriptor) original;
            return this;
        }

        @NotNull
        @Override
        public <V> FunctionDescriptor.CopyBuilder<FunctionDescriptor> putUserData(@NotNull CallableDescriptor.UserDataKey<V> userDataKey, V value) {
            userDataMap.put(userDataKey, value);
            return this;
        }

        @NotNull
        public TypeSubstitution getSubstitution() {
            return substitution;
        }

        @NotNull
        @Override
        public CopyConfiguration setSubstitution(@NotNull TypeSubstitution substitution) {
            this.substitution = substitution;
            return this;
        }


        @NotNull
        public CopyConfiguration setJustForTypeSubstitution(boolean value) {
            justForTypeSubstitution = value;
            return this;
        }
    }
}
