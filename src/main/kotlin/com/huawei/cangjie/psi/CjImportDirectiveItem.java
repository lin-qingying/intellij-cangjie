package com.huawei.cangjie.psi;

import com.huawei.cangjie.name.FqName;
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveItemStub;
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
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
