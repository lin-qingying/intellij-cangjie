package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.resolve.calls.model.CangJieCallDiagnostic
import com.huawei.cangjie.resolve.calls.model.DiagnosticReporter
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo

@JvmName("getResultApplicabilityForConstraintErrors")
fun getResultApplicability(diagnostics: Collection<ConstraintSystemError>): CandidateApplicability =
    diagnostics.minByOrNull { it.applicability }?.applicability ?: CandidateApplicability.RESOLVED

@JvmName("getResultApplicabilityForCallDiagnostics")
fun getResultApplicability(diagnostics: Collection<CangJieCallDiagnostic>): CandidateApplicability =
    diagnostics.minByOrNull { it.candidateApplicability }?.candidateApplicability ?: CandidateApplicability.RESOLVED
interface ScopeTowerLevel {
    fun getVariables(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

//    fun getObjects(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun getFunctions(name: Name, extensionReceiver: ReceiverValueWithSmartCastInfo?): Collection<CandidateWithBoundDispatchReceiver>

    fun recordLookup(name: Name)
}
interface ImplicitScopeTower{
    val lexicalScope: LexicalScope
//    val syntheticScopes: SyntheticScopes
val location: LookupLocation

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
