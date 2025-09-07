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


import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.FunctionClassScope
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.functions.FunctionClassKind
import org.cangnova.cangjie.types.functions.FunctionTypeKind


class FunctionClassDescriptor(
    private val storageManager: StorageManager,
    override val containingDeclaration: DeclarationDescriptor,
    val functionTypeKind: FunctionTypeKind,
    val arity: Int
) : AbstractClassDescriptor(storageManager, functionTypeKind.numberedClassName(arity)) {

    private val _typeConstructor = FunctionTypeConstructor()
    private val memberScope = FunctionClassScope(storageManager, this)
    override val typeConstructor: FunctionTypeConstructor
        get() = _typeConstructor
    private val parameters: List<TypeParameterDescriptor>

    init {
        val result = ArrayList<TypeParameterDescriptor>()

        fun typeParameter(variance: Variance, name: String) {
            result.add(
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    this@FunctionClassDescriptor,
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
            typeParameter(Variance.INVARIANT, "P$i")
        }

        typeParameter(Variance.INVARIANT, "R")

        parameters = result.toList()
    }


    override val staticScope: MemberScope
        get() = MemberScope.Empty

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner  ) = memberScope
    override val constructors: Collection<ClassConstructorDescriptor>
        get() = emptyList<ClassConstructorDescriptor>()
    override val kind: ClassKind
        get() = ClassKind.INTERFACE
    override val modality: Modality
        get() = Modality.ABSTRACT
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = null
    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = emptySet()

    override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC


    override val annotations: Annotations get() = Annotations.EMPTY

    override val source: SourceElement = SourceElement.NO_SOURCE
    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = emptyList()
    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = parameters

      inner class FunctionTypeConstructor : AbstractClassTypeConstructor(storageManager) {
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
            get() = this@FunctionClassDescriptor.parameters
        override val isDenotable: Boolean
            get() = true
        override val declarationDescriptor: FunctionClassDescriptor
            get() = this@FunctionClassDescriptor

        override fun toString() = declarationDescriptor.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = SupertypeLoopChecker.EMPTY
    }

    override fun toString() = name.asString()

    companion object {
        fun create(
            storageManager: StorageManager,
            containingDeclaration: DeclarationDescriptor,
            functionTypeKind: FunctionTypeKind,
            arity: Int
        ): FunctionClassDescriptor {
            return FunctionClassDescriptor(
                storageManager, containingDeclaration, functionTypeKind, arity
            )
        }

//        fun create(builtIns: CangJieBuiltIns, parameterCount: Int): FunctionClassDescriptor {
//            return create(
//                builtIns.storageManager,
//                builtIns.builtInsModule,
//                FunctionTypeKind.Function,
//                parameterCount
//            )
//        }

        private val functionClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Function"))
//        private val kFunctionClassId = ClassId(CANGJIE_REFLECT_FQ_NAME, Name.identifier("KFunction"))
    }


    val functionKind: FunctionClassKind = FunctionClassKind.getFunctionClassKind(functionTypeKind)

    /**
     * 创建函数类型实例
     *
     * 基于当前函数类描述符创建对应的FunctionType实例。
     *
     * @param receiverType 接收者类型（可选）
     * @param contextReceiverTypes 上下文接收者类型列表
     * @param parameterTypes 参数类型列表
     * @param returnType 返回类型
     * @param attributes 类型属性
     * @param isOption 是否为Option类型
     * @return 创建的FunctionType实例
     */
    fun createFunctionType(
        parameterTypes: List<CangJieType>,
        returnType: CangJieType,

        isOption: Boolean = false
    ): FunctionType {
        return FunctionType(
            constructor = typeConstructor,

            parameterTypes = parameterTypes,
            returnType = returnType,
            isOption = isOption
        )
    }
}
