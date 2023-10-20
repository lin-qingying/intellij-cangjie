package com.huawei.cangjie.types


enum class Variance(
    val label: String,
    val allowsInPosition: Boolean,
    val allowsOutPosition: Boolean,
    private val superpositionFactor: Int
) {
    INVARIANT("", true, true, 0),
    IN_VARIANCE("in", true, false, -1);

    fun allowsPosition(position: Variance): Boolean
            = when (position) {
        IN_VARIANCE -> allowsInPosition

        INVARIANT -> allowsInPosition && allowsOutPosition
    }

    fun superpose(other: Variance): Variance {
        val r = this.superpositionFactor * other.superpositionFactor
        return when (r) {
            0 -> INVARIANT
            -1 -> IN_VARIANCE

            else -> throw IllegalStateException("Illegal factor: $r")
        }
    }

    fun opposite(): Variance {
        return when (this) {
            INVARIANT -> INVARIANT
            IN_VARIANCE -> TODO()
        }
    }

    override fun toString() = label
}
