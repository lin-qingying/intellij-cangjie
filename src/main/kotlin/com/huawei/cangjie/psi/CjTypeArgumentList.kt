package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;

public class CjTypeArgumentList extends CjElementImplStub<CangJiePlaceHolderStub<CjTypeArgumentList>> {
    public CjTypeArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeArgumentList(@NotNull CangJiePlaceHolderStub<CjTypeArgumentList> stub) {
        super(stub, CjStubElementTypes.TYPE_ARGUMENT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitTypeArgumentList(this, data);
    }

    @NotNull
    public List<CjTypeProjection> getArguments() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_PROJECTION);
    }

    @NotNull
    public CjTypeProjection addArgument(@NotNull CjTypeProjection typeArgument) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getArguments(), typeArgument, CjTokens.LT);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(findChildByType(CjTokens.GT));
    }
}
