package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public  class CjEnumEntry  extends CjElementImplStub<CangJiePlaceHolderStub<? extends CjEnumEntry>>{


    public CjEnumEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjEnumEntry(
            @NotNull CangJiePlaceHolderStub<? extends CjEnumEntry> stub,
            @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitEnumEntry(this, data);
    }
    @Override
    public String toString() {
        return   getNode().getElementType().toString();
    }
    @Nullable
    public CjTypeReference getTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

}
