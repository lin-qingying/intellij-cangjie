package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.psi.stubs.CangJieConstantExpressionStub;
import com.linqingying.cangjie.psi.stubs.elements.CjConstantExpressionElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjConstantExpression
        extends CjElementImplStub<CangJieConstantExpressionStub> implements CjExpression {
    public CjConstantExpression(@NotNull ASTNode node) {
        super(node);
    }

    public CjConstantExpression(@NotNull CangJieConstantExpressionStub stub) {
        super(stub, CjConstantExpressionElementType.Companion.kindToConstantElementType(stub.kind()));
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitConstantExpression(this, data);
    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return CjExpressionImpl.Companion.replaceExpression(this, newElement, true, super::replace);
    }
}
