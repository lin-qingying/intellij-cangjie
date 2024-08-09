package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CjIsExpression extends CjExpressionImpl implements CjOperationExpression {

    public CjIsExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitIsExpression(this, data);
    }

    @NotNull
    public CjExpression getLeftHandSide() {
        return findChildByClass(CjExpression.class);
    }

    @Nullable
    @IfNotParsed
    public CjTypeReference getTypeReference() {
        return findChildByType(CjNodeTypes.TYPE_REFERENCE);
    }

    @Override
    @NotNull
    public CjSimpleNameExpression getOperationReference() {
        return findChildByType(CjNodeTypes.OPERATION_REFERENCE);
    }



}
