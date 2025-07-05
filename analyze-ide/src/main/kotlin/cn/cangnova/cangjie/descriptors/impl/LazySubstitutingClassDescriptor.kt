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
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.descriptorUtil.getCangJieTypeRefiner
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.resolve.scopes.SubstitutingScope
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.util.TypeUtils

class LazySubstitutingClassDescriptor(
    override val original: ModuleAwareClassDescriptor, private val originalSubstitutor: TypeSubstitutor
) : ModuleAwareClassDescriptor() {
    private var newSubstitutor: TypeSubstitutor? = null
    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>
    private lateinit var myDeclaredTypeParameters: MutableList<TypeParameterDescriptor>
    private var myTypeConstructor: TypeConstructor? = null


    private fun getSubstitutor(): TypeSubstitutor {
        if (newSubstitutor == null) {
            if (originalSubstitutor.isEmpty) {
                newSubstitutor = originalSubstitutor
            } else {
                val originalTypeParameters: List<TypeParameterDescriptor> =
                    original.typeConstructor.parameters
                typeConstructorParameters =
                    ArrayList(originalTypeParameters.size)
                newSubstitutor = DescriptorSubstitutor.substituteTypeParameters(
                    originalTypeParameters, originalSubstitutor.substitution, this, typeConstructorParameters
                )

                myDeclaredTypeParameters =
                    typeConstructorParameters.filter { descriptor: TypeParameterDescriptor -> !descriptor.isCapturedFromOuterDeclaration }
                        .toMutableList()
            }
        }
        return newSubstitutor!!
    }


    override fun getMemberScope(
        typeSubstitution: TypeSubstitution,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        val memberScope =
            original.getMemberScope(typeSubstitution, cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }

    override fun getMemberScope(
        typeArguments: List<TypeProjection>,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        val memberScope =
            original.getMemberScope(typeArguments, cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }

    @OptIn(TypeRefinement::class)
    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope {
        return getMemberScope(
            typeArguments, DescriptorUtils.getContainingModule(
                this
            ).getCangJieTypeRefiner(

            )
        )

    }

    @OptIn(TypeRefinement::class)
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        return getMemberScope(
            typeSubstitution, DescriptorUtils.getContainingModule(
                this
            ).getCangJieTypeRefiner(

            )
        )

    }

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {

        val memberScope =
            original.getUnsubstitutedMemberScope(cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }

    @OptIn(TypeRefinement::class)
    override fun getUnsubstitutedMemberScope(): MemberScope {
        return getUnsubstitutedMemberScope(
            DescriptorUtils.getContainingModule(
                original
            ).getCangJieTypeRefiner()
        )
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        return visitor.visitClassDescriptor(this, data!!)

    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        TODO("Not yet implemented")
    }

    override val source: SourceElement  = SourceElement.NO_SOURCE


    override fun getTypeConstructor(): TypeConstructor {
        val originalTypeConstructor: TypeConstructor = original.typeConstructor
        if (originalSubstitutor.isEmpty) {
            return originalTypeConstructor
        }

        if (myTypeConstructor == null) {
            val substitutor: TypeSubstitutor = getSubstitutor()

            val originalSupertypes: Collection<CangJieType> =
                originalTypeConstructor.supertypes
            val supertypes: MutableCollection<CangJieType> =
                java.util.ArrayList<CangJieType>(originalSupertypes.size)
            for (supertype in originalSupertypes) {
                substitutor.substitute(supertype, Variance.INVARIANT)?.let { supertypes.add(it) }
            }

            myTypeConstructor = ClassTypeConstructorImpl(
                this,
                typeConstructorParameters,
                supertypes,
                LockBasedStorageManager.NO_LOCKS
            )
        }

        return myTypeConstructor!!
    }

    override fun getDefaultType(): SimpleType {
        val typeProjections: List<TypeProjection> =
            TypeUtils.getDefaultTypeProjections(
                typeConstructor.parameters
            )
        return simpleTypeWithNonTrivialMemberScope(
            DefaultTypeAttributeTranslator.toAttributes(annotations, null, null),
            typeConstructor,
            typeProjections,
            false,
            unsubstitutedMemberScope
        )
    }



    override val visibility: DescriptorVisibility
        get() =original.visibility

    override fun getModality(): Modality {
        return original.modality
    }

    override fun setModality(modality: Modality) {
        original.modality = modality
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters {
        if (substitutor.isEmpty) return this
        return LazySubstitutingClassDescriptor(
            this,
            TypeSubstitutor.createChainedSubstitutor(
                substitutor.substitution,
                getSubstitutor().substitution
            )
        )

    }

    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> {
        getSubstitutor()
        return myDeclaredTypeParameters
    }

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor {
        TODO("Not yet implemented")
    }

    override fun getContextReceivers(): List<ReceiverParameterDescriptor> {
        return emptyList()
    }

    override fun getUnsubstitutedInnerClassesScope(): MemberScope {
        return original.unsubstitutedInnerClassesScope

    }

    override fun getStaticScope(): MemberScope {
        return original.staticScope

    }

    override fun getConstructors(): MutableCollection<ClassConstructorDescriptor> {
        val originalConstructors: Collection<ClassConstructorDescriptor> =
            original.constructors
        val result: MutableCollection<ClassConstructorDescriptor> =
            java.util.ArrayList<ClassConstructorDescriptor>(originalConstructors.size)
        for (constructor in originalConstructors) {
            val copy: ClassConstructorDescriptor = constructor.newCopyBuilder()
                .setOriginal(constructor.original)
                .setModality(constructor.getModality())
                .setVisibility(constructor.visibility)
                .setKind(constructor.kind)
                .setCopyOverrides(false)
                .build() as ClassConstructorDescriptor
            copy.substitute(getSubstitutor())?.let { result.add(it) }
        }
        return result
    }
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> =emptySet()


    override fun getKind(): ClassKind {
        return original.kind

    }

    override fun isFun(): Boolean {
        return original.isFun

    }

    override fun isValue(): Boolean {
        return original.isValue

    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        return original.unsubstitutedPrimaryConstructor

    }

    override fun getSealedSubclasses(): MutableCollection<ClassDescriptor> {
        return original.sealedSubclasses

    }

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? {
        return substituteSimpleType(original.defaultFunctionTypeForSamInterface)

    }

    private fun substituteSimpleType(type: SimpleType?): SimpleType? {
        if (type == null || originalSubstitutor.isEmpty) return type

        val substitutor: TypeSubstitutor = getSubstitutor()
        val substitutedType: CangJieType? =
            substitutor.substitute(type, Variance.INVARIANT)

        assert(substitutedType is SimpleType) {
            """
            Substitution for SimpleType should also be a SimpleType, but it is $substitutedType
            Unsubstituted: $type
            """.trimIndent()
        }
        return substitutedType as SimpleType
    }

    override fun isDefinitelyNotSamInterface(): Boolean {
        return original.isDefinitelyNotSamInterface

    }

    override val containingDeclaration: DeclarationDescriptor
        get() = original.containingDeclaration
    override val annotations: Annotations
        get() = original.annotations

    override val name: Name
        get() = original.name
}
