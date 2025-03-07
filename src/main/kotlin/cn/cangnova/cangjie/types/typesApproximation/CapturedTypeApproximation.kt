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

package cn.cangnova.cangjie.types.typesApproximation

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.renderer.ClassifierNamePolicy
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.resolve.calls.inference.CapturedTypeConstructor
import cn.cangnova.cangjie.resolve.calls.inference.isCaptured
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.types.*
import cn.cangnova.cangjie.types.checker.CangJieTypeChecker
import cn.cangnova.cangjie.types.util.TypeUtils
import cn.cangnova.cangjie.types.util.builtIns


data class ApproximationBounds<out T>(
    val lower: T,
    val upper: T
)

fun approximateCapturedTypesIfNecessary(
    typeProjection: TypeProjection?,
    approximateContravariant: Boolean
): TypeProjection? {
    if (typeProjection == null) return null

    val type = typeProjection.type
    if (!TypeUtils.contains(type, { it.isCaptured() })) {
        return typeProjection
    }
    val howThisTypeIsUsed = typeProjection.projectionKind


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
//    if (type.isCaptured()) {
//        val typeProjection = (typeConstructor as CapturedTypeConstructor).projection
//        fun CangJieType.makeNullableIfNeeded() = TypeUtils.makeOptionalIfNeeded(this, type.isMarkedOption)
//
//        return when (typeProjection.projectionKind) {
//
//            else -> throw AssertionError("Only nontrivial projections should have been captured, not: $typeProjection")
//        }
//    }
    if (type.arguments.isEmpty() || type.arguments.size != typeConstructor.parameters.size) {
        return ApproximationBounds(type, type)
    }
    val lowerBoundArguments = ArrayList<TypeArgument>()
    val upperBoundArguments = ArrayList<TypeArgument>()
    for ((typeProjection, typeParameter) in type.arguments.zip(typeConstructor.parameters)) {
        val typeArgument = typeProjection.toTypeArgument(typeParameter)

        // Protection from infinite recursion caused by star projection

        val (lower, upper) = approximateProjection(typeArgument)
        lowerBoundArguments.add(lower)
        upperBoundArguments.add(upper)

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
                "[${descriptorRenderer.render(typeParameter)}: <${descriptorRenderer.renderType(inProjection)}, ${
                    descriptorRenderer.renderType(
                        outProjection
                    )
                }>]" +
                " was found"
    }
    fun removeProjectionIfRedundant(variance: Variance) =
        if (variance == typeParameter.variance) Variance.INVARIANT else variance
    return when {
        inProjection == outProjection  -> TypeProjectionImpl(
            inProjection
        )

        CangJieBuiltIns.isNothing(inProjection) && typeParameter.variance != Variance.INVARIANT ->
            TypeProjectionImpl(removeProjectionIfRedundant(Variance.INVARIANT), outProjection)

        CangJieBuiltIns.isAny(outProjection) -> TypeProjectionImpl(
            removeProjectionIfRedundant(Variance.INVARIANT),
            inProjection
        )

        else -> TypeProjectionImpl(removeProjectionIfRedundant(Variance.INVARIANT), outProjection)
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

    }

private fun substituteCapturedTypesWithProjections(typeProjection: TypeProjection): TypeProjection? {
    val typeSubstitutor = TypeSubstitutor.create(object : TypeConstructorSubstitution() {
        override fun get(key: TypeConstructor): TypeProjection? {
            val capturedTypeConstructor = key as? CapturedTypeConstructor ?: return null

            return capturedTypeConstructor.projection
        }
    })
    return typeSubstitutor.substituteWithoutApproximation(typeProjection)
}

