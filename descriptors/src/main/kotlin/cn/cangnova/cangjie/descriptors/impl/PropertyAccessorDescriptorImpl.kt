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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.types.TypeSubstitutor


abstract class PropertyAccessorDescriptorImpl(
    override val  modality: Modality,
    override var  visibility: DescriptorVisibility,
    override val  correspondingProperty: PropertyDescriptor,
    annotations: Annotations,
    name: Name,
    override var isDefault: Boolean,

    override val kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : DeclarationDescriptorNonRootImpl(correspondingProperty.containingDeclaration, annotations, name, source), PropertyAccessorDescriptor {


    override var initialSignatureDescriptor: FunctionDescriptor? = null





    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor {
        return this // no substitution since we work with originals of accessors in the backend anyway
    }

    override val typeParameters: List<TypeParameterDescriptor>
        get() =   emptyList()


    override fun hasStableParameterNames(): Boolean = false

    override fun hasSynthesizedParameterNames(): Boolean = false











    override val contextReceiverParameters: List<ReceiverParameterDescriptor>
        get() = correspondingProperty.contextReceiverParameters

    override val extensionReceiverParameter: ReceiverParameterDescriptor?
        get() = correspondingProperty.extensionReceiverParameter


    override val dispatchReceiverParameter: ReceiverParameterDescriptor?
        get() = correspondingProperty.dispatchReceiverParameter

    override val isOperator: Boolean
        get() = false


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out FunctionDescriptor> {
        throw UnsupportedOperationException("Accessors must be copied by the corresponding property")
    }


    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): PropertyAccessorDescriptor {
        throw UnsupportedOperationException("Accessors must be copied by the corresponding property")
    }

    protected fun getOverriddenDescriptors(isGetter: Boolean): Collection<PropertyAccessorDescriptor> {
        val result = mutableListOf<PropertyAccessorDescriptor>()
        for (overriddenProperty in correspondingProperty.overriddenDescriptors) {
            val accessorDescriptor = if (isGetter) overriddenProperty.getter else overriddenProperty.setter
            accessorDescriptor?.let { result.add(it) }
        }
        return result
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<  CallableMemberDescriptor>) {
        require(overriddenDescriptors.isEmpty()) { "Overridden accessors should be empty" }
    }



    abstract override val original: PropertyAccessorDescriptor


    override val isHiddenToOvercomeSignatureClash: Boolean
        get() = false

    override val isHiddenForResolutionEverywhereBesideSupercalls: Boolean
        get() = false


    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? = null
}
