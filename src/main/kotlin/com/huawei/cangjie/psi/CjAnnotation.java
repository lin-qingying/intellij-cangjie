package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CjAnnotation extends CjElementImplStub<CangJiePlaceHolderStub<CjAnnotation>>  {

    public CjAnnotation(@NotNull ASTNode node) {
        super(node);
    }

    public CjAnnotation(CangJiePlaceHolderStub<CjAnnotation> stub) {
        super(stub, CjStubElementTypes.ANNOTATION);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotation(this, data);
    }

    public List<CjAnnotationEntry> getEntries() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.ANNOTATION_ENTRY);
    }



    public void removeEntry(@NotNull CjAnnotationEntry entry) {
        if (getEntries().size() > 1) {
            entry.delete();
        }
        else {
            delete();
        }
    }
}

