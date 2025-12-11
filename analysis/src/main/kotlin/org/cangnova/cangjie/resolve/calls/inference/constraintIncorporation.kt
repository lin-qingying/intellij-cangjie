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

package org.cangnova.cangjie.resolve.calls.inference

import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariable
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.typesApproximation.approximateCapturedTypes

data class ConstraintContext(
    val position: ConstraintPosition,
    // see TypeBounds.Bound.derivedFrom
    val derivedFrom: Set<TypeVariable>? = null,
    val initial: Boolean = false,
    val initialReduction: Boolean = false
)

fun ConstraintSystemBuilderImpl.incorporateBound(newBound: TypeBounds.Bound) {
    val typeVariable = newBound.typeVariable
    val typeBounds = getTypeBounds(typeVariable)

    // Here and afterwards we're iterating indices of the original bounds list to prevent ConcurrentModificationException
    for (oldBoundIndex in typeBounds.bounds.indices) {
        addConstraintFromBounds(typeBounds.bounds[oldBoundIndex], newBound)
    }
    val boundsUsedIn = usedInBounds[typeVariable] ?: emptyList<TypeBounds.Bound>()
    for (index in boundsUsedIn.indices) {
        val boundUsedIn = boundsUsedIn[index]
        generateNewBound(boundUsedIn, newBound)
    }

    val constrainingType = newBound.constrainingType
    if (isMyTypeVariable(constrainingType)) {
        val context = ConstraintContext(newBound.position, newBound.derivedFrom)
        addBound(getMyTypeVariable(constrainingType)!!, typeVariable.type, newBound.kind.reverse(), context)
        return
    }

    getNestedTypeVariables(constrainingType).forEach {
        val boundsForNestedVariable = getTypeBounds(it).bounds
        for (index in boundsForNestedVariable.indices) {
            generateNewBound(newBound, boundsForNestedVariable[index])
        }
    }
}

private fun ConstraintSystemBuilderImpl.addConstraintFromBounds(old: TypeBounds.Bound, new: TypeBounds.Bound) {
    if (old == new) return

    val oldType = old.constrainingType
    val newType = new.constrainingType
    val context =
        ConstraintContext(CompoundConstraintPosition(old.position, new.position), old.derivedFrom + new.derivedFrom)

    when {
        old.kind.ordinal < new.kind.ordinal -> addConstraint(
            ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE,
            oldType,
            newType,
            context
        )

        old.kind.ordinal > new.kind.ordinal -> addConstraint(
            ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE,
            newType,
            oldType,
            context
        )

        old.kind == new.kind && old.kind == TypeBounds.BoundKind.EXACT_BOUND -> addConstraint(
            ConstraintSystemBuilderImpl.ConstraintKind.EQUAL, oldType, newType, context
        )
    }
}

private fun ConstraintSystemBuilderImpl.generateNewBound(bound: TypeBounds.Bound, substitution: TypeBounds.Bound) {
    if (bound === substitution) return
    // Let's have a bound 'T <=> My<R>', and a substitution 'R <=> Type'.
    // Here <=> means lower_bound, upper_bound or exact_bound constraint.
    // Then a new bound 'T <=> My<_/in/out Type>' can be generated.

    val substitutedType = when (substitution.kind) {
        TypeBounds.BoundKind.EXACT_BOUND -> substitution.constrainingType
        TypeBounds.BoundKind.UPPER_BOUND -> CapturedType(
            TypeProjectionImpl(
                Variance.INVARIANT,
                substitution.constrainingType
            )
        )

        TypeBounds.BoundKind.LOWER_BOUND -> CapturedType(
            TypeProjectionImpl(
                Variance.INVARIANT,
                substitution.constrainingType
            )
        )
    }

    val newTypeProjection = TypeProjectionImpl(substitutedType)
    val substitutor = TypeSubstitutor.create(mapOf(substitution.typeVariable.type.constructor to newTypeProjection))
    val type = substitutor.substitute(bound.constrainingType, Variance.INVARIANT) ?: return

    val position = CompoundConstraintPosition(bound.position, substitution.position)

    fun addNewBound(newConstrainingType: CangJieType, newBoundKind: TypeBounds.BoundKind) {
        // We don't generate new recursive constraints
        if (bound.typeVariable in getNestedTypeVariables(newConstrainingType)) return

        // We don't generate constraint if a type variable was substituted twice
        val derivedFrom = HashSet(bound.derivedFrom + substitution.derivedFrom)
        if (derivedFrom.contains(substitution.typeVariable)) return

        derivedFrom.add(substitution.typeVariable)
        addBound(bound.typeVariable, newConstrainingType, newBoundKind, ConstraintContext(position, derivedFrom))
    }

    if (substitution.kind == TypeBounds.BoundKind.EXACT_BOUND) {
        addNewBound(type, bound.kind)
        return
    }
    val approximationBounds = approximateCapturedTypes(type)

    // todo
    // if we allow non-trivial type projections, we bump into errors like
    // "Empty intersection for types [MutableCollection<in ('Int'..'Int?')>, MutableCollection<out Any?>, MutableCollection<in Int>]"
    fun CangJieType.containsConstrainingTypeWithoutProjection() = this.getNestedArguments().any {
        it.type.constructor == substitution.constrainingType.constructor && it.projectionKind == Variance.INVARIANT
    }
    if (approximationBounds.upper.containsConstrainingTypeWithoutProjection() && bound.kind != TypeBounds.BoundKind.LOWER_BOUND) {
        addNewBound(approximationBounds.upper, TypeBounds.BoundKind.UPPER_BOUND)
    }
    if (approximationBounds.lower.containsConstrainingTypeWithoutProjection() && bound.kind != TypeBounds.BoundKind.UPPER_BOUND) {
        addNewBound(approximationBounds.lower, TypeBounds.BoundKind.LOWER_BOUND)
    }
}
