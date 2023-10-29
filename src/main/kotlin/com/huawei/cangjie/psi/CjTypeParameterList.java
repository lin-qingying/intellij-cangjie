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

public class CjTypeParameterList extends CjElementImplStub<CangJiePlaceHolderStub<CjTypeParameterList>> {
    public CjTypeParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeParameterList(@NotNull CangJiePlaceHolderStub<CjTypeParameterList> stub) {
        super(stub, CjStubElementTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @NotNull
    public List<CjTypeParameter> getParameters() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_PARAMETER);
    }

    @NotNull
    public CjTypeParameter addParameter(@NotNull CjTypeParameter typeParameter) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getParameters(), typeParameter, CjTokens.LT);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitTypeParameterList(this, data);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(findChildByType(CjTokens.GT));
    }
}
