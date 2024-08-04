package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjMatchEntry extends CjElementImpl {
    public CjMatchEntry(@NotNull ASTNode node) {
        super(node);
    }

    public boolean is_() {
        return get_Keyword() != null;
    }

    public PsiElement get_Keyword() {
        return findChildByType(CjTokens.UNDERLINE);
    }

    @Nullable
    public CjExpression getExpression() {
        return findChildByClass(CjExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitMatchEntry(this, data);
    }

    @NotNull
    public CjMatchCondition[] getConditions() {
        return findChildrenByClass(CjMatchCondition.class);
    }

    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(getArrow());
    }

    @Nullable
    public PsiElement getArrow() {
        return findChildByType(CjTokens.DOUBLE_ARROW);
    }
}
