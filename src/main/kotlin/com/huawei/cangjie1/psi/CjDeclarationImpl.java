package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.doc.psi.CDoc;
import com.huawei.cangjie1.lexer.CjModifierKeywordToken;
import com.huawei.cangjie1.psi.psiUtil.AddRemoveModifierKt;
import com.huawei.cangjie1.psi.psiUtil.FindDocCommentKt;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import com.huawei.cangjie1.CjNodeTypes;


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
