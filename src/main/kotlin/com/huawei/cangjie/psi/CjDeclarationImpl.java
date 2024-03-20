package com.huawei.cangjie.psi;

import com.huawei.cangjie.doc.psi.CDoc;
import com.huawei.cangjie.lexer.CjModifierKeywordToken;
import com.huawei.cangjie.psi.psiUtil.AddRemoveModifierKt;
import com.huawei.cangjie.psi.psiUtil.FindDocCommentKt;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTreeUtilKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.huawei.cangjie.CjNodeTypes;


public abstract class CjDeclarationImpl extends CjExpressionImpl implements CjDeclaration {
    public CjDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public CjModifierList getModifierList() {
        return findChildByType(CjNodeTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifier(@NotNull CjModifierKeywordToken modifier) {
        CjModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(modifier);
    }

    @Override
    public @Nullable CjExpression getExpression() {
        return PsiTreeUtil.getStubChildOfType(this, CjExpression.class);

    }

    @Override
    public void addModifier(@NotNull CjModifierKeywordToken modifier) {
        AddRemoveModifierKt.addModifier(this, modifier);
    }

    @Override
    public void removeModifier(@NotNull CjModifierKeywordToken modifier) {
        AddRemoveModifierKt.removeModifier(this, modifier);
    }





    @Nullable
    @Override
    public CDoc getDocComment() {
        return FindDocCommentKt.findDocComment(this);
    }
}
