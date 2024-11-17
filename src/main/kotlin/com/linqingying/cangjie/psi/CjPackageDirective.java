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

package com.linqingying.cangjie.psi;


import com.linqingying.cangjie.descriptors.DescriptorVisibilities;
import com.linqingying.cangjie.descriptors.DescriptorVisibility;
import com.linqingying.cangjie.lexer.CjKeywordToken;
import com.linqingying.cangjie.lexer.CjModifierKeywordToken;
import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.name.SpecialNames;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.linqingying.cangjie.psi.psiUtil.CjStubbedPsiUtil;
import com.linqingying.cangjie.psi.stubs.CangJiePackageDirectiveStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.linqingying.cangjie.psi.stubs.elements.CjTokenSets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.linqingying.cangjie.name.FqName.fromString;

//public class CjPackageDirective extends CjModifierListOwnerStub<CangJiePlaceHolderStub<CjPackageDirective>>
public class CjPackageDirective extends CjDeclarationStub<CangJiePackageDirectiveStub> {

    private String qualifiedNameCache = null;

    public CjPackageDirective(@NotNull ASTNode node) {
        super(node);
    }

    public CjPackageDirective(@NotNull CangJiePackageDirectiveStub stub) {
        super(stub, CjStubElementTypes.PACKAGE_DIRECTIVE);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Nullable
    public CjExpression getPackageNameExpression() {
        return CjStubbedPsiUtil.getStubOrPsiChild(this, CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.Companion.getARRAY_FACTORY());
    }

    @NotNull
    public List<CjSimpleNameExpression> getPackageNames() {
        CjExpression nameExpression = getPackageNameExpression();
        if (nameExpression == null) return Collections.emptyList();

        List<CjSimpleNameExpression> packageNames = new ArrayList<>();
        while (nameExpression instanceof CjQualifiedExpression qualifiedExpression) {

            CjExpression selector = qualifiedExpression.getSelectorExpression();
            if (selector instanceof CjSimpleNameExpression) {
                packageNames.add((CjSimpleNameExpression) selector);
            }

            nameExpression = qualifiedExpression.getReceiverExpression();
        }

        if (nameExpression instanceof CjSimpleNameExpression) {
            packageNames.add((CjSimpleNameExpression) nameExpression);
        }

        Collections.reverse(packageNames);

        return packageNames;
    }

    @Nullable
    public CjSimpleNameExpression getLastReferenceExpression() {
        CjExpression nameExpression = getPackageNameExpression();
        if (nameExpression == null) return null;

        return (CjSimpleNameExpression) CjPsiUtilKt.getQualifiedElementSelector(nameExpression);
    }

    @Nullable
    public PsiElement getNameIdentifier() {
        CjSimpleNameExpression lastPart = getLastReferenceExpression();
        return lastPart != null ? lastPart.getIdentifier() : null;
    }

    @Override
    @NotNull
    public String getName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? "" : nameIdentifier.getText();
    }

    @Override
    public void navigate(boolean requestFocus) {
        super.navigate(requestFocus);
    }

    @Override
    public boolean canNavigateToSource() {
        return super.canNavigateToSource();
    }

    @Override
    public boolean canNavigate() {
        return super.canNavigate();
    }

    @Override
    public @NotNull PsiElement getNavigationElement() {
        return super.getNavigationElement();
    }

    @Nullable
    public PsiElement getModifier(@NotNull CjKeywordToken tokenType) {
        return findChildByType(tokenType);
    }

    public boolean hasModifier(@NotNull CjModifierKeywordToken tokenType) {
//        CangJieImportDirectiveStub stub = getStub();
//        if (stub != null) {
//            return stub.getModifierVisibility(tokenType);
//        }
        return getModifier(tokenType) != null;
    }

    @NotNull
    public DescriptorVisibility getModifierVisibility() {
        CangJiePackageDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.getModifierVisibility();
        }

        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE;
        if (hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL;
        if (hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED;
        if (hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC;
        return DescriptorVisibilities.PUBLIC;
    }


    public boolean isMacroPackage() {

//        try {
            return findChildByType(CjTokens.MACRO_KEYWORD) != null;
//        } catch (Exception e) {
//            return false;
//        }
    }

    @NotNull
    public Name getNameAsName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? SpecialNames.ROOT_PACKAGE : Name.identifier(nameIdentifier.getText());
    }

    public boolean isRoot() {
        return getName().isEmpty();
    }

    @NotNull
    public FqName getFqName() {
        String qualifiedName = getQualifiedName();
        return qualifiedName.isEmpty() ? FqName.ROOT : fromString(qualifiedName);
    }

    public void setFqName(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            if (!getFqName().isRoot()) {

                replace(Objects.requireNonNull(new CjPsiFactory(getProject()).createFile("").getPackageDirective()));
            }
            return;
        }

        CjPsiFactory psiFactory = new CjPsiFactory(getProject());
        PsiElement newExpression = psiFactory.createExpression(fqName.asString());
        CjExpression currentExpression = getPackageNameExpression();
        if (currentExpression != null) {
            currentExpression.replace(newExpression);
            return;
        }

        PsiElement keyword = getPackageKeyword();
        if (keyword != null) {
            addAfter(newExpression, keyword);
            addAfter(psiFactory.createWhiteSpace(), keyword);
            return;
        }

        replace(psiFactory.createPackageDirective(fqName));
    }

    @NotNull
    public FqName getFqName(CjSimpleNameExpression nameExpression) {
        return new FqName(getQualifiedNameOf(nameExpression));
    }

    @NotNull
    public String getQualifiedName() {
        if (qualifiedNameCache == null) {
            qualifiedNameCache = getQualifiedNameOf(null);
        }

        return qualifiedNameCache;
    }

    @NotNull
    private String getQualifiedNameOf(@Nullable CjSimpleNameExpression nameExpression) {
        StringBuilder builder = new StringBuilder();
        for (CjSimpleNameExpression e : getPackageNames()) {
            if (!builder.isEmpty()) {
                builder.append(".");
            }
            builder.append(e.getReferencedName());

            if (e == nameExpression) break;
        }
        return builder.toString();
    }

    @Nullable
    public PsiElement getPackageKeyword() {
        return findChildByType(CjTokens.PACKAGE_KEYWORD);
    }

    @Override
    public void subtreeChanged() {
        qualifiedNameCache = null;
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitPackageDirective(this, data);
    }

}
