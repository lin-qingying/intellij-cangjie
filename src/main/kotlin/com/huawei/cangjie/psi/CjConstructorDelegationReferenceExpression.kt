package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;


public class CjConstructorDelegationReferenceExpression extends CjExpressionImpl implements CjReferenceExpression {
    public CjConstructorDelegationReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }
@NotNull
    public boolean isThis() {
        return findChildByType(CjTokens.THIS_KEYWORD) != null;
    }
}
