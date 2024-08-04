package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import org.jetbrains.annotations.Nullable;

public class CjEscapeStringTemplateEntry extends CjStringTemplateEntry {
    public CjEscapeStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjEscapeStringTemplateEntry(@NotNull CangJiePlaceHolderWithTextStub<CjEscapeStringTemplateEntry> stub) {
        super(stub, CjStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitEscapeStringTemplateEntry(this, data);
    }

    public String getUnescapedValue() {
        return StringUtil.unescapeStringCharacters(getText());
    }
}
