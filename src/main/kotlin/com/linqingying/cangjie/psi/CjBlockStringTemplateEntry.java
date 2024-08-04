package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjBlockStringTemplateEntry extends CjStringTemplateEntryWithExpression {
    public CjBlockStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjBlockStringTemplateEntry(@NotNull CangJiePlaceHolderWithTextStub<CjBlockStringTemplateEntry> stub) {
        super(stub, CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitBlockStringTemplateEntry(this, data);
    }
}
