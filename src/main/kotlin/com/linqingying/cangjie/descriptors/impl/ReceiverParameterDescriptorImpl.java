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

import com.linqingying.cangjie.descriptors.DeclarationDescriptor;
import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.name.SpecialNames;
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.linqingying.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReceiverParameterDescriptorImpl extends AbstractReceiverParameterDescriptor{

    private final DeclarationDescriptor containingDeclaration;
    private final ReceiverValue value;
    public ReceiverParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ReceiverValue value,
            @NotNull Annotations annotations
    ) {
        this(containingDeclaration, value, annotations, SpecialNames.THIS);
    }
    public ReceiverParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ReceiverValue value,
            @NotNull Annotations annotations,
            @NotNull Name name
    ) {
        super(annotations, name);
        this.containingDeclaration = containingDeclaration;
        this.value = value;
    }
    @Override
    public @NotNull DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;

    }

    @Override
    public @NotNull ReceiverValue getValue() {
        return value;

    }
//    public void setOutType(@NotNull CangJieType outType) {
//        assert TypeUtilsKt.shouldBeUpdated(this.value.getType());
//        this.value = value.replaceType(outType);
//    }
    @Override
    public @NotNull ReceiverParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner) {
        return new ReceiverParameterDescriptorImpl(newOwner, value, getAnnotations());

    }


}
