package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;


public abstract class CjStringTemplateEntryWithExpression extends CjStringTemplateEntry {
    public CjStringTemplateEntryWithExpression(@NotNull ASTNode node) {
        super(node);
    }

    public CjStringTemplateEntryWithExpression(
            @NotNull CangJiePlaceHolderWithTextStub<? extends CjStringTemplateEntryWithExpression> stub,
            @NotNull IStubElementType elementType) {
        super(stub, elementType);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateEntryWithExpression(this, data);
    }
}
