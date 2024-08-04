package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjSuperTypeEntry extends CjSuperTypeListEntry {
    public CjSuperTypeEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjSuperTypeEntry(@NotNull CangJiePlaceHolderStub<? extends CjSuperTypeListEntry> stub) {
        super(stub, CjStubElementTypes.SUPER_TYPE_ENTRY);
    }

    @Override
    public String toString() {
        return   getNode().getElementType().toString();
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitSuperTypeEntry(this, data);
    }
}
