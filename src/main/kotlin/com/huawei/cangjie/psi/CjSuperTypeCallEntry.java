package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjSuperTypeCallEntry extends CjSuperTypeListEntry implements CjCallElement {
    public CjSuperTypeCallEntry(@NotNull ASTNode node) {
        super(node);
    }

    public CjSuperTypeCallEntry(@NotNull CangJiePlaceHolderStub<? extends CjSuperTypeListEntry> stub) {
        super(stub, CjStubElementTypes.SUPER_TYPE_CALL_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitSuperTypeCallEntry(this, data);
    }

    @NotNull
    @Override
    public CjConstructorCalleeExpression getCalleeExpression() {
        return getRequiredStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public @Nullable CjValueArgumentList getValueArgumentList() {
        return (CjValueArgumentList) findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST);

    }


    @Override
    public CjTypeReference getTypeReference() {
        return getCalleeExpression().getTypeReference();
    }



}
