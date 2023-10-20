package com.huawei.cangjie.psi.stubs.elements;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.CjDotQualifiedExpression;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;



public class CjDotQualifiedExpressionElementType extends CjPlaceHolderStubElementType<CjDotQualifiedExpression> {
    public CjDotQualifiedExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjDotQualifiedExpression.class);
    }

    private static boolean checkNodeTypesTraversal(ASTNode node) {

        IElementType type = node.getElementType();
        if (type != CjStubElementTypes.DOT_QUALIFIED_EXPRESSION &&
                type != CjStubElementTypes.REFERENCE_EXPRESSION &&
                type != CjTokens.IDENTIFIER &&
                type != CjTokens.DOT
        ) {
            return false;
        }

        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (!checkNodeTypesTraversal(child)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null) return false;

        IElementType parentElementType = treeParent.getElementType();
        if (
                parentElementType == CjStubElementTypes.PACKAGE_DIRECTIVE ||
                parentElementType == CjStubElementTypes.VALUE_ARGUMENT ||

                parentElementType == CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
        ) {
            return checkNodeTypesTraversal(node) && super.shouldCreateStub(node);
        }

        return false;
    }
}
