package com.huawei.cangjie.resolve.scopes.receivers;

import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractReceiverValue implements ReceiverValue {
    protected final CangJieType receiverType;
    private final ReceiverValue original;

    public AbstractReceiverValue(@NotNull CangJieType receiverType, @Nullable ReceiverValue original) {
        this.receiverType = receiverType;
        this.original = original != null ? original : this;
    }

    @Override
    @NotNull
    public CangJieType getType() {
        return receiverType;
    }

    @NotNull
    @Override
    public ReceiverValue getOriginal() {
        return original;
    }
}
