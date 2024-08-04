package com.linqingying.cangjie.psi.stubs.impl;

import com.linqingying.cangjie.lexer.CjKeywordToken;
import com.linqingying.cangjie.lexer.CjModifierKeywordToken;
import com.linqingying.cangjie.psi.CjDeclarationModifierList;
import com.linqingying.cangjie.psi.stubs.CangJieModifierListStub;
import com.linqingying.cangjie.psi.stubs.elements.CjModifierListElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;


public class CangJieModifierListStubImpl extends CangJieStubBaseImpl<CjDeclarationModifierList> implements CangJieModifierListStub {

    private final long mask;

    public CangJieModifierListStubImpl(StubElement parent, long mask, @NotNull CjModifierListElementType<?> elementType) {
        super(parent, elementType);
        this.mask = mask;
    }

    public long getMask() {
        return mask;
    }

    @Override
    public boolean hasModifier(@NotNull CjModifierKeywordToken modifierToken) {
        return ModifierMaskUtils.maskHasModifier(mask, modifierToken);
    }

    @NotNull
    @Override
    public String toString() {
        return super.toString() + ModifierMaskUtils.maskToString(mask);
    }
}
