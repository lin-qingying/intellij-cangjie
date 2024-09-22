package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJieForeignDirectiveStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
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
