package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CjBinaryExpressionWithTypeRHS  extends CjExpressionImpl implements CjOperationExpression{


    public CjBinaryExpressionWithTypeRHS(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitBinaryWithTypeRHSExpression(this, data);
    }

    @NotNull
    public CjExpression getLeft() {
        CjExpression left = findChildByClass(CjExpression.class);
        assert left != null;
        return left;
    }

    @Nullable
    @IfNotParsed
    public CjTypeReference getRight() {
        ASTNode node = getOperationReference().getNode();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof CjTypeReference) {
                return (CjTypeReference) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @NotNull
    public CjSimpleNameExpression getOperationReference() {
        return Objects.requireNonNull(findChildByType(CjNodeTypes.OPERATION_REFERENCE));
    }
}
