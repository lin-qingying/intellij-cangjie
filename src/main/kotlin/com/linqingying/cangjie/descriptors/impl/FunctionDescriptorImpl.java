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
            @NotNull final DeclarationDescriptor containingDeclaration,
            @Nullable final FunctionDescriptor original,
            @NotNull final Annotations annotations,
            @NotNull final Name name,
            @NotNull final CallableMemberDescriptor.Kind kind,
            @NotNull final SourceElement source
    ) {
        super(containingDeclaration, annotations, name, source);
        this.original = null == original ? this : original;
        this.kind = kind;
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(
            final FunctionDescriptor substitutedDescriptor,
            @NotNull final List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull final TypeSubstitutor substitutor
    ) {
        return FunctionDescriptorImpl.getSubstitutedValueParameters(substitutedDescriptor, unsubstitutedValueParameters, substitutor, false, false, null);
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(
            final FunctionDescriptor substitutedDescriptor,
            @NotNull final List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull final TypeSubstitutor substitutor,
            final boolean dropOriginal,
            final boolean preserveSourceElement,
            @Nullable final boolean[] wereChanges
    ) {
        final List<ValueParameterDescriptor> result = new ArrayList<>(unsubstitutedValueParameters.size());
        for (final ValueParameterDescriptor unsubstitutedValueParameter : unsubstitutedValueParameters) {
            // TODO : Lazy?
            final CangJieType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType(), Variance.INVARIANT);
            final CangJieType varargElementType = unsubstitutedValueParameter.getVarargElementType();
            final CangJieType substituteVarargElementType =
                    null == varargElementType ? null : substitutor.substitute(varargElementType, Variance.INVARIANT);
            if (null == substitutedType) return null;
            if (substitutedType != unsubstitutedValueParameter.getType() || varargElementType != substituteVarargElementType) {
                if (null != wereChanges) {
                    wereChanges[0] = true;
                }
            }

            final Function0<List<VariableDescriptor>> destructuringVariablesAction = FunctionDescriptorImpl.getDestructuringVariablesAction(unsubstitutedValueParameter);

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

    private static @Nullable Function0<List<VariableDescriptor>> getDestructuringVariablesAction(final ValueParameterDescriptor unsubstitutedValueParameter) {
        Function0<List<VariableDescriptor>> destructuringVariablesAction = null;
        if (unsubstitutedValueParameter instanceof ValueParameterDescriptorImpl.WithDestructuringDeclaration) {
            List<VariableDescriptor> destructuringVariables =
                    ((ValueParameterDescriptorImpl.WithDestructuringDeclaration) unsubstitutedValueParameter)
                            .getDestructuringVariables();
            destructuringVariablesAction = () -> destructuringVariables;
        }
        return destructuringVariablesAction;
    }

    public void setExpect(final boolean isExpect) {
        this.isExpect = isExpect;
    }

    public void setHasStableParameterNames(final boolean hasStableParameterNames) {
        this.hasStableParameterNames = hasStableParameterNames;
    }

    @Override
    public boolean hasStableParameterNames() {
        return this.hasStableParameterNames;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getUserData(final CallableDescriptor.UserDataKey<V> key) {
        if (null == userDataMap) return null;
        return (V) this.userDataMap.get(key);
    }

    public void setIsStatic(final boolean isStatic) {
        this.isStatic = isStatic;
    }

    public void setIsOperator(final boolean isOperator) {
        this.isOperator = isOperator;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getDispatchReceiverParameter() {
        return this.dispatchReceiverParameter;
    }

    @NotNull
    public FunctionDescriptorImpl initialize(
            @Nullable final ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable final ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull final List<ReceiverParameterDescriptor> contextReceiverParameters,
            @NotNull final List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull final List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable final CangJieType unsubstitutedReturnType,
            @Nullable final Modality modality,
            @NotNull final DescriptorVisibility visibility
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
            final TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            if (typeParameterDescriptor.getIndex() != i) {
                throw new IllegalStateException(typeParameterDescriptor + " index is " + typeParameterDescriptor.getIndex() + " but position is " + i);
            }
        }

        for (int i = 0; i < unsubstitutedValueParameters.size(); ++i) {
            // TODO fill me
            final int firstValueParameterOffset = 0; // receiverParameter.exists() ? 1 : 0;
            final ValueParameterDescriptor valueParameterDescriptor = unsubstitutedValueParameters.get(i);
            if (valueParameterDescriptor.getIndex() != i + firstValueParameterOffset) {
                throw new IllegalStateException(valueParameterDescriptor + "index is " + valueParameterDescriptor.getIndex() + " but position is " + i);
            }
        }

        return this;
    }

    @Override
    public boolean getIsExtend() {
        if (null == dispatchReceiverParameter) return false;
        return this.dispatchReceiverParameter.getContainingDeclaration() instanceof LazyExtendClassDescriptor;

    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getContextReceiverParameters() {
        return Objects.requireNonNullElse(this.contextReceiverParameters, Collections.emptyList());
    }


    @Nullable
    @Override
    public ReceiverParameterDescriptor getExtensionReceiverParameter() {
        return this.extensionReceiverParameter;
    }

    public void setExtensionReceiverParameter(@NotNull final ReceiverParameterDescriptor extensionReceiverParameter) {
        this.extensionReceiverParameter = extensionReceiverParameter;
    }

    @NotNull
    @Override
    public Collection<? extends FunctionDescriptor> getOverriddenDescriptors() {
        this.performOverriddenLazyCalculationIfNeeded();
        return null != overriddenFunctions ? this.overriddenFunctions : Collections.emptyList();
    }


    /**
     * 重写规则  this 表示的该对象 (已经重写的对象，open修饰): 注意 这里的open
     *
     * @param overriddenDescriptors 被重写方法，也就是抽象方法
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setOverriddenDescriptors(@NotNull final Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {

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
        final Function0<Collection<FunctionDescriptor>> overriddenTask = this.lazyOverriddenFunctionsTask;
        if (null != overriddenTask) {
            this.overriddenFunctions = overriddenTask.invoke();
            // Here it's important that this assignment is strictly after previous one
            // `lazyOverriddenFunctionsTask` is volatile, so when someone will see that it's null,
            // he can read consistent collection from `overriddenFunctions`,
            // because it's assignment happens-before of "lazyOverriddenFunctionsTask = null"
            this.lazyOverriddenFunctionsTask = null;
        }
    }

    @NotNull
    @Override
    public Modality getModality() {
        return this.modality;
    }

    public void setModality(@NotNull final Modality modality) {
        this.modality = modality;
    }

    @NotNull
    @Override
    public DescriptorVisibility getVisibility() {
        return this.visibility;
    }

    public void setVisibility(@NotNull final DescriptorVisibility visibility) {
        this.visibility = visibility;
    }

    @Override
    public boolean isStatic() {


        final PsiElement element = PsiSourceElementKt.getPsi(this.getSource());
        if (element instanceof CjFunction) {
            this.isStatic = ((CjFunction) element).isStatic();
        }
        return this.isStatic;

    }

    @Override
    public boolean isUnsafe() {
        final PsiElement element = PsiSourceElementKt.getPsi(this.getSource());
        if (element instanceof CjFunction) {
            this.isUnsafe = ((CjFunction) element).isUnsafe();
        }
        return this.isUnsafe;
    }


    @Override
    public boolean isOperator() {
        if (this.isOperator) return true;

        for (final FunctionDescriptor descriptor : this.getOriginal().getOverriddenDescriptors()) {
            if (descriptor.isOperator()) return true;
        }

        return false;
    }


    public void setOperator(final boolean isOperator) {
        this.isOperator = isOperator;
    }


    @Override
    public boolean isHiddenToOvercomeSignatureClash() {
        return this.isHiddenToOvercomeSignatureClash;
    }


    private void setHiddenToOvercomeSignatureClash(final boolean hiddenToOvercomeSignatureClash) {
        this.isHiddenToOvercomeSignatureClash = hiddenToOvercomeSignatureClash;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        return this.hasSynthesizedParameterNames;
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        final List<TypeParameterDescriptor> parameters = this.typeParameters;
        // Diagnostics for EA-141456
        if (null == parameters) {
            throw new IllegalStateException("typeParameters == null for " + this);
        }
        return parameters;
    }

    @Override
    public @NotNull List<TypeParameterDescriptor> getTypeParametersNotExtend() {
        final List<TypeParameterDescriptor> parameters = this.typeParameters;
        // Diagnostics for EA-141456
        if (null == parameters) {
            throw new IllegalStateException("typeParameters == null for " + this);
        }

        if (1 == parameters.size() && null != getExtensionReceiverParameter() && null == ((CjFunctionImpl) ((CangJieSourceElement) getSource()).getPsi()).getOriginalTypeParameterList()) {
            return Collections.emptyList();
        }
        return parameters;
    }

    @Override
    public void validate() {
        this.getTypeParameters();
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return this.unsubstitutedValueParameters;
    }


    @Override
    public CangJieType getReturnType() {


        return this.unsubstitutedReturnType;
    }

    public void setReturnType(@NotNull final CangJieType unsubstitutedReturnType) {
//        if (this.unsubstitutedReturnType != null) {
        // TODO: uncomment and fix tests
        //throw new IllegalStateException("returnType already set");
//        }
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return this.original == this ? this : this.original.getOriginal();
    }

    @NotNull
    @Override
    public CallableMemberDescriptor.Kind getKind() {
        return this.kind;
    }

    @Override
    public FunctionDescriptor substitute(@NotNull final TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }

        return this.newCopyBuilder(originalSubstitutor)
                .setOriginal(this.getOriginal())
                .setPreserveSourceElement()
                .setJustForTypeSubstitution(true)
                .build();
    }

    @Nullable
    private CangJieType getExtensionReceiverParameterType() {
        if (null == extensionReceiverParameter) return null;
        return this.extensionReceiverParameter.getType();
    }

    @Override
    public boolean isHiddenForResolutionEverywhereBesideSupercalls() {
        return this.isHiddenForResolutionEverywhereBesideSupercalls;
    }

    private void setHiddenForResolutionEverywhereBesideSupercalls(final boolean hiddenForResolutionEverywhereBesideSupercalls) {
        this.isHiddenForResolutionEverywhereBesideSupercalls = hiddenForResolutionEverywhereBesideSupercalls;
    }

    @Override
    @NotNull
    public FunctionDescriptor.CopyBuilder<? extends FunctionDescriptor> newCopyBuilder() {
        return this.newCopyBuilder(TypeSubstitutor.EMPTY);
    }

    @NotNull
    protected CopyConfiguration newCopyBuilder(@NotNull final TypeSubstitutor substitutor) {
        return new CopyConfiguration(
                substitutor.getSubstitution(),
                this.getContainingDeclaration(), modality, this.getVisibility(), kind, this.getValueParameters(), this.getContextReceiverParameters(),
                extensionReceiverParameter, this.getReturnType(), null);
    }

    @Nullable
    protected FunctionDescriptor doSubstitute(@NotNull final CopyConfiguration configuration) {
        final boolean[] wereChanges = new boolean[1];
        final Annotations resultAnnotations =
                null != configuration.additionalAnnotations
                        ? AnnotationsKt.composeAnnotations(this.getAnnotations(), configuration.additionalAnnotations)
                        : this.getAnnotations();

        final FunctionDescriptorImpl substitutedDescriptor = this.createSubstitutedCopy(
                configuration.newOwner, configuration.original, configuration.kind, configuration.name, resultAnnotations,
                this.getSourceToUseForCopy(configuration.preserveSourceElement, configuration.original));

        final List<TypeParameterDescriptor> unsubstitutedTypeParameters =
                null == configuration.newTypeParameters ? this.getTypeParameters() : configuration.newTypeParameters;

        wereChanges[0] |= !unsubstitutedTypeParameters.isEmpty();

        final List<TypeParameterDescriptor> substitutedTypeParameters =
                new ArrayList<>(unsubstitutedTypeParameters.size());
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(
                unsubstitutedTypeParameters, configuration.substitution, substitutedDescriptor, substitutedTypeParameters, wereChanges
        );
        if (null == substitutor) return null;

        final List<ReceiverParameterDescriptor> substitutedContextReceiverParameters = new ArrayList<>();
        if (!configuration.newContextReceiverParameters.isEmpty()) {
            int index = 0;
            for (final ReceiverParameterDescriptor newContextReceiverParameter : configuration.newContextReceiverParameters) {
                final CangJieType substitutedContextReceiverType =
                        substitutor.substitute(newContextReceiverParameter.getType(), Variance.INVARIANT);
                if (null == substitutedContextReceiverType) {
                    return null;
                }
                final ReceiverParameterDescriptor substitutedContextReceiverParameter =
                        DescriptorFactory.createContextReceiverParameterForCallable(substitutedDescriptor, substitutedContextReceiverType,
                                ((ImplicitContextReceiver) newContextReceiverParameter.getValue()).getCustomLabelName(),
                                newContextReceiverParameter.getAnnotations(),
                                index);
                index++;
                substitutedContextReceiverParameters.add(substitutedContextReceiverParameter);

                wereChanges[0] |= substitutedContextReceiverType != newContextReceiverParameter.getType();
            }
        }

        ReceiverParameterDescriptor substitutedReceiverParameter = null;
        if (null != configuration.newExtensionReceiverParameter) {
            final CangJieType substitutedExtensionReceiverType =
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

        ReceiverParameterDescriptor substitutedExpectedThis = null;
        if (null != configuration.dispatchReceiverParameter) {
            // When generating fake-overridden member it's dispatch receiver parameter has type of Base, and it's correct.
            // E.g.
            // class Base { fun foo() }
            // class Derived : Base
            // val x: Base
            // if (x is Derived) {
            //    // `x` shouldn't be marked as smart-cast
            //    // but it would if fake-overridden `foo` had `Derived` as it's dispatch receiver parameter type
            //    x.foo()
            // }
            substitutedExpectedThis = configuration.dispatchReceiverParameter.substitute(substitutor);
            if (null == substitutedExpectedThis) {
                return null;
            }

            wereChanges[0] |= substitutedExpectedThis != configuration.dispatchReceiverParameter;
        }

        final List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorImpl.getSubstitutedValueParameters(
                substitutedDescriptor, configuration.newValueParameterDescriptors, substitutor, configuration.dropOriginalInContainingParts,
                configuration.preserveSourceElement, wereChanges
        );
        if (null == substitutedValueParameters) {
            return null;
        }

        final CangJieType substitutedReturnType = substitutor.substitute(configuration.newReturnType, Variance.INVARIANT);
        if (null == substitutedReturnType) {
            return null;
        }

        wereChanges[0] |= substitutedReturnType != configuration.newReturnType;

        if (!wereChanges[0] && configuration.justForTypeSubstitution) {
            return this;
        }

        substitutedDescriptor.initialize(
                substitutedReceiverParameter, substitutedExpectedThis, substitutedContextReceiverParameters,
                substitutedTypeParameters,
                substitutedValueParameters,
                substitutedReturnType,
                configuration.newModality,
                configuration.newVisibility
        );
        substitutedDescriptor.isOperator = this.isOperator;
//        substitutedDescriptor.setInfix(isInfix);
//        substitutedDescriptor.setExternal(isExternal);
//        substitutedDescriptor.setInline(isInline);
//        substitutedDescriptor.setTailrec(isTailrec);
//        substitutedDescriptor.setSuspend(isSuspend);
        substitutedDescriptor.isExpect = this.isExpect;
//        substitutedDescriptor.setActual(isActual);
        substitutedDescriptor.hasStableParameterNames = this.hasStableParameterNames;
        substitutedDescriptor.isHiddenToOvercomeSignatureClash = configuration.isHiddenToOvercomeSignatureClash;
        substitutedDescriptor.isHiddenForResolutionEverywhereBesideSupercalls = configuration.isHiddenForResolutionEverywhereBesideSupercalls;

        substitutedDescriptor.hasSynthesizedParameterNames = null != configuration.newHasSynthesizedParameterNames ? configuration.newHasSynthesizedParameterNames : this.hasSynthesizedParameterNames;

        if (!configuration.userDataMap.isEmpty() || null != userDataMap) {
            final Map<CallableDescriptor.UserDataKey<?>, Object> newMap = configuration.userDataMap;

            if (null != userDataMap) {
                for (final Map.Entry<CallableDescriptor.UserDataKey<?>, Object> entry : this.userDataMap.entrySet()) {
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

        if (configuration.signatureChange || null != getInitialSignatureDescriptor()) {
            final FunctionDescriptor initialSignature = (null != getInitialSignatureDescriptor() ? initialSignatureDescriptor : this);
            final FunctionDescriptor initialSignatureSubstituted = initialSignature.substitute(substitutor);
            substitutedDescriptor.initialSignatureDescriptor = initialSignatureSubstituted;
        }

        if (configuration.copyOverrides && !this.getOriginal().getOverriddenDescriptors().isEmpty()) {
            if (configuration.substitution.isEmpty()) {
                final Function0<Collection<FunctionDescriptor>> overriddenFunctionsTask = this.lazyOverriddenFunctionsTask;
                if (null != overriddenFunctionsTask) {
                    substitutedDescriptor.lazyOverriddenFunctionsTask = overriddenFunctionsTask;
                } else {
                    substitutedDescriptor.setOverriddenDescriptors(this.getOverriddenDescriptors());
                }
            } else {
                substitutedDescriptor.lazyOverriddenFunctionsTask = () -> {
                    final Collection<FunctionDescriptor> result = new SmartList<>();
                    for (final FunctionDescriptor overriddenFunction : this.getOverriddenDescriptors()) {
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
            final DeclarationDescriptor newOwner,
            final Modality modality,
            final DescriptorVisibility visibility,
            final CallableMemberDescriptor.Kind kind,
            final boolean copyOverrides
    ) {
        return Objects.requireNonNull(this.newCopyBuilder()
                .setOwner(newOwner)
                .setModality(modality)
                .setVisibility(visibility)
                .setKind(kind)
                .setCopyOverrides(copyOverrides)
                .build());
    }

    public void setHasSynthesizedParameterNames(final boolean hasSynthesizedParameterNames) {
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
    private SourceElement getSourceToUseForCopy(final boolean preserveSource, @Nullable final FunctionDescriptor original) {
        return preserveSource
                ? (null != original ? original : this.getOriginal()).getSource()
                : SourceElement.NO_SOURCE;
    }

    @Override
    public <R, D> R accept(final DeclarationDescriptorVisitor<R, D> visitor, final D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }

    @Override
    @Nullable
    public FunctionDescriptor getInitialSignatureDescriptor() {
        return this.initialSignatureDescriptor;
    }

    private void setInitialSignatureDescriptor(@Nullable final FunctionDescriptor initialSignatureDescriptor) {
        this.initialSignatureDescriptor = initialSignatureDescriptor;
    }

    // Don't use on published descriptors
    public <V> void putInUserDataMap(final CallableDescriptor.UserDataKey<V> key, final Object value) {
        if (null == userDataMap) {
            this.userDataMap = new LinkedHashMap<>();
        }
        this.userDataMap.put(key, value);
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
        private boolean isHiddenToOvercomeSignatureClash = FunctionDescriptorImpl.this.isHiddenToOvercomeSignatureClash();
        private Annotations additionalAnnotations;
        private boolean isHiddenForResolutionEverywhereBesideSupercalls = FunctionDescriptorImpl.this.isHiddenForResolutionEverywhereBesideSupercalls();
        private List<TypeParameterDescriptor> newTypeParameters;
        private Boolean newHasSynthesizedParameterNames;

        public CopyConfiguration(
                @NotNull final TypeSubstitution substitution,
                @NotNull final DeclarationDescriptor newOwner,
                @NotNull final Modality newModality,
                @NotNull final DescriptorVisibility newVisibility,
                @NotNull final CallableMemberDescriptor.Kind kind,
                @NotNull final List<ValueParameterDescriptor> newValueParameterDescriptors,
                @NotNull final List<ReceiverParameterDescriptor> newContextReceiverParameters,
                @Nullable final ReceiverParameterDescriptor newExtensionReceiverParameter,
                @NotNull final CangJieType newReturnType,
                @Nullable final Name name
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
        public CopyConfiguration setOwner(@NotNull final DeclarationDescriptor owner) {
            newOwner = owner;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setModality(@NotNull final Modality modality) {
            newModality = modality;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setVisibility(@NotNull final DescriptorVisibility visibility) {
            newVisibility = visibility;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setKind(@NotNull final CallableMemberDescriptor.Kind kind) {
            this.kind = kind;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setCopyOverrides(final boolean copyOverrides) {
            this.copyOverrides = copyOverrides;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setName(@NotNull final Name name) {
            this.name = name;
            return this;
        }


        @Override
        @NotNull
        public CopyConfiguration setValueParameters(@NotNull final List<ValueParameterDescriptor> parameters) {
            newValueParameterDescriptors = parameters;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setTypeParameters(@NotNull final List<TypeParameterDescriptor> parameters) {
            newTypeParameters = parameters;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setReturnType(@NotNull final CangJieType type) {
            newReturnType = type;
            return this;
        }


        @NotNull
        @Override
        public FunctionDescriptor.CopyBuilder<FunctionDescriptor> setContextReceiverParameters(@NotNull final List<ReceiverParameterDescriptor> contextReceiverParameters) {
            newContextReceiverParameters = contextReceiverParameters;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setExtensionReceiverParameter(@Nullable final ReceiverParameterDescriptor extensionReceiverParameter) {
            newExtensionReceiverParameter = extensionReceiverParameter;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setDispatchReceiverParameter(@Nullable final ReceiverParameterDescriptor dispatchReceiverParameter) {
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

        public CopyConfiguration setHasSynthesizedParameterNames(final boolean value) {
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
            this.isHiddenToOvercomeSignatureClash = true;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setHiddenForResolutionEverywhereBesideSupercalls() {
            this.isHiddenForResolutionEverywhereBesideSupercalls = true;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setAdditionalAnnotations(@NotNull final Annotations additionalAnnotations) {
            this.additionalAnnotations = additionalAnnotations;
            return this;
        }

        @Override
        @Nullable
        public FunctionDescriptor build() {
            return FunctionDescriptorImpl.this.doSubstitute(this);
        }

        @Nullable
        public FunctionDescriptor getOriginal() {
            return this.original;
        }

        @Override
        @NotNull
        public CopyConfiguration setOriginal(@Nullable final CallableMemberDescriptor original) {
            this.original = (FunctionDescriptor) original;
            return this;
        }

        @NotNull
        @Override
        public <V> FunctionDescriptor.CopyBuilder<FunctionDescriptor> putUserData(@NotNull final CallableDescriptor.UserDataKey<V> userDataKey, final V value) {
            this.userDataMap.put(userDataKey, value);
            return this;
        }

        @NotNull
        public TypeSubstitution getSubstitution() {
            return this.substitution;
        }

        @NotNull
        @Override
        public CopyConfiguration setSubstitution(@NotNull final TypeSubstitution substitution) {
            this.substitution = substitution;
            return this;
        }


        @NotNull
        public CopyConfiguration setJustForTypeSubstitution(final boolean value) {
            this.justForTypeSubstitution = value;
            return this;
        }
    }
}
