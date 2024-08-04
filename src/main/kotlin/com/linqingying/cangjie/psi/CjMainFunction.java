package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CjMainFunction extends CjFunctionImpl {
    public CjMainFunction(@NotNull CangJieFunctionStub stub) {
        super(stub, CjStubElementTypes.MAIN_FUNC);
    }
    public CjMainFunction(@NotNull ASTNode node) {
        super(node);
    }
    public CjMainFunction(@NotNull CangJieFunctionStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }
    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitMainFunction(this, data);
    }

}
