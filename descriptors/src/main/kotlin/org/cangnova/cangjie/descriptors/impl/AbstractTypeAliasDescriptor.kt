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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

abstract class AbstractTypeAliasDescriptor(
    protected val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,

    name: Name,
    sourceElement: SourceElement,
    private val visibilityImpl: DescriptorVisibility,
    annotations: Annotations = Annotations.EMPTY,
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


    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = declaredTypeParametersImpl


    override val modality: Modality = Modality.FINAL

    override val visibility: DescriptorVisibility
        get() = visibilityImpl


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

            cangjieTypeRefiner.refineDescriptor(this)?.defaultType

        override val declarationDescriptor: TypeAliasDescriptor
            get() =
                this@AbstractTypeAliasDescriptor


        override val parameters: List<TypeParameterDescriptor>
            get() = getTypeConstructorTypeParameters()

        override val supertypes: Collection<CangJieType>
            get() = declarationDescriptor.underlyingType.constructor.supertypes

        override val isFinal: Boolean
            get() = declarationDescriptor.underlyingType.constructor.isFinal


        override val isDenotable: Boolean = true

        override val builtIns: CangJieBuiltIns
            get() = declarationDescriptor.builtIns

        override fun toString(): String = "[typealias ${declarationDescriptor.name.asString()}]"

        // There must be @TypeRefinement, but there is a bug with anonymous objects and experimental annotations

        @OptIn(TypeRefinement::class)
        override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
    }
}

