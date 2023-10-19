package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.lexer.CjTokens;
import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
import com.huawei.cangjie1.psi.stubs.elements.CjTokenSets;
public class CjSuperTypeList extends CjElementImplStub<CangJiePlaceHolderStub<CjSuperTypeList>> {
    private final AtomicLong modificationStamp = new AtomicLong();

    public CjSuperTypeList(@NotNull ASTNode node) {
        super(node);
    }

    public CjSuperTypeList(@NotNull CangJiePlaceHolderStub<CjSuperTypeList> stub) {
        super(stub, CjStubElementTypes.SUPER_TYPE_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeList(this, data);
    }

    @NotNull
    public CjSuperTypeListEntry addEntry(@NotNull CjSuperTypeListEntry entry) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getEntries(), entry);
    }

    public void removeEntry(@NotNull CjSuperTypeListEntry entry) {
        EditCommaSeparatedListHelper.INSTANCE.removeItem(entry);
        if (getEntries().isEmpty()) {
            delete();
        }
    }

    @Override
    public void delete() throws IncorrectOperationException {
        PsiElement left = PsiTreeUtil.skipSiblingsBackward(this, PsiWhiteSpace.class, PsiComment.class);
        if (left == null || left.getNode().getElementType() != CjTokens.COLON) left = this;
        getParent().deleteChildRange(left, this);
    }

    public List<CjSuperTypeListEntry> getEntries() {
        return Arrays.asList(getStubOrPsiChildren(CjTokenSets.SUPER_TYPE_LIST_ENTRIES, CjSuperTypeListEntry.ARRAY_FACTORY));
    }


    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        modificationStamp.getAndIncrement();
    }

    public long getModificationStamp() {
        return modificationStamp.get();
    }
}
