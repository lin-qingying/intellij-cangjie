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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.types.CangJieType


class PropertySetterDescriptorImpl(
    correspondingProperty: PropertyDescriptor,
    annotations: Annotations,
    modality: Modality,
    visibility: DescriptorVisibility,
//    isDefault: Boolean,

    kind: CallableMemberDescriptor.Kind,
    original: PropertySetterDescriptor?,
    source: SourceElement
) : PropertyAccessorDescriptorImpl(
    modality, visibility, correspondingProperty, annotations,
    Name.special("<set-${correspondingProperty.name}>"), /*isDefault,  */kind, source
) , PropertySetterDescriptor{
    private var parameter: ValueParameterDescriptor? = null

    override val original: PropertySetterDescriptor = original ?: this

    fun initialize(parameter: ValueParameterDescriptor) {
        check(this.parameter == null) { "Parameter is already initialized" }
        this.parameter = parameter
    }

    fun initializeDefault() {
        initialize(createSetterParameter(this, correspondingProperty.type, Annotations.EMPTY))
    }

    companion object {

@JvmStatic
        fun createSetterParameter(
            setterDescriptor: PropertySetterDescriptor,
            type: CangJieType,
            annotations: Annotations
        ): ValueParameterDescriptorImpl {
            return ValueParameterDescriptorImpl(
                setterDescriptor, null, 0, annotations, SpecialNames.IMPLICIT_SET_PARAMETER,false, type,
                false, // declaresDefaultValue
//                false, // isCrossinline
//                false, // isNoinline
                /*null,*/ SourceElement.NO_SOURCE
            )
        }
    }


    override val overriddenDescriptors: Collection<PropertySetterDescriptor>
        get() = super.getOverriddenDescriptors(false) as Collection<PropertySetterDescriptor>



    override val valueParameters: List<ValueParameterDescriptor>
        get() = parameter?.let { listOf(it) } ?: throw IllegalStateException("Parameter is not initialized")



    override val returnType: CangJieType?
        get() = builtIns.unitType

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitPropertySetterDescriptor(this, data)
    }


}
