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

package cn.cangnova.cangjie.types.error

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.annotations.Annotations.Companion.EMPTY
import cn.cangnova.cangjie.descriptors.impl.FunctionDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils
import cn.cangnova.cangjie.types.TypeSubstitution


class ErrorFunctionDescriptor(containingDeclaration: ClassDescriptor) : SimpleFunctionDescriptorImpl(
    containingDeclaration,
    null,
    EMPTY,
    Name.special(ErrorEntity.ERROR_FUNCTION.debugText),
    CallableMemberDescriptor.Kind.DECLARATION,
    SourceElement.NO_SOURCE
) {
    init {
        initialize(
            null,
            null,
            emptyList(),
            emptyList(),
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
    ): FunctionDescriptorImpl = this

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor = this


    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
        object : FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> {
            override fun setOwner(owner: DeclarationDescriptor): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setModality(modality: Modality): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setVisibility(visibility: DescriptorVisibility): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setKind(kind: CallableMemberDescriptor.Kind): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setCopyOverrides(copyOverrides: Boolean): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setName(name: Name): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setSubstitution(substitution: TypeSubstitution): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setValueParameters(
                parameters: List<ValueParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun <V> putUserData(
                userDataKey: CallableDescriptor.UserDataKey<V>, value: V
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setTypeParameters(
                parameters: List<TypeParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setReturnType(type: CangJieType): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setContextReceiverParameters(
                contextReceiverParameters: List<ReceiverParameterDescriptor>
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setExtensionReceiverParameter(
                extensionReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setDispatchReceiverParameter(
                dispatchReceiverParameter: ReceiverParameterDescriptor?
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun setOriginal(original: CallableMemberDescriptor?): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setSignatureChange(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setPreserveSourceElement(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this
            override fun setDropOriginalInContainingParts(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setHiddenToOvercomeSignatureClash(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setHiddenForResolutionEverywhereBesideSupercalls(): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> =
                this

            override fun setAdditionalAnnotations(
                additionalAnnotations: Annotations
            ): FunctionDescriptor.CopyBuilder<SimpleFunctionDescriptor?> = this

            override fun build(): SimpleFunctionDescriptor = this@ErrorFunctionDescriptor
        }



    //    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? = null
    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor?>) {}
    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}
