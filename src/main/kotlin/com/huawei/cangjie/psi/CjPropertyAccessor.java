package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJiePropertyAccessorStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


public class CjPropertyAccessor extends CjDeclarationStub<CangJiePropertyAccessorStub>
        implements CjDeclarationWithBody, CjModifierListOwner, CjDeclarationWithInitializer {
    public CjPropertyAccessor(@NotNull ASTNode node) {
        super(node);
    }

    public CjPropertyAccessor(@NotNull CangJiePropertyAccessorStub stub) {
        super(stub, CjStubElementTypes.PROPERTY_ACCESSOR);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyAccessor(this, data);
    }

    public boolean isSetter() {
        CangJiePropertyAccessorStub stub = getStub();
        if (stub != null) {
            return !stub.isGetter();
        }
        return findChildByType(CjTokens.SET_KEYWORD) != null;
    }

    public boolean isGetter() {
        CangJiePropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.isGetter();
        }
        return findChildByType(CjTokens.GET_KEYWORD) != null;
    }

    @Nullable
    public CjParameterList getParameterList() {
        return getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Nullable
    public CjParameter getParameter() {
        CjParameterList parameterList = getParameterList();
        if (parameterList == null) return null;
        List<CjParameter> parameters = parameterList.getParameters();
        if (parameters.isEmpty()) return null;
        return parameters.get(0);
    }

    @NotNull
    @Override
    public List<CjParameter> getValueParameters() {
        CjParameter parameter = getParameter();
        if (parameter == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(parameter);
    }

    @Nullable
    @Override
    public CjExpression getBodyExpression() {
        CangJiePropertyAccessorStub stub = getStub();
        if (stub != null) {
            if (!stub.hasBody()) {
                return null;
            }

            if (getContainingCjFile().isCompiled()) {
                return null;
            }
        }

        return  findChildByClass(CjExpression.class);
    }

    @Nullable
    @Override
    public CjBlockExpression getBodyBlockExpression() {
        CangJiePropertyAccessorStub stub = getStub();
        if (stub != null) {
            if (!(stub.hasBlockBody() && stub.hasBody())) {
                return null;
            }
            if (getContainingCjFile().isCompiled()) {
                return null;
            }
        }

        CjExpression bodyExpression = findChildByClass(CjExpression.class);
        if (bodyExpression instanceof CjBlockExpression) {
            return (CjBlockExpression) bodyExpression;
        }

        return null;
    }

    @Override
    public boolean hasBlockBody() {
        CangJiePropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.hasBlockBody();
        }
        return getEqualsToken() == null;
    }

    @Override
    public boolean hasBody() {
        CangJiePropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.hasBody();
        }
        return getBodyExpression() != null;
    }

    @Override
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(CjTokens.EQ);
    }



    @Override
    public boolean hasDeclaredReturnType() {
        return true;
    }

    @Nullable
    public CjTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    public PsiElement getNamePlaceholder() {
        PsiElement get = findChildByType(CjTokens.GET_KEYWORD);
        if (get != null) {
            return get;
        }
        return findChildByType(CjTokens.SET_KEYWORD);
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
    @Override
    public CjExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), CjExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    @NotNull
    public CjProperty getProperty() {
        return (CjProperty) getParent();
    }

    @Override
    public int getTextOffset() {
        return getNamePlaceholder().getTextRange().getStartOffset();
    }
}
