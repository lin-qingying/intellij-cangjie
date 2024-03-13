package com.huawei.cangjie.psi;

import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementType;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CjElementImplStub <T extends StubElement<?>> extends StubBasedPsiElementBase<T>
        implements CjElement, StubBasedPsiElement<T>{
    public CjElementImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public CjElementImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public CjElement getPsiOrParent() {
        return this;
    }


    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }


    @Override
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        return (references.length > 0) ? references[0] : null;
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
        return CangJieReferenceProvidersService.getReferencesFromProviders(this);

    }

    @NotNull
    @Override
    public CjFile getContainingCjFile() {
        PsiFile file = getContainingFile();
        if (!(file instanceof CjFile)) {

            String fileString = "";
            if (file.isValid()) {
                try {
                    fileString = " " + file.getText();
                }
                catch (Exception e) {

                }
            }
            // getNode() will fail if getContainingFile() returns not PsiFileImpl instance
            String nodeString = (file instanceof PsiFileImpl ? (" node = " + getNode()) : "");

            throw new IllegalStateException("CjElement not inside CjFile: " +
                    file + fileString + " of type " + file.getClass() +
                    " for element " + this + " of type " + this.getClass() + nodeString);
        }
        return (CjFile) file;
    }


    @Override
    public @NotNull Language getLanguage() {
        return CangJieLanguage.INSTANCE;
    }


    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof CjVisitor) {
            accept((CjVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public <D> void acceptChildren(@NotNull CjVisitor<Void, D> visitor, D data) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof CjElement) {
                ((CjElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @NotNull
    protected <PsiT extends CjElementImplStub<?>, StubT extends StubElement<?>> List<PsiT> getStubOrPsiChildrenAsList(
            @NotNull CjStubElementType<StubT, PsiT> elementType
    ) {
        return Arrays.asList(getStubOrPsiChildren(elementType, elementType.getArrayFactory()));
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitCjElement(this, data);
    }
}
