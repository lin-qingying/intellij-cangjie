package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.name.SpecialNames;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjFunctionLiteral extends CjFunctionNotStubbed {
    public CjFunctionLiteral(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean hasBlockBody() {
        return false;
    }

    @Override
    public String getName() {
        return SpecialNames.ANONYMOUS_STRING;
    }

    @Override
    public PsiElement getNameIdentifier() {
        return null;
    }

    public boolean hasParameterSpecification() {
        return findChildByType(CjTokens.ARROW) != null;
    }

    @Override
    public CjBlockExpression getBodyExpression() {
        return (CjBlockExpression) super.getBodyExpression();
    }

    @Nullable
    @Override
    public PsiElement getEqualsToken() {
        return null;
    }

    @NotNull
    public PsiElement getLBrace() {
        return findChildByType(CjTokens.LBRACE);
    }

    @Nullable
    @IfNotParsed
    public PsiElement getRBrace() {
        return findChildByType(CjTokens.RBRACE);
    }

    @Nullable
    public PsiElement getArrow() {
        return findChildByType(CjTokens.ARROW);
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @Override
    public boolean hasBody() {
        return getBodyExpression() != null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return new LocalSearchScope(this);
    }
}
