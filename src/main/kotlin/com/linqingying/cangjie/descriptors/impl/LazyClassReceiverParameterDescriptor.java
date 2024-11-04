package com.linqingying.cangjie.descriptors.impl;

import com.linqingying.cangjie.descriptors.ClassDescriptor;
import com.linqingying.cangjie.descriptors.DeclarationDescriptor;
import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitClassReceiver;
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.annotations.NotNull;


public class LazyClassReceiverParameterDescriptor extends AbstractReceiverParameterDescriptor {
    private final ClassDescriptor descriptor;
    private final ImplicitClassReceiver receiverValue;

    public LazyClassReceiverParameterDescriptor(@NotNull ClassDescriptor descriptor) {
        super(Annotations.EMPTY);
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
        return descriptor.getKind() + " " + descriptor.getName() + "::this";
    }



}
