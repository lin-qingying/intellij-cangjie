package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJieFunctionStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CjMacroFunction extends CjFunctionImpl{

    public CjMacroFunction(@NotNull ASTNode node) {
        super(node);
    }

    public CjMacroFunction(@NotNull CangJieFunctionStub stub) {
        super(stub, CjStubElementTypes.MACRO);
    }

}
