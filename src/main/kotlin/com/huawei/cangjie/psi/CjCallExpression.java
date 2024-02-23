package com.huawei.cangjie.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


//public class CjCallExpression extends CjExpressionImpl implements CjCallElement, CjReferenceExpression {
//    public CjCallExpression(@NotNull ASTNode node) {
//        super(node);
//    }
//
//    @Override
//    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
//        return visitor.visitCallExpression(this, data);
//    }
//
//    @Override
//    @Nullable
//    public CjExpression getCalleeExpression() {
//        return findChildByClass(CjExpression.class);
//    }
//
//
//    @NotNull
//    @Override
//    public String toString() {
//        return getNode().getElementType().toString();
//    }
//}
