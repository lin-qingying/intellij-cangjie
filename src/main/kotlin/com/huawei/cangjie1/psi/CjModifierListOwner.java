package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.lexer.CjModifierKeywordToken;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface CjModifierListOwner extends PsiElement ,CjElement {
    @Nullable
    CjModifierList getModifierList();

    boolean hasModifier(@NotNull CjModifierKeywordToken modifier);

    void addModifier(@NotNull CjModifierKeywordToken modifier);
    void removeModifier(@NotNull CjModifierKeywordToken modifier);


}
