package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJieForeignDirectiveStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;


public class CjForeignDirective extends CjElementImplStub<CangJieForeignDirectiveStub>{

    public CjForeignDirective(@NotNull ASTNode node) {
        super(node);
    }

    public CjForeignDirective(@NotNull CangJieForeignDirectiveStub stub) {
        super(stub, CjStubElementTypes.FOREIGN);
    }

}
