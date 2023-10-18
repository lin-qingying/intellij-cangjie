package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;



public class CjTypeConstraint extends CjElementImplStub<CangJiePlaceHolderStub<CjTypeConstraint>>
        implements  CjElement{
    public CjTypeConstraint(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeConstraint(@NotNull CangJiePlaceHolderStub<CjTypeConstraint> stub) {
        super(stub, CjStubElementTypes.TYPE_CONSTRAINT);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraint(this, data);
    }

    @Nullable
    @IfNotParsed
    public CjSimpleNameExpression getSubjectTypeParameterName() {
        return getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION);
    }

    @Nullable @IfNotParsed
    public CjTypeReference getBoundTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }



}
