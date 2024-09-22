package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class CjConstructorCalleeExpression extends CjExpressionImplStub<CangJiePlaceHolderStub<CjConstructorCalleeExpression>> {
    public CjConstructorCalleeExpression(@NotNull ASTNode node) {
        super(node);
    }

    public CjConstructorCalleeExpression(@NotNull CangJiePlaceHolderStub<CjConstructorCalleeExpression> stub) {
        super(stub, CjStubElementTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitConstructorCalleeExpression(this, data);
    }

    @Nullable
    @IfNotParsed
    public CjTypeReference getTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

    @Nullable @IfNotParsed
    public CjSimpleNameExpression getConstructorReferenceExpression() {
        CjTypeReference typeReference = getTypeReference();
        if (typeReference == null) {
            return null;
        }
        CjTypeElement typeElement = typeReference.getTypeElement();
        if (!(typeElement instanceof CjUserType)) {
            return null;
        }
        return ((CjUserType) typeElement).getReferenceExpression();
    }

}
