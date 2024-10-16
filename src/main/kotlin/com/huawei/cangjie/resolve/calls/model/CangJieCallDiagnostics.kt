package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.components.DescriptorKind
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintError
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintWarning
import com.huawei.cangjie.resolve.calls.inference.model.transformToWarning
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.UnwrappedType


interface TransformableToWarning<T : CangJieCallDiagnostic> {
    fun transformToWarning(): T?
}

class EnumEntryAmbiguityWarning(val property: PropertyDescriptor, val enumEntry: ClassDescriptor) :
    CangJieCallDiagnostic(
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
    val cangjieCall: CangJieCall?
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

class NotCallableExpectedType(
    val argument: CallableReferenceCangJieCallArgument,
    val expectedType: UnwrappedType,
    val notCallableTypeConstructor: TypeConstructor
) : CallableReferenceInapplicableDiagnostic(argument)

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

class MissingNamedArgumentPrefix(
    val argument: CangJieCallArgument,
    val names: Set<Name>

) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)

}

class ManyCandidatesCallDiagnostic(val candidates: Collection<ResolutionCandidate>) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class NamedArgumentNotAllowed(val argument: CangJieCallArgument, val descriptor: CallableDescriptor) :
    CangJieCallDiagnostic(
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

class StubBuilderInferenceReceiver(
    val receiver: SimpleCangJieCallArgument,
    val extensionReceiverParameter: ReceiverParameterDescriptor,
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

class NoneOperatorCallDiagnostic(val left: CangJieType, val right: CangJieType) :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
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

class PositionalAfierNamedArgument(override val argument: CangJieCallArgument) : InapplicableArgumentDiagnostic()


abstract class InapplicableArgumentDiagnostic : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
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
    override val argument: CangJieCallArgument,
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

//type arguments cannot appear after 'enum entry' when enum type 'enum' is given
class TypeArgumentsAfterEnumEntry(val enumEntry: ClassDescriptor,val enum :ClassDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter)= reporter.onTypeArguments(this)
}
object TypeArgumentsCompilerError  :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter)= reporter.onTypeArguments(this)
}


class CallableReferenceCallCandidatesAmbiguity(
    val argument: CallableReferenceCangJieCallArgument,
    val candidates: Collection<CallableReferenceResolutionCandidate>
) : CallableReferenceInapplicableDiagnostic(argument)

abstract class CallableReferenceInapplicableDiagnostic(
    private val argument: CallableReferenceResolutionAtom,
    applicability: CandidateApplicability = CandidateApplicability.INAPPLICABLE
) : CangJieCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) {
        when (argument) {
            is CallableReferenceCangJieCall -> reporter.onCall(this)
            is CallableReferenceCangJieCallArgument -> reporter.onCallArgument(argument, this)
        }
    }
}

class CompatibilityWarning(val candidate: CallableDescriptor) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class CompatibilityWarningOnArgument(
    val argument: CangJieCallArgument,
    val candidate: CallableDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

class NoneCallableReferenceCallCandidates(val argument: CallableReferenceCangJieCallArgument) :
    CallableReferenceInapplicableDiagnostic(argument)

class NotEnoughInformationForLambdaParameter(
    val lambdaArgument: LambdaCangJieCallArgument,
    val parameterIndex: Int
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(lambdaArgument, this)
    }
}

class NonVarargSpread(val argument: CangJieCallArgument) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentSpread(argument, this)
}

sealed interface ArgumentNullabilityMismatchDiagnostic {
    val expectedType: UnwrappedType
    val actualType: UnwrappedType
    val expressionArgument: ExpressionCangJieCallArgument
}

object TypeCheckerHasRanIntoRecursion : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

class ArgumentNullabilityErrorDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionCangJieCallArgument
) : CangJieCallDiagnostic(CandidateApplicability.UNSAFE_CALL),
    TransformableToWarning<ArgumentNullabilityWarningDiagnostic>, ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }

    override fun transformToWarning() =
        ArgumentNullabilityWarningDiagnostic(expectedType, actualType, expressionArgument)
}

class ArgumentNullabilityWarningDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionCangJieCallArgument
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED), ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }
}

//非静态上下文访问静态成员
class NonStaticContextAccessStaticMemberDiagnostic(val kind: DescriptorKind, val descriptor: DeclarationDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {

        reporter.onCall(this)
    }
}

//静态上下文访问非静态成员
class StaticContextAccessNonStaticMemberDiagnostic(val kind: DescriptorKind, val descriptor: DeclarationDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)


    }
}

class SuperAsExtensionReceiver(val receiver: SimpleCangJieCallArgument) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallReceiver(receiver, this)
    }
}

class NoCallOperatorFunction(val descriptor: DeclarationDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.UNSAFE_CALL) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

object AbstractFakeOverrideSuperCall : CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class AbstractSuperCall(val receiver: SimpleCangJieCallArgument) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}
