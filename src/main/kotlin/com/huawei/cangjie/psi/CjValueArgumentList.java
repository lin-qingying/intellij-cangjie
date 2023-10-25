package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CjValueArgumentList  extends CjElementImplStub<CangJiePlaceHolderStub<CjValueArgumentList>> {
    public CjValueArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    public CjValueArgumentList(@NotNull CangJiePlaceHolderStub<CjValueArgumentList> stub) {
        super(stub, CjStubElementTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitValueArgumentList(this, data);
    }

    @NotNull
    public List<CjValueArgument> getArguments() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.VALUE_ARGUMENT);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(CjTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(CjTokens.LPAR);
    }

    @NotNull
    public CjValueArgument addArgument(@NotNull CjValueArgument argument) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getArguments(), argument);
    }

    @NotNull
    public CjValueArgument addArgumentAfter(@NotNull CjValueArgument argument, @Nullable CjValueArgument anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemAfter(this, getArguments(), argument, anchor);
    }

    @NotNull
    public CjValueArgument addArgumentBefore(@NotNull CjValueArgument argument, @Nullable CjValueArgument anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemBefore(this, getArguments(), argument, anchor);
    }

    public void removeArgument(@NotNull CjValueArgument argument) {
        assert argument.getParent() == this;
        EditCommaSeparatedListHelper.INSTANCE.removeItem(argument);
    }

    public void removeArgument(int index) {
        removeArgument(getArguments().get(index));
    }

    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(getRightParenthesis());
    }
}
