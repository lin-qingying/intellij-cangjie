package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;

public class CjLiteralStringTemplateEntry extends CjStringTemplateEntry {
    public CjLiteralStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjLiteralStringTemplateEntry(@NotNull CangJiePlaceHolderWithTextStub<CjLiteralStringTemplateEntry> stub) {
        super(stub, CjStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
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
