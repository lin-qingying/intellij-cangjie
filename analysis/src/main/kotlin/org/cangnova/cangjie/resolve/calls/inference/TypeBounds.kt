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

import org.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariable
import org.cangnova.cangjie.types.CangJieType

interface TypeBounds {


    val typeVariable: TypeVariable

    val bounds: Collection<Bound>

    val value: CangJieType?
        get() = if (values.size == 1) values.first() else null

    val values: Collection<CangJieType>

    enum class BoundKind {
        LOWER_BOUND,
        EXACT_BOUND,
        UPPER_BOUND
    }

    class Bound(
        val typeVariable: TypeVariable,
        val constrainingType: CangJieType,
        val kind: BoundKind,
        val position: ConstraintPosition,
        val isProper: Boolean,
        // to prevent infinite recursion in incorporation we store the variables that was substituted to derive this bound
        val derivedFrom: Set<TypeVariable>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class.java != other::class.java) return false

            val bound = other as Bound

            if (typeVariable != bound.typeVariable) return false
            if (constrainingType != bound.constrainingType) return false
            if (kind != bound.kind) return false

            if (position.isStrong() != bound.position.isStrong()) return false

            return true
        }

        override fun hashCode(): Int {
            var result = typeVariable.hashCode()
            result = 31 * result + constrainingType.hashCode()
            result = 31 * result + kind.hashCode()
            result = 31 * result + if (position.isStrong()) 1 else 0
            return result
        }

        override fun toString() = "Bound($constrainingType, $kind, $position, isProper = $isProper)"
    }
}

fun TypeBounds.BoundKind.reverse() = when (this) {
    TypeBounds.BoundKind.LOWER_BOUND -> TypeBounds.BoundKind.UPPER_BOUND
    TypeBounds.BoundKind.UPPER_BOUND -> TypeBounds.BoundKind.LOWER_BOUND
    TypeBounds.BoundKind.EXACT_BOUND -> TypeBounds.BoundKind.EXACT_BOUND
}
