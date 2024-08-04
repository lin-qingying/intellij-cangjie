package com.linqingying.cangjie.psi;

import com.google.common.collect.Lists;
import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;



public class CjUserType extends  CjElementImplStub<CangJieUserTypeStub> implements CjTypeElement{


    public CjUserType(@NotNull ASTNode node) {
        super(node);
    }

    public CjUserType(@NotNull CangJieUserTypeStub stub) {
        super(stub, CjStubElementTypes.USER_TYPE);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitUserType(this, data);
    }

    @Nullable
    public CjTypeArgumentList getTypeArgumentList() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<CjTypeProjection> getTypeArguments() {

        CjTypeArgumentList typeArgumentList = getTypeArgumentList();
        return typeArgumentList == null ? Collections.emptyList() : typeArgumentList.getArguments();
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @NotNull
    @Override
    public List<CjTypeReference> getTypeArgumentsAsTypes() {
        List<CjTypeReference> result = Lists.newArrayList();
        for (CjTypeProjection projection : getTypeArguments()) {
            result.add(projection.getTypeReference());
        }
        return result;
    }

    @Nullable @IfNotParsed
    public CjSimpleNameExpression getReferenceExpression() {
        CjNameReferenceExpression nameRefExpr = getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION);
        return  nameRefExpr  ;
    }

    @Nullable
    public CjUserType getQualifier() {
        return getStubOrPsiChild(CjStubElementTypes.USER_TYPE);
    }

    public void deleteQualifier() {
        CjUserType qualifier = getQualifier();
        assert qualifier != null;
        PsiElement dot = findChildByType(CjTokens.DOT);
        assert dot != null;
        qualifier.delete();
        dot.delete();
    }

    @Nullable
    public String getReferencedName() {
        CjSimpleNameExpression referenceExpression = getReferenceExpression();
        return referenceExpression == null ? null : referenceExpression.getReferencedName();
    }
}
