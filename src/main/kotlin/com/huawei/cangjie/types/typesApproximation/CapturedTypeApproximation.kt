package com.huawei.cangjie.types.typesApproximation

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.renderer.ClassifierNamePolicy
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.resolve.builtIns
import com.huawei.cangjie.resolve.calls.inference.CapturedTypeConstructor
import com.huawei.cangjie.resolve.calls.inference.isCaptured

import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeChecker


data class ApproximationBounds<out T>(
    val lower: T,
    val upper: T
)

fun approximateCapturedTypesIfNecessary(
    typeProjection: TypeProjection?,
    approximateContravariant: Boolean
): TypeProjection? {
    if (typeProjection == null) return null
    if (typeProjection.isStarProjection) return typeProjection

    val type = typeProjection.type
    if (!TypeUtils.contains(type, { it.isCaptured() })) {
        return typeProjection
    }
    val howThisTypeIsUsed = typeProjection.projectionKind
    if (howThisTypeIsUsed == Variance.OUT_VARIANCE) {
        // only 'return' type containing captured types should be over-approximated
        val approximation = approximateCapturedTypes(type)
        return TypeProjectionImpl(howThisTypeIsUsed, approximation.upper)
    }

    if (approximateContravariant) {
        // TODO: assert that howThisTypeIsUsed is always IN
        val approximation = approximateCapturedTypes(type).lower
        return TypeProjectionImpl(howThisTypeIsUsed, approximation)
    }

    return substituteCapturedTypesWithProjections(typeProjection)
}

// todo: dynamic & raw type?
fun approximateCapturedTypes(type: CangJieType): ApproximationBounds<CangJieType> {
    if (type.isFlexible()) {
        val boundsForFlexibleLower = approximateCapturedTypes(type.lowerIfFlexible())
        val boundsForFlexibleUpper = approximateCapturedTypes(type.upperIfFlexible())

        return ApproximationBounds(
            CangJieTypeFactory.flexibleType(
                boundsForFlexibleLower.lower.lowerIfFlexible(),
                boundsForFlexibleUpper.lower.upperIfFlexible()
            ).inheritEnhancement(type),
            CangJieTypeFactory.flexibleType(
                boundsForFlexibleLower.upper.lowerIfFlexible(),
                boundsForFlexibleUpper.upper.upperIfFlexible()
            ).inheritEnhancement(type)
        )
    }

    val typeConstructor = type.constructor
    if (type.isCaptured()) {
        val typeProjection = (typeConstructor as CapturedTypeConstructor).projection
        fun CangJieType.makeNullableIfNeeded() = TypeUtils.makeNullableIfNeeded(this, type.isMarkedNullable)
        val bound = typeProjection.type.makeNullableIfNeeded()

        return when (typeProjection.projectionKind) {
            Variance.IN_VARIANCE -> ApproximationBounds(bound, type.builtIns.nullableAnyType)
            Variance.OUT_VARIANCE -> ApproximationBounds(type.builtIns.nothingType.makeNullableIfNeeded(), bound)
            else -> throw AssertionError("Only nontrivial projections should have been captured, not: $typeProjection")
        }
    }
    if (type.arguments.isEmpty() || type.arguments.size != typeConstructor.parameters.size) {
        return ApproximationBounds(type, type)
    }
    val lowerBoundArguments = ArrayList<TypeArgument>()
    val upperBoundArguments = ArrayList<TypeArgument>()
    for ((typeProjection, typeParameter) in type.arguments.zip(typeConstructor.parameters)) {
        val typeArgument = typeProjection.toTypeArgument(typeParameter)

        // Protection from infinite recursion caused by star projection
        if (typeProjection.isStarProjection) {
            lowerBoundArguments.add(typeArgument)
            upperBoundArguments.add(typeArgument)
        } else {
            val (lower, upper) = approximateProjection(typeArgument)
            lowerBoundArguments.add(lower)
            upperBoundArguments.add(upper)
        }
    }
    val lowerBoundIsTrivial = lowerBoundArguments.any { !it.isConsistent }
    return ApproximationBounds(
        if (lowerBoundIsTrivial) type.builtIns.nothingType else type.replaceTypeArguments(lowerBoundArguments),
        type.replaceTypeArguments(upperBoundArguments)
    )
}
private fun TypeArgument.toTypeProjection(): TypeProjection {
    assert(isConsistent) {
        val descriptorRenderer = DescriptorRenderer.withOptions {
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
        }
        "Only consistent enhanced type projection can be converted to type projection, but " +
                "[${descriptorRenderer.render(typeParameter)}: <${descriptorRenderer.renderType(inProjection)}, ${descriptorRenderer.renderType(
                    outProjection
                )}>]" +
                " was found"
    }
    fun removeProjectionIfRedundant(variance: Variance) = if (variance == typeParameter.variance) Variance.INVARIANT else variance
    return when {
        inProjection == outProjection || typeParameter.variance == Variance.IN_VARIANCE -> TypeProjectionImpl(inProjection)
        CangJieBuiltIns.isNothing(inProjection) && typeParameter.variance != Variance.IN_VARIANCE ->
            TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
        CangJieBuiltIns.isNullableAny(outProjection) -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.IN_VARIANCE), inProjection)
        else -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.OUT_VARIANCE), outProjection)
    }
}
private fun CangJieType.replaceTypeArguments(newTypeArguments: List<TypeArgument>): CangJieType {
    assert(arguments.size == newTypeArguments.size) { "Incorrect type arguments $newTypeArguments" }
    return replace(newTypeArguments.map { it.toTypeProjection() })
}

private fun approximateProjection(typeArgument: TypeArgument): ApproximationBounds<TypeArgument> {
    val (inLower, inUpper) = approximateCapturedTypes(typeArgument.inProjection)
    val (outLower, outUpper) = approximateCapturedTypes(typeArgument.outProjection)
    return ApproximationBounds(
        lower = TypeArgument(typeArgument.typeParameter, inUpper, outLower),
        upper = TypeArgument(typeArgument.typeParameter, inLower, outUpper)
    )
}

private class TypeArgument(
    val typeParameter: TypeParameterDescriptor,
    val inProjection: CangJieType,
    val outProjection: CangJieType
) {
    val isConsistent: Boolean
        get() = CangJieTypeChecker.DEFAULT.isSubtypeOf(inProjection, outProjection)
}

private fun TypeProjection.toTypeArgument(typeParameter: TypeParameterDescriptor) =
    when (TypeSubstitutor.combine(typeParameter.variance, this)) {
        Variance.INVARIANT -> TypeArgument(typeParameter, type, type)
        Variance.IN_VARIANCE -> TypeArgument(typeParameter, type, typeParameter.builtIns.nullableAnyType)
        Variance.OUT_VARIANCE -> TypeArgument(typeParameter, typeParameter.builtIns.nothingType, type)
    }

private fun substituteCapturedTypesWithProjections(typeProjection: TypeProjection): TypeProjection? {
    val typeSubstitutor = TypeSubstitutor.create(object : TypeConstructorSubstitution() {
        override fun get(key: TypeConstructor): TypeProjection? {
            val capturedTypeConstructor = key as? CapturedTypeConstructor ?: return null
            if (capturedTypeConstructor.projection.isStarProjection) {
                return TypeProjectionImpl(Variance.OUT_VARIANCE, capturedTypeConstructor.projection.type)
            }
            return capturedTypeConstructor.projection
        }
    })
    return typeSubstitutor.substituteWithoutApproximation(typeProjection)
}

