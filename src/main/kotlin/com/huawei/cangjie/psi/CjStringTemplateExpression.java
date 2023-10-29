package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.*;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CjStringTemplateExpression extends CjElementImplStub<CangJiePlaceHolderStub<CjStringTemplateExpression>>
        implements CjExpression, PsiLanguageInjectionHost, ContributedReferenceHost {
    private static final TokenSet CLOSE_QUOTE_TOKEN_SET = TokenSet.create(CjTokens.CLOSING_QUOTE);

    public CjStringTemplateExpression(@NotNull ASTNode node) {
        super(node);
    }

    public CjStringTemplateExpression(@NotNull CangJiePlaceHolderStub<CjStringTemplateExpression> stub) {
        super(stub, CjStubElementTypes.STRING_TEMPLATE);
    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return CjExpressionImpl.Companion.replaceExpression(this, newElement, true, super::replace);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitStringTemplateExpression(this, data);
    }

    private static final TokenSet STRING_ENTRIES_TYPES = TokenSet.create(
            CjStubElementTypes.LONG_STRING_TEMPLATE_ENTRY

    );

    @NotNull
    public CjStringTemplateEntry[] getEntries() {
        return getStubOrPsiChildren(STRING_ENTRIES_TYPES, CjStringTemplateEntry.EMPTY_ARRAY);
    }

    @Override
    public boolean isValidHost() {
        return getNode().getChildren(CLOSE_QUOTE_TOKEN_SET).length != 0;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        CjExpression newExpression = new CjPsiFactory(getProject()).createExpressionIfPossible(text);
        if (newExpression instanceof CjStringTemplateExpression) return (CjStringTemplateExpression) replace(newExpression);
        return ElementManipulators.handleContentChange(this, text);
    }

    @NotNull
    @Override
    public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new CangJieStringLiteralTextEscaper(this);
    }

    public boolean hasInterpolation() {
        for (PsiElement child : getChildren()) {
            if (child instanceof CjSimpleNameStringTemplateEntry || child instanceof CjBlockStringTemplateEntry) {
                return true;
            }
        }

        return false;
    }
}
