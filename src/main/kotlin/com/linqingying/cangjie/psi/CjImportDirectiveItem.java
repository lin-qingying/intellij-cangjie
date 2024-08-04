package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveItemStub;
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CjImportDirectiveItem extends CjElementImplStub<CangJieImportDirectiveItemStub>   {
    public CjImportDirectiveItem(@NotNull ASTNode node) {
        super(node);
    }

    public CjImportDirectiveItem(@NotNull CangJieImportDirectiveItemStub stub) {
        super(stub, CjStubElementTypes.IMPORT_DIRECTIVE_ITEM);
    }

}
