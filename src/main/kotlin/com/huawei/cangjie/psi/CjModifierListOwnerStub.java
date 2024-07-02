package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjModifierKeywordToken;
import com.huawei.cangjie.psi.psiUtil.AddRemoveModifierKt;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;

import java.util.Collections;
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
    public @NotNull List<CjAnnotation> getAnnotations() {
        CjModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }

    @Override
    @NotNull
    public List<CjAnnotationEntry> getAnnotationEntries() {
        CjModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
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
