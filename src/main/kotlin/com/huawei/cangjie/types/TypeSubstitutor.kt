package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.storage.isProcessCanceledException
import com.huawei.cangjie.types.CangJieTypeFactory.flexibleType
import com.huawei.cangjie.types.DisjointKeysUnionTypeSubstitution.Companion.create
import com.huawei.cangjie.types.ErrorUtils.createErrorType
import com.huawei.cangjie.types.TypeConstructorSubstitution.Companion.create
import com.huawei.cangjie.types.TypeConstructorSubstitution.Companion.createByConstructorsMap

import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.model.TypeSubstitutorMarker
import com.huawei.cangjie.types.typesApproximation.approximateCapturedTypesIfNecessary


class TypeSubstitutor(val substitution: TypeSubstitution) : TypeSubstitutorMarker {
    fun substituteWithoutApproximation(typeProjection: TypeProjection): TypeProjection? {
        if (isEmpty) {
            return typeProjection
        }

        return try {
            unsafeSubstitute(typeProjection, null, 0)
        } catch (e: SubstitutionException) {
            null
        }
    }

    fun substitute(typeProjection: TypeProjection): TypeProjection? {
        val substitutedTypeProjection = substituteWithoutApproximation(typeProjection)
        if (!substitution.approximateCapturedTypes() && !substitution.approximateContravariantCapturedTypes()) {
            return substitutedTypeProjection
        }
        return approximateCapturedTypesIfNecessary(
            substitutedTypeProjection, substitution.approximateContravariantCapturedTypes()
        )
    }

    fun substitute(type: CangJieType, howThisTypeIsUsed: Variance): CangJieType? {
        val projection =
            substitute(TypeProjectionImpl(howThisTypeIsUsed, substitution.prepareTopLevelType(type, howThisTypeIsUsed)))
        return projection?.type
    }


    @Throws(SubstitutionException::class)
    private fun unsafeSubstitute(
        originalProjection: TypeProjection,
        typeParameter: TypeParameterDescriptor?,
        recursionDepth: Int
    ): TypeProjection {

        assertRecursionDepth(
            recursionDepth,
            originalProjection,
            substitution
        )


        // The type is within the substitution range, i.e. T or T?
        val type: CangJieType = originalProjection.type
        if (type is TypeWithEnhancement) {
            val origin: CangJieType =
                (type as TypeWithEnhancement).origin
            val enhancement: CangJieType =
                (type as TypeWithEnhancement).enhancement

            val substitution: TypeProjection = unsafeSubstitute(
                TypeProjectionImpl(originalProjection.projectionKind, origin),
                typeParameter,
                recursionDepth + 1
            )

            val substitutedEnhancement: CangJieType? =
                substitute(enhancement, originalProjection.projectionKind)
            val resultingType: CangJieType = substitution.getType().unwrap()
                .wrapEnhancement(
                    substitutedEnhancement
                )

            return TypeProjectionImpl(substitution.getProjectionKind(), resultingType)
        }

        if (type.isDynamic() || type.unwrap() is RawType) {
            return originalProjection // todo investigate
        }

        val substituted: TypeProjection? = substitution[type]
        val replacement: TypeProjection? =
            if (substituted != null) projectedTypeForConflictedTypeWithUnsafeVariance(
                type,
                substituted,
                typeParameter,
                originalProjection
            ) else null

        val originalProjectionKind: Variance = originalProjection.projectionKind
        if (replacement == null && type.isFlexible() && !type.isCustomTypeParameter()) {
            val flexibleType: FlexibleType = type.asFlexibleType()
            val substitutedLower: TypeProjection =
                unsafeSubstitute(
                    TypeProjectionImpl(originalProjectionKind, flexibleType.lowerBound),
                    typeParameter,
                    recursionDepth + 1
                )
            val substitutedUpper: TypeProjection =
                unsafeSubstitute(
                    TypeProjectionImpl(originalProjectionKind, flexibleType.upperBound),
                    typeParameter,
                    recursionDepth + 1
                )

            val substitutedProjectionKind: Variance = substitutedLower.getProjectionKind()
            assert(
                (substitutedProjectionKind == substitutedUpper.getProjectionKind()) &&
                        originalProjectionKind == Variance.INVARIANT || originalProjectionKind == substitutedProjectionKind
            ) { "Unexpected substituted projection kind: $substitutedProjectionKind; original: $originalProjectionKind" }

            if (substitutedLower.getType() === flexibleType.lowerBound && substitutedUpper.getType() === flexibleType.upperBound) return originalProjection

            val substitutedFlexibleType: CangJieType = flexibleType(
                substitutedLower.getType().asSimpleType(), substitutedUpper.getType().asSimpleType()
            )
            return TypeProjectionImpl(substitutedProjectionKind, substitutedFlexibleType)
        }

        if (CangJieBuiltIns.isNothing(type) || type.isError) return originalProjection

        if (replacement != null) {
            val varianceConflict: VarianceConflictType =
                conflictType(
                    originalProjectionKind,
                    replacement.getProjectionKind()
                )


            val customTypeParameter = type.getCustomTypeParameter()
            val substitutedType: CangJieType = customTypeParameter?.substitutionResult(replacement.getType())
                ?: replacement.getType()

            // substitutionType.annotations = replacement.annotations ++ type.annotations
//            if (!type.annotations.isEmpty()) {
//                val typeAnnotations:  Annotations =
//                    TypeSubstitutor.filterOutUnsafeVariance(
//                        substitution.filterAnnotations(type.annotations)
//                    )
//                substitutedType = TypeUtilsKt.replaceAnnotations(
//                    substitutedType,
//                     CompositeAnnotations(
//                        substitutedType.annotations,
//                        typeAnnotations
//                    )
//                )
//            }

            val resultingProjectionKind: Variance =
                if (varianceConflict == VarianceConflictType.NO_CONFLICT
                ) combine(
                    originalProjectionKind,
                    replacement.getProjectionKind()
                )
                else originalProjectionKind
            return TypeProjectionImpl(resultingProjectionKind, substitutedType)
        }

        // The type is not within the substitution range, i.e. Foo, Bar<T> etc.
        return substituteCompoundType(originalProjection, recursionDepth)

    }

