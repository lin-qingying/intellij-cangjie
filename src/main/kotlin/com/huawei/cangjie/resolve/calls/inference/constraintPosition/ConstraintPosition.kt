package com.huawei.cangjie.resolve.calls.inference.constraintPosition

interface ConstraintPosition {
    val kind: ConstraintPositionKind

    fun isStrong(): Boolean = kind != ConstraintPositionKind.TYPE_BOUND_POSITION

    fun isParameter(): Boolean =
        kind in setOf(ConstraintPositionKind.VALUE_PARAMETER_POSITION, ConstraintPositionKind.RECEIVER_POSITION)
}

private data class ConstraintPositionImpl(override val kind: ConstraintPositionKind) : ConstraintPosition {
    override fun toString() = "$kind"
}

fun ConstraintPosition.derivedFrom(kind: ConstraintPositionKind): Boolean {
    return if (this !is CompoundConstraintPosition) this.kind == kind else positions.any { it.kind == kind }
}

class CompoundConstraintPosition(vararg positions: ConstraintPosition) : ConstraintPosition {

    override val kind: ConstraintPositionKind
        get() = ConstraintPositionKind.COMPOUND_CONSTRAINT_POSITION

    val positions: Collection<ConstraintPosition> =
        positions.flatMap { (it as? CompoundConstraintPosition)?.positions ?: listOf(it) }.toSet()

    override fun isStrong() = positions.any { it.isStrong() }

    override fun toString() = "$kind(${positions.joinToString()})"
}

enum class ConstraintPositionKind {
    RECEIVER_POSITION,
    EXPECTED_TYPE_POSITION,
    VALUE_PARAMETER_POSITION,
    TYPE_BOUND_POSITION,
    COMPOUND_CONSTRAINT_POSITION,
    FROM_COMPLETER,
    SPECIAL;

    fun position(): ConstraintPosition {
        assert(this in setOf(RECEIVER_POSITION, EXPECTED_TYPE_POSITION, FROM_COMPLETER, SPECIAL))
        return ConstraintPositionImpl(this)
    }

    fun position(index: Int): ConstraintPosition {
        assert(this in setOf(VALUE_PARAMETER_POSITION, TYPE_BOUND_POSITION))
        return ConstraintPositionWithIndex(this, index)
    }
}

private data class ConstraintPositionWithIndex(override val kind: ConstraintPositionKind, val index: Int) :
    ConstraintPosition {
    override fun toString() = "$kind($index)"
}
