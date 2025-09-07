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
import org.cangnova.cangjie.types.CangJieType


class PropertyGetterDescriptorImpl(
    correspondingProperty: PropertyDescriptor,
    annotations: Annotations,
    modality: Modality,
    visibility: DescriptorVisibility,
//    isDefault: Boolean,

    kind: CallableMemberDescriptor.Kind,
    original: PropertyGetterDescriptor?,
    source: SourceElement
) : PropertyAccessorDescriptorImpl(
    modality, visibility, correspondingProperty, annotations,
    Name.special("<get-${correspondingProperty.name}>"), /*isDefault, */kind, source
), PropertyGetterDescriptor {
    override var returnType: CangJieType? = null

    override val original: PropertyGetterDescriptor = original ?: this

    fun initialize(returnType: CangJieType?) {
        this.returnType = returnType ?: correspondingProperty.type
    }

    override val overriddenDescriptors: Collection<PropertyAccessorDescriptor>
        get() =  super.getOverriddenDescriptors(true) as Collection<PropertyGetterDescriptor>

    override val valueParameters: List<ValueParameterDescriptor>
        get() =emptyList()





    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitPropertyGetterDescriptor(this, data)
    }


}
