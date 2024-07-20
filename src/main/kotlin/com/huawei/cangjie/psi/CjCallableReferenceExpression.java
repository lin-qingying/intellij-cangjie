package com.huawei.cangjie.psi;


import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.lexer.CjTokens;

public class CjCallableReferenceExpression extends CjExpressionImpl implements CjDoubleColonExpression {
    public CjCallableReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public CjSimpleNameExpression getCallableReference() {
        PsiElement psi = getDoubleColonTokenReference();
        while (psi != null) {
            if (psi instanceof CjSimpleNameExpression) {
                return (CjSimpleNameExpression) psi;
            }
            psi = psi.getNextSibling();
        }

        throw new IllegalStateException("Callable reference simple name shouldn't be parsed to null");
    }

    @Nullable
    @Override
    public PsiElement findColonColon() {
        return null;
//        return findChildByType(CjTokens.COLONCOLON);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitCallableReferenceExpression(this, data);
    }
}
