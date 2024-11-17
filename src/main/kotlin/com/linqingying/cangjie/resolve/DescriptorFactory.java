/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.resolve;

import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.descriptors.impl.*;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.name.NameUtils;
import com.linqingying.cangjie.resolve.scopes.receivers.ContextClassReceiver;
import com.linqingying.cangjie.resolve.scopes.receivers.ContextReceiver;
import com.linqingying.cangjie.resolve.scopes.receivers.ExtensionReceiver;
import com.linqingying.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

import static com.linqingying.cangjie.resolve.DescriptorUtils.getDefaultConstructorVisibility;

public class DescriptorFactory {
    @NotNull
    public static PropertySetterDescriptorImpl createDefaultSetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            @NotNull Annotations parameterAnnotations
    ) {
        return createSetter(propertyDescriptor, annotations, parameterAnnotations, true, propertyDescriptor.getSource());
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createDefaultGetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations
    ) {
        return createGetter(propertyDescriptor, annotations, true   );
    }
    @NotNull
    public static PropertySetterDescriptorImpl createSetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            @NotNull Annotations parameterAnnotations,
            boolean isDefault,

            @NotNull SourceElement sourceElement
    ) {
        return createSetter(
                propertyDescriptor, annotations, parameterAnnotations, isDefault,
                propertyDescriptor.getVisibility(), sourceElement
        );
    }

    @NotNull
    public static PropertySetterDescriptorImpl createSetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            @NotNull Annotations parameterAnnotations,
            boolean isDefault,

            @NotNull DescriptorVisibility visibility,
            @NotNull SourceElement sourceElement
    ) {
        PropertySetterDescriptorImpl setterDescriptor = new PropertySetterDescriptorImpl(
                propertyDescriptor, annotations, propertyDescriptor.getModality(), visibility, isDefault,
               CallableMemberDescriptor.Kind.DECLARATION, null, sourceElement
        );
        ValueParameterDescriptorImpl parameter =
                PropertySetterDescriptorImpl.createSetterParameter(setterDescriptor, propertyDescriptor.getType(), parameterAnnotations);
        setterDescriptor.initialize(parameter);
        return setterDescriptor;
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createGetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            boolean isDefault

    ) {
        return createGetter(propertyDescriptor, annotations, isDefault, propertyDescriptor.getSource());
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createGetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            boolean isDefault,


            @NotNull SourceElement sourceElement
    ) {
        return new PropertyGetterDescriptorImpl(
                propertyDescriptor, annotations, propertyDescriptor.getModality(), propertyDescriptor.getVisibility(),
                isDefault, CallableMemberDescriptor.Kind.DECLARATION, null, sourceElement
        );
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

    private static class DefaultClassConstructorDescriptor extends ClassConstructorDescriptorImpl {
        public DefaultClassConstructorDescriptor(
                @NotNull ClassDescriptor containingClass,
                @NotNull SourceElement source,
                boolean freedomForSealedInterfacesSupported
        ) {
            super(containingClass, null, Annotations.EMPTY, true, Kind.DECLARATION, source);
            initialize(Collections.emptyList(),
                    getDefaultConstructorVisibility(containingClass, freedomForSealedInterfacesSupported));
        }
    }
}
