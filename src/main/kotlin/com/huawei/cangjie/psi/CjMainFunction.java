package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJieFunctionStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;

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


}
