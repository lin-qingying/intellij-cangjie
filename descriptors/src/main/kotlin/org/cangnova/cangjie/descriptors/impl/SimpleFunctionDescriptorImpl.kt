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
package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType


open class SimpleFunctionDescriptorImpl protected constructor(
    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : FunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, source), SimpleFunctionDescriptor {
    @Deprecated(message = "This method is left for binary compatibility with android.nav.safearg plugin. Used in SafeArgSyntheticDescriptorGenerator.kt")
    fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility
    ): SimpleFunctionDescriptorImpl {
        return initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            mutableListOf(),
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            null
        )
    }

    public override fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: List<ReceiverParameterDescriptor>,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility
    ): SimpleFunctionDescriptorImpl {
        return initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            contextReceiverParameters,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            null
        )
    }

    open fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        contextReceiverParameters: List<ReceiverParameterDescriptor>,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: CangJieType?,
        modality: Modality?,
        visibility: DescriptorVisibility,
        userData: Map<out CallableDescriptor.UserDataKey<*>, Any>?
    ): SimpleFunctionDescriptorImpl {
        super.initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            contextReceiverParameters,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility
        )

        if (userData != null && !userData.isEmpty()) {
            userDataMap = LinkedHashMap(userData)
        }

        //        internal func <T : std.core.Countable<T>> a(a: T, b: T, c: std.core.Int /* = Int64 */): std.core.Range<T> where T : std.core.Comparable<T>, T : std.core.Equatable<T> defined in untitled3 in file ab.cj[SimpleFunctionDescriptorImpl@1eddf18d]
//        public func <T : std.core.Countable<T>> rangeOf(start: T, end: T, layer: Int64): std.core.Range<T> where T : std.core.Comparable<T>, T : std.core.Equatable<T>[RangeOfFunctionDescriptor@2e666b6b]
        return this
    }

    override val original: SimpleFunctionDescriptor
        get() = super.original as SimpleFunctionDescriptor

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        return SimpleFunctionDescriptorImpl(
            newOwner,

            original as SimpleFunctionDescriptor?, annotations, if (newName != null) newName else name, kind, source
        )
    }


    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor {
        return super.copy(newOwner, modality, visibility, kind, copyOverrides) as SimpleFunctionDescriptor
    }


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor> {
        return super.newCopyBuilder() as FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor>
    }

    companion object {
        fun create(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,
            name: Name,
            kind: CallableMemberDescriptor.Kind,
            source: SourceElement
        ): SimpleFunctionDescriptorImpl {
            return SimpleFunctionDescriptorImpl(containingDeclaration, null, annotations, name, kind, source)
        }
    }
}
