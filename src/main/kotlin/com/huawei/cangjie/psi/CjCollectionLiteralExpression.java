package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt.getTrailingCommaByClosingElement;


public class CjCollectionLiteralExpression extends CjElementImplStub<CangJieCollectionLiteralExpressionStub> implements CjReferenceExpression {
    public CjCollectionLiteralExpression(@NotNull CangJieCollectionLiteralExpressionStub stub) {
        super(stub, CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION);
    }

    public CjCollectionLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitCollectionLiteralExpression(this, data);
    }

    @Nullable
    public PsiElement getLeftBracket() {
        ASTNode astNode = getNode().findChildByType(CjTokens.LBRACKET);
        return astNode != null ? astNode.getPsi() : null;
    }

    @Nullable
    public PsiElement getRightBracket() {
        ASTNode astNode = getNode().findChildByType(CjTokens.RBRACKET);
        return astNode != null ? astNode.getPsi() : null;
    }

    @Nullable
    public PsiElement getTrailingComma() {
        PsiElement rightBracket = getRightBracket();
        return getTrailingCommaByClosingElement(rightBracket);
    }

//    public List<CjExpression> getInnerExpressions() {
//        CangJieCollectionLiteralExpressionStub stub = getStub();
//        if (stub != null) {
//
//            return Arrays.asList(stub.getChildrenByType(CjStubElementTypes.CONSTANT_EXPRESSIONS_TYPES, CjExpression.EMPTY_ARRAY));
//        }
//        return PsiTreeUtil.getChildrenOfTypeAsList(this, CjExpression.class);
//    }
}
