package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.linqingying.cangjie.resolve.calls.model.CangJieCallArgument
import com.linqingying.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.linqingying.cangjie.resolve.calls.model.DiagnosticReporter
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.resolve.scopes.ResolutionScope
import com.linqingying.cangjie.resolve.scopes.SyntheticScopes
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.resolve.scopes.util.parentsWithSelf
import com.linqingying.cangjie.types.TypeApproximator

@JvmName("getResultApplicabilityForConstraintErrors")
fun getResultApplicability(diagnostics: Collection<ConstraintSystemError>): CandidateApplicability =
    diagnostics.minByOrNull { it.applicability }?.applicability ?: CandidateApplicability.RESOLVED

@JvmName("getResultApplicabilityForCallDiagnostics")
fun getResultApplicability(diagnostics: Collection<CangJieCallDiagnostic>): CandidateApplicability =
    diagnostics.minByOrNull { it.candidateApplicability }?.candidateApplicability ?: CandidateApplicability.RESOLVED


// todo error for this access from nested class
class VisibilityError(val invisibleMember: DeclarationDescriptorWithVisibility) : ResolutionDiagnostic(
    CandidateApplicability.RUNTIME_ERROR
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class ContextReceiverAmbiguity : ResolutionDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class NoMatchingContextReceiver : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

object HiddenDescriptor : ResolutionDiagnostic(CandidateApplicability.HIDDEN)

interface ScopeTowerLevel {
    fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    fun getEnumTypeByKind(
        name: Name,
        kind:ClassKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> = emptyList()

    fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    fun getObjects(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    fun recordLookup(name: Name)
}

object UnstableSmartCastDiagnostic : ResolutionDiagnostic(CandidateApplicability.UNSTABLE_SMARTCAST)

interface ImplicitScopeTower {
    val lexicalScope: LexicalScope
    val areContextReceiversEnabled: Boolean
    val syntheticScopes: SyntheticScopes
    val dynamicScope: MemberScope

    val typeApproximator: TypeApproximator
    val isNewInferenceEnabled: Boolean
    fun getContextReceivers(scope: LexicalScope): List<ReceiverValueWithSmartCastInfo>

    fun interceptVariableCandidates(
        resolutionScope: ResolutionScope,
        name: Name,
        initialResults: Collection<VariableDescriptor>,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<VariableDescriptor>

    fun allScopesWithImplicitsResolutionInfo(): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> =
        implicitsResolutionFilter.getScopesWithInfo(lexicalScope.parentsWithSelf)

    fun interceptFunctionCandidates(
        resolutionScope: ResolutionScope,
        name: Name,
        initialResults: Collection<FunctionDescriptor>,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<FunctionDescriptor>

    //    val syntheticScopes: SyntheticScopes
    val location: LookupLocation
    val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter
    fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo?

}

abstract class ResolutionDiagnostic(candidateApplicability: CandidateApplicability) :
    CangJieCallDiagnostic(candidateApplicability) {
    override fun report(reporter: DiagnosticReporter) {
        // do nothing
    }
}

class  CandidateWithBoundDispatchReceiver(
       val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
      val descriptor: CallableDescriptor,
        val diagnostics: List<ResolutionDiagnostic>
)

object ErrorDescriptorDiagnostic :
    ResolutionDiagnostic(CandidateApplicability.RESOLVED) // todo discuss and change to INAPPLICABLE
object EmptyDiagnostic: ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)
object DynamicDescriptorDiagnostic : ResolutionDiagnostic(CandidateApplicability.RESOLVED_LOW_PRIORITY)

class ResolvedUsingDeprecatedVisibility(val baseSourceScope: ResolutionScope, val lookupLocation: LookupLocation) :
    ResolutionDiagnostic(
        CandidateApplicability.RESOLVED
    )

class VisibilityErrorOnArgument(
    val argument: CangJieCallArgument,
    val invisibleMember: DeclarationDescriptorWithVisibility
) : ResolutionDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}
