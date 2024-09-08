package com.huawei.cangjie.resolve.calls.inference.model

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeVariableMarker

interface OnlyInputTypeConstraintPosition
class LowerPriorityToPreserveCompatibility(val needToReportWarning: Boolean) :
    ConstraintSystemError(CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY)

abstract class FixVariableConstraintPosition<T>(val variable: TypeVariableMarker, val resolvedAtom: T) :
    ConstraintPosition() {
    override fun toString(): String = "Fix variable $variable"
}

class ConstrainingTypeIsError(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: IncorporationConstraintPosition
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

class NoSuccessfulFork(val position: IncorporationConstraintPosition) :
    ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

// TODO: should be used only in SimpleConstraintSystemImpl
object SimpleConstraintSystemConstraintPosition : ConstraintPosition()

sealed class ConstraintSystemError(val applicability: CandidateApplicability)
sealed class ConstraintPosition
data class IncorporationConstraintPosition(
    val initialConstraint: InitialConstraint,
    var isFromDeclaredUpperBound: Boolean = false
) : ConstraintPosition() {
    val from: ConstraintPosition get() = initialConstraint.position

    override fun toString(): String = "Incorporate $initialConstraint from position $from"
}

abstract class ArgumentConstraintPosition<out T>(val argument: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Argument $argument"
}

abstract class CallableReferenceConstraintPosition<out T>(val call: T) : ConstraintPosition(),
    OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Callable reference $call"
}

class ArgumentConstraintPositionImpl(argument: CangJieCallArgument) :
    ArgumentConstraintPosition<CangJieCallArgument>(argument)

class CallableReferenceConstraintPositionImpl(val callableReferenceCall: CallableReferenceCangJieCall) :
    CallableReferenceConstraintPosition<CallableReferenceResolutionAtom>(callableReferenceCall)

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

class ExplicitTypeParameterConstraintPositionImpl(
    typeArgument: SimpleTypeArgument
) : ExplicitTypeParameterConstraintPosition<SimpleTypeArgument>(typeArgument)

class DeclaredUpperBoundConstraintPositionImpl(
    typeParameter: TypeParameterDescriptor,
    val cangjieCall: CangJieCall
) : DeclaredUpperBoundConstraintPosition<TypeParameterDescriptor>(typeParameter) {
    override fun toString() = "DeclaredUpperBound ${typeParameter.name} from ${typeParameter.containingDeclaration}"
}

class ReceiverConstraintPositionImpl(
    argument: CangJieCallArgument,
    val selectorCall: CangJieCall?
) : ReceiverConstraintPosition<CangJieCallArgument>(argument)

class NewConstraintWarning(
    override val lowerType: CangJieTypeMarker,
    override val upperType: CangJieTypeMarker,
    override val position: IncorporationConstraintPosition,
) : ConstraintSystemError(CandidateApplicability.RESOLVED), NewConstraintMismatch

fun NewConstraintError.transformToWarning() = NewConstraintWarning(lowerType, upperType, position)
abstract class DeclaredUpperBoundConstraintPosition<T>(val typeParameter: T) : ConstraintPosition() {
    override fun toString(): String = "DeclaredUpperBound $typeParameter"
}

class FixVariableConstraintPositionImpl(
    variable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom?
) : FixVariableConstraintPosition<ResolvedAtom?>(variable, resolvedAtom)

class NotEnoughInformationForTypeParameterImpl(
    typeVariable: TypeVariableMarker,
    resolvedAtom: ResolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : NotEnoughInformationForTypeParameter<ResolvedAtom>(
    typeVariable,
    resolvedAtom,
    couldBeResolvedWithUnrestrictedBuilderInference
)
class OnlyInputTypesDiagnostic(val typeVariable: TypeVariableMarker) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)
class CapturedTypeFromSubtyping(
    val typeVariable: TypeVariableMarker,
    val constraintType: CangJieTypeMarker,
    val position: ConstraintPosition
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

open class NotEnoughInformationForTypeParameter<T>(
    val typeVariable: TypeVariableMarker,
    val resolvedAtom: T,
    val couldBeResolvedWithUnrestrictedBuilderInference: Boolean
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE)

class MultipleMinimalCommonSupertypes(
    val typeVariable: TypeVariableMarker,
    val candidates: List<CangJieType>
) : ConstraintSystemError(CandidateApplicability.INAPPLICABLE) {

    override fun toString(): String {
        return "Multiple minimal common supertypes found for $typeVariable: ${candidates.joinToString(", ")}"
    }
}
class InferredIntoDeclaredUpperBounds(val typeVariable: TypeVariableMarker) : ConstraintSystemError(
    CandidateApplicability.RESOLVED
)

abstract class BuilderInferenceSubstitutionConstraintPosition<L>(
    private val builderInferenceLambda: L,
    val initialConstraint: InitialConstraint,
    val isFromNotSubstitutedDeclaredUpperBound: Boolean = false
) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "Incorporated builder inference constraint $initialConstraint " +
            "into $builderInferenceLambda call"
}
abstract class ExplicitTypeParameterConstraintPosition<T>(val typeArgument: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "TypeParameter $typeArgument"
}
class ExpectedTypeConstraintPositionImpl(topLevelCall: CangJieCall) : ExpectedTypeConstraintPosition<CangJieCall>(topLevelCall)
abstract class ExpectedTypeConstraintPosition<T>(val topLevelCall: T) : ConstraintPosition(), OnlyInputTypeConstraintPosition {
    override fun toString(): String = "ExpectedType for call $topLevelCall"
}
