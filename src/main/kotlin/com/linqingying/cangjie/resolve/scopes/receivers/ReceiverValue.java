package com.linqingying.cangjie.resolve.scopes.receivers;

import com.linqingying.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;

public interface ReceiverValue extends Receiver {

    @NotNull
    CangJieType getType();

    @NotNull
    ReceiverValue replaceType(@NotNull CangJieType newType);

    @NotNull
    ReceiverValue getOriginal();
}
