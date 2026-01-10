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

package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.*
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*


interface DescriptorWithContainerSource : MemberDescriptor {
    val containerSource: DeserializedContainerSource?
}

interface DeserializedMemberDescriptor : DeserializedDescriptor, MemberDescriptor, DescriptorWithContainerSource {
    val decl: DeclarationWrapper


    // Information about the origin of this callable's container (class or package part on JVM) or null if there's no such information.
    // TODO: merge with sourceElement of containingDeclaration
    override val containerSource: DeserializedContainerSource?
}

interface DeserializedCallableMemberDescriptor : DeserializedMemberDescriptor, CallableMemberDescriptor

class DeserializedSimpleFunctionDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    override val decl: FunctionWrapper,

    override val containerSource: DeserializedContainerSource?,
    source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
    SimpleFunctionDescriptorImpl(
        containingDeclaration, original, annotations, name, kind,
        source ?: SourceElement.NO_SOURCE
    ) {


    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,

        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        return DeserializedSimpleFunctionDescriptor(
            newOwner, original as SimpleFunctionDescriptor?, annotations, newName ?: name, kind,
            decl, containerSource, source
        ).also {
            it.setHasStableParameterNames(hasStableParameterNames())
        }
    }
}

class DeserializedVariableDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: VariableDescriptor?,
    annotations: Annotations,
    modality: Modality,
    visibility: DescriptorVisibility,
    isVar: Boolean,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    override val declaresDefaultValue: Boolean,
    override val decl: VariableWrapper,

    override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor, VariableDescriptorImpl(
    containingDeclaration, name, null, isVar, SourceElement.NO_SOURCE, visibility
) {


    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        newModality: Modality,
        newVisibility: DescriptorVisibility,
        original: VariableDescriptor?,
        kind: CallableMemberDescriptor.Kind,

        newName: Name,
        source: SourceElement
    ): VariableDescriptorImpl {
        return DeserializedVariableDescriptor(
            newOwner, original, annotations, newModality, newVisibility, isVar, newName, kind,
            this.declaresDefaultValue, decl, containerSource
        )
    }
}

class DeserializedPropertyDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: PropertyDescriptor?,
    annotations: Annotations,
    modality: Modality,
    visibility: DescriptorVisibility,
    isVar: Boolean,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    override val decl: PropertyWrapper,

    override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor, PropertyDescriptorImpl(
    containingDeclaration, original, annotations, modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE
) {


    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        newModality: Modality,
        newVisibility: DescriptorVisibility,
        original: PropertyDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name,
        source: SourceElement
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
            newOwner, original, annotations, newModality, newVisibility, isVar, newName, kind,
            decl, containerSource
        )
    }
}

class DeserializedClassConstructorDescriptor(
    containingDeclaration: ClassDescriptor,
    original: ConstructorDescriptor?,
    annotations: Annotations,
    isPrimary: Boolean,
    kind: CallableMemberDescriptor.Kind,
    override val decl: ConstructorWrapper,

    override val containerSource: DeserializedContainerSource?,
    source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
    ClassConstructorDescriptorImpl(
        containingDeclaration,
        original,
        annotations,
        isPrimary,
        kind,
        source ?: SourceElement.NO_SOURCE
    ) {


    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): DeserializedClassConstructorDescriptor {
        return DeserializedClassConstructorDescriptor(
            newOwner as ClassDescriptor, original as ConstructorDescriptor?, annotations, isPrimary, kind,
            decl, containerSource, source
        ).also {
            it.setHasStableParameterNames(hasStableParameterNames())
        }
    }
}

class DeserializedTypeAliasDescriptor(
    storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    visibility: DescriptorVisibility,
    override val decl: TypeAliasWrapper,

    override val containerSource: DeserializedContainerSource?
) : AbstractTypeAliasDescriptor(
    storageManager,
    containingDeclaration,
    name,
    SourceElement.NO_SOURCE,
    visibility,
    annotations,
),
    DeserializedMemberDescriptor {


    /**
     * 底层类型，即类型别名直接指向的类型。
     * 例如：typealias MyList = List<String>，则 underlyingType 为 List<String>
     */
    override lateinit var underlyingType: SimpleType private set

    /**
     * 展开类型，即递归展开所有类型别名后的最终类型。
     * 例如：
     * ```cangjie
     * type StringList = List<String>;
     * type MyStringList = StringList
     * ```
     * 对于 MyStringList，underlyingType 是 StringList，expandedType 是 List<String>
     *
     * 但是仓颉序列化文件目前不支持underlyingType，只有expandedType完全递归展开后的
     */
    override lateinit var expandedType: SimpleType private set
    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>
    private lateinit var defaultTypeImpl: SimpleType

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        underlyingType: SimpleType,
        expandedType: SimpleType
    ) {
        initialize(declaredTypeParameters)
        this.underlyingType = underlyingType
        this.expandedType = expandedType
        typeConstructorParameters = computeConstructorTypeParameters()
        defaultTypeImpl = computeDefaultType()
    }

    override val classDescriptor: ClassDescriptor?
        get() = if (expandedType.isError) null else expandedType.constructor.declarationDescriptor as? ClassDescriptor

    override val defaultType: SimpleType
        get() = defaultTypeImpl

    override fun substitute(substitutor: DefaultTypeSubstitutor): ClassifierDescriptorWithTypeParameters? {
        if (substitutor.isEmpty) return this
        val substituted = DeserializedTypeAliasDescriptor(
            storageManager, containingDeclaration, annotations, name, visibility,
            decl, containerSource
        )
        substituted.initialize(
            declaredTypeParameters,
            substitutor.safeSubstitute(underlyingType).asSimpleType(),
            substitutor.safeSubstitute(expandedType).asSimpleType()
        )

        return substituted
    }

    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> = typeConstructorParameters
}
