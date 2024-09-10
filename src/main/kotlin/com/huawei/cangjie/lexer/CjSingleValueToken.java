package com.huawei.cangjie.lexer;


import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CjSingleValueToken extends CjToken {
    private final String myValue;

    @Deprecated
    public CjSingleValueToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value) {
        super(debugName);
        myValue = value;
    }

    public CjSingleValueToken(@NotNull @NonNls String debugName, @NotNull @NonNls String value, int tokenId) {
        super(debugName, tokenId);
        myValue = value;
    }

    @NotNull
    @NonNls
    public String getValue() {
        return myValue;
    }
}
