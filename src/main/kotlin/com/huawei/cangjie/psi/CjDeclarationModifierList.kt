package com.huawei.cangjie.psi;


import com.huawei.cangjie.psi.stubs.CangJieModifierListStub;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
public class CjDeclarationModifierList extends CjModifierList {
    public CjDeclarationModifierList(@NotNull ASTNode node) {
        super(node);
    }

    public CjDeclarationModifierList(@NotNull CangJieModifierListStub stub) {
        super(stub, CjStubElementTypes.MODIFIER_LIST);
    }
}
