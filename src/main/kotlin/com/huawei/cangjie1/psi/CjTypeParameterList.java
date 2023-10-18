package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.lexer.CjTokens;
import com.huawei.cangjie1.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;

public class CjTypeParameterList extends CjElementImplStub<CangJiePlaceHolderStub<CjTypeParameterList>> {
    public CjTypeParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public CjTypeParameterList(@NotNull CangJiePlaceHolderStub<CjTypeParameterList> stub) {
        super(stub, CjStubElementTypes.TYPE_PARAMETER_LIST);
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
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterList(this, data);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(findChildByType(CjTokens.GT));
    }
}
