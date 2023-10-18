package com.huawei.cangjie1.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;



public class CjCallExpression extends CjExpressionImpl implements CjCallElement, CjReferenceExpression {
    public CjCallExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitCallExpression(this, data);
    }

    @Override
    @Nullable
    public CjExpression getCalleeExpression() {
        return findChildByClass(CjExpression.class);
    }







}
