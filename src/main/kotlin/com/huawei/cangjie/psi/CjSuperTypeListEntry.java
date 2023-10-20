package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class CjSuperTypeListEntry extends CjElementImplStub<CangJiePlaceHolderStub<? extends CjSuperTypeListEntry>> {
    private static final CjSuperTypeListEntry[] EMPTY_ARRAY = new CjSuperTypeListEntry[0];

    public static ArrayFactory<CjSuperTypeListEntry> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new CjSuperTypeListEntry[count];

    public CjSuperTypeListEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjSuperTypeListEntry(
            @NotNull CangJiePlaceHolderStub<? extends CjSuperTypeListEntry> stub,
            @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeListEntry(this, data);
    }
    @Override
    public String toString() {
        return   getNode().getElementType().toString();
    }
    @Nullable
    public CjTypeReference getTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

    @Nullable
    public CjUserType getTypeAsUserType() {
        CjTypeReference reference = getTypeReference();
        if (reference != null) {
            CjTypeElement element = reference.getTypeElement();
            if (element instanceof CjUserType) {
                return ((CjUserType) element);
            }
        }
        return null;
    }
}
