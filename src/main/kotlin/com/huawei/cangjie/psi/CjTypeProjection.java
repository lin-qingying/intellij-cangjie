package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJieTypeProjectionStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class CjTypeProjection extends CjModifierListOwnerStub<CangJieTypeProjectionStub> {

    public CjTypeProjection(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeProjection(@NotNull CangJieTypeProjectionStub stub) {
        super(stub, CjStubElementTypes.TYPE_PROJECTION);
    }

    @NotNull
    public CjProjectionKind getProjectionKind() {
        CangJieTypeProjectionStub stub = getStub();
        if (stub != null) {
            return stub.getProjectionKind();
        }

        PsiElement projectionToken = getProjectionToken();
        IElementType token = projectionToken != null ? projectionToken.getNode().getElementType() : null;
        for (CjProjectionKind projectionKind : CjProjectionKind.values()) {
            if (projectionKind.getToken() == token) {
                return projectionKind;
            }
        }
        throw new IllegalStateException(projectionToken.getText());
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitTypeProjection(this, data);
    }

    @Nullable
    public CjTypeReference getTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

    @Nullable
    public PsiElement getProjectionToken() {
        PsiElement star = findChildByType(CjTokens.MUL);
        if (star != null) {
            return star;
        }

        CjModifierList modifierList = getModifierList();
        if (modifierList != null) {
            PsiElement element = modifierList.getModifier(CjTokens.IN_KEYWORD);
            return element;


        }

        return null;
    }
}
