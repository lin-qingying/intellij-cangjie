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

package org.cangnova.cangjie.descriptors.macro

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeSubstitution
import org.cangnova.cangjie.types.error.ErrorEntity
import org.cangnova.cangjie.types.error.ErrorTypeKind


class ErrorMacroDescriptor(containingDeclaration: ClassDescriptor) : MacroDescriptorImpl(
    containingDeclaration,

    Name.special(ErrorEntity.ERROR_FUNCTION.debugText),

    SourceElement.NO_SOURCE
) {
    init {
        initialize(
            null,
            emptyList(),
            ErrorUtils.createErrorType(ErrorTypeKind.RETURN_TYPE_FOR_FUNCTION),
            Modality.OPEN,
            DescriptorVisibilities.PUBLIC
        )
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): MacroDescriptorImpl = this

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): MacroDescriptorImpl = this


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
        object : FunctionDescriptor.CopyBuilder<MacroDescriptor> {
            override fun setOwner(owner: DeclarationDescriptor): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setModality(modality: Modality): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setVisibility(visibility: DescriptorVisibility): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setKind(kind: CallableMemberDescriptor.Kind): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setCopyOverrides(copyOverrides: Boolean): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setName(name: Name): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this
            override fun setSubstitution(substitution: TypeSubstitution): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setValueParameters(parameters: List<ValueParameterDescriptor>): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun <V> putUserData(
                userDataKey: CallableDescriptor.UserDataKey<V>, value: V
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setReturnType(type: CangJieType): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this



            override fun setDispatchReceiverParameter(
                dispatchReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun setOriginal(original: CallableMemberDescriptor?): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setSignatureChange(): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this
            override fun setPreserveSourceElement(): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this
            override fun setDropOriginalInContainingParts(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setHiddenToOvercomeSignatureClash(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setHiddenForResolutionEverywhereBesideSupercalls(): FunctionDescriptor.CopyBuilder<MacroDescriptor> =
                this

            override fun setAdditionalAnnotations(
                additionalAnnotations: Annotations
            ): FunctionDescriptor.CopyBuilder<MacroDescriptor> = this

            override fun build(): MacroDescriptor = this@ErrorMacroDescriptor
        }

    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? {
        return null
    }
}
