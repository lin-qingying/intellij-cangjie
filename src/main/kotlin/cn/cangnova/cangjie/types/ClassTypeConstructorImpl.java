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

package cn.cangnova.cangjie.types;

import cn.cangnova.cangjie.descriptors.ClassDescriptor;
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor;
import cn.cangnova.cangjie.descriptors.SupertypeLoopChecker;
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor;
import cn.cangnova.cangjie.resolve.DescriptorUtils;
import cn.cangnova.cangjie.storage.StorageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ClassTypeConstructorImpl extends AbstractClassTypeConstructor implements TypeConstructor{
    private final ClassDescriptor classDescriptor;
    private final List<TypeParameterDescriptor> parameters;
    private final Collection<CangJieType> supertypes;

    public ClassTypeConstructorImpl(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull List<? extends TypeParameterDescriptor> parameters,
            @NotNull Collection<CangJieType> supertypes,
            @NotNull StorageManager storageManager
    ) {
        super(storageManager);
        this.classDescriptor = classDescriptor;
        this.parameters = List.copyOf(parameters);
        this.supertypes = Collections.unmodifiableCollection(supertypes);
    }

    @Override
    public boolean isDenotable() {
        return true;
    }

    @Override
    public String toString() {
        return DescriptorUtils.getFqName(classDescriptor).asString();
    }


    @Override
    public @NotNull ClassDescriptor getDeclarationDescriptor() {
        return classDescriptor;

    }

    @Override
    public @NotNull List<TypeParameterDescriptor> getParameters() {
        return parameters;

    }

    @NotNull
    @Override
    protected Collection<CangJieType> computeSupertypes() {
        return supertypes;
    }

    @Override
    protected @NotNull Collection<CangJieType> computeExtendSuperTypes(@Nullable String extendId) {
        return List.of();
    }

    @NotNull
    @Override
    protected SupertypeLoopChecker getSupertypeLoopChecker() {
        return SupertypeLoopChecker.EMPTY.INSTANCE;

    }


}
