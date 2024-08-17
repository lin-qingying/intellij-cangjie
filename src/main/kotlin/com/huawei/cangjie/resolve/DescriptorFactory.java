package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.impl.ReceiverParameterDescriptorImpl;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.NameUtils;
import com.huawei.cangjie.resolve.scopes.receivers.ContextClassReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ContextReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ExtensionReceiver;
import com.huawei.cangjie.types.CangJieType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DescriptorFactory {
    @Nullable
    public static ReceiverParameterDescriptor createExtensionReceiverParameterForCallable(
            @NotNull CallableDescriptor owner,
            @Nullable CangJieType receiverParameterType,
            @NotNull Annotations annotations
    ) {
        return receiverParameterType == null
                ? null
                : new ReceiverParameterDescriptorImpl(owner, new ExtensionReceiver(owner, receiverParameterType, null), annotations);
    }
    @Nullable
    public static ReceiverParameterDescriptor createContextReceiverParameterForClass(
            @NotNull ClassDescriptor owner,
            @Nullable CangJieType receiverParameterType,
            @Nullable Name customLabelName,
            @NotNull Annotations annotations,
            int index
    ) {
        return receiverParameterType == null
                ? null
                : new ReceiverParameterDescriptorImpl(owner, new ContextClassReceiver(owner, receiverParameterType, customLabelName, null),
                annotations, NameUtils.contextReceiverName(index));
    }
    @Nullable
    public static ReceiverParameterDescriptor createContextReceiverParameterForCallable(
            @NotNull CallableDescriptor owner,
            @Nullable CangJieType receiverParameterType,
            @Nullable Name customLabelName,
            @NotNull Annotations annotations,
            int index
    ) {
        return receiverParameterType == null
                ? null
                : new ReceiverParameterDescriptorImpl(owner, new ContextReceiver(owner, receiverParameterType, customLabelName, null), annotations,
                NameUtils.contextReceiverName(index));
    }
}
