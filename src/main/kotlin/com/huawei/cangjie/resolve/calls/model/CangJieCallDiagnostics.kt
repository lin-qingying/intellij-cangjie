package com.huawei.cangjie.resolve.calls.model

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

class ManyCandidatesCallDiagnostic(val candidates: Collection<ResolutionCandidate>) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

// candidates result
class NoneCandidatesCallDiagnostic : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}
fun List<CangJieCallDiagnostic>.filterErrorDiagnostics() =
    filter { it !is CangJieConstraintSystemDiagnostic || it.error !is NewConstraintWarning }
