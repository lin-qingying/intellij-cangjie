package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJieValueArgumentStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
public class CjValueArgument extends CjElementImplStub<CangJieValueArgumentStub<? extends CjValueArgument>> implements ValueArgument {
    public CjValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    public CjValueArgument(@NotNull CangJieValueArgumentStub<CjValueArgument> stub) {
        super(stub, CjStubElementTypes.VALUE_ARGUMENT);
    }

    protected CjValueArgument(CangJieValueArgumentStub<? extends CjValueArgument> stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }



    @Nullable
    @Override
    public CjExpression getArgumentExpression() {
        return null;
    }

    @Nullable
    @Override
    public ValueArgumentName getArgumentName() {
        return null;
    }

    @Override
    public boolean isNamed() {
        return false;
    }

    @NotNull
    @Override
    public CjElement asElement() {
        return null;
    }

    @Nullable
    @Override
    public LeafPsiElement getSpreadElement() {
        return null;
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    public boolean isSpread() {
     CangJieValueArgumentStub stub = getStub();
        if (stub != null) {
            return stub.isSpread();
        }

        return getSpreadElement() != null;
    }
}

