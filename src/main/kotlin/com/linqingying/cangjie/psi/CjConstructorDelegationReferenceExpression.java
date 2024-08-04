package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.lexer.CjTokens;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;


public class CjConstructorDelegationReferenceExpression extends CjExpressionImpl implements CjReferenceExpression {
    public CjConstructorDelegationReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    public boolean isThis() {
        return findChildByType(CjTokens.THIS_KEYWORD) != null;
    }
}
