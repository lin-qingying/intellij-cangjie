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

package org.cangnova.cangjie.resolve.lazy.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractTypeAliasDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.NullableLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.types.*

class LazyTypeAliasDescriptor(
    storageManager: StorageManager,
    private val trace: BindingTrace,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations = Annotations.EMPTY,
    name: Name,
    sourceElement: SourceElement,
    visibility: DescriptorVisibility
) : AbstractTypeAliasDescriptor(storageManager, containingDeclaration, name, sourceElement, visibility),
    TypeAliasDescriptor {
    override val constructors: Collection<TypeAliasConstructorDescriptor> by storageManager.createLazyValue {
        getTypeAliasConstructors()
    }
    private val lazyTypeConstructorParameters =
        storageManager.createRecursionTolerantLazyValue({ this.computeConstructorTypeParameters() }, emptyList())
    private lateinit var underlyingTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var expandedTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var defaultTypeImpl: NotNullLazyValue<SimpleType>
    private lateinit var classDescriptorImpl: NullableLazyValue<ClassDescriptor>
    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> =
        lazyTypeConstructorParameters()

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        underlyingType: SimpleType,
        expandedType: SimpleType
    ) = initialize(
        declaredTypeParameters,
        storageManager.createLazyValue { underlyingType },
        storageManager.createLazyValue { expandedType }
    )

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        lazyUnderlyingType: NotNullLazyValue<SimpleType>,
        lazyExpandedType: NotNullLazyValue<SimpleType>
    ) {
        super.initialize(declaredTypeParameters)
        this.underlyingTypeImpl = lazyUnderlyingType
        this.expandedTypeImpl = lazyExpandedType
        this.defaultTypeImpl = storageManager.createLazyValue { computeDefaultType() }
        this.classDescriptorImpl =
            storageManager.createRecursionTolerantNullableLazyValue({ computeClassDescriptor() }, null)
    }

    private fun computeClassDescriptor(): ClassDescriptor? {
        if (underlyingType.isError) return null
        return when (val underlyingTypeDescriptor = underlyingType.constructor.declarationDescriptor) {
            is ClassDescriptor -> underlyingTypeDescriptor
            is TypeAliasDescriptor -> underlyingTypeDescriptor.classDescriptor
            else -> null
        }
    }

    override val underlyingType: SimpleType get() = underlyingTypeImpl()
    override val expandedType: SimpleType get() = expandedTypeImpl()
    override val classDescriptor: ClassDescriptor? get() = classDescriptorImpl()
    override val defaultType: SimpleType
        get() = defaultTypeImpl()

    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        if (substitutor.isEmpty) return this
        val substituted = LazyTypeAliasDescriptor(
            storageManager, trace,
            containingDeclaration, annotations, name, source, visibility
        )
        substituted.initialize(
            declaredTypeParameters,
            storageManager.createLazyValue {
                substitutor.substitute(underlyingType, Variance.INVARIANT)!!.asSimpleType()
            },
            storageManager.createLazyValue {
                substitutor.substitute(expandedType, Variance.INVARIANT)!!.asSimpleType()
            }
        )
        return substituted
    }

    override val kind: ClassKind
        get() = TODO("Not yet implemented")

    companion object {
        @JvmStatic
        fun create(
            storageManager: StorageManager,
            trace: BindingTrace,
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,
            name: Name,
            sourceElement: SourceElement,
            visibility: DescriptorVisibility
        ): LazyTypeAliasDescriptor =
            LazyTypeAliasDescriptor(
                storageManager, trace,
                containingDeclaration, annotations, name, sourceElement, visibility
            )
    }
}
