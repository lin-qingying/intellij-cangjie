package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.resolve.scopes.receivers.ImplicitClassReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.annotations.NotNull;


public class LazyClassReceiverParameterDescriptor extends AbstractReceiverParameterDescriptor {
    private final ClassDescriptor descriptor;
    private final ImplicitClassReceiver receiverValue;

    public LazyClassReceiverParameterDescriptor(@NotNull ClassDescriptor descriptor) {
        super(Annotations.Companion.getEMPTY());
        this.descriptor = descriptor;
        this.receiverValue = new ImplicitClassReceiver(descriptor, null);

    }

    @NotNull
    @Override
    public ReceiverValue getValue() {
        return receiverValue;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return descriptor;
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull String toString() {
        return "class " + descriptor.getName() + "::this";
    }


}
