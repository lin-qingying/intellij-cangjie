package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;



public class CjImportList extends CjElementImplStub<CangJiePlaceHolderStub<CjImportList>> {

    public CjImportList(@NotNull ASTNode node) {
        super(node);
    }

    public CjImportList(@NotNull CangJiePlaceHolderStub<CjImportList> stub) {
        super(stub, CjStubElementTypes.IMPORT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitImportList(this, data);
    }

    @NotNull
    public List<CjImportDirective> getImports() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.IMPORT_DIRECTIVE);
    }
}
