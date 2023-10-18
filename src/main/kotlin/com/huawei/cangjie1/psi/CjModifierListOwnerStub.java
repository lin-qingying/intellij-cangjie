package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.lexer.CjModifierKeywordToken;
import com.huawei.cangjie1.psi.psiUtil.AddRemoveModifierKt;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
import java.util.List;


public class CjModifierListOwnerStub<T extends StubElement<?>> extends CjElementImplStub<T> implements CjModifierListOwner {
    public CjModifierListOwnerStub(ASTNode node) {
        super(node);
    }

    public CjModifierListOwnerStub(T stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    @Nullable
    public CjModifierList getModifierList() {
        return getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST);
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





}
