package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.lexer.CjTokens;
import com.huawei.cangjie1.psi.stubs.CangJieTypeParameterStub;
import com.huawei.cangjie1.types.Variance;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;

public class CjTypeParameter extends CjNamedDeclarationStub<CangJieTypeParameterStub> {

    public CjTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeParameter(@NotNull CangJieTypeParameterStub stub) {
        super(stub, CjStubElementTypes.TYPE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
    }

    @NotNull
    public Variance getVariance() {
        CangJieTypeParameterStub stub = getStub();
        if (stub != null) {

            if (stub.isInVariance()) return Variance.IN_VARIANCE;
            return Variance.INVARIANT;
        }

        CjModifierList modifierList = getModifierList();
        if (modifierList == null) return Variance.INVARIANT;


        if (modifierList.hasModifier(CjTokens.IN_KEYWORD)) return Variance.IN_VARIANCE;
        return Variance.INVARIANT;
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
