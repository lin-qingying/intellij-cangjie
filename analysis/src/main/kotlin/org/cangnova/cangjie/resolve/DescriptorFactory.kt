/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.NameUtils
import org.cangnova.cangjie.resolve.DescriptorUtils.getDefaultConstructorVisibility
import org.cangnova.cangjie.types.CangJieType

object DescriptorFactory {
    fun createDefaultSetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        parameterAnnotations: Annotations
    ): PropertySetterDescriptorImpl = createSetter(
        propertyDescriptor,
        annotations,
        parameterAnnotations,
        true,
        propertyDescriptor.source
    )

    fun createDefaultGetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations
    ): PropertyGetterDescriptorImpl = createGetter(propertyDescriptor, annotations, true)

    fun createSetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        parameterAnnotations: Annotations,
        isDefault: Boolean,
        sourceElement: SourceElement
    ): PropertySetterDescriptorImpl = createSetter(
        propertyDescriptor,
        annotations,
        parameterAnnotations,
        isDefault,
        propertyDescriptor.visibility,
        sourceElement
    )

    fun createSetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        parameterAnnotations: Annotations,
        isDefault: Boolean,
        visibility: DescriptorVisibility,
        sourceElement: SourceElement
    ): PropertySetterDescriptorImpl {
        val setterDescriptor = PropertySetterDescriptorImpl(
            propertyDescriptor,
            annotations,
            propertyDescriptor.modality,
            visibility,
            CallableMemberDescriptor.Kind.DECLARATION,


            null,
            sourceElement
        )
        val parameter = PropertySetterDescriptorImpl.createSetterParameter(
            setterDescriptor,
            propertyDescriptor.type,
            parameterAnnotations
        )
        setterDescriptor.initialize(parameter)
        return setterDescriptor
    }

    fun createGetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        isDefault: Boolean
    ): PropertyGetterDescriptorImpl =
        createGetter(propertyDescriptor, annotations, isDefault, propertyDescriptor.source)

    fun createGetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        isDefault: Boolean,
        sourceElement: SourceElement
    ): PropertyGetterDescriptorImpl = PropertyGetterDescriptorImpl(
        propertyDescriptor,
        annotations,
        propertyDescriptor.modality,
        propertyDescriptor.visibility,

        CallableMemberDescriptor.Kind.DECLARATION,
        null,
        sourceElement
    )

    fun createExtensionReceiverParameterForCallable(
        owner: CallableDescriptor,
        receiverParameterType: CangJieType?,
        annotations: Annotations
    ): ReceiverParameterDescriptor? = receiverParameterType?.let {
        ReceiverParameterDescriptorImpl(owner, ExtensionReceiver(owner, it, null), annotations)
    }

    fun createPrimaryConstructorForObject(
        containingClass: ClassDescriptor,
        source: SourceElement
    ): ClassConstructorDescriptorImpl {
        /*
         * Language version settings are needed here only for computing default visibility of constructors of sealed classes
         *   Since object can not be sealed class it's OK to pass default settings here
         */
        return DefaultClassConstructorDescriptor(containingClass, source, false)
    }

    fun createContextReceiverParameterForClass(
        owner: ClassDescriptor,
        receiverParameterType: CangJieType?,
        customLabelName: Name?,
        annotations: Annotations,
        index: Int
    ): ReceiverParameterDescriptor? = receiverParameterType?.let {
        ReceiverParameterDescriptorImpl(
            owner,
            ContextClassReceiver(owner, it, customLabelName, null),
            annotations,
            NameUtils.contextReceiverName(index)
        )
    }

    fun createContextReceiverParameterForCallable(
        owner: CallableDescriptor,
        receiverParameterType: CangJieType?,
        customLabelName: Name?,
        annotations: Annotations,
        index: Int
    ): ReceiverParameterDescriptor? = receiverParameterType?.let {
        ReceiverParameterDescriptorImpl(
            owner,
            ContextReceiver(owner, it, customLabelName, null),
            annotations,
            NameUtils.contextReceiverName(index)
        )
    }

    private class DefaultClassConstructorDescriptor(
        containingClass: ClassDescriptor,
        source: SourceElement,
        freedomForSealedInterfacesSupported: Boolean
    ) : ClassConstructorDescriptorImpl(
        containingClass,
        null,
        Annotations.EMPTY,
        true,
        CallableMemberDescriptor.Kind.DECLARATION,
        source
    ) {
        init {
            initialize(
                emptyList(),
                getDefaultConstructorVisibility(containingClass, freedomForSealedInterfacesSupported)
            )
        }
    }
}
