package com.huawei.cangjie.psi;

import com.huawei.cangjie.descriptors.DescriptorVisibilities;
import com.huawei.cangjie.descriptors.DescriptorVisibility;
import com.huawei.cangjie.lexer.CjKeywordToken;
import com.huawei.cangjie.lexer.CjModifierKeywordToken;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets;
import com.huawei.cangjie.resolve.ImportPath;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CjImportDirective extends CjDeclarationStub<CangJieImportDirectiveStub> implements CjImportInfo {

    private volatile FqName importedFqName;

    public CjImportDirective(@NotNull ASTNode node) {
        super(node);
    }

    public CjImportDirective(@NotNull CangJieImportDirectiveStub stub) {
        super(stub, CjStubElementTypes.IMPORT_DIRECTIVE);
    }

    @Nullable
    public static FqName fqNameFromExpression(@Nullable CjExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof CjDotQualifiedExpression dotQualifiedExpression) {
            FqName parentFqn = fqNameFromExpression(dotQualifiedExpression.getReceiverExpression());
            Name child = nameFromExpression(dotQualifiedExpression.getSelectorExpression());
            if (child == null) {
                return parentFqn;
            }
            if (parentFqn != null) {
                return parentFqn.child(child);
            }
            return null;
        } else if (expression instanceof CjSimpleNameExpression simpleNameExpression) {
            return FqName.topLevel(simpleNameExpression.getReferencedNameAsName());
        } else {
            throw new IllegalArgumentException("Can't construct fqn for: " + expression.getClass());
        }
    }

    @Nullable
    private static Name nameFromExpression(@Nullable CjExpression expression) {
        if (expression == null) {
            return null;
        }

        if (expression instanceof CjSimpleNameExpression) {
            return ((CjSimpleNameExpression) expression).getReferencedNameAsName();
        } else {
            throw new IllegalArgumentException("Can't construct name for: " + expression.getClass());
        }
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitImportDirective(this, data);
    }

    @Nullable
    @IfNotParsed
    public CjExpression getImportedReference() {
        CjExpression[] references = getStubOrPsiChildren(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.Companion.getARRAY_FACTORY());
        if (references.length > 0) {
            return references[0];
        }
        return null;
    }

    @Nullable
    public CjImportAlias getAlias() {
        return getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS);
    }

    @Override
    @Nullable
    public String getAliasName() {
        CjImportAlias alias = getAlias();
        return alias != null ? alias.getName() : null;
    }

    @Override
    public boolean isAllUnder() {
        CangJieImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isAllUnder();
        }
        return getNode().findChildByType(CjTokens.MUL) != null;
    }

    @Nullable
    @Override
    public ImportContent getImportContent() {
        CjExpression reference = getImportedReference();
        if (reference == null) return null;
        return new ImportContent.ExpressionBased(reference);
    }

    @Override
    @Nullable
    @IfNotParsed
    public FqName getImportedFqName() {


        PsiElement parentStub = this.getParent();

        CangJieImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.getImportedFqName();
        }

        FqName importedFqName = this.importedFqName;
        if (importedFqName != null) return importedFqName;
        CjExpression importedReference = getImportedReference();
        // in case it's not parsed
        if (importedReference == null) return null;


        importedFqName = fqNameFromExpression(importedReference);

        if (parentStub instanceof CjMultiImportDirective) {
            FqName name = ((CjMultiImportDirective) parentStub).getFqName();
            if (name != null) {
                importedFqName = name.child(importedFqName);
            }
        }

        this.importedFqName = importedFqName;
        return importedFqName;
    }

    @Nullable
    @IfNotParsed
    public ImportPath getImportPath() {
        FqName importFqn = getImportedFqName();
        if (importFqn == null) {
            return null;
        }

        Name alias = null;
        String aliasName = getAliasName();
        if (aliasName != null) {
            alias = Name.identifier(aliasName);
        }

        return new ImportPath(importFqn, isAllUnder(), alias);
    }

    public boolean isValidImport() {
        CangJieImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.isValid();
        }
        return !PsiTreeUtil.hasErrorElements(this);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        importedFqName = null;
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
    @Override
    public DescriptorVisibility getModifierVisibility() {
        CangJieImportDirectiveStub stub = getStub();
        if (stub != null) {
            return stub.getModifierVisibility();
        }

        if (hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE;
        if (hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL;
        if (hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED;
        if (hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC;
        return DescriptorVisibilities.PRIVATE;
//        return resolveVisibilityFromModifiers(this, DescriptorVisibilities.PRIVATE);
    }
}
