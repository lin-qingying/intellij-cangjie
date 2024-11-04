package com.linqingying.cangjie.lexer;


import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CjKeywordToken extends CjSingleValueToken  {

    /**
     * 生成关键字(在所有可能的上下文中具有关键字含义的标识符)
     */
    @Deprecated
    public static CjKeywordToken keyword(String value) {
        return keyword(value, value);
    }

    public static CjKeywordToken keyword(String value, int tokenId) {
        return keyword(value, value, tokenId);
    }

    @Deprecated
    public static CjKeywordToken keyword(String debugName, String value) {
        return new CjKeywordToken(debugName, value, false);
    }

    public static CjKeywordToken keyword(String debugName, String value, int tokenId) {
        return new CjKeywordToken(debugName, value, false, tokenId);
    }


    @Deprecated
    public static CjKeywordToken softKeyword(String value) {
        return new CjKeywordToken(value, value, true);
    }

    public static CjKeywordToken softKeyword(String value, int tokenId) {
        return new CjKeywordToken(value, value, true, tokenId);
    }

    private final boolean myIsSoft;

    @Deprecated
    protected CjKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft) {
        super(debugName, value);
        myIsSoft = isSoft;
    }



    protected CjKeywordToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, boolean isSoft, int tokenId) {
        super(debugName, value, tokenId);
        myIsSoft = isSoft;
    }

    public boolean isSoft() {
        return myIsSoft;
    }
}
