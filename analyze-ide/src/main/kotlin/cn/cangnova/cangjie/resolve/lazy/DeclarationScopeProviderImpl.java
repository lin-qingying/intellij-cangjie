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

package cn.cangnova.cangjie.resolve.lazy;

import cn.cangnova.cangjie.descriptors.ClassDescriptorWithResolutionScopes;
import cn.cangnova.cangjie.psi.*;
import cn.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil;
import cn.cangnova.cangjie.psi.psiUtil.PsiUtilsKt;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory;
import cn.cangnova.cangjie.resolve.scopes.LexicalScope;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import cn.cangnova.cangjie.incremental.components.NoLookupLocation;
public class DeclarationScopeProviderImpl implements DeclarationScopeProvider {

    private final LazyDeclarationResolver lazyDeclarationResolver;

    private final FileScopeProvider fileScopeProvider;

    public DeclarationScopeProviderImpl(
            @NotNull LazyDeclarationResolver lazyDeclarationResolver,
            @NotNull FileScopeProvider fileScopeProvider
    ) {
        this.lazyDeclarationResolver = lazyDeclarationResolver;
        this.fileScopeProvider = fileScopeProvider;
    }

    @NotNull
    @Override
    public LexicalScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {


        CjDeclaration cjDeclaration = CjStubbedPsiUtil.getPsiOrStubParent(elementOfDeclaration, CjDeclaration.class, false);
        assert !(elementOfDeclaration instanceof CjDeclaration) || cjDeclaration == elementOfDeclaration :
                "For CjDeclaration element getParentOfType() should return itself.";
        assert cjDeclaration != null : "Should be contained inside declaration.";
        CjDeclaration parentDeclaration = CjStubbedPsiUtil.getContainingDeclaration(cjDeclaration);
        if (cjDeclaration instanceof CjPropertyAccessor) {
            parentDeclaration = CjStubbedPsiUtil.getContainingDeclaration(parentDeclaration, CjDeclaration.class);
        }

        if (parentDeclaration == null) {
            return fileScopeProvider.getFileResolutionScope((CjFile) elementOfDeclaration.getContainingFile());
        }


        if (parentDeclaration instanceof CjTypeStatement parentClassOrStruct) {
            ClassDescriptorWithResolutionScopes parentClassDescriptor = (ClassDescriptorWithResolutionScopes) lazyDeclarationResolver.getClassDescriptor(parentClassOrStruct, NoLookupLocation.MATCH_GET_DECLARATION_SCOPE);

            if (cjDeclaration instanceof CjAnonymousInitializer || cjDeclaration instanceof CjProperty  || cjDeclaration instanceof CjVariable) {
                return parentClassDescriptor.scopeForInitializerResolution;
            }


            return parentClassDescriptor.scopeForMemberDeclarationResolution;
        }
        throw new IllegalStateException("Don't call this method for local declarations: " + cjDeclaration + "\n" +
                PsiUtilsKt.getElementTextWithContext(cjDeclaration));
    }

    @NotNull
    @Override
    public DataFlowInfo getOuterDataFlowInfoForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        return DataFlowInfoFactory.EMPTY;

    }
}
