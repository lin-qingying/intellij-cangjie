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

package com.linqingying.cangjie.resolve.lazy.declarations;


import com.linqingying.cangjie.descriptors.DeclarationDescriptorVisitor;
import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithSource;
import com.linqingying.cangjie.descriptors.ModuleDescriptor;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.resolve.lazy.LazyEntity;
import com.linqingying.cangjie.resolve.lazy.ResolveSession;
import com.linqingying.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorImpl;
import com.linqingying.cangjie.resolve.scopes.MemberScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil;

public class LazyPackageDescriptor extends PackageFragmentDescriptorImpl implements LazyEntity {
    private final MemberScope memberScope;
    private final PackageMemberDeclarationProvider declarationProvider;

    public LazyPackageDescriptor(
            @NotNull ModuleDescriptor module,
            @NotNull FqName fqName,
            @NotNull ResolveSession resolveSession,
            @NotNull PackageMemberDeclarationProvider declarationProvider
    ) {
        super(module, fqName);
        this.declarationProvider = declarationProvider;

        this.memberScope = new LazyPackageMemberScope(resolveSession, declarationProvider, this);
    }

    @NotNull
    @Override
    public MemberScope getMemberScope() {
        return memberScope;
    }

    @Override
    public void forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(memberScope);
    }

    @NotNull
    @Override
    public PackageMemberDeclarationProvider getDeclarationProvider() {
        return declarationProvider;
    }




//    @Override
//    public @NotNull DeclarationDescriptorWithSource getOriginal() {
//        return null;
//    }
}
