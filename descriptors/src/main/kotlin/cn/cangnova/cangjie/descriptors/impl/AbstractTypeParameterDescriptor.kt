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

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.resolve.scopes.LazyScopeAdapter
import cn.cangnova.cangjie.resolve.scopes.TypeIntersectionScope.Companion.create
import cn.cangnova.cangjie.storage.NotNullLazyValue
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import cn.cangnova.cangjie.types.ErrorUtils.createErrorType
import cn.cangnova.cangjie.types.TypeAttributes.Companion.Empty
import cn.cangnova.cangjie.types.error.ErrorTypeKind


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
                    create(
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


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
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

        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
            return listOf()
        }

        override fun getParameters(): List<TypeParameterDescriptor> {
            return emptyList()
        }

        override fun isFinal(): Boolean {
            return false
        }

        override fun isDenotable(): Boolean {
            return true
        }

        override fun getDeclarationDescriptor(): ClassifierDescriptor {
            return this@AbstractTypeParameterDescriptor
        }

        override fun getBuiltIns(): CangJieBuiltIns {
            return this@AbstractTypeParameterDescriptor.builtIns
        }

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
