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

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns

import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.storage.getValue

abstract class AbstractTypeAliasDescriptor(
    protected val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations = Annotations.EMPTY,
    name: Name,
    sourceElement: SourceElement,
    private val visibilityImpl: DescriptorVisibility
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, sourceElement),
    TypeAliasDescriptor {
    override val constructors: Collection<TypeAliasConstructorDescriptor> by storageManager.createLazyValue {
        getTypeAliasConstructors()
    }

    // TODO cangjieize some interfaces
    private lateinit var declaredTypeParametersImpl: List<TypeParameterDescriptor>

    fun initialize(declaredTypeParameters: List<TypeParameterDescriptor>) {
        this.declaredTypeParametersImpl = declaredTypeParameters
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        visitor.visitTypeAliasDescriptor(this, data)


//    override fun isInner(): Boolean =
//    // NB: it's ok to use underlyingType here, since referenced inner type aliases also capture type parameters.
//    // Using expandedType looks "proper", but in fact will cause a recursion in expandedType resolution,
//        // which will silently produce wrong result.
//        TypeUtils.contains(underlyingType) { type ->
//            !type.isError && run {
//                val constructorDescriptor = type.constructor.declarationDescriptor
//                constructorDescriptor is TypeParameterDescriptor &&
//                        constructorDescriptor.containingDeclaration != this@AbstractTypeAliasDescriptor
//            }
//        }


    fun getTypeAliasConstructors(): Collection<TypeAliasConstructorDescriptor> {
        val classDescriptor = this.classDescriptor ?: return emptyList()

        return classDescriptor.constructors.mapNotNull {
            TypeAliasConstructorDescriptorImpl.createIfAvailable(storageManager, this, it)
        }
    }

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> =
        declaredTypeParametersImpl

    override fun getModality() = Modality.FINAL
    override fun setModality(modality: Modality) {

    }

    override val visibility: DescriptorVisibility
        get() = visibilityImpl
//    override fun isExpect(): Boolean = false
//
//    override fun isActual(): Boolean = false
//
//    override fun isExternal() = false

    override fun getTypeConstructor(): TypeConstructor =
        typeConstructor

    override fun toString(): String = "typealias ${name.asString()}"


    override val original: TypeAliasDescriptor
        get() = super.original as TypeAliasDescriptor

    protected abstract fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor>

    @OptIn(TypeRefinement::class)
    protected fun computeDefaultType(): SimpleType =
        TypeUtils.makeUnsubstitutedType(
            this,
            classDescriptor?.unsubstitutedMemberScope ?: MemberScope.Empty
        ) { cangjieTypeRefiner ->
            cangjieTypeRefiner?.refineDescriptor(this)?.defaultType
        }

    private val typeConstructor = object : TypeConstructor {
        override fun getDeclarationDescriptor(): TypeAliasDescriptor =
            this@AbstractTypeAliasDescriptor

        override fun getParameters(): List<TypeParameterDescriptor> =
            getTypeConstructorTypeParameters()

        override fun getSupertypes(): Collection<CangJieType> =
            declarationDescriptor.underlyingType.constructor.supertypes

        override fun isFinal(): Boolean =
            declarationDescriptor.underlyingType.constructor.isFinal

        override fun isDenotable(): Boolean =
            true

        override fun getBuiltIns(): CangJieBuiltIns =
            declarationDescriptor.builtIns

        override fun toString(): String = "[typealias ${declarationDescriptor.name.asString()}]"

        // There must be @TypeRefinement, but there is a bug with anonymous objects and experimental annotations

        @OptIn(TypeRefinement::class)
        override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
    }
}

