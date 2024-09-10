package com.huawei.cangjie.descriptors.impl;


import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SimpleFunctionDescriptorImpl extends FunctionDescriptorImpl implements SimpleFunctionDescriptor {
    protected SimpleFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, original, annotations, name, kind, source);
    }

    @NotNull
    public static SimpleFunctionDescriptorImpl create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        return new SimpleFunctionDescriptorImpl(containingDeclaration, null, annotations, name, kind, source);
    }

    @NotNull
    @kotlin.Deprecated(message = "This method is left for binary compatibility with android.nav.safearg plugin. Used in SafeArgSyntheticDescriptorGenerator.kt")
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable CangJieType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull DescriptorVisibility visibility
    ) {
        return initialize(extensionReceiverParameter, dispatchReceiverParameter, Collections.emptyList(),
                typeParameters, unsubstitutedValueParameters, unsubstitutedReturnType, modality, visibility, null);
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<ReceiverParameterDescriptor> contextReceiverParameters,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable CangJieType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull DescriptorVisibility visibility
    ) {
        return initialize(extensionReceiverParameter, dispatchReceiverParameter, contextReceiverParameters, typeParameters, unsubstitutedValueParameters,
                unsubstitutedReturnType, modality, visibility, null);
    }

    @NotNull
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<ReceiverParameterDescriptor> contextReceiverParameters,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable CangJieType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull DescriptorVisibility visibility,
            @Nullable Map<? extends UserDataKey<?>, ?> userData
    ) {
        super.initialize(extensionReceiverParameter, dispatchReceiverParameter, contextReceiverParameters, typeParameters, unsubstitutedValueParameters,
                unsubstitutedReturnType, modality, visibility);

        if (userData != null && !userData.isEmpty()) {
            userDataMap = new LinkedHashMap<UserDataKey<?>, Object>(userData);
        }

        return this;
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor getOriginal() {
        return (SimpleFunctionDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        return new SimpleFunctionDescriptorImpl(
                newOwner,

                (SimpleFunctionDescriptor) original,
                annotations,
                newName != null ? newName : getName(),
                kind,
                source
        );
    }


    @NotNull
    @Override
    public SimpleFunctionDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            DescriptorVisibility visibility,
            Kind kind,
            boolean copyOverrides
    ) {
        return (SimpleFunctionDescriptor) super.copy(newOwner, modality, visibility, kind, copyOverrides);
    }







    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public CopyBuilder<? extends SimpleFunctionDescriptor> newCopyBuilder() {
        return (CopyBuilder<? extends SimpleFunctionDescriptor>) super.newCopyBuilder();
    }



}
