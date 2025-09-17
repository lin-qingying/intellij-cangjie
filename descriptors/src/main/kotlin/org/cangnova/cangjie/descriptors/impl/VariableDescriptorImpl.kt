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
import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.resolve.scopes.receivers.ContextReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ExtensionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitContextReceiver
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.*


open class VariableDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    outType: CangJieType?,
    isVar: Boolean,
    source: SourceElement,
    visibility: DescriptorVisibility
) : VariableDescriptorWithInitializerImpl(containingDeclaration, Annotations.EMPTY, name, outType, isVar, source),
    CallableMemberDescriptor {
    override val original: VariableDescriptorImpl
        get() = super.original as VariableDescriptorImpl
    private var _visibility: DescriptorVisibility = visibility


    protected open fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        newModality: Modality,
        newVisibility: DescriptorVisibility,
        original: VariableDescriptor?,
        kind: CallableMemberDescriptor.Kind,

        newName: Name,
        source: SourceElement
    ): VariableDescriptorImpl {
        return VariableDescriptorImpl(
            newOwner, newName, null, isVar, source, newVisibility

        )
    }

    private var _overriddenDescriptors: Collection<VariableDescriptorImpl> = emptyList()



    override val overriddenDescriptors: Collection<VariableDescriptorImpl>
        get() = _overriddenDescriptors

    override val modality: Modality
        get() = Modality.FINAL



    override val kind: CallableMemberDescriptor.Kind
        get() = CallableMemberDescriptor.Kind.DECLARATION

    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<  CallableMemberDescriptor>) {
//        this._overriddenDescriptors = overriddenDescriptors
    }



    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V? {
        return null
    }
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        return this
    }

    override fun newCopyBuilder(): CopyConfiguration {
//        return object : CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//            override fun setOwner(owner: DeclarationDescriptor): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//            }
//
//            override fun setModality(modality: Modality): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setVisibility(visibility: DescriptorVisibility): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setKind(kind: CallableMemberDescriptor.Kind): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setTypeParameters(parameters: MutableList<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setSubstitution(substitution: TypeSubstitution): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setCopyOverrides(copyOverrides: Boolean): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setName(name: Name): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setOriginal(original: CallableMemberDescriptor?): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setPreserveSourceElement(): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun setReturnType(type: CangJieType): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
//                return this
//
//            }
//
//            override fun build(): CallableMemberDescriptor {
//                return this@VariableDescriptorImpl
//            }
        return CopyConfiguration()

    }


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitVariableDescriptor(this, data)
    }

    override var visibility: DescriptorVisibility = _visibility

    inner class CopyConfiguration : CallableMemberDescriptor.CopyBuilder<VariableDescriptorImpl> {
        var _owner: DeclarationDescriptor = containingDeclaration
        var _modality: Modality = modality
        var _visibility = this@VariableDescriptorImpl.visibility
        var _original: VariableDescriptorImpl? = null
        var _preserveSourceElement = true
        var _kind: CallableMemberDescriptor.Kind = kind
        var _substitution = TypeSubstitution.EMPTY
        var _copyOverrides = true
        var _dispatchReceiverParameter: ReceiverParameterDescriptor? =
            this@VariableDescriptorImpl.dispatchReceiverParameter
        var _newTypeParameters: List<TypeParameterDescriptor>? = null
        var _name = this@VariableDescriptorImpl.name
        var _returnType: CangJieType = type

        override fun setOwner(owner: DeclarationDescriptor): CopyConfiguration {
            this._owner = owner
            return this
        }

        override fun setOriginal(original: CallableMemberDescriptor?): CopyConfiguration {
            this._original = original as VariableDescriptorImpl?
            return this
        }

        override fun setPreserveSourceElement(): CopyConfiguration {
            _preserveSourceElement = true
            return this
        }

        override fun setReturnType(type: CangJieType): CallableMemberDescriptor.CopyBuilder<VariableDescriptorImpl> {
            _returnType = type
            return this
        }

        override fun setModality(modality: Modality): CopyConfiguration {
            this._modality = modality
            return this
        }

        override fun setVisibility(visibility: DescriptorVisibility): CopyConfiguration {
            this._visibility = visibility
            return this
        }

        override fun setKind(kind: CallableMemberDescriptor.Kind): CopyConfiguration {
            this._kind = kind
            return this
        }

        override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<VariableDescriptorImpl> {
            this._newTypeParameters = parameters
            return this
        }

        override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyConfiguration {
            this._dispatchReceiverParameter = dispatchReceiverParameter
            return this
        }

        override fun setSubstitution(substitution: TypeSubstitution): CopyConfiguration {
            this._substitution = substitution
            return this
        }

        override fun setCopyOverrides(copyOverrides: Boolean): CopyConfiguration {
            this._copyOverrides = copyOverrides
            return this
        }

        override fun setName(name: Name): CallableMemberDescriptor.CopyBuilder<VariableDescriptorImpl > {
            this._name = name
            return this
        }

        override fun build(): VariableDescriptorImpl? {
            return doSubstitute(this)
        }


    }

    private fun getSourceToUseForCopy(preserveSource: Boolean, original: VariableDescriptorImpl?): SourceElement {
        return if (preserveSource)
            (original ?: original)!!.source
        else
            SourceElement.NO_SOURCE
    }

    protected fun doSubstitute(copyConfiguration: CopyConfiguration): VariableDescriptorImpl? {
        val substitutedDescriptor = createSubstitutedCopy(
            copyConfiguration._owner, copyConfiguration._modality, copyConfiguration._visibility,
            copyConfiguration._original, copyConfiguration._kind,copyConfiguration._name,
            getSourceToUseForCopy(copyConfiguration._preserveSourceElement, copyConfiguration._original)
        )
        val originalTypeParameters =
            if (copyConfiguration._newTypeParameters == null) typeParameters else copyConfiguration._newTypeParameters
                ?: emptyList()
        val substitutedTypeParameters  = ArrayList<TypeParameterDescriptor>(originalTypeParameters.size)
        val substitutor = DescriptorSubstitutor.substituteTypeParameters(
            originalTypeParameters, copyConfiguration._substitution, substitutedDescriptor, substitutedTypeParameters
        )

        val originalOutType: CangJieType = copyConfiguration._returnType
        val outType = substitutor.substitute(originalOutType, Variance.INVARIANT)
            ?: return null // TODO : tell the user that the property was projected out

        val substitutedDispatchReceiver: ReceiverParameterDescriptor?
        val dispatchReceiver = copyConfiguration._dispatchReceiverParameter
        if (dispatchReceiver != null) {
            substitutedDispatchReceiver = dispatchReceiver.substitute(substitutor)
            if (substitutedDispatchReceiver == null) return null
        } else {
            substitutedDispatchReceiver = null
        }
        val substitutedExtensionReceiver = if (extensionReceiverParameter != null) {
            substituteParameterDescriptor(
                substitutor, substitutedDescriptor,
                extensionReceiverParameter!!
            )
        } else {
            null
        }

        val substitutedContextReceivers: MutableList<ReceiverParameterDescriptor> = ArrayList()
        for (contextReceiverParameter in contextReceiverParameters) {
            val substitutedContextReceiver = substituteContextParameterDescriptor(
                substitutor, substitutedDescriptor,
                contextReceiverParameter
            )
            if (substitutedContextReceiver != null) {
                substitutedContextReceivers.add(substitutedContextReceiver)
            }
        }

        substitutedDescriptor.setType(
            outType, substitutedTypeParameters, substitutedDispatchReceiver, substitutedExtensionReceiver,
            substitutedContextReceivers
        )
        return substitutedDescriptor

    }

    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptorImpl? {
        if (substitutor.isEmpty) {
            return this
        }

        return newCopyBuilder()
            .setSubstitution(substitutor.substitution)
            .setOriginal(original)
            .build()

    }
    companion object {
        private fun substituteContextParameterDescriptor(
            substitutor: TypeSubstitutor,
            substitutedPropertyDescriptor: VariableDescriptorImpl,
            receiverParameterDescriptor: ReceiverParameterDescriptor
        ): ReceiverParameterDescriptor? {
            val substitutedType = substitutor.substitute(receiverParameterDescriptor.type, Variance.INVARIANT)
                ?: return null
            return ReceiverParameterDescriptorImpl(
                substitutedPropertyDescriptor,
                ContextReceiver(
                    substitutedPropertyDescriptor,
                    substitutedType,
                    (receiverParameterDescriptor.value as ImplicitContextReceiver).customLabelName,
                    receiverParameterDescriptor.value
                ),
                receiverParameterDescriptor.annotations
            )
        }

        private fun substituteParameterDescriptor(
            substitutor: TypeSubstitutor,
            substitutedPropertyDescriptor: VariableDescriptor,
            receiverParameterDescriptor: ReceiverParameterDescriptor
        ): ReceiverParameterDescriptor? {
            val substitutedType = substitutor.substitute(receiverParameterDescriptor.type, Variance.INVARIANT)
                ?: return null
            return ReceiverParameterDescriptorImpl(
                substitutedPropertyDescriptor,
                ExtensionReceiver(substitutedPropertyDescriptor, substitutedType, receiverParameterDescriptor.value),
                receiverParameterDescriptor.annotations
            )
        }

        @JvmStatic
        fun create(
            containingDeclaration: DeclarationDescriptor,
            name: Name,
            visibility: DescriptorVisibility,
            isVar: Boolean,
            source: SourceElement
        ): VariableDescriptorImpl {
            return VariableDescriptorImpl(containingDeclaration, name, null, isVar, source, visibility)
        }
    }


}
