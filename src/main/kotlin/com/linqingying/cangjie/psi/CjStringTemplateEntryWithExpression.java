package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


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
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitStringTemplateEntryWithExpression(this, data);
    }
}
