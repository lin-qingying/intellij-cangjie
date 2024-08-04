package com.linqingying.cangjie.psi;



import com.linqingying.cangjie.lexer.CjKeywordToken;
import com.linqingying.cangjie.lexer.CjModifierKeywordToken;
import com.linqingying.cangjie.psi.stubs.CangJieModifierListStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;

public abstract class CjModifierList extends CjElementImplStub<CangJieModifierListStub> implements CjAnnotationsContainer {

    public CjModifierList(@NotNull CangJieModifierListStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }
    @NotNull
    public List<CjAnnotation> getAnnotations() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.ANNOTATION);
    }

    public CjModifierList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitModifierList(this, data);
    }
    @NotNull
    public List<CjAnnotationEntry> getAnnotationEntries() {
        return CjPsiUtilKt.collectAnnotationEntriesFromStubOrPsi(this);
    }
    @NotNull


    public boolean hasModifier(@NotNull CjModifierKeywordToken tokenType) {
        CangJieModifierListStub stub = getStub();
        if (stub != null) {
            return stub.hasModifier(tokenType);
        }
        return getModifier(tokenType) != null;
    }

    @Nullable
    public PsiElement getModifier(@NotNull CjKeywordToken tokenType) {
        return findChildByType(tokenType);
    }

    @Nullable
    public PsiElement getModifier(@NotNull TokenSet tokenTypes) {
        return findChildByType(tokenTypes);
    }


    public PsiElement getOwner() {
        return getParentByStub();
    }

    @Override
    public void deleteChildInternal(@NotNull ASTNode child) {
        super.deleteChildInternal(child);
        if (getFirstChild() == null) {
            delete();
        }
    }
}
