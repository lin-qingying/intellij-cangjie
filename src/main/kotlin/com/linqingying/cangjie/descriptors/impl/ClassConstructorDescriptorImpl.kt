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

package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeSubstitutor


open class ClassConstructorDescriptorImpl protected constructor(
    containingDeclaration: ClassDescriptor,
    original: ConstructorDescriptor?,
    annotations: Annotations,
    private val isPrimary: Boolean,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement,
    private val isEnd: Boolean = false
) : FunctionDescriptorImpl(
    containingDeclaration, original, annotations, if (isEnd) {
        SpecialNames.END_INIT
    } else {
        SpecialNames.INIT
    }, kind, source
),
    ClassConstructorDescriptor {

    constructor(
        containingDeclaration: ClassDescriptor,
        original: ConstructorDescriptor?,
        annotations: Annotations,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind,
        source: SourceElement,
    ) : this(containingDeclaration, original, annotations, isPrimary, kind, source, false)

    override fun getReturnType(): CangJieType {
        return super.getReturnType()!!
    }

    fun initialize(
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        visibility: DescriptorVisibility,
        typeParameterDescriptors: List<TypeParameterDescriptor?>
    ): ClassConstructorDescriptorImpl {
        super.initialize(
            null, calculateDispatchReceiverParameter(), calculateContextReceiverParameters(),
            typeParameterDescriptors,
            unsubstitutedValueParameters, null,
            Modality.FINAL, visibility
        )
        return this
    }

    fun initialize(
        unsubstitutedValueParameters: List<ValueParameterDescriptor>
    ): ClassConstructorDescriptorImpl {
        initialize(
            unsubstitutedValueParameters,
            DescriptorVisibilities.PUBLIC,
            containingDeclaration.declaredTypeParameters
        )
        return this
    }

    fun initialize(
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        visibility: DescriptorVisibility
    ): ClassConstructorDescriptorImpl {
        initialize(unsubstitutedValueParameters, visibility, containingDeclaration.declaredTypeParameters)
        return this
    }

    private fun calculateDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return null
    }

    private fun calculateContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        val classDescriptor = containingDeclaration
        if (classDescriptor.contextReceivers.isNotEmpty()) {
            return classDescriptor.contextReceivers
        }
        return emptyList()
    }

    override val containingDeclaration: ClassDescriptor
        get() = super.containingDeclaration as ClassDescriptor

    override fun getConstructedClass(): ClassDescriptor {
        return containingDeclaration
    }

    override val original: ClassConstructorDescriptor
        get() = super.original as ClassConstructorDescriptor

    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor? {
        return super.substitute(substitutor) as ClassConstructorDescriptor?
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitConstructorDescriptor(this, data)
    }



    override fun isEnd(): Boolean {
        return isEnd
    }

    override fun isPrimary(): Boolean {
        return isPrimary
    }


    override fun getOverriddenDescriptors(): Collection<FunctionDescriptor> {
        return emptySet()
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor?>) {
        assert(overriddenDescriptors.isEmpty()) { "Constructors cannot override anything" }
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): ClassConstructorDescriptorImpl {
        check(!(kind != CallableMemberDescriptor.Kind.DECLARATION && kind != CallableMemberDescriptor.Kind.SYNTHESIZED)) {
            """
                Attempt at creating a constructor that is not a declaration: 
                copy from: $this
                newOwner: $newOwner
                kind: $kind
                """.trimIndent()
        }
        assert(newName == null) { "Attempt to rename constructor: $this" }
        return ClassConstructorDescriptorImpl(
            newOwner as ClassDescriptor,
            this,
            annotations,
            isPrimary,
            CallableMemberDescriptor.Kind.DECLARATION,
            source
        )
    }

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): ClassConstructorDescriptor {
        return super.copy(newOwner, modality, visibility, kind, copyOverrides) as ClassConstructorDescriptor
    }

    companion object {
        @JvmOverloads
        fun create(
            containingDeclaration: ClassDescriptor,
            annotations: Annotations,
            isPrimary: Boolean,
            source: SourceElement,
            isEnd: Boolean = false
        ): ClassConstructorDescriptorImpl {
            return ClassConstructorDescriptorImpl(
                containingDeclaration,
                null,
                annotations,
                isPrimary,
                CallableMemberDescriptor.Kind.DECLARATION,
                source, isEnd
            )
        }

        fun createSynthesized(
            containingDeclaration: ClassDescriptor,
            annotations: Annotations,
            isPrimary: Boolean,
            source: SourceElement
        ): ClassConstructorDescriptorImpl {
            return ClassConstructorDescriptorImpl(
                containingDeclaration,
                null,
                annotations,
                isPrimary,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                source
            )
        }
    }
}
