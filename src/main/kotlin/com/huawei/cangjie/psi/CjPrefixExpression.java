package com.huawei.cangjie.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CjPrefixExpression extends CjUnaryExpression {
    public CjPrefixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitPrefixExpression(this, data);
    }

    @Override
    @Nullable
    @IfNotParsed
    public CjExpression getBaseExpression() {
        return PsiTreeUtil.getNextSiblingOfType(getOperationReference(), CjExpression.class);
    }
}
