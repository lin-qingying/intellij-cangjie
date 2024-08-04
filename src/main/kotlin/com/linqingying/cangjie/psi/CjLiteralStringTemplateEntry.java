package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import org.jetbrains.annotations.Nullable;

public class CjLiteralStringTemplateEntry extends CjStringTemplateEntry {
    public CjLiteralStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjLiteralStringTemplateEntry(@NotNull CangJiePlaceHolderWithTextStub<CjLiteralStringTemplateEntry> stub) {
        super(stub, CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitLiteralStringTemplateEntry(this, data);
    }

    @Override
    public String getText() {
        CangJiePlaceHolderWithTextStub<? extends CjStringTemplateEntry> stub = getStub();
        if (stub != null) {
            return stub.text();
        }

        return super.getText();
    }
}
