package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.TypeAliasDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.util.TypeUtils


class TypeAliasExpander(
    private val reportStrategy: TypeAliasExpansionReportStrategy,
    private val shouldCheckBounds: Boolean
){

    fun expandWithoutAbbreviation(typeAliasExpansion: TypeAliasExpansion, attributes: TypeAttributes) =
        expandRecursively(
            typeAliasExpansion, attributes,
            isNullable = false, recursionDepth = 0, withAbbreviatedType = false
        )

    private fun expandNonArgumentTypeProjection(
        originalProjection: TypeProjection,
        typeAliasExpansion: TypeAliasExpansion,
        recursionDepth: Int
    ): TypeProjection {
        val originalType = originalProjection.type.unwrap()

        if (originalType.isDynamic()) return originalProjection

        val type = originalType.asSimpleType()

//        if (type.isError || !type.requiresTypeAliasExpansion()) {
//            return originalProjection
//        }

        val typeConstructor = type.constructor
        val typeDescriptor = typeConstructor.declarationDescriptor

        assert(typeConstructor.parameters.size == type.arguments.size) { "Unexpected malformed type: $type" }

        return when (typeDescriptor) {
            is TypeParameterDescriptor -> {
                originalProjection
            }
            is TypeAliasDescriptor -> {
                if (typeAliasExpansion.isRecursion(typeDescriptor)) {
                    reportStrategy.recursiveTypeAlias(typeDescriptor)
                    return TypeProjectionImpl(
                        Variance.INVARIANT,
                        ErrorUtils.createErrorType(
                            ErrorTypeKind.RECURSIVE_TYPE_ALIAS, typeDescriptor.name.toString())
                    )
                }

                val expandedArguments = type.arguments.mapIndexed { i, typeAliasArgument ->
                    expandTypeProjection(typeAliasArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1)
                }

                val nestedExpansion =
                    TypeAliasExpansion.create(typeAliasExpansion, typeDescriptor, expandedArguments)

                val nestedExpandedType = expandRecursively(
                    nestedExpansion, type.attributes,
                    isNullable = type.isMarkedOption,
                    recursionDepth = recursionDepth + 1,
                    withAbbreviatedType = false
                )

                val substitutedType = type.substituteArguments(typeAliasExpansion, recursionDepth)

                // 'dynamic' type can't be abbreviated - will be reported separately
                val typeWithAbbreviation =
                    if (nestedExpandedType.isDynamic()) nestedExpandedType else nestedExpandedType.withAbbreviation(substitutedType)

                TypeProjectionImpl(originalProjection.projectionKind, typeWithAbbreviation)
            }
            else -> {
                val substitutedType = type.substituteArguments(typeAliasExpansion, recursionDepth)

//                checkTypeArgumentsSubstitution(type, substitutedType)

                TypeProjectionImpl(originalProjection.projectionKind, substitutedType)
            }
        }
    }
    private fun SimpleType.substituteArguments(typeAliasExpansion: TypeAliasExpansion, recursionDepth: Int): SimpleType {
        val typeConstructor = this.constructor

        val substitutedArguments = this.arguments.mapIndexed { i, originalArgument ->
            val projection = expandTypeProjection(
                originalArgument, typeAliasExpansion, typeConstructor.parameters[i], recursionDepth + 1
            )

              TypeProjectionImpl(
                projection.projectionKind,
                TypeUtils.makeOptionalIfNeeded(projection.type, originalArgument.type.isMarkedOption)
            )
        }

        return this.replace(newArguments = substitutedArguments)
    }
    private fun expandTypeProjection(
        underlyingProjection: TypeProjection,
        typeAliasExpansion: TypeAliasExpansion,
        typeParameterDescriptor: TypeParameterDescriptor?,
        recursionDepth: Int
    ): TypeProjection {
        // TODO refactor TypeSubstitutor to introduce custom diagnostics
//        assertRecursionDepth(recursionDepth, typeAliasExpansion.descriptor)

//        if (underlyingProjection.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val underlyingType = underlyingProjection.type
        val argument = typeAliasExpansion.getReplacement(underlyingType.constructor)
            ?: return expandNonArgumentTypeProjection(
                underlyingProjection,
                typeAliasExpansion,
                recursionDepth
            )

//        if (argument.isStarProjection) return TypeUtils.makeStarProjection(typeParameterDescriptor!!)

        val argumentType = argument.type.unwrap()

        val resultingVariance = run {
            val argumentVariance = argument.projectionKind
            val underlyingVariance = underlyingProjection.projectionKind

            val substitutionVariance =
                when {
                    underlyingVariance == argumentVariance -> argumentVariance
                    underlyingVariance == Variance.INVARIANT -> argumentVariance
                    argumentVariance == Variance.INVARIANT -> underlyingVariance
                    else -> {
                        reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                        argumentVariance
                    }
                }

            val parameterVariance = typeParameterDescriptor?.variance ?: Variance.INVARIANT

            when {
                parameterVariance == substitutionVariance -> substitutionVariance
                parameterVariance == Variance.INVARIANT -> substitutionVariance
                substitutionVariance == Variance.INVARIANT -> Variance.INVARIANT
                else -> {
                    reportStrategy.conflictingProjection(typeAliasExpansion.descriptor, typeParameterDescriptor, argumentType)
                    substitutionVariance
                }
            }
        }

//        checkRepeatedAnnotations(underlyingType.annotations, argumentType.annotations)

        val substitutedType =
            if (argumentType is DynamicType)
                argumentType.combineAttributes(underlyingType.attributes)
            else
                argumentType.asSimpleType().combineNullabilityAndAnnotations(underlyingType)

        return TypeProjectionImpl(resultingVariance, substitutedType)
    }
    private fun SimpleType.combineNullability(fromType: CangJieType) =
        TypeUtils.makeOptionalIfNeeded(this, fromType.isMarkedOption)

    private fun SimpleType.combineNullabilityAndAnnotations(fromType: CangJieType) =
        combineNullability(fromType).combineAttributes(fromType.attributes)

    private fun CangJieType.createdCombinedAttributes(newAttributes: TypeAttributes): TypeAttributes {
        if (isError) return attributes

        return newAttributes.add(attributes)
    }
    private fun DynamicType.combineAttributes(newAttributes: TypeAttributes): DynamicType =
        replaceAttributes(createdCombinedAttributes(newAttributes))

    private fun SimpleType.combineAttributes(newAttributes: TypeAttributes): SimpleType =
        if (isError) this else replace(newAttributes = createdCombinedAttributes(newAttributes))

    private fun expandRecursively(
        typeAliasExpansion: TypeAliasExpansion,
        attributes: TypeAttributes,
        isNullable: Boolean,
        recursionDepth: Int,
        withAbbreviatedType: Boolean
    ): SimpleType {
        val underlyingProjection = TypeProjectionImpl(
            Variance.INVARIANT,
            typeAliasExpansion.descriptor.underlyingType
        )
        val expandedProjection = expandTypeProjection(underlyingProjection, typeAliasExpansion, null, recursionDepth)
        val expandedType = expandedProjection.type.asSimpleType()

        if (expandedType.isError) return expandedType

        assert(expandedProjection.projectionKind == Variance.INVARIANT) {
            "Type alias expansion: result for ${typeAliasExpansion.descriptor} is ${expandedProjection.projectionKind}, should be invariant"
        }

//        checkRepeatedAnnotations(expandedType.annotations, attributes.annotations)
        val expandedTypeWithExtraAnnotations =
            expandedType.combineAttributes(attributes).let { TypeUtils.makeOptionalIfNeeded(it, isNullable) }

        return if (withAbbreviatedType)
            expandedTypeWithExtraAnnotations.withAbbreviation(typeAliasExpansion.createAbbreviation(attributes, isNullable))
        else
            expandedTypeWithExtraAnnotations
    }

    private fun TypeAliasExpansion.createAbbreviation(attributes: TypeAttributes, isNullable: Boolean) =
       CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            attributes,
            descriptor.typeConstructor,
            arguments,
            isNullable,
            MemberScope.Empty
        )

    fun expand(typeAliasExpansion: TypeAliasExpansion, attributes: TypeAttributes) =
        expandRecursively(
            typeAliasExpansion, attributes,
            isNullable = false, recursionDepth = 0, withAbbreviatedType = true
        )

    companion object{
        val NON_REPORTING =
            TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING, false)
    }
}

