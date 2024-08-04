package com.linqingying.cangjie.psi;


import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CjStringTemplateEntry extends CjElementImplStub<CangJiePlaceHolderWithTextStub<? extends CjStringTemplateEntry>> {
    public static final CjStringTemplateEntry[] EMPTY_ARRAY = new CjStringTemplateEntry[0];

    public CjStringTemplateEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjStringTemplateEntry(
            @NotNull CangJiePlaceHolderWithTextStub<? extends CjStringTemplateEntry> stub,
            @NotNull IStubElementType elementType
    ) {
        super(stub, elementType);
    }

    @Nullable
    public CjExpression getExpression() {
        return findChildByClass(CjExpression.class);
    }
}
