package com.huawei.cangjie1.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;



public class CjConstructorDelegationCall extends CjElementImpl implements CjCallElement {
    public CjConstructorDelegationCall(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitConstructorDelegationCall(this, data);
    }



    @Nullable
    @Override
    public CjConstructorDelegationReferenceExpression getCalleeExpression() {
        return findChildByClass(CjConstructorDelegationReferenceExpression.class);
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
