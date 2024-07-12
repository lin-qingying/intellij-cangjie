package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.types.UnwrappedType

abstract class ResolutionPart {
    abstract fun ResolutionCandidate.process(workIndex: Int)

    open fun ResolutionCandidate.workCount(): Int = 1

    // helper functions
    protected inline val ResolutionCandidate.candidateDescriptor get() = resolvedCall.candidateDescriptor
    protected inline val ResolutionCandidate.cangjieCall get() = resolvedCall.atom
}

fun CangJieDiagnosticsHolder.addDiagnosticIfNotNull(diagnostic: CangJieCallDiagnostic?) {
    diagnostic?.let { addDiagnostic(it) }
}
open class MutableResolvedCallAtom(
    override val atom: CangJieCall,
    originalCandidateDescriptor: CallableDescriptor, // original candidate descriptor
    override val explicitReceiverKind: ExplicitReceiverKind,
    override val dispatchReceiverArgument: SimpleCangJieCallArgument?,
    override var extensionReceiverArgument: SimpleCangJieCallArgument?,
    override val extensionReceiverArgumentCandidates: List<SimpleCangJieCallArgument>?,
    open val reflectionCandidateType: UnwrappedType? = null,
    open val candidate: CallableReferenceResolutionCandidate? = null
) : ResolvedCallAtom() {
    private var _candidateDescriptor = originalCandidateDescriptor
    private var unitAdapterMap: HashMap<CangJieCallArgument, UnwrappedType>? = null

    override val candidateDescriptor: CallableDescriptor
        get() = _candidateDescriptor

    override var contextReceiversArguments: List<SimpleCangJieCallArgument> = listOf()
    override lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    override lateinit var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor

    lateinit var argumentToCandidateParameter: Map<CangJieCallArgument, ValueParameterDescriptor>
    private var samAdapterMap: HashMap<CangJieCallArgument, SamConversionDescription>? = null

//
//    override val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
//        get() = suspendAdapterMap ?: emptyMap()

    val hasSamConversion: Boolean
        get() = samAdapterMap != null

    override val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = unitAdapterMap ?: emptyMap()

    override fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor) {
        if (newCandidateDescriptor == candidateDescriptor) return
        _candidateDescriptor = newCandidateDescriptor
    }

    public override fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        super.setAnalyzedResults(subResolvedAtoms)
    }
}

interface CangJieDiagnosticsHolder {
    fun addDiagnostic(diagnostic: CangJieCallDiagnostic)

    class SimpleHolder : CangJieDiagnosticsHolder {
        private val diagnostics = arrayListOf<CangJieCallDiagnostic>()

        override fun addDiagnostic(diagnostic: CangJieCallDiagnostic) {
            diagnostics.add(diagnostic)
        }

        fun getDiagnostics(): List<CangJieCallDiagnostic> = diagnostics
    }
}


class ResolvedCallableReferenceCallAtom(
    atom: CangJieCall,
    candidateDescriptor: CallableDescriptor,
    explicitReceiverKind: ExplicitReceiverKind,
    dispatchReceiverArgument: SimpleCangJieCallArgument?,
    extensionReceiverArgument: SimpleCangJieCallArgument?,
    reflectionCandidateType: UnwrappedType? = null,
    candidate: CallableReferenceResolutionCandidate? = null
) : MutableResolvedCallAtom(
    atom, candidateDescriptor, explicitReceiverKind, dispatchReceiverArgument, extensionReceiverArgument, emptyList(), reflectionCandidateType, candidate
), ResolvedCallableReferenceAtom
