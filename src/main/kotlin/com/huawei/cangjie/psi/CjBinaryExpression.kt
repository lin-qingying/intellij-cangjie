package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class CjBinaryExpression extends CjExpressionImpl implements CjOperationExpression{

    public CjBinaryExpression(@NotNull ASTNode node) {
        super(node);
    }


    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitBinaryExpression(this, data);
    }

    @Nullable
    @IfNotParsed
    public CjExpression getLeft() {
        ASTNode node = getOperationReference().getNode().getTreePrev();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof CjExpression) {
                return (CjExpression) psi;
            }
            node = node.getTreePrev();
        }

        return null;
    }

    @Nullable @IfNotParsed
    public CjExpression getRight() {
        ASTNode node = getOperationReference().getNode().getTreeNext();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof CjExpression) {
                return (CjExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @NotNull
    public CjOperationReferenceExpression getOperationReference() {
        PsiElement operationReference = findChildByType(CjNodeTypes.OPERATION_REFERENCE);
        if (operationReference == null) {
            throw new NullPointerException("No operation reference for binary expression: " + Arrays.toString(getChildren()));
        }

        return (CjOperationReferenceExpression) operationReference;
    }

    @NotNull
    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }

}
