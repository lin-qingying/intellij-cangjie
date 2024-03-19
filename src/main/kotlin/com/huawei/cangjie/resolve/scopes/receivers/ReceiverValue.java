package com.huawei.cangjie.resolve.scopes.receivers;

import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;

public interface ReceiverValue extends Receiver {

    @NotNull
    CangJieType getType();

    @NotNull
    ReceiverValue replaceType(@NotNull CangJieType newType);

    @NotNull
    ReceiverValue getOriginal();
}
