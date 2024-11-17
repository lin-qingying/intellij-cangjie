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

package com.linqingying.cangjie.types;


import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.descriptors.ClassDescriptor;
import com.linqingying.cangjie.descriptors.ClassifierDescriptor;
import com.linqingying.cangjie.descriptors.ModalityUtilsKt;
import com.linqingying.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.linqingying.cangjie.storage.StorageManager;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractClassTypeConstructor extends AbstractTypeConstructor implements TypeConstructor {
    public AbstractClassTypeConstructor(@NotNull StorageManager storageManager) {
        super(storageManager);
    }

    @NotNull
    @Override
    public abstract ClassDescriptor getDeclarationDescriptor();


    @Override
    protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
        return classifier instanceof ClassDescriptor && areFqNamesEqual(getDeclarationDescriptor(), classifier);
    }
    @Override
    public final boolean isFinal() {
        ClassDescriptor descriptor = getDeclarationDescriptor();
        return ModalityUtilsKt.isFinalClass(descriptor) && !descriptor.isExpect();
    }
    @NotNull
    @Override
    public CangJieBuiltIns getBuiltIns() {
        return DescriptorUtilsKt.getBuiltIns(getDeclarationDescriptor());
    }

}
