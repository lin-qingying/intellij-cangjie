package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;

import com.huawei.cangjie.CjNodeTypes;

import static com.huawei.cangjie.lexer.CjTokens.*;

public class CjDestructuringDeclaration extends CjDeclarationImpl implements CjValVarKeywordOwner, CjDeclarationWithInitializer {
    private static final TokenSet VAL_VAR_KEYWORDS = TokenSet.create(LET_KEYWORD, VAR_KEYWORD);

    public CjDestructuringDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitDestructuringDeclaration(this, data);
    }

    @NotNull
    public List<CjDestructuringDeclarationEntry> getEntries() {
        return findChildrenByType(CjNodeTypes.DESTRUCTURING_DECLARATION_ENTRY);
    }

    @Nullable
    @Override
    public CjExpression getInitializer() {
        ASTNode eqNode = getNode().findChildByType(EQ);
        if (eqNode == null) {
            return null;
        }
        return PsiTreeUtil.getNextSiblingOfType(eqNode.getPsi(), CjExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    public boolean isVar() {
        return getNode().findChildByType(VAR_KEYWORD) != null;
    }

    @Override
    @Nullable
    public PsiElement getValOrVarKeyword() {
        return findChildByType(VAL_VAR_KEYWORDS);
    }

    @Nullable
    public PsiElement getRPar() {
        return findChildByType(CjTokens.RPAR);
    }

    @Nullable
    public PsiElement getLPar() {
        return findChildByType(CjTokens.LPAR);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return CjPsiUtilKt.getTrailingCommaByClosingElement(getRPar());
    }
}
