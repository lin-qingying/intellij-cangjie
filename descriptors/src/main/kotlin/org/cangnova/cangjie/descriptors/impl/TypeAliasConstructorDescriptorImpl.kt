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
import org.cangnova.cangjie.resolve.DescriptorFactory
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitContextReceiver
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.*




class TypeAliasConstructorDescriptorImpl private constructor(
    val storageManager: StorageManager,
    override val typeAliasDescriptor: TypeAliasDescriptor,
    underlyingConstructorDescriptor: ClassConstructorDescriptor,
    original: TypeAliasConstructorDescriptor?,
    annotations: Annotations,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement
) : FunctionDescriptorImpl(typeAliasDescriptor, original, annotations, SpecialNames.INIT, kind, source),
    TypeAliasConstructorDescriptor {

    override val isPrimary: Boolean
        get() =  underlyingConstructorDescriptor.isPrimary


    override val isEnd: Boolean
        get() =  underlyingConstructorDescriptor.isEnd

    override var underlyingConstructorDescriptor: ClassConstructorDescriptor = underlyingConstructorDescriptor
        private set


    override val returnType: CangJieType
        get() = super.returnType!!

    override val original: TypeAliasConstructorDescriptor =
        super.original as TypeAliasConstructorDescriptor
    override val containingDeclaration: TypeAliasDescriptor =
        typeAliasDescriptor


    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptorImpl? {
        //    class C<T>(val x: T)
        //    typealias A<Q> = C<List<Q>>
        //
        //    val test = A(listOf(42))
        //
        // Here return type for substituted type alias constructor is 'C<List<Int>>',
        // which yields us a substitution [T -> List<Int>] that should be applied to
        // the unsubstituted underlying constructor with signature '<T> (T) -> C<T>'
        // producing substituted underlying constructor with signature '(List<Int>) -> C<List<Int>>'.
        val substitutedTypeAliasConstructor = super.substitute(substitutor) as TypeAliasConstructorDescriptorImpl
        val underlyingConstructorSubstitutor = TypeSubstitutor.create(substitutedTypeAliasConstructor.returnType)
        val substitutedUnderlyingConstructor =
            underlyingConstructorDescriptor.original.substitute(underlyingConstructorSubstitutor)
        substitutedTypeAliasConstructor.underlyingConstructorDescriptor = substitutedUnderlyingConstructor
        return substitutedTypeAliasConstructor
    }


    // When resolution is ran for common calls, type aliases constructors are resolved as extensions
    // (i.e. after members, and with extension receiver)
    // But when resolving super-calls (with known set of candidates) constructors of inner classes are expected to have
    // a dispatch receiver
    override val withDispatchReceiver: TypeAliasConstructorDescriptor? by storageManager.createNullableLazyValue {
        TypeAliasConstructorDescriptorImpl(
            storageManager,
            typeAliasDescriptor,
            underlyingConstructorDescriptor,
            this,
            underlyingConstructorDescriptor.annotations,
            underlyingConstructorDescriptor.kind,
            typeAliasDescriptor.source
        ).also { typeAliasConstructor ->
            val substitutorForUnderlyingClass =
                typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return@createNullableLazyValue null

            typeAliasConstructor.initialize(
                null,
                underlyingConstructorDescriptor.dispatchReceiverParameter?.substitute(substitutorForUnderlyingClass),
                underlyingConstructorDescriptor.contextReceiverParameters.mapNotNull {
                    it.substitute(
                        substitutorForUnderlyingClass
                    )
                },
                typeAliasDescriptor.declaredTypeParameters,
                valueParameters,
                returnType,
                Modality.FINAL,
                typeAliasDescriptor.visibility
            )
        }
    }

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor =
        newCopyBuilder()
            .setOwner(newOwner)
            .setModality(modality)
            .setVisibility(visibility)
            .setKind(kind)
            .setCopyOverrides(copyOverrides)
            .build() as TypeAliasConstructorDescriptor

    override fun hasSynthesizedParameterNames(): Boolean = false

    override val constructedClass: ClassDescriptor
        get() =  underlyingConstructorDescriptor.constructedClass

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        assert(kind == CallableMemberDescriptor.Kind.DECLARATION || kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            "Creating a type alias constructor that is not a declaration: \ncopy from: ${this}\nnewOwner: $newOwner\nkind: $kind"
        }
        assert(newName == null) { "Renaming type alias constructor: $this" }
        return TypeAliasConstructorDescriptorImpl(
            storageManager,
            typeAliasDescriptor,
            underlyingConstructorDescriptor,
            this,
            annotations,
            CallableMemberDescriptor.Kind.DECLARATION, source
        )
    }

    companion object {
        private fun TypeAliasDescriptor.getTypeSubstitutorForUnderlyingClass(): TypeSubstitutor? {
            if (classDescriptor == null) return null
            return TypeSubstitutor.create(expandedType)
        }

        fun createIfAvailable(
            storageManager: StorageManager,
            typeAliasDescriptor: TypeAliasDescriptor,
            constructor: ClassConstructorDescriptor
        ): TypeAliasConstructorDescriptor? {
            val substitutorForUnderlyingClass =
                typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return null
            val substitutedConstructor = constructor.substitute(substitutorForUnderlyingClass) ?: return null

            val typeAliasConstructor =
                TypeAliasConstructorDescriptorImpl(
                    storageManager, typeAliasDescriptor,
                    substitutedConstructor,
                    null, constructor.annotations,
                    constructor.kind, typeAliasDescriptor.source
                )

            val valueParameters =
                getSubstitutedValueParameters(
                    typeAliasConstructor, constructor.valueParameters, substitutorForUnderlyingClass
                ) ?: return null

            val returnType = substitutedConstructor.returnType.unwrap().lowerIfFlexible()
                .withAbbreviation(typeAliasDescriptor.defaultType)

            val receiverParameter = constructor.dispatchReceiverParameter?.let {
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    typeAliasConstructor,
                    substitutorForUnderlyingClass.safeSubstitute(it.type, Variance.INVARIANT),
                    Annotations.EMPTY
                )
            }

            val classDescriptor = typeAliasDescriptor.classDescriptor
            val contextReceiverParameters = classDescriptor?.let {
                constructor.contextReceiverParameters.mapIndexedNotNull { index, contextReceiver ->
                    DescriptorFactory.createContextReceiverParameterForClass(
                        classDescriptor,
                        substitutorForUnderlyingClass.safeSubstitute(contextReceiver.type, Variance.INVARIANT),
                        (contextReceiver.value as ImplicitContextReceiver).customLabelName,
                        Annotations.EMPTY,
                        index
                    )
                }
            } ?: emptyList()

            typeAliasConstructor.initialize(
                receiverParameter,
                null,
                contextReceiverParameters,
                typeAliasDescriptor.declaredTypeParameters,
                valueParameters,
                returnType,
                Modality.FINAL,
                typeAliasDescriptor.visibility
            )

            return typeAliasConstructor
        }
    }
}
