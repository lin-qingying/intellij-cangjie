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
import org.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PropertyGetterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PropertySetterDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PropertySetterDescriptorImpl.Companion.createSetterParameter
import org.cangnova.cangjie.descriptors.impl.ReceiverParameterDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils.getDefaultConstructorVisibility

import org.cangnova.cangjie.types.CangJieType

object   DescriptorFactory {
    fun createDefaultSetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        parameterAnnotations: Annotations
    ): PropertySetterDescriptorImpl {
        return createSetter(propertyDescriptor, annotations, parameterAnnotations, true, propertyDescriptor.source)
    }

    fun createDefaultGetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations
    ): PropertyGetterDescriptorImpl {
        return createGetter(propertyDescriptor, annotations, true)
    }

    fun createSetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        parameterAnnotations: Annotations,
        isDefault: Boolean,

        sourceElement: SourceElement
    ): PropertySetterDescriptorImpl {
        return createSetter(
            propertyDescriptor, annotations, parameterAnnotations, isDefault,
            propertyDescriptor.visibility, sourceElement
        )
    }

    fun createSetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        parameterAnnotations: Annotations,
        isDefault: Boolean,

        visibility: DescriptorVisibility,
        sourceElement: SourceElement
    ): PropertySetterDescriptorImpl {
        val setterDescriptor = PropertySetterDescriptorImpl(
            propertyDescriptor, annotations, propertyDescriptor.modality, visibility, /*isDefault,*/
            CallableMemberDescriptor.Kind.DECLARATION, null, sourceElement
        )
        val parameter =
            createSetterParameter(setterDescriptor, propertyDescriptor.type, parameterAnnotations)
        setterDescriptor.initialize(parameter)
        return setterDescriptor
    }

    @JvmOverloads
    fun createGetter(
        propertyDescriptor: PropertyDescriptor,
        annotations: Annotations,
        isDefault: Boolean,


        sourceElement: SourceElement = propertyDescriptor.source
    ): PropertyGetterDescriptorImpl {
        return PropertyGetterDescriptorImpl(
            propertyDescriptor, annotations, propertyDescriptor.modality, propertyDescriptor.visibility,
            /*isDefault, */CallableMemberDescriptor.Kind.DECLARATION, null, sourceElement
        )
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
                mutableListOf<ValueParameterDescriptor>(),
                getDefaultConstructorVisibility(containingClass, freedomForSealedInterfacesSupported)
            )
        }
    }
}
