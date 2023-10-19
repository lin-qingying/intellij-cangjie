package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;


public class CjSuperTypeEntry extends CjSuperTypeListEntry {
    public CjSuperTypeEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjSuperTypeEntry(@NotNull CangJiePlaceHolderStub<? extends CjSuperTypeListEntry> stub) {
        super(stub, CjStubElementTypes.SUPER_TYPE_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeEntry(this, data);
    }
}
