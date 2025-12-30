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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.builtIns
import org.cangnova.cangjie.resolve.scopes.LazyScopeAdapter
import org.cangnova.cangjie.resolve.scopes.TypeIntersectionScope
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.TypeAttributes.Companion.Empty
import org.cangnova.cangjie.types.error.ErrorTypeKind

/**
 * 抽象类型参数描述符基类。
 *
 * 该类为实现类型参数（泛型参数）描述符提供公共逻辑，包括类型构造器的延迟计算、默认类型的创建、
 * 上界解析与超类型循环检查的支持。子类需要实现上界解析与循环错误报告的具体逻辑。
 *
 * @param storageManager 存储管理器，用于延迟缓存等功能
 * @param containingDeclaration 类型参数所在的声明描述符
 * @param annotations 注解集合
 * @param name 类型参数名称
 * @param variance 方差（in/out/invariant）
 * @param index 在声明中的位置索引（0 基）
 * @param source 源信息
 * @param supertypeLoopChecker 超类型循环检测器
 */
abstract class AbstractTypeParameterDescriptor protected constructor(
    override val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    override val variance: Variance,

    override val index: Int,
    source: SourceElement,
    supertypeLoopChecker: SupertypeLoopChecker
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, source),
    TypeParameterDescriptor {

    private val _typeConstructor: NotNullLazyValue<TypeConstructor> = storageManager.createLazyValue<TypeConstructor> {
        TypeParameterTypeConstructor(
            storageManager, supertypeLoopChecker
        )
    }


    override val typeConstructor: TypeConstructor
        get() = _typeConstructor.invoke()
    private val _defaultType: NotNullLazyValue<SimpleType>

    init {
        this._defaultType = storageManager.createLazyValue {
            simpleTypeWithNonTrivialMemberScope(
                Empty,
                typeConstructor, emptyList(), false,
                LazyScopeAdapter {
                    TypeIntersectionScope. create(
                        "Scope for type parameter " + name.asString(),
                        upperBounds
                    )
                }
            )
        }
    }

    override val original: TypeParameterDescriptor
        get() = super.original as TypeParameterDescriptor


    override val defaultType: SimpleType
        get() = _defaultType.invoke()


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
        return visitor.visitTypeParameterDescriptor(this, data)
    }


    override val upperBounds: List<CangJieType>
        get() = (typeConstructor as? TypeParameterTypeConstructor)?.supertypes ?: emptyList()


    override val isCapturedFromOuterDeclaration: Boolean
        get() = false

    protected abstract fun reportSupertypeLoopError(type: CangJieType)

    protected fun processBoundsWithoutCycles(bounds: List<CangJieType>): List<CangJieType> {
        return bounds
    }

    override fun validate() {
        super<DeclarationDescriptorNonRootImpl>.validate()
    }

    protected abstract fun resolveUpperBounds(): List<CangJieType>

    private inner class TypeParameterTypeConstructor(
        storageManager: StorageManager,
        override val supertypeLoopChecker: SupertypeLoopChecker
    ) :
        AbstractTypeConstructor(storageManager) {
        override fun computeSupertypes(): Collection<CangJieType> {
            return resolveUpperBounds()
        }





        override val parameters: List<TypeParameterDescriptor>
            get() = emptyList()


        override val isFinal: Boolean
            get() = false
        override val isDenotable: Boolean
            get() = true

        override val declarationDescriptor: ClassifierDescriptor
            get() =  this@AbstractTypeParameterDescriptor

        override val builtIns: CangJieBuiltIns
            get() = this@AbstractTypeParameterDescriptor.builtIns



        override fun toString(): String {
            return name.toString()
        }

        override fun reportSupertypeLoopError(type: CangJieType) {
            this@AbstractTypeParameterDescriptor.reportSupertypeLoopError(type)
        }

        override fun processSupertypesWithoutCycles(supertypes: List<CangJieType>): List<CangJieType> {
            return processBoundsWithoutCycles(supertypes)
        }

        override fun defaultSupertypeIfEmpty(): CangJieType {
            return createErrorType(ErrorTypeKind.CYCLIC_UPPER_BOUNDS)
        }

        override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
            return classifier is TypeParameterDescriptor /*&&
                    DescriptorEquivalenceForOverrides.INSTANCE.areTypeParametersEquivalent(
                            AbstractTypeParameterDescriptor.this,
                            (TypeParameterDescriptor) classifier,
                            true
                    )*/
        }
    }
}
