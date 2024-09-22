package com.huawei.cangjie.psi;

import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CjValueArgumentName extends CjElementImplStub<CangJiePlaceHolderStub<CjValueArgumentName>> implements ValueArgumentName {
    public CjValueArgumentName(@NotNull ASTNode node) {
        super(node);
    }

    public CjValueArgumentName(@NotNull CangJiePlaceHolderStub<CjValueArgumentName> stub) {
        super(stub, CjStubElementTypes.VALUE_ARGUMENT_NAME);
    }

    @Override
    @NotNull
    public CjSimpleNameExpression getReferenceExpression() {
        return getStubOrPsiChild(CjStubElementTypes.REFERENCE_EXPRESSION);
    }

    @NotNull
    @Override
    public Name getAsName() {
        return getReferenceExpression().getReferencedNameAsName();
    }
}
