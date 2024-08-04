package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import org.jetbrains.annotations.Nullable;

public class CjSimpleNameStringTemplateEntry extends CjStringTemplateEntryWithExpression {
    public CjSimpleNameStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjSimpleNameStringTemplateEntry(@NotNull CangJiePlaceHolderWithTextStub<CjSimpleNameStringTemplateEntry> stub) {
        super(stub, CjStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitSimpleNameStringTemplateEntry(this, data);
    }
}
