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
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.TupleClassScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

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

    private val parameters: List<TypeParameterDescriptor>

    init {
        val result = ArrayList<TypeParameterDescriptor>()

        fun typeParameter(variance: Variance, name: String) {
            result.add(
                TypeParameterDescriptorImpl.createWithDefaultBound(
                    this@TupleClassDescriptor,
                    Annotations.EMPTY,
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

    inner class TupleTypeConstructor : AbstractClassTypeConstructor(storageManager) {
        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
            return emptyList()
        }

        override fun computeSupertypes(): Collection<CangJieType> {
            return listOf(builtIns.anyType)
        }

        override val parameters: List<TypeParameterDescriptor>
            get() = this@TupleClassDescriptor.parameters

        override val declarationDescriptor: TupleClassDescriptor
            get() = this@TupleClassDescriptor

        override val isDenotable: Boolean
            get() = true
            
        override fun toString() = declarationDescriptor.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            get() = SupertypeLoopChecker.EMPTY
    }

    override val staticScope: MemberScope
        get() = MemberScope.Empty
        
    override val typeConstructor: TupleTypeConstructor
        get() = _typeConstructor
        
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = memberScope

    override val source: SourceElement = SourceElement.NO_SOURCE

    override val constructors: Collection<ClassConstructorDescriptor>
        get() = emptyList()

    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = emptyList()

    override val kind: ClassKind
        get() = ClassKind.TUPLE
        
    override val modality: Modality
        get() = Modality.FINAL
        
    override val visibility: DescriptorVisibility 
        get() = DescriptorVisibilities.PUBLIC
        
    override val annotations: Annotations 
        get() = Annotations.EMPTY
        
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = null

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = parameters
        
    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = emptyList()

    override fun toString(): String {
        return "Tuple$arity"
    }

    /**
     * 创建元组类型实例
     *
     * 基于当前元组类描述符创建对应的TupleType实例。
     *
     * @param elementTypes 元组元素类型列表
     * @param attributes 类型属性
     * @param isOption 是否为Option类型
     * @return 创建的TupleType实例
     */
    fun createTupleType(
        elementTypes: List<CangJieType>,
        attributes: TypeAttributes = TypeAttributes.Empty,
        isOption: Boolean = false
    ): TupleType {
        require(elementTypes.size == arity) { 
            "Element types count (${elementTypes.size}) must match tuple arity ($arity)" 
        }
        return TupleType(
            constructor = typeConstructor,
            attributes = attributes,
            elementTypes = elementTypes,
            isOption = isOption
        )
    }
}
