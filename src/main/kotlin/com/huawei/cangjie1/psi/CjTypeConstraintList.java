package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import  com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
public class CjTypeConstraintList extends CjElementImplStub<CangJiePlaceHolderStub<CjTypeConstraintList>> {
    public CjTypeConstraintList(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeConstraintList(@NotNull CangJiePlaceHolderStub<CjTypeConstraintList> stub) {
        super(stub, CjStubElementTypes.TYPE_CONSTRAINT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraintList(this, data);
    }

    @NotNull
    public List<CjTypeConstraint> getConstraints() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_CONSTRAINT);
    }
}
