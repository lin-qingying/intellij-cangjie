package com.huawei.cangjie.resolve;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.descriptors.impl.ClassConstructorDescriptorImpl;
import com.huawei.cangjie.descriptors.impl.ReceiverParameterDescriptorImpl;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.NameUtils;
import com.huawei.cangjie.resolve.scopes.receivers.ContextClassReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ContextReceiver;
import com.huawei.cangjie.resolve.scopes.receivers.ExtensionReceiver;
import com.huawei.cangjie.types.CangJieType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static com.huawei.cangjie.resolve.DescriptorUtils.getDefaultConstructorVisibility;

public class DescriptorFactory {


    private static class DefaultClassConstructorDescriptor extends ClassConstructorDescriptorImpl {
        public DefaultClassConstructorDescriptor(
                @NotNull ClassDescriptor containingClass,
                @NotNull SourceElement source,
                boolean freedomForSealedInterfacesSupported
        ) {
            super(containingClass, null, Annotations.EMPTY, true, Kind.DECLARATION, source);
            initialize(Collections.<ValueParameterDescriptor>emptyList(),
                    getDefaultConstructorVisibility(containingClass, freedomForSealedInterfacesSupported));
        }
    }
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
    @NotNull
    public static ClassConstructorDescriptorImpl createPrimaryConstructorForObject(
            @NotNull ClassDescriptor containingClass,
            @NotNull SourceElement source
    ) {
        /*
         * Language version settings are needed here only for computing default visibility of constructors of sealed classes
         *   Since object can not be sealed class it's OK to pass default settings here
         */
        return new DefaultClassConstructorDescriptor(containingClass, source, false);
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
