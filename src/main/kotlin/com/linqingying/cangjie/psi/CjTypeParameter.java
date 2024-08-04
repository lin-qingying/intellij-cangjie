package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.stubs.CangJieTypeParameterStub;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;

public class CjTypeParameter extends CjNamedDeclarationStub<CangJieTypeParameterStub> {

    public CjTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeParameter(@NotNull CangJieTypeParameterStub stub) {
        super(stub, CjStubElementTypes.TYPE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitTypeParameter(this, data);
    }


    @Override
    public String toString() {
        return  getNode().getElementType().toString();
    }


    @Nullable
    public CjTypeReference setExtendsBound(@Nullable CjTypeReference typeReference) {
        CjTypeReference currentExtendsBound = getExtendsBound();
        if (currentExtendsBound != null) {
            if (typeReference == null) {
                PsiElement colon = findChildByType(CjTokens.COLON);
                if (colon != null) colon.delete();
                currentExtendsBound.delete();
                return null;
            }
            return (CjTypeReference) currentExtendsBound.replace(typeReference);
        }

        if (typeReference != null) {
            PsiElement colon = addAfter(new CjPsiFactory(getProject()).createColon(), getNameIdentifier());
            return (CjTypeReference) addAfter(typeReference, colon);
        }

        return null;
    }

    @Nullable
    public CjTypeReference getExtendsBound() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        CjTypeParameterListOwner owner = PsiTreeUtil.getParentOfType(this, CjTypeParameterListOwner.class);
        return new LocalSearchScope(owner != null ? owner : this);
    }
}
