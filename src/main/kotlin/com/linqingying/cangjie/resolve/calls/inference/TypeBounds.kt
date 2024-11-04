package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariable
import com.linqingying.cangjie.types.CangJieType

interface TypeBounds
{


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
    TypeBounds.BoundKind.UPPER_BOUND ->TypeBounds.BoundKind. LOWER_BOUND
    TypeBounds.BoundKind. EXACT_BOUND -> TypeBounds.BoundKind.EXACT_BOUND
}
