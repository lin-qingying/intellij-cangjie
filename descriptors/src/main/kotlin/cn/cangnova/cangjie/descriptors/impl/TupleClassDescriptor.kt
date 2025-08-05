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
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.resolve.scopes.TupleClassScope
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.AbstractClassTypeConstructor
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner

class TupleClassDescriptor(
    private val storageManager: StorageManager,
    override val containingDeclaration: DeclarationDescriptor,

    val arity: Int
) : AbstractClassDescriptor(storageManager, numberedClassName(arity)) {
    private val _typeConstructor = TupleTypeConstructor()
    private val memberScope = TupleClassScope(storageManager, this)

    companion object {
        fun numberedClassName(arity: Int) = Name.identifier("Tuple$arity")

        fun create(
            storageManager: StorageManager,
            containingDeclaration: DeclarationDescriptor,

            arity: Int
        ): TupleClassDescriptor {
            return TupleClassDescriptor(storageManager, containingDeclaration, arity)
        }
    }

//    private val typeConstructor = TupleTypeConstructor()
//    private val memberScope = FunctionClassScope(storageManager, this)

    private val parameters: List<TypeParameterDescriptor>

    init {
        val result = ArrayList<TypeParameterDescriptor>()

        fun typeParameter(variance: Variance, name: String) {
            result.add(
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    this@TupleClassDescriptor,
                    Annotations.EMPTY,
//                    false,
                    variance,
                    Name.identifier(name),
                    result.size,
                    storageManager
                )
            )
        }

        (1..arity).map { i ->
            typeParameter(Variance.INVARIANT, "T$i")
        }


        parameters = result.toList()
    }


    private inner class TupleTypeConstructor : AbstractClassTypeConstructor(storageManager) {
        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {

            return emptyList()
        }

        override fun computeSupertypes(): Collection<CangJieType> {
//                 val supertypes = when (functionTypeKind) {
//                FunctionTypeKind.Function -> // Function$N <: Function
//                    listOf(functionClassId)
//
//
//                else -> shouldNotBeCalled()
//            }
//
//            val moduleDescriptor = containingDeclaration.containingDeclaration
//            return supertypes.map { id ->
//                val descriptor =
//                    moduleDescriptor.findClassAcrossModuleDependencies(id) ?: error("Built-in class $id not found")
//
//                // Substitute all type parameters of the super class with our last type parameters
//                val arguments = parameters.takeLast(descriptor.typeConstructor.parameters.size).map {
//                    TypeProjectionImpl(it.defaultType)
//                }
//
//                CangJieTypeFactory.simpleNotNullType(TypeAttributes.Empty, descriptor, arguments)
//            }.toList()
            return listOf(builtIns.anyType)
        }

        override val parameters: List<TypeParameterDescriptor>
            get() = this@TupleClassDescriptor.parameters

        override val declarationDescriptor: ClassDescriptor
            get() = this@TupleClassDescriptor

        override val isDenotable: Boolean
            get() = true
        override fun toString() = declarationDescriptor.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = SupertypeLoopChecker.EMPTY
    }

    override val staticScope: MemberScope
        get() = MemberScope.Empty
    override val typeConstructor: TypeConstructor
        get() = _typeConstructor
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope


    override val source: SourceElement = SourceElement.NO_SOURCE

    override val constructors: Collection<ClassConstructorDescriptor>
        get() = emptyList()

    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = emptyList()

    override val kind: ClassKind
        get() =  ClassKind.TUPLE
    override val modality: Modality
        get() =  Modality.FINAL
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = null

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = parameters
    override fun toString(): String {
        return "Tuple$arity"
    }


    override fun getSealedSubclasses() = emptyList<ClassDescriptor>()
}
