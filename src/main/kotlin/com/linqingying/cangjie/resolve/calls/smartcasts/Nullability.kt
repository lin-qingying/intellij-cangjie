package com.linqingying.cangjie.resolve.calls.smartcasts

enum class Nullability(private val canBeNull: Boolean, private val canBeNonNull: Boolean) {
    NULL(true, false),
    NOT_NULL(false, true),
    UNKNOWN(true, true),
    IMPOSSIBLE(false, false);

    fun canBeNull(): Boolean {
        return canBeNull
    }

    fun canBeNonNull(): Boolean {
        return canBeNonNull
    }

    fun refine(other: Nullability): Nullability {
        return when (this) {
            UNKNOWN -> other
            IMPOSSIBLE -> other
            NULL -> when (other) {
                NOT_NULL -> NOT_NULL
                else -> NULL
            }

            NOT_NULL -> when (other) {
                NULL -> NOT_NULL
                else -> NOT_NULL
            }
        }

    }

    fun invert(): Nullability {
        return when (this) {
            NULL -> NOT_NULL
            NOT_NULL -> UNKNOWN
            UNKNOWN -> UNKNOWN
            IMPOSSIBLE -> UNKNOWN
        }

    }

    fun and(other: Nullability): Nullability {
        return fromFlags(this.canBeNull && other.canBeNull, this.canBeNonNull && other.canBeNonNull)
    }

    fun or(other: Nullability): Nullability {
        return fromFlags(this.canBeNull || other.canBeNull, this.canBeNonNull || other.canBeNonNull)
    }

    companion object {
        fun fromFlags(canBeNull: Boolean, canBeNonNull: Boolean): Nullability {
            if (!canBeNull && !canBeNonNull) return IMPOSSIBLE
            if (!canBeNull && canBeNonNull) return NOT_NULL
            if (canBeNull && !canBeNonNull) return NULL
            return UNKNOWN
        }
    }
}
