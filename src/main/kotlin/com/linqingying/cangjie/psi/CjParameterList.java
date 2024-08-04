package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import java.util.List;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;

public class CjParameterList extends CjElementImplStub<CangJiePlaceHolderStub<CjParameterList>> {
    public CjParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public CjParameterList(@NotNull CangJiePlaceHolderStub<CjParameterList> stub) {
        super(stub, CjStubElementTypes.VALUE_PARAMETER_LIST);
    }


    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitParameterList(this, data);
    }

    @Override
    public PsiElement getParent() {
        CangJiePlaceHolderStub<CjParameterList> stub = getStub();
        return stub != null ? stub.getParentStub().getPsi() : super.getParent();
    }

    @NotNull
    public List<CjParameter> getParameters() {
        return getStubOrPsiChildrenAsList(CjStubElementTypes.VALUE_PARAMETER);
    }

    @NotNull
    public CjParameter addParameter(@NotNull CjParameter parameter) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getParameters(), parameter);
    }

    @NotNull
    public CjParameter addParameterBefore(@NotNull CjParameter parameter, @Nullable CjParameter anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemBefore(this, getParameters(), parameter, anchor);
    }

    @NotNull
    public CjParameter addParameterAfter(@NotNull CjParameter parameter, @Nullable CjParameter anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemAfter(this, getParameters(), parameter, anchor);
    }

    public void removeParameter(@NotNull CjParameter parameter) {
        EditCommaSeparatedListHelper.INSTANCE.removeItem(parameter);
    }

    public void removeParameter(int index) {
        removeParameter(getParameters().get(index));
    }

    public CjDeclarationWithBody getOwnerFunction() {
        PsiElement parent = getParentByStub();
        if (!(parent instanceof CjDeclarationWithBody)) return null;
        return (CjDeclarationWithBody) parent;
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(CjTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(CjTokens.LPAR);
    }

    @Nullable
    public PsiElement getFirstComma() {
        return findChildByType(CjTokens.COMMA);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        PsiElement parentElement = getParent();
//        if (parentElement instanceof CjFunctionLiteral || parentElement instanceof CjPropertyAccessor) {
//            return CjPsiUtilKt.getTrailingCommaByElementsList(this);
//        } else {
            return CjPsiUtilKt.getTrailingCommaByClosingElement(getRightParenthesis());
//        }
    }
}
