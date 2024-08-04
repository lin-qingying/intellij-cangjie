package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
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
