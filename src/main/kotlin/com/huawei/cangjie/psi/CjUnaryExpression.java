package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class CjUnaryExpression extends CjExpressionImpl implements CjOperationExpression {
    public CjUnaryExpression(ASTNode node) {
        super(node);
    }

    @Nullable
    @IfNotParsed
    public abstract CjExpression getBaseExpression();

    @Override
    @NotNull
    public CjSimpleNameExpression getOperationReference() {
        return  Objects.requireNonNull(findChildByType(CjNodeTypes.OPERATION_REFERENCE));
    }

    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }
}
