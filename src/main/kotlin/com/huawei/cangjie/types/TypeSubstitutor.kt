package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.isCaptured
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
        return originalProjection
//         TypeSubstitutor.assertRecursionDepth(
//            recursionDepth,
//            originalProjection,
//            substitution
//        )
//
//        if (originalProjection.isStarProjection) return originalProjection
//
//
//        // The type is within the substitution range, i.e. T or T?
//        val type:  CangJieType = originalProjection.type
//        if (type is  TypeWithEnhancement) {
//            val origin:  CangJieType =
//                (type as  TypeWithEnhancement).origin
//            val enhancement:  CangJieType =
//                (type as  TypeWithEnhancement).enhancement
//
//            val substitution:  TypeProjection = unsafeSubstitute(
//                 TypeProjectionImpl(originalProjection.projectionKind, origin),
//                typeParameter,
//                recursionDepth + 1
//            )
//            if (substitution.isStarProjection()) return substitution
//
//            val substitutedEnhancement:  CangJieType? =
//                substitute(enhancement, originalProjection.projectionKind)
//            val resultingType:  CangJieType = substitution.getType().unwrap()
//                .wrapEnhancement(
//                    substitutedEnhancement
//                )
//
//            return  TypeProjectionImpl(substitution.getProjectionKind(), resultingType)
//        }
//
//        if (type.isDynamic() || type.unwrap() is  RawType) {
//            return originalProjection // todo investigate
//        }
//
//        val substituted:  TypeProjection? = substitution[type]
//        val replacement:  TypeProjection? =
//            if (substituted != null)  TypeSubstitutor.projectedTypeForConflictedTypeWithUnsafeVariance(
//                type,
//                substituted,
//                typeParameter,
//                originalProjection
//            ) else null
//
//        val originalProjectionKind:  Variance = originalProjection.projectionKind
//        if (replacement == null && type.isFlexible() && !type.isCustomTypeParameter()) {
//            val flexibleType:  FlexibleType = type.asFlexibleType()
//            val substitutedLower:  TypeProjection =
//                unsafeSubstitute(
//                     TypeProjectionImpl(originalProjectionKind, flexibleType.lowerBound),
//                    typeParameter,
//                    recursionDepth + 1
//                )
//            val substitutedUpper:  TypeProjection =
//                unsafeSubstitute(
//                     TypeProjectionImpl(originalProjectionKind, flexibleType.upperBound),
//                    typeParameter,
//                    recursionDepth + 1
//                )
//
//            val substitutedProjectionKind:  Variance = substitutedLower.getProjectionKind()
//            assert(
//                (substitutedProjectionKind == substitutedUpper.getProjectionKind()) &&
//                        originalProjectionKind ==  Variance.INVARIANT || originalProjectionKind == substitutedProjectionKind
//            ) { "Unexpected substituted projection kind: $substitutedProjectionKind; original: $originalProjectionKind" }
//
//            if (substitutedLower.getType() === flexibleType.lowerBound && substitutedUpper.getType() === flexibleType.upperBound) return originalProjection
//
//            val substitutedFlexibleType:  CangJieType = flexibleType(
//                substitutedLower.getType().asSimpleType(), substitutedUpper.getType().asSimpleType()
//            )
//            return  TypeProjectionImpl(substitutedProjectionKind, substitutedFlexibleType)
//        }
//
//        if ( CangJieBuiltIns.isNothing(type) || type.isError) return originalProjection
//
//        if (replacement != null) {
//            val varianceConflict:  TypeSubstitutor.VarianceConflictType =
//                 TypeSubstitutor.conflictType(
//                    originalProjectionKind,
//                    replacement.getProjectionKind()
//                )
//
//            // Captured type might be substituted in an opposite projection:
//            // out 'Captured (in Int)' = out Int
//            // in 'Captured (out Int)' = in Int
//            val allowVarianceConflict: Boolean = type.isCaptured()
//            if (!allowVarianceConflict) {
//                when (varianceConflict) {
//                     TypeSubstitutor.VarianceConflictType.OUT_IN_IN_POSITION -> throw  TypeSubstitutor.SubstitutionException(
//                        "Out-projection in in-position"
//                    )
//
//                     TypeSubstitutor.VarianceConflictType.IN_IN_OUT_POSITION ->                         // todo use the right type parameter variance and upper bound
//                        return  TypeProjectionImpl(
//                             Variance.OUT_VARIANCE,
//                            type.constructor.getBuiltIns().anyType
//                        )
//                }
//            }
//            var substitutedType:  CangJieType
//            val customTypeParameter:  CustomTypeParameter = type.getCustomTypeParameter()
//            substitutedType = if (replacement.isStarProjection()) {
//                return replacement
//            } else if (customTypeParameter != null) {
//                customTypeParameter.substitutionResult(replacement.getType())
//            } else {
//                // this is a simple type T or T?: if it's T, we should just take replacement, if T? - we make replacement nullable
//                 TypeUtils.makeNullableIfNeeded(replacement.getType(), type.isMarkedNullable)
//            }
//
//            // substitutionType.annotations = replacement.annotations ++ type.annotations
//            if (!type.annotations.isEmpty()) {
//                val typeAnnotations: org.jetbrains.kotlin.descriptors.annotations.Annotations =
//                     TypeSubstitutor.filterOutUnsafeVariance(
//                        substitution.filterAnnotations(type.annotations)
//                    )
//                substitutedType = TypeUtilsKt.replaceAnnotations(
//                    substitutedType,
//                    org.jetbrains.kotlin.descriptors.annotations.CompositeAnnotations(
//                        substitutedType.annotations,
//                        typeAnnotations
//                    )
//                )
//            }
//
//            val resultingProjectionKind:  Variance =
//                if (varianceConflict ==  TypeSubstitutor.VarianceConflictType.NO_CONFLICT
//                )  TypeSubstitutor.combine(
//                    originalProjectionKind,
//                    replacement.getProjectionKind()
//                )
//                else originalProjectionKind
//            return  TypeProjectionImpl(resultingProjectionKind, substitutedType)
//        }
//
//        // The type is not within the substitution range, i.e. Foo, Bar<T> etc.
//        return substituteCompoundType(originalProjection, recursionDepth)

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
        @JvmStatic

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
            if (typeProjection.isStarProjection) return Variance.OUT_VARIANCE

            return combine(typeParameterVariance, typeProjection.projectionKind)
        }
        @JvmStatic

        fun create(context: CangJieType): TypeSubstitutor {
            return create(create(context.constructor, context.arguments))
        }
        @JvmStatic

        fun create(substitution: TypeSubstitution): TypeSubstitutor {
            return TypeSubstitutor(substitution)
        }
    }
}
