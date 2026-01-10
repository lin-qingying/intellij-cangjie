/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.getCangJieTypeRefiner
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.CangJieTypeFactory.enumTypeWithNonTrivialMemberScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

class LazySubstitutingEnumDescriptor(
    override val original: ModuleAwareEnumDescriptor, private val originalSubstitutor: DefaultTypeSubstitutor
) : ModuleAwareEnumDescriptor(), EnumDescriptor {



    val originalEnum = original as EnumDescriptor
    private var newSubstitutor: DefaultTypeSubstitutor? = null
    private lateinit var typeConstructorParameters: MutableList<TypeParameterDescriptor>
    private lateinit var myDeclaredTypeParameters: MutableList<TypeParameterDescriptor>
    private var myTypeConstructor: TypeConstructor? = null


    private fun getSubstitutor(): DefaultTypeSubstitutor {
        if (newSubstitutor == null) {
            if (originalSubstitutor.isEmpty) {
                newSubstitutor = originalSubstitutor
            } else {
                val originalTypeParameters =
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
        typeArguments: List<TypeArgument>,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope {
        val memberScope =
            original.getMemberScope(typeArguments, cangjieTypeRefiner)
        if (originalSubstitutor.isEmpty) {
            return memberScope
        }
        return SubstitutingScope(memberScope, getSubstitutor())
    }


    
    override fun getMemberScope(typeArguments: List<TypeArgument>): MemberScope {
        return getMemberScope(
            typeArguments, DescriptorUtils.getContainingModule(
                this
            ).getCangJieTypeRefiner(

            )
        )

    }

    
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

    


    override val unsubstitutedMemberScope: MemberScope
        get() {
            return getUnsubstitutedMemberScope(
                DescriptorUtils.getContainingModule(
                    original
                ).getCangJieTypeRefiner()
            )
        }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitEnumDescriptor(this, data!!)

    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
        TODO("Not yet implemented")
    }

    override val source: SourceElement = SourceElement.NO_SOURCE

    override val typeConstructor: TypeConstructor
        get() {
            val originalTypeConstructor: TypeConstructor = original.typeConstructor
            if (originalSubstitutor.isEmpty) {
                return originalTypeConstructor
            }

            if (myTypeConstructor == null) {
                val substitutor: DefaultTypeSubstitutor = getSubstitutor()

                val originalSupertypes: Collection<CangJieType> =
                    originalTypeConstructor.supertypes
                val supertypes =
                    ArrayList<CangJieType>(originalSupertypes.size)
                for (supertype in originalSupertypes) {
                    substitutor.substitute(supertype )?.let { supertypes.add(it) }
                }

                myTypeConstructor = EnumTypeConstructorImpl(
                    this,
                    typeConstructorParameters,
                    supertypes,
                    LockBasedStorageManager.NO_LOCKS
                )
            }

            return myTypeConstructor!!
        }
    override val defaultType: SimpleType
        get() {

            val TypeArguments: List<TypeArgument> =
                TypeUtils.getDefaultTypeArguments(
                    typeConstructor.parameters
                )
            return enumTypeWithNonTrivialMemberScope(
                DefaultTypeAttributeTranslator.toAttributes(annotations, null, null),
                typeConstructor as EnumTypeConstructor,
                TypeArguments,
                false,
                unsubstitutedMemberScope
            )
        }


    override val visibility: DescriptorVisibility
        get() = original.visibility


    override val modality: Modality
        get() = original.modality


    override fun substitute(substitutor: DefaultTypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        if (substitutor.isEmpty) return this
        return LazySubstitutingEnumDescriptor(
            this,
            DefaultTypeSubstitutor.createChainedSubstitutor(
                substitutor.substitution,
                getSubstitutor().substitution
            )
        )

    }

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() {

            getSubstitutor()
            return myDeclaredTypeParameters
        }

    override fun getAllConstructors(): Collection<EnumConstructorDescriptor> {
        return originalEnum.getAllConstructors()
    }


    override val staticScope: MemberScope
        get() = original.staticScope
    override val constructors: Collection<EnumConstructorDescriptor>
        get() = originalEnum.constructors

    override val hasArguments: Boolean
        get() = originalEnum.hasArguments
    override val isNonExhaustive: Boolean
        get() = originalEnum.isNonExhaustive


    private fun substituteSimpleType(type: SimpleType?): SimpleType? {
        if (type == null || originalSubstitutor.isEmpty) return type

        val substitutor: DefaultTypeSubstitutor = getSubstitutor()
        val substitutedType: CangJieType? =
            substitutor.substitute(type)

        assert(substitutedType is SimpleType) {
            """
            Substitution for SimpleType should also be a SimpleType, but it is $substitutedType
            Unsubstituted: $type
            """.trimIndent()
        }
        return substitutedType as SimpleType
    }


    override val containingDeclaration: DeclarationDescriptor
        get() = original.containingDeclaration
    override val annotations: Annotations
        get() = original.annotations

    override val name: Name
        get() = original.name

    override val thisAsReceiverParameter: ReceiverParameterDescriptor
        get() = original.thisAsReceiverParameter

    /**
     * 返回枚举描述符的字符串表示，用于调试
     *
     * 格式: `enum EnumName<TypeArg1, TypeArg2, ...>`
     *
     * 示例:
     * - `enum Option<Int>` - 类型参数已替换
     * - `enum Result<String, Error>` - 多个类型参数
     * - `enum Status` - 无类型参数的枚举
     */
    override fun toString(): String {
        return buildString {
            append("enum ")
            append(name.asString())

            // 获取类型参数
            val typeParams = typeConstructor.parameters
            if (typeParams.isNotEmpty()) {
                append("<")
                typeParams.joinTo(this, ", ") { param ->
                    // 获取替换后的类型
                    val substitutor = getSubstitutor()
                    val substitutedType = substitutor.substitute(param.defaultType)
                    substitutedType?.toString() ?: param.name.asString()
                }
                append(">")
            }
        }
    }
}
