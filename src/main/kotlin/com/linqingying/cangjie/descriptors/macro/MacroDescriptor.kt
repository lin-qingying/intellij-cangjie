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

package com.linqingying.cangjie.descriptors.macro

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.FunctionDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType


interface MacroDescriptor : FunctionDescriptor {

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<MacroDescriptor>
    fun initialize(
        dispatchReceiverParameterIfNeeded: ReceiverParameterDescriptor?,
        contextReceiverDescriptors: List<ReceiverParameterDescriptor>,
        valueParameterDescriptors: List<ValueParameterDescriptor>,
        returnType: CangJieType,
        modality: Modality,
        visibility: DescriptorVisibility
    )
}

open class MacroDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    source: SourceElement
) : FunctionDescriptorImpl(
    containingDeclaration, null, Annotations.EMPTY, name, CallableMemberDescriptor.Kind.DECLARATION, source
), MacroDescriptor {
    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): MacroDescriptorImpl {


        return this
    }

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<MacroDescriptor> {
        return super.newCopyBuilder() as FunctionDescriptor.CopyBuilder<MacroDescriptor>
    }

    override fun initialize(
        dispatchReceiverParameterIfNeeded: ReceiverParameterDescriptor?,
        contextReceiverDescriptors: List<ReceiverParameterDescriptor>,
        valueParameterDescriptors: List<ValueParameterDescriptor>,
        returnType: CangJieType,
        modality: Modality,
        visibility: DescriptorVisibility
    ) {
        super.initialize(
            null,
            dispatchReceiverParameterIfNeeded,
            contextReceiverDescriptors,
            emptyList(),
            valueParameterDescriptors,
            returnType,
            modality,
            visibility
        )
    }

    companion object {

        fun create(
            containingDeclaration: DeclarationDescriptor,

            name: Name,

            source: SourceElement
        ): MacroDescriptorImpl {
            return MacroDescriptorImpl(containingDeclaration, name, source)
        }
    }
}
