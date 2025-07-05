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

package cn.cangnova.cangjie.descriptors.impl;

import cn.cangnova.cangjie.descriptors.*;
import cn.cangnova.cangjie.descriptors.annotations.Annotations;
import cn.cangnova.cangjie.name.Name;
import cn.cangnova.cangjie.name.SpecialNames;
import cn.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver;
import cn.cangnova.cangjie.types.CangJieType;
import cn.cangnova.cangjie.types.TypeSubstitutor;
import cn.cangnova.cangjie.types.Variance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractReceiverParameterDescriptor extends DeclarationDescriptorImpl implements ReceiverParameterDescriptor {
    public AbstractReceiverParameterDescriptor(@NotNull Annotations annotations) {
        super(annotations, SpecialNames.THIS);
    }

    public AbstractReceiverParameterDescriptor(@NotNull Annotations annotations, @NotNull Name name) {
        super(annotations, name);
    }

    @NotNull
    @Override
    public ParameterDescriptor getOriginal() {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitReceiverParameterDescriptor(this, data);
    }
    @Override
    public boolean hasSynthesizedParameterNames() {
        return false;
    }

    @Override
    public boolean hasStableParameterNames() {
        return false;
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
    public @NotNull Collection<? extends @NotNull TypeParameterDescriptor> getTypeParameters() {
        return Collections.emptyList();

    }

    @Override
    public @NotNull CangJieType getType() {
        return value.getType();
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
    public @NotNull CallableDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;

        CangJieType substitutedType;

        substitutedType = substitutor.substitute(getType(), Variance.INVARIANT);


        if (substitutedType == null) return null;
        if (substitutedType == getType()) return this;

        return new ReceiverParameterDescriptorImpl(containingDeclaration, new TransientReceiver(substitutedType), annotations);

    }


}
