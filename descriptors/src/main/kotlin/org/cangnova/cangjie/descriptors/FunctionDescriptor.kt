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
package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitution
import org.cangnova.cangjie.types.TypeSubstitutor

interface FunctionDescriptor : CallableMemberDescriptor {



    override val containingDeclaration: DeclarationDescriptor


    override val original: FunctionDescriptor

    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor?

    /**
     * This method should be used with a great care, because if descriptor is substituted one, calling 'getOverriddenDescriptors'
     * may force lazy computation, that's unnecessary in most cases.
     * So, if 'getOriginal().getOverriddenDescriptors()' is enough for you, please use it instead.
     * @return
     */

    override val overriddenDescriptors: Collection<FunctionDescriptor>
    /**
     * @return descriptor that represents initial signature, e.g in case of result SimpleFunctionDescriptor.createRenamedCopy it returns
     * descriptor before rename
     */

    val initialSignatureDescriptor: FunctionDescriptor?

    /**
     * @return true if descriptor signature clashed with some other signature and it's supposed to be legal
     * See java.nio.CharBuffer
     */
    val isHiddenToOvercomeSignatureClash: Boolean

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor


    val isOperator: Boolean
    val isConst: Boolean
        get() = false

    //    bool isInfix();
    //    bool isInline();
    //    bool isTailrec();
    val isHiddenForResolutionEverywhereBesideSupercalls: Boolean
        //    bool isInfix();
        get

    //    bool isSuspend();
    override fun newCopyBuilder(): CopyBuilder<out FunctionDescriptor>

    interface CopyBuilder<D : FunctionDescriptor> : CallableMemberDescriptor.CopyBuilder<D> {
        override fun setOwner(owner: DeclarationDescriptor): CopyBuilder<D>

        override fun setModality(modality: Modality): CopyBuilder<D>

        override fun setVisibility(visibility: DescriptorVisibility): CopyBuilder<D>

        override fun setKind(kind: CallableMemberDescriptor.Kind): CopyBuilder<D>

        override fun setCopyOverrides(copyOverrides: Boolean): CopyBuilder<D>

        override fun setName(name: Name): CopyBuilder<D>

        fun setValueParameters(parameters: List<ValueParameterDescriptor>): CopyBuilder<D>

        override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<D>

        override fun setReturnType(type: CangJieType): CopyBuilder<D>

        fun setContextReceiverParameters(contextReceiverParameters: List<ReceiverParameterDescriptor>): CopyBuilder<D>

        fun setExtensionReceiverParameter(extensionReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        override fun setOriginal(original: CallableMemberDescriptor?): CopyBuilder<D>

        fun setSignatureChange(): CopyBuilder<D>

        override fun setPreserveSourceElement(): CopyBuilder<D>

        fun setDropOriginalInContainingParts(): CopyBuilder<D>

        fun setHiddenToOvercomeSignatureClash(): CopyBuilder<D>

        fun setHiddenForResolutionEverywhereBesideSupercalls(): CopyBuilder<D>

        fun setAdditionalAnnotations(additionalAnnotations: Annotations): CopyBuilder<D>

        override fun setSubstitution(substitution: TypeSubstitution): CopyBuilder<D>

        fun <V> putUserData(userDataKey: CallableDescriptor.UserDataKey<V>, value: V): CopyBuilder<D>

        override fun build(): D?
    }
}
