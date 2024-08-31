package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJieBasicTypeStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CjBasicType extends CjElementImplStub<CangJieBasicTypeStub> implements CjTypeElement {

//public class CjBasicType extends  CjElementImpl  implements CjTypeElement {


    public CjBasicType(@NotNull ASTNode node) {
        super(node);
    }

    public CjBasicType(@NotNull CangJieBasicTypeStub stub) {
        super(stub, CjStubElementTypes.BASIC_TYPE);
    }

    @Override
    public String toString() {
        return getElementType().toString();
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitBasicType(this, data);
    }

    @Override
    public String getText() {
        CangJieBasicTypeStub stub = getStub();
        if (stub != null) {
            return stub.getBasicType();


        }
        return super.getText();
    }



    @Override
    @NotNull
    public String getName() {
return getText();
    }


    @NotNull
    public List<CjTypeProjection> getTypeArguments() {
        return Collections.emptyList();

    }

    @Override
    public @NotNull List<CjTypeReference> getTypeArgumentsAsTypes() {

        return Collections.emptyList();
    }
}
