package com.huawei.cangjie.resolve.calls.inference.model

import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeVariableMarker

interface OnlyInputTypeConstraintPosition
class LowerPriorityToPreserveCompatibility(val needToReportWarning: Boolean) :
    ConstraintSystemError(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)

abstract class FixVariableConstraintPosition<T>(val variable: TypeVariableMarker, val resolvedAtom: T) :
    ConstraintPosition() {
    override fun toString(): String = "Fix variable $variable"
}

class NoSuccessfulFork(val position: IncorporationConstraintPosition) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

sealed class ConstraintSystemError(val applicability: CandidateApplicability)
sealed class ConstraintPosition
data class IncorporationConstraintPosition(
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    val from: ConstraintPosition get() = initialConstraint.position

    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}

abstract class ReceiverConstraintPosition<T>(val argument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Receiver $argument"
}

sealed interface NewConstraintMismatch {
    val lowerType: CangJieTypeMarker
    val upperType: CangJieTypeMarker
    val position: IncorporationConstraintPosition
}

class NewConstraintError(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(if (position.from is ReceiverConstraintPosition<*>) CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER else CandidateApplicability.INAPPLICABLE),
    NewConstraintMismatch {
    override fun toString(): String {
        return "$lowerType <: $upperType"
    }
}

class NewConstraintWarning(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(CandidateApplicability.RESOLVED), NewConstraintMismatch

fun NewConstraintError.transformToWarning() = NewConstraintWarning(lowerType, upperType, position)
