package com.huawei.cangjie.psi;

import com.google.common.collect.Lists;
import com.huawei.cangjie.psi.stubs.CangJieBasicTypeStub;

import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


public class CjBasicType extends CjElementImplStub<CangJieBasicTypeStub> implements CjTypeElement{
    public CjBasicType(@NotNull ASTNode node) {
        super(node);
    }

    public CjBasicType(@NotNull CangJieBasicTypeStub stub) {
        super(stub, CjStubElementTypes.BASIC_TYPE);
    }

    @Override
    public String toString() {
        return  getNode().getElementType().toString();
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitBasicType(this, data);
    }
    public CjTypeArgumentList getTypeArgumentList() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<CjTypeProjection> getTypeArguments() {

        CjTypeArgumentList typeArgumentList = getTypeArgumentList();
        return typeArgumentList == null ? Collections.emptyList() : typeArgumentList.getArguments();
    }
    @Override
    public @NotNull List<CjTypeReference> getTypeArgumentsAsTypes() {
        List<CjTypeReference> result = Lists.newArrayList();
        for (CjTypeProjection projection : getTypeArguments()) {
            result.add(projection.getTypeReference());
        }
        return result;
    }
}
