package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintError
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintWarning
import com.huawei.cangjie.resolve.calls.inference.model.transformToWarning
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.types.UnwrappedType


interface TransformableToWarning<T : CangJieCallDiagnostic> {
    fun transformToWarning(): T?
}
class EnumEntryAmbiguityWarning(val property: PropertyDescriptor, val enumEntry: ClassDescriptor) :CangJieCallDiagnostic(
    CandidateApplicability.RESOLVED
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}
class CangJieConstraintSystemDiagnostic(
    val error: ConstraintSystemError
) : CangJieCallDiagnostic(error.applicability), TransformableToWarning<CangJieConstraintSystemDiagnostic> {
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(error)

    override fun transformToWarning(): CangJieConstraintSystemDiagnostic? =
        if (error is NewConstraintError) CangJieConstraintSystemDiagnostic(error.transformToWarning()) else null
}

val CangJieCallDiagnostic.constraintSystemError: ConstraintSystemError?
    get() = (this as? CangJieConstraintSystemDiagnostic)?.error

fun ConstraintSystemError.asDiagnostic(): CangJieConstraintSystemDiagnostic = CangJieConstraintSystemDiagnostic(this)
fun Collection<ConstraintSystemError>.asDiagnostics(): List<CangJieConstraintSystemDiagnostic> =
    map(ConstraintSystemError::asDiagnostic)

// SmartCasts
class SmartCastDiagnostic(
    val argument: ExpressionCangJieCallArgument,
    val smartCastType: UnwrappedType,
    val kotlinCall: CangJieCall?
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}


class UnsafeCallError(
    val receiver: SimpleCangJieCallArgument,
    val isForImplicitInvoke: Boolean = false
) : CangJieCallDiagnostic(CandidateApplicability.UNSAFE_CALL) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

class UnstableSmartCastResolutionError(
    argument: ExpressionCangJieCallArgument,
    targetType: UnwrappedType,
) : UnstableSmartCast(argument, targetType, CandidateApplicability.UNSTABLE_SMARTCAST)


sealed class UnstableSmartCast(
    val argument: ExpressionCangJieCallArgument,
    val targetType: UnwrappedType,
    applicability: CandidateApplicability,
) : CangJieCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)

    companion object {
        operator fun invoke(
            argument: ExpressionCangJieCallArgument,
            targetType: UnwrappedType,
            @Suppress("UNUSED_PARAMETER") isReceiver: Boolean = false, // for reproducing OI behaviour
        ): UnstableSmartCast {
            return UnstableSmartCastResolutionError(argument, targetType)
        }
    }
}
class ArgumentPassedTwice(
    val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val firstOccurrence: ResolvedCallArgument
) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

class ManyCandidatesCallDiagnostic(val candidates: Collection<ResolutionCandidate>) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}
class NamedArgumentNotAllowed(val argument:CangJieCallArgument, val descriptor: CallableDescriptor) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}
// candidates result
class NoneCandidatesCallDiagnostic : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}
fun List<CangJieCallDiagnostic>.filterErrorDiagnostics() =
    filter { it !is CangJieConstraintSystemDiagnostic || it.error !is NewConstraintWarning }
// ArgumentsToParameterMapper
class TooManyArguments(val argument: CangJieCallArgument, val descriptor: CallableDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}
class NamedArgumentReference(
    val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}
class MixingNamedAndPositionArguments(override val argument: CangJieCallArgument) : InapplicableArgumentDiagnostic()
abstract class InapplicableArgumentDiagnostic :CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    abstract val argument: CangJieCallArgument

    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}
class NameNotFound(val argument: CangJieCallArgument, val descriptor: CallableDescriptor) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}
class NameForAmbiguousParameter(
    val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val overriddenParameterWithOtherName: ValueParameterDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}
class VarargArgumentOutsideParentheses(
    override val argument:CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : InapplicableArgumentDiagnostic()
class NoValueForParameter(
    val parameterDescriptor: ValueParameterDescriptor,
    val descriptor: CallableDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}
// TypeArgumentsToParameterMapper
class WrongCountOfTypeArguments(
    val descriptor: CallableDescriptor,
    val currentCount: Int
) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onTypeArguments(this)
}
