package com.linqingying.cangjie.psi;

import com.google.common.collect.Lists;
import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import com.linqingying.cangjie.CjNodeTypes;

public class CjArrayAccessExpression extends CjExpressionImpl implements CjReferenceExpression {
    public CjArrayAccessExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitArrayAccessExpression(this, data);
    }

    @Nullable
    @IfNotParsed
    public CjExpression getArrayExpression() {
        return findChildByClass(CjExpression.class);
    }

    @NotNull
    public List<CjExpression> getIndexExpressions() {
        return PsiTreeUtil.getChildrenOfTypeAsList(getIndicesNode(), CjExpression.class);
    }

    @NotNull
    public CjContainerNode getIndicesNode() {
        CjContainerNode indicesNode = findChildByType(CjNodeTypes.INDICES);
        assert indicesNode != null : "Can't be null because of parser";
        return indicesNode;
    }

    @NotNull
    public List<TextRange> getBracketRanges() {
        PsiElement lBracket = getLeftBracket();
        PsiElement rBracket = getRightBracket();
        if (lBracket == null || rBracket == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(lBracket.getTextRange(), rBracket.getTextRange());
    }

    @Nullable
    public PsiElement getLeftBracket() {
        return getIndicesNode().findChildByType(CjTokens.LBRACKET);
    }

    @Nullable
    public PsiElement getRightBracket() {
        return getIndicesNode().findChildByType(CjTokens.RBRACKET);
    }

    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(getRightBracket());
    }
}
