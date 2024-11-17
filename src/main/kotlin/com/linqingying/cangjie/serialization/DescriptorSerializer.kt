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

package com.linqingying.cangjie.serialization


import com.intellij.openapi.project.Project
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotated
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptor
import com.linqingying.cangjie.extensions.TypeAttributeTranslatorExtension
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.deserialization.VersionRequirement
import com.linqingying.cangjie.metadata.serialization.Interner
import com.linqingying.cangjie.metadata.serialization.MutableTypeTable
import com.linqingying.cangjie.metadata.serialization.MutableVersionRequirementTable
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.DescriptorUtils.isEnumEntry
import com.linqingying.cangjie.resolve.MemberComparator
import com.linqingying.cangjie.resolve.RequireCangJieConstants
import com.linqingying.cangjie.resolve.constants.Int64Value
import com.linqingying.cangjie.resolve.constants.StringValue
import com.linqingying.cangjie.serialization.deserialization.ProtoEnumFlags
import com.linqingying.cangjie.serialization.deserialization.descriptorVisibility
import com.linqingying.cangjie.serialization.deserialization.memberKind
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.expressions.TypeAttributeTranslators
import com.linqingying.cangjie.types.util.replaceAnnotations
import java.util.*

class DescriptorSerializer private constructor(
    private val containingDeclaration: DeclarationDescriptor?,
    private val typeParameters: Interner<TypeParameterDescriptor>,
    private val extension: SerializerExtension,
    val typeTable: MutableTypeTable,
    private val versionRequirementTable: MutableVersionRequirementTable?,
    private val serializeTypeTableToFunction: Boolean,
    private val languageVersionSettings: LanguageVersionSettings,
    val plugins: List<DescriptorSerializerPlugin> = emptyList(),
    val typeAttributeTranslators: TypeAttributeTranslators? = null,
) {
//    private val contractSerializer = ContractSerializer()

    private var metDefinitelyNotNullType: Boolean = false

    private fun createChildSerializer(descriptor: DeclarationDescriptor): DescriptorSerializer =
        DescriptorSerializer(
            descriptor, Interner(typeParameters), extension, typeTable, versionRequirementTable,
            serializeTypeTableToFunction = false, typeAttributeTranslators = typeAttributeTranslators,
            languageVersionSettings = languageVersionSettings,
            plugins = plugins,
        )

    val stringTable: DescriptorAwareStringTable
        get() = extension.stringTable

    private fun useTypeTable(): Boolean = extension.shouldUseTypeTable()

    fun classProto(classDescriptor: ClassDescriptor): ProtoBuf.Class.Builder {
        val builder = ProtoBuf.Class.newBuilder()


        val flags = Flags.getClassFlags(

            hasAnnotations(classDescriptor),
            ProtoEnumFlags.descriptorVisibility(normalizeVisibility(classDescriptor)),
            ProtoEnumFlags.modality(classDescriptor.modality),
            ProtoEnumFlags.classKind(classDescriptor.kind)


        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.fqName = getClassifierId(classDescriptor)

        for (typeParameterDescriptor in classDescriptor.declaredTypeParameters) {
            builder.addTypeParameter(typeParameter(typeParameterDescriptor))
        }

        val contextReceivers = classDescriptor.contextReceivers
        if (useTypeTable()) {
            contextReceivers.forEach { builder.addContextReceiverTypeId(typeId(it.type)) }
        } else {
            contextReceivers.forEach { builder.addContextReceiverType(type(it.type)) }
        }

        if (!CangJieBuiltIns.isSpecialClassWithNoSupertypes(classDescriptor)) {
            // Special classes (Any, Nothing) have no supertypes
            for (supertype in classDescriptor.typeConstructor.supertypes) {
                if (useTypeTable()) {
                    builder.addSupertypeId(typeId(supertype))
                } else {
                    builder.addSupertype(type(supertype))
                }
            }
        }

        if (!DescriptorUtils.isAnonymousObject(classDescriptor) && classDescriptor.kind != ClassKind.ENUM_ENTRY) {
            for (descriptor in classDescriptor.constructors) {
                builder.addConstructor(constructorProto(descriptor))
            }
        }

        val callableMembers =
            extension.customClassMembersProducer?.getCallableMembers(classDescriptor)
                ?: sort(
                    DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)
                        .filterIsInstance<CallableMemberDescriptor>()
                        .filter { it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
                )

        for (descriptor in callableMembers) {
            when (descriptor) {

                is PropertyDescriptor -> propertyProto(descriptor)?.let { builder.addProperty(it) }
                is VariableDescriptor -> variableProto(descriptor)?.let { builder.addVariable(it) }
                is FunctionDescriptor -> functionProto(descriptor)?.let { builder.addFunction(it) }
            }
        }

        val nestedClassifiers = sort(DescriptorUtils.getAllDescriptors(classDescriptor.unsubstitutedInnerClassesScope))
        for (descriptor in nestedClassifiers) {
            if (descriptor is TypeAliasDescriptor) {
                typeAliasProto(descriptor)?.let { builder.addTypeAlias(it) }
            } else {
                val name = getSimpleNameIndex(descriptor.name)
                if (isEnumEntry(descriptor)) {
                    builder.addEnumEntry(enumEntryProto(descriptor as ClassDescriptor))
                } else {
                    builder.addNestedClassName(name)
                }
            }
        }

        for (sealedSubclass in classDescriptor.sealedSubclasses) {
            builder.addSealedSubclassFqName(getClassifierId(sealedSubclass))
        }


//        classDescriptor.multiFieldValueClassRepresentation?.let { multiFieldValueClassRepresentation ->
//            val namesToTypes = multiFieldValueClassRepresentation.underlyingPropertyNamesToTypes
//            builder.addAllMultiFieldValueClassUnderlyingName(namesToTypes.map { (name, _) -> getSimpleNameIndex(name) })
//            if (useTypeTable()) {
//                builder.addAllMultiFieldValueClassUnderlyingTypeId(namesToTypes.map { (_, kotlinType) -> typeId(kotlinType) })
//            } else {
//                builder.addAllMultiFieldValueClassUnderlyingType(namesToTypes.map { (_, kotlinType) -> type(kotlinType).build() })
//            }
//        }

        if (versionRequirementTable == null) error("Version requirements must be serialized for classes: $classDescriptor")

        builder.addAllVersionRequirement(versionRequirementTable.serializeVersionRequirements(classDescriptor))

        extension.serializeClass(classDescriptor, builder, versionRequirementTable, this)

        plugins.forEach { it.afterClass(classDescriptor, builder, versionRequirementTable, this, extension) }

        if (metDefinitelyNotNullType) {
            builder.addVersionRequirement(
                writeLanguageVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes, versionRequirementTable)
            )
        }

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    fun propertyProto(descriptor: PropertyDescriptor): ProtoBuf.Property.Builder? {
        val builder = ProtoBuf.Property.newBuilder()

        val local = createChildSerializer(descriptor)

        var hasGetter = false
        var hasSetter = false

        val compileTimeConstant = descriptor.getCompileTimeInitializer()
        val hasConstant = compileTimeConstant != null

        val hasAnnotations =
            hasAnnotations(descriptor)

        val defaultAccessorFlags = Flags.getAccessorFlags(
            hasAnnotations,
            ProtoEnumFlags.descriptorVisibility(normalizeVisibility(descriptor)),
            ProtoEnumFlags.modality(descriptor.modality),
            false
        )

        val getter = descriptor.getter
        if (getter != null) {
            hasGetter = true
            val accessorFlags = getAccessorFlags(getter)
            if (accessorFlags != defaultAccessorFlags) {
                builder.getterFlags = accessorFlags
            }
        }

        val setter = descriptor.setter
        if (setter != null) {
            hasSetter = true
            val accessorFlags = getAccessorFlags(setter)
            if (accessorFlags != defaultAccessorFlags) {
                builder.setterFlags = accessorFlags
            }

            if (!setter.isDefault) {
                val setterLocal = local.createChildSerializer(setter)
                for (valueParameterDescriptor in setter.valueParameters) {
                    builder.setSetterValueParameter(setterLocal.valueParameter(valueParameterDescriptor))
                }
            }
        }

        val flags = Flags.getPropertyFlags(
            hasAnnotations,
            ProtoEnumFlags.descriptorVisibility(normalizeVisibility(descriptor)),
            ProtoEnumFlags.modality(descriptor.modality),
            ProtoEnumFlags.memberKind(descriptor.kind),
            descriptor.isVar, hasGetter, hasSetter, hasConstant, descriptor.isConst/*, descriptor.isLateInit, descriptor.isExternal,
            descriptor.isDelegated, descriptor.isExpect*/
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(descriptor.type)
        } else {
            builder.setReturnType(local.type(descriptor.type))
        }

        for (typeParameterDescriptor in descriptor.typeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverParameter.type)
            } else {
                builder.setReceiverType(local.type(receiverParameter.type))
            }
        }

        val contextReceivers = descriptor.contextReceiverParameters
        if (useTypeTable()) {
            contextReceivers.forEach { builder.addContextReceiverTypeId(local.typeId(it.type)) }
        } else {
            contextReceivers.forEach { builder.addContextReceiverType(local.type(it.type)) }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(descriptor))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        extension.serializeProperty(descriptor, builder, versionRequirementTable, local)

        plugins.forEach { it.afterProperty(descriptor, builder, versionRequirementTable, this, extension) }

        return builder
    }

    fun variableProto(descriptor: VariableDescriptor): ProtoBuf.Variable.Builder? {
        val builder = ProtoBuf.Variable.newBuilder()

        val local = createChildSerializer(descriptor)


        val compileTimeConstant = descriptor.getCompileTimeInitializer()
        val hasConstant = compileTimeConstant != null

        val hasAnnotations =
            hasAnnotations(descriptor)


        val flags = Flags.getVariableFlags(
            hasAnnotations,
            ProtoEnumFlags.descriptorVisibility(normalizeVisibility(descriptor)),
            ProtoEnumFlags.modality(descriptor.modality),
            ProtoBuf.MemberKind.DECLARATION,
            descriptor.isVar, hasConstant, descriptor.isConst, descriptor.isStatic,
            /* descriptor.isExternal,
                        descriptor.isDelegated, descriptor.isExpect*/
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(descriptor.type)
        } else {
            builder.setReturnType(local.type(descriptor.type))
        }

        for (typeParameterDescriptor in descriptor.typeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverParameter.type)
            } else {
                builder.setReceiverType(local.type(receiverParameter.type))
            }
        }

        val contextReceivers = descriptor.contextReceiverParameters
        if (useTypeTable()) {
            contextReceivers.forEach { builder.addContextReceiverTypeId(local.typeId(it.type)) }
        } else {
            contextReceivers.forEach { builder.addContextReceiverType(local.type(it.type)) }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(descriptor))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        extension.serializeVariable(descriptor, builder, versionRequirementTable, local)

        plugins.forEach { it.afterVariable(descriptor, builder, versionRequirementTable, this, extension) }

        return builder
    }

    private fun normalizeVisibility(descriptor: DeclarationDescriptorWithVisibility) =
        // It can be necessary for Java classes serialization having package-private visibility
        if (extension.shouldUseNormalizedVisibility())
            descriptor.visibility.normalize()
        else
            descriptor.visibility

    private fun shouldSerializeHasStableParameterNames(descriptor: CallableMemberDescriptor): Boolean {
        return when {
            descriptor.hasStableParameterNames() -> true
            descriptor.kind == CallableMemberDescriptor.Kind.DELEGATION -> true
            else -> false
        }
    }

    fun functionProto(descriptor: FunctionDescriptor): ProtoBuf.Function.Builder? {
        val builder = ProtoBuf.Function.newBuilder()

        val local = createChildSerializer(descriptor)

        val flags = Flags.getFunctionFlags(
            hasAnnotations(descriptor),
            ProtoEnumFlags.descriptorVisibility(normalizeVisibility(descriptor)),
            ProtoEnumFlags.modality(descriptor.modality),
            ProtoEnumFlags.memberKind(descriptor.kind),
            descriptor.isOperator
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(descriptor.returnType!!)
        } else {
            builder.setReturnType(local.type(descriptor.returnType!!))
        }

        for (typeParameterDescriptor in descriptor.typeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverParameter.type)
            } else {
                builder.setReceiverType(local.type(receiverParameter.type))
            }
        }

        val contextReceivers = descriptor.contextReceiverParameters
        if (useTypeTable()) {
            contextReceivers.forEach { builder.addContextReceiverTypeId(local.typeId(it.type)) }
        } else {
            contextReceivers.forEach { builder.addContextReceiverType(local.type(it.type)) }
        }

        for (valueParameterDescriptor in descriptor.valueParameters) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor))
        }

