package com.linqingying.cangjie.lexer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


public final class CjModifierKeywordToken extends CjKeywordToken {

    /**
     * 生成关键字(在所有可能的上下文中具有关键字含义的标识符)
     */
    @Deprecated
    public static CjModifierKeywordToken keywordModifier(String value) {
        return new CjModifierKeywordToken(value, value, false);
    }

    public static CjModifierKeywordToken keywordModifier(String value, int tokenId) {
        return new CjModifierKeywordToken(value, value, false, tokenId);
    }

    /**
     * 生成软关键字(仅在某些上下文中具有关键字含义的标识符)
     */
    @Deprecated
    public static CjModifierKeywordToken softKeywordModifier(String value) {
        return new CjModifierKeywordToken(value, value, true);
    }

    public static CjModifierKeywordToken softKeywordModifier(String value, int tokenId) {
        return new CjModifierKeywordToken(value, value, true, tokenId);
    }

    @Deprecated
    private CjModifierKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft) {
        super(debugName, value, isSoft);
    }

    private CjModifierKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft, int tokenId) {
        super(debugName, value, isSoft, tokenId);
    }
}
