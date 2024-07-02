package com.huawei.cangjie.resolve.scopes.receivers;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtensionReceiver extends AbstractReceiverValue implements ImplicitReceiver{

    private final CallableDescriptor descriptor;

    public ExtensionReceiver(
            @NotNull CallableDescriptor callableDescriptor,
            @NotNull CangJieType receiverType,
            @Nullable ReceiverValue original
    ) {
        super(receiverType, original);
        this.descriptor = callableDescriptor;
    }
    @NotNull
    @Override
    public DeclarationDescriptor getDeclarationDescriptor() {
        return null;
    }

    @Override
    public @NotNull ReceiverValue replaceType(@NotNull CangJieType newType) {
        return null;
    }
}
