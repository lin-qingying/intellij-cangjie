package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class CjConstructorDelegationCall extends CjElementImpl implements CjCallElement {
    public CjConstructorDelegationCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitConstructorDelegationCall(this, data);
    }



    @Nullable
    @Override
    public CjConstructorDelegationReferenceExpression getCalleeExpression() {
        return findChildByClass(CjConstructorDelegationReferenceExpression.class);
    }

    @Override
    public @NotNull List<CjLambdaArgument> getLambdaArguments() {
        return null;
    }

    @Override
    public @Nullable CjTypeArgumentList getTypeArgumentList() {
        return null;
    }

    @Override
        public @Nullable CjValueArgumentList getValueArgumentList() {
        return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST);

    }


    public boolean isImplicit() {
        CjConstructorDelegationReferenceExpression callee = getCalleeExpression();
        return callee != null && callee.getFirstChild() == null;
    }

    public boolean isCallToThis() {
        CjConstructorDelegationReferenceExpression callee = getCalleeExpression();
        return callee != null && callee.isThis();
    }
}
