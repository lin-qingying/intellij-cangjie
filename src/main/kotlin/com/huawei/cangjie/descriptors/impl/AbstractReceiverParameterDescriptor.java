package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeSubstitutor;
import com.sun.xml.bind.v2.TODO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractReceiverParameterDescriptor extends DeclarationDescriptorImpl implements ReceiverParameterDescriptor {
    public AbstractReceiverParameterDescriptor(@NotNull Annotations annotations) {
        super(annotations, SpecialNames.THIS);
    }
    @NotNull
    @Override
    public ParameterDescriptor getOriginal() {
        return this;
    }

    public AbstractReceiverParameterDescriptor(@NotNull Annotations annotations, @NotNull Name name) {
        super(annotations, name);
    }
    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitReceiverParameterDescriptor(this, data);
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getDispatchReceiverParameter() {
        return null;
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getValueParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getContextReceiverParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CangJieType getReturnType() {
        return getType();
    }

    @Override
    public @Nullable ReceiverParameterDescriptor getExtensionReceiverParameter() {
        return null;
    }

    @Override
    public @NotNull Collection<? extends CallableDescriptor> getOverriddenDescriptors() {
        return Collections.emptySet();

    }

    @Override
    public @NotNull List<TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();

    }

    @Override
    public @NotNull CangJieType getType() {
        return getValue().getType();
    }


    @Override
    public @NotNull SourceElement getSource() {
        return SourceElement.NO_SOURCE;

    }

    @Override
    public @NotNull DescriptorVisibility getVisibility() {
        return DescriptorVisibilities.LOCAL;

    }



    @Override
    public @Nullable ReceiverParameterDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
    throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }


}