    private fun replaceWithNonApproximatingSubstitution(): TypeSubstitutor {
        if (substitution !is IndexedParametersSubstitution || !substitution.approximateContravariantCapturedTypes()) return this

        return TypeSubstitutor(
            IndexedParametersSubstitution(
                substitution.parameters,
                substitution.arguments,
                false
            )
        )
    }

    private fun substituteCompoundType(
        originalProjection: TypeProjection,
        recursionDepth: Int
    ): TypeProjection {
        val type = originalProjection.type
        val projectionKind = originalProjection.projectionKind

        if (type.constructor.declarationDescriptor is TypeParameterDescriptor) {
            // substitution can't change type parameter
            // todo substitute bounds
            return originalProjection
        }

        val substitutedAbbreviation = type.getAbbreviation()?.let { abbreviation ->
            // We shouldn't approximate abbreviation at the top-level as they can't be projected: below we substitute this always as invariant
            val substitutorForAbbreviation = replaceWithNonApproximatingSubstitution()
            substitutorForAbbreviation.substitute(abbreviation, Variance.INVARIANT)
        }

        val substitutedArguments = substituteTypeArguments(
            type.constructor.parameters, type.arguments, recursionDepth
        )

        var substitutedType =
            type.replace(substitutedArguments, substitution.filterAnnotations(type.annotations))
        if (substitutedType is SimpleType && substitutedAbbreviation is SimpleType) {
            substitutedType = substitutedType.withAbbreviation(substitutedAbbreviation)
        }

        return TypeProjectionImpl(projectionKind, substitutedType)
    }

