package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithVisibility
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.DiagnosticReporter
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.resolve.scopes.ResolutionScope
import com.huawei.cangjie.resolve.scopes.SyntheticScopes
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.resolve.scopes.util.parentsWithSelf
import com.huawei.cangjie.types.TypeApproximator

@JvmName("getResultApplicabilityForConstraintErrors")
fun getResultApplicability(diagnostics: Collection<ConstraintSystemError>): CandidateApplicability =
    diagnostics.minByOrNull { it.applicability }?.applicability ?: CandidateApplicability.RESOLVED

@JvmName("getResultApplicabilityForCallDiagnostics")
fun getResultApplicability(diagnostics: Collection<CangJieCallDiagnostic>): CandidateApplicability =
    diagnostics.minByOrNull { it.candidateApplicability }?.candidateApplicability ?: CandidateApplicability.RESOLVED



// todo error for this access from nested class
class VisibilityError(val invisibleMember: DeclarationDescriptorWithVisibility) : ResolutionDiagnostic(
    CandidateApplicability. RUNTIME_ERROR
) {
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

//    fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

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

class CandidateWithBoundDispatchReceiver(
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val descriptor: CallableDescriptor,
    val diagnostics: List<ResolutionDiagnostic>
)
object ErrorDescriptorDiagnostic : ResolutionDiagnostic(CandidateApplicability.RESOLVED) // todo discuss and change to INAPPLICABLE
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
