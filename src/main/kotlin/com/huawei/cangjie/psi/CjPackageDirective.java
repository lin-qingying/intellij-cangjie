package com.huawei.cangjie.psi;


import com.huawei.cangjie.descriptors.DescriptorVisibilities;
import com.huawei.cangjie.descriptors.DescriptorVisibility;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.huawei.cangjie.resolve.ModifiersChecker.resolveVisibilityFromModifiers;

public class CjPackageDirective extends CjModifierListOwnerStub<CangJiePlaceHolderStub<CjPackageDirective>> {

    private String qualifiedNameCache = null;

    public CjPackageDirective(@NotNull ASTNode node) {
        super(node);
    }

    public CjPackageDirective(@NotNull CangJiePlaceHolderStub<CjPackageDirective> stub) {
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

    @NotNull

    public DescriptorVisibility getModifierVisibility() {
        return resolveVisibilityFromModifiers(this, DescriptorVisibilities.PUBLIC);
    }

    @NotNull
    public Name getNameAsName() {
        PsiElement nameIdentifier = getNameIdentifier();
        return nameIdentifier == null ? SpecialNames.ROOT_PACKAGE : Name.identifier(nameIdentifier.getText());
    }

    public boolean isRoot() {
        return getName().length() == 0;
    }

    @NotNull
    public FqName getFqName() {
        String qualifiedName = getQualifiedName();
        return qualifiedName.isEmpty() ? FqName.ROOT : new FqName(qualifiedName);
    }

    public void setFqName(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            if (!getFqName().isRoot()) {

                replace(new CjPsiFactory(getProject()).createFile("").getPackageDirective());
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