//        contractSerializer.serializeContractOfFunctionIfAny(descriptor, builder, this)

        extension.serializeFunction(descriptor, builder, versionRequirementTable, local)

        plugins.forEach { it.afterFunction(descriptor, builder, versionRequirementTable, this, extension) }

        if (serializeTypeTableToFunction) {
            typeTable.serialize()?.let { builder.typeTable = it }
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(descriptor))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        return builder
    }

    private fun constructorProto(descriptor: ConstructorDescriptor): ProtoBuf.Constructor.Builder {
        val builder = ProtoBuf.Constructor.newBuilder()

        val local = createChildSerializer(descriptor)

        val flags = Flags.getConstructorFlags(
            hasAnnotations(descriptor),
            ProtoEnumFlags.descriptorVisibility(normalizeVisibility(descriptor)),
            !descriptor.isPrimary,
            shouldSerializeHasStableParameterNames(descriptor)
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        for (valueParameterDescriptor in descriptor.valueParameters) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor))
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(descriptor))

            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(writeVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes))
            }
        }

        extension.serializeConstructor(descriptor, builder, local)

        plugins.forEach { it.afterConstructor(descriptor, builder, versionRequirementTable, this, extension) }

        return builder
    }

    private fun typeAliasProto(descriptor: TypeAliasDescriptor): ProtoBuf.TypeAlias.Builder? {
        val builder = ProtoBuf.TypeAlias.newBuilder()
        val local = createChildSerializer(descriptor)

        val flags =
            Flags.getTypeAliasFlags(
                hasAnnotations(descriptor),
                ProtoEnumFlags.descriptorVisibility(normalizeVisibility(descriptor))
            )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        for (typeParameterDescriptor in descriptor.declaredTypeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val underlyingType = descriptor.underlyingType
        if (useTypeTable()) {
            builder.underlyingTypeId = local.typeId(underlyingType)
        } else {
            builder.setUnderlyingType(local.type(underlyingType))
        }

        val expandedType = descriptor.expandedType
        if (useTypeTable()) {
            builder.expandedTypeId = local.typeId(expandedType)
        } else {
            builder.setExpandedType(local.type(expandedType))
        }

        versionRequirementTable?.run {
            builder.addAllVersionRequirement(serializeVersionRequirements(descriptor))
            if (local.metDefinitelyNotNullType) {
                builder.addVersionRequirement(
                    writeLanguageVersionRequirement(LanguageFeature.DefinitelyNonNullableTypes, versionRequirementTable)
                )
            }
        }



        extension.serializeTypeAlias(descriptor, builder)

        plugins.forEach { it.afterTypealias(descriptor, builder, versionRequirementTable, this, extension) }

        return builder
    }

    private fun enumEntryProto(descriptor: ClassDescriptor): ProtoBuf.EnumEntry.Builder {
        val builder = ProtoBuf.EnumEntry.newBuilder()
        builder.name = getSimpleNameIndex(descriptor.name)

        val constructor = descriptor.unsubstitutedPrimaryConstructor

        constructor?.valueParameters?.forEach {
            builder.addType(type(it.type))

        }


        extension.serializeEnumEntry(descriptor, builder)
        return builder
    }

    private fun valueParameter(descriptor: ValueParameterDescriptor): ProtoBuf.ValueParameter.Builder {
        val builder = ProtoBuf.ValueParameter.newBuilder()

        val declaresDefaultValue = descriptor.declaresDefaultValue()
        val isNamed = descriptor.isNamed

        val flags = Flags.getValueParameterFlags(
            hasAnnotations(descriptor),
            declaresDefaultValue,
            isNamed/* descriptor.isCrossinline, descriptor.isNoinline*/
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.typeId = typeId(descriptor.type)
        } else {
            builder.setType(type(descriptor.type))
        }

        val varargElementType = descriptor.varargElementType
        if (varargElementType != null) {
            if (useTypeTable()) {
                builder.varargElementTypeId = typeId(varargElementType)
            } else {
                builder.setVarargElementType(type(varargElementType))
            }
        }

        extension.serializeValueParameter(descriptor, builder)

        return builder
    }

    private fun typeParameter(typeParameter: TypeParameterDescriptor): ProtoBuf.TypeParameter.Builder {
        val builder = ProtoBuf.TypeParameter.newBuilder()

        builder.id = getTypeParameterId(typeParameter)

        builder.name = getSimpleNameIndex(typeParameter.name)


        val variance = variance(typeParameter.variance)
        if (variance != builder.variance) {
            builder.variance = variance
        }
        extension.serializeTypeParameter(typeParameter, builder)

        val upperBounds = typeParameter.upperBounds
        if (upperBounds.size == 1 && CangJieBuiltIns.isDefaultBound(upperBounds.single())) return builder

        for (upperBound in upperBounds) {
            if (useTypeTable()) {
                builder.addUpperBoundId(typeId(upperBound))
            } else {
                builder.addUpperBound(type(upperBound))
            }
        }

        return builder
    }

    fun typeId(type: CangJieType): Int = typeTable[type(type)]

    internal fun type(type: CangJieType): ProtoBuf.Type.Builder {
        val builder = ProtoBuf.Type.newBuilder()

        if (type.isError) {
            extension.serializeErrorType(type, builder)
            return builder
        }

        if (type.isFlexible()) {
            val flexibleType = type.asFlexibleType()

            val lowerBound = type(flexibleType.lowerBound)
            val upperBound = type(flexibleType.upperBound)
            extension.serializeFlexibleType(flexibleType, lowerBound, upperBound)
            if (useTypeTable()) {
                lowerBound.flexibleUpperBoundId = typeTable[upperBound]
            } else {
                lowerBound.setFlexibleUpperBound(upperBound)
            }
            return lowerBound
        }



        when (val descriptor = type.constructor.declarationDescriptor) {
            is ClassDescriptor, is TypeAliasDescriptor -> {
                val possiblyInnerType =
                    type.buildPossiblyInnerType() ?: error("possiblyInnerType should not be null: $type")
                fillFromPossiblyInnerType(builder, possiblyInnerType)
            }

            is TypeParameterDescriptor -> {
                if (descriptor.containingDeclaration === containingDeclaration) {
                    builder.typeParameterName = getSimpleNameIndex(descriptor.name)
                } else {
                    builder.typeParameter = getTypeParameterId(descriptor)
                }

                if (type.unwrap() is DefinitelyNotNullType) {
                    metDefinitelyNotNullType = true
                    builder.flags = Flags.getTypeFlags(false, true)
                }

                assert(type.arguments.isEmpty()) { "Found arguments for type constructor build on type parameter: $descriptor" }
            }
        }

        if (type.isMarkedOption != builder.nullable) {
            builder.nullable = type.isMarkedOption
        }

        val abbreviation = type.getAbbreviatedType()?.abbreviation
        if (abbreviation != null) {
            if (useTypeTable()) {
                builder.abbreviatedTypeId = typeId(abbreviation)
            } else {
                builder.setAbbreviatedType(type(abbreviation))
            }
        }

        val typeWithUpdatedAnnotations = typeAttributeTranslators?.let {
            type.replaceAnnotations(it.toAnnotations(type.attributes))
        } ?: type

        extension.serializeType(typeWithUpdatedAnnotations, builder)

        return builder
    }

    private fun fillFromPossiblyInnerType(builder: ProtoBuf.Type.Builder, type: PossiblyInnerType) {
        val classifierDescriptor = type.classifierDescriptor
        val classifierId = getClassifierId(classifierDescriptor)

        when (classifierDescriptor) {
            is ClassDescriptor -> builder.className = classifierId
            is TypeAliasDescriptor -> builder.typeAliasName = classifierId
        }

        // Shouldn't write type parameters of a generic local anonymous type if it was replaced with Any
        if (stringTable.isLocalClassIdReplacementKeptGeneric || !DescriptorUtils.isLocal(classifierDescriptor)) {
            for (projection in type.arguments) {
                builder.addArgument(typeArgument(projection))
            }
        }

        if (type.outerType != null) {
            val outerBuilder = ProtoBuf.Type.newBuilder()
            fillFromPossiblyInnerType(outerBuilder, type.outerType)
            if (useTypeTable()) {
                builder.outerTypeId = typeTable[outerBuilder]
            } else {
                builder.setOuterType(outerBuilder)
            }
        }
    }

    private fun typeArgument(typeProjection: TypeProjection): ProtoBuf.Type.Argument.Builder {
        val builder = ProtoBuf.Type.Argument.newBuilder()


        val projection = projection(typeProjection.projectionKind)

        if (projection != builder.projection) {
            builder.projection = projection
        }

        if (useTypeTable()) {
            builder.typeId = typeId(typeProjection.type)
        } else {
            builder.setType(type(typeProjection.type))
        }


        return builder
    }

    fun packagePartProto(packageFqName: FqName, members: Collection<DeclarationDescriptor>): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        for (declaration in sort(members)) {
            when (declaration) {
//                is PropertyDescriptor -> propertyProto(declaration)?.let { builder.addProperty(it)  }
                is VariableDescriptor -> variableProto(declaration)?.let { builder.addVariable(it) }

                is FunctionDescriptor -> functionProto(declaration)?.let { builder.addFunction(it) }
                is TypeAliasDescriptor -> typeAliasProto(declaration)?.let { builder.addTypeAlias(it) }
            }
        }

        extension.serializePackage(packageFqName, builder)

        typeTable.serialize()?.let { builder.typeTable = it }
        versionRequirementTable?.serialize()?.let { builder.versionRequirementTable = it }

        return builder
    }

    private fun MutableVersionRequirementTable.writeVersionRequirement(languageFeature: LanguageFeature): Int {
        return writeLanguageVersionRequirement(languageFeature, this)
    }

    // Returns a list of indices into versionRequirementTable, or empty list if there's no @RequireCangJie on the descriptor
    private fun MutableVersionRequirementTable.serializeVersionRequirements(descriptor: DeclarationDescriptor): List<Int> =
        descriptor.annotations
            .filter { it.fqName == RequireCangJieConstants.FQ_NAME }
            .mapNotNull(::serializeVersionRequirementFromRequireCangJie)
            .map(::get)

    private fun serializeVersionRequirementFromRequireCangJie(annotation: AnnotationDescriptor): ProtoBuf.VersionRequirement.Builder? {
        val args = annotation.allValueArguments

        val versionString = (args[RequireCangJieConstants.VERSION] as? StringValue)?.value ?: return null
        val matchResult = RequireCangJieConstants.VERSION_REGEX.matchEntire(versionString) ?: return null

        val major = matchResult.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = matchResult.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = matchResult.groupValues.getOrNull(4)?.toIntOrNull() ?: 0

        val proto = ProtoBuf.VersionRequirement.newBuilder()
        VersionRequirement.Version(major, minor, patch).encode(
            writeVersion = { proto.version = it },
            writeVersionFull = { proto.versionFull = it }
        )

        val message = (args[RequireCangJieConstants.MESSAGE] as? StringValue)?.value
        if (message != null) {
            proto.message = stringTable.getStringIndex(message)
        }


        val errorCode = (args[RequireCangJieConstants.ERROR_CODE] as? Int64Value)?.value?.toInt()
        if (errorCode != null && errorCode != -1) {
            proto.errorCode = errorCode
        }

        return proto
    }

    private fun getClassifierId(descriptor: ClassifierDescriptorWithTypeParameters): Int =
        stringTable.getFqNameIndex(descriptor)

    private fun getSimpleNameIndex(name: Name): Int =
        stringTable.getStringIndex(name.asString())

    private fun getTypeParameterId(descriptor: TypeParameterDescriptor): Int =
        typeParameters.intern(descriptor)

    private fun getAccessorFlags(accessor: PropertyAccessorDescriptor): Int = Flags.getAccessorFlags(
        hasAnnotations(accessor),
        ProtoEnumFlags.descriptorVisibility(normalizeVisibility(accessor)),
        ProtoEnumFlags.modality(accessor.modality),
        !accessor.isDefault,

        )

    companion object {
        @JvmStatic
        fun createTopLevel(
            extension: SerializerExtension, languageVersionSettings: LanguageVersionSettings,
            project: Project? = null
        ): DescriptorSerializer =
            DescriptorSerializer(
                null,
                Interner(),
                extension,
                MutableTypeTable(),
                MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                typeAttributeTranslators = project?.let { TypeAttributeTranslatorExtension.createTranslators(it) },
                languageVersionSettings = languageVersionSettings,
                plugins = project?.let { DescriptorSerializerPlugin.getInstances(it) }.orEmpty(),
            )

        @JvmStatic
        fun createForLambda(
            extension: SerializerExtension,
            languageVersionSettings: LanguageVersionSettings
        ): DescriptorSerializer =
            DescriptorSerializer(
                null, Interner(), extension, MutableTypeTable(), versionRequirementTable = null,
                serializeTypeTableToFunction = true, languageVersionSettings = languageVersionSettings,
            )

        @JvmStatic
        fun create(
            descriptor: ClassDescriptor,
            extension: SerializerExtension,
            parentSerializer: DescriptorSerializer?,
            languageVersionSettings: LanguageVersionSettings,
            project: Project? = null
        ): DescriptorSerializer {
            val container = descriptor.containingDeclaration
            val parent = if (container is ClassDescriptor)
                parentSerializer ?: create(container, extension, null, languageVersionSettings, project)
            else
                createTopLevel(extension, languageVersionSettings)
            val plugins = project?.let { DescriptorSerializerPlugin.getInstances(it) }.orEmpty()
            val typeAttributeTranslators = project?.let { TypeAttributeTranslatorExtension.createTranslators(it) }

            // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
            // serializing outer classes before nested classes.
            // Otherwise our interner can get wrong ids because we may serialize classes in any order.
            val serializer = DescriptorSerializer(
                descriptor,
                Interner(parent.typeParameters),
                extension,
                MutableTypeTable(),
                if (container is ClassDescriptor/* && !isVersionRequirementTableWrittenCorrectly(extension.metadataVersion)*/)
                    parent.versionRequirementTable else MutableVersionRequirementTable(),
                serializeTypeTableToFunction = false,
                languageVersionSettings,
                plugins,
                typeAttributeTranslators
            )
            for (typeParameter in descriptor.declaredTypeParameters) {
                serializer.typeParameters.intern(typeParameter)
            }
            return serializer
        }

        private fun variance(variance: Variance): ProtoBuf.TypeParameter.Variance = when (variance) {
            Variance.INVARIANT -> ProtoBuf.TypeParameter.Variance.INV

        }

        private fun projection(projectionKind: Variance): ProtoBuf.Type.Argument.Projection = when (projectionKind) {
            Variance.INVARIANT -> ProtoBuf.Type.Argument.Projection.INV

        }

        private fun hasAnnotations(descriptor: Annotated?): Boolean =
            descriptor != null /*&& descriptor.nonSourceAnnotations.isNotEmpty()*/

        fun <T : DeclarationDescriptor> sort(descriptors: Collection<T>): List<T> =
            ArrayList(descriptors).apply {
                //NOTE: the exact comparator does matter here
                Collections.sort(this, MemberComparator)
            }

        fun writeLanguageVersionRequirement(
            languageFeature: LanguageFeature,
            versionRequirementTable: MutableVersionRequirementTable
        ): Int {
            val languageVersion = languageFeature.sinceVersion!!
            return writeVersionRequirement(
                languageVersion.major, languageVersion.minor, 0,
                ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION,
                versionRequirementTable
            )
        }

        fun writeVersionRequirement(
            major: Int,
            minor: Int,
            patch: Int,
            versionKind: ProtoBuf.VersionRequirement.VersionKind,
            versionRequirementTable: MutableVersionRequirementTable
        ): Int {
            val requirement = ProtoBuf.VersionRequirement.newBuilder().apply {
                VersionRequirement.Version(major, minor, patch).encode(
                    writeVersion = { version = it },
                    writeVersionFull = { versionFull = it }
                )
                if (versionKind != defaultInstanceForType.versionKind) {
                    this.versionKind = versionKind
                }
            }
            return versionRequirementTable[requirement]
        }
    }
}
