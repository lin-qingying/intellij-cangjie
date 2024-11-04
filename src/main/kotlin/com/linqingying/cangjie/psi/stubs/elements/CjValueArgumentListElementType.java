package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjValueArgumentList;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CjValueArgumentListElementType extends CjPlaceHolderStubElementType<CjValueArgumentList> {
    public CjValueArgumentListElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjValueArgumentList.class);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null  ) {
            return false;
        }

        CjValueArgumentList psi = node.getPsi(CjValueArgumentList.class);
        if (psi.getArguments().isEmpty()) return false;

        return super.shouldCreateStub(node);
    }
}