    private fun substituteTypeArguments(
        typeParameters: List<TypeParameterDescriptor>,
        typeArguments: List<TypeProjection>,
        recursionDepth: Int
    ): List<TypeProjection> {
        val substitutedArguments = ArrayList<TypeProjection>(typeParameters.size)

        var wereChanges = false

        for (i in typeParameters.indices) {
            val typeParameter = typeParameters[i]
            val typeArgument = typeArguments[i]

            var substitutedTypeArgument = unsafeSubstitute(typeArgument, typeParameter, recursionDepth + 1)

            when (conflictType(typeParameter.variance, substitutedTypeArgument.projectionKind)) {
                VarianceConflictType.NO_CONFLICT -> {
                    // if the corresponding type parameter is already co/contra-variant, there's no need for an explicit projection
                    if (typeParameter.variance != Variance.INVARIANT) {
                        substitutedTypeArgument = TypeProjectionImpl(Variance.INVARIANT, substitutedTypeArgument.type)
                    }
                }

            }

            if (substitutedTypeArgument != typeArgument) {
                wereChanges = true
            }

            substitutedArguments.add(substitutedTypeArgument)
        }

        return if (!wereChanges) typeArguments else substitutedArguments
    }

    fun safeSubstitute(type: CangJieType, howThisTypeIsUsed: Variance): CangJieType {
        if (isEmpty) {
            return type
        }

        return try {
            unsafeSubstitute(TypeProjectionImpl(howThisTypeIsUsed, type), null, 0).type
        } catch (e: SubstitutionException) {
            createErrorType(
                ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE,
                e.message!!
            )
        }
    }

    val isEmpty: Boolean
        get() = substitution.isEmpty()

    private class SubstitutionException(message: String?) : Exception(message)
    companion object {
        @JvmField
        val EMPTY: TypeSubstitutor = create(TypeSubstitution.EMPTY)

        @JvmStatic

        fun createChainedSubstitutor(first: TypeSubstitution, second: TypeSubstitution): TypeSubstitutor {
            return create(create(first, second))
        }

        @JvmStatic

        fun create(substitutionContext: Map<TypeConstructor, TypeProjection>): TypeSubstitutor {
            return create(createByConstructorsMap(substitutionContext))
        }


        private const val MAX_RECURSION_DEPTH: Int = 100

        private fun combine(typeParameterVariance: Variance, projectionKind: Variance): Variance {
            if (typeParameterVariance == Variance.INVARIANT) return projectionKind
            if (projectionKind == Variance.INVARIANT) return typeParameterVariance
            if (typeParameterVariance == projectionKind) return projectionKind
            throw AssertionError(
                "Variance conflict: type parameter variance '" + typeParameterVariance + "' and " +
                        "projection kind '" + projectionKind + "' cannot be combined"
            )
        }

        @JvmStatic

        fun combine(typeParameterVariance: Variance, typeProjection: TypeProjection): Variance {


            return combine(typeParameterVariance, typeProjection.projectionKind)
        }

        @JvmStatic

        fun create(context: CangJieType): TypeSubstitutor {
            return create(create(context.constructor, context.arguments))
        }

        @JvmStatic
        private fun safeToString(o: Any): String {
            try {
                return o.toString()
            } catch (e: Throwable) {
                if (e.isProcessCanceledException()) {
                    throw (e as RuntimeException)
                }
                return "[Exception while computing toString(): $e]"
            }
        }

        private fun assertRecursionDepth(
            recursionDepth: Int,
            projection: TypeProjection,
            substitution: TypeSubstitution
        ) {
            check(recursionDepth <= MAX_RECURSION_DEPTH) {
                "Recursion too deep. Most likely infinite loop while substituting " + safeToString(
                    projection
                ) + "; substitution: " + safeToString(substitution)
            }
        }

        private fun projectedTypeForConflictedTypeWithUnsafeVariance(
            originalType: CangJieType,
            substituted: TypeProjection,
            typeParameter: TypeParameterDescriptor?,
            originalProjection: TypeProjection
        ): TypeProjection {


            if (typeParameter == null) return substituted


            return substituted
        }

        private fun conflictType(
            position: Variance,
            argument: Variance
        ): VarianceConflictType {

            return VarianceConflictType.NO_CONFLICT
        }

        @JvmStatic
        fun create(substitution: TypeSubstitution): TypeSubstitutor {
            return TypeSubstitutor(substitution)
        }
    }

    private enum class VarianceConflictType {
        NO_CONFLICT,

    }
}
