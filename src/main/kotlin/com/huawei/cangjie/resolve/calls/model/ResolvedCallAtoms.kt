package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.components.FreshVariableNewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstant
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
    private var suspendAdapterMap: HashMap<CangJieCallArgument, UnwrappedType>? = null

    override val candidateDescriptor: CallableDescriptor
        get() = _candidateDescriptor
    private var signedUnsignedConstantConversions: HashMap<CangJieCallArgument, IntegerValueTypeConstant>? = null

    override var contextReceiversArguments: List<SimpleCangJieCallArgument> = listOf()
    override lateinit var argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>
    override lateinit var freshVariablesSubstitutor: FreshVariableNewTypeSubstitutor
    override val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = suspendAdapterMap ?: emptyMap()

    override lateinit var knownParametersSubstitutor: NewTypeSubstitutor

    fun registerArgumentWithSuspendConversion(argument: CangJieCallArgument, convertedType: UnwrappedType) {
        if (suspendAdapterMap == null)
            suspendAdapterMap = hashMapOf()

        suspendAdapterMap!![argument] = convertedType
    }

    lateinit var argumentToCandidateParameter: Map<CangJieCallArgument, ValueParameterDescriptor>
    private var samAdapterMap: HashMap<CangJieCallArgument, SamConversionDescription>? = null

    override val argumentsWithConversion: Map<CangJieCallArgument, SamConversionDescription>
        get() = samAdapterMap ?: emptyMap()

//    override val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>
//        get() = suspendAdapterMap ?: emptyMap()

    val hasSamConversion: Boolean
        get() = samAdapterMap != null

    override val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>
        get() = unitAdapterMap ?: emptyMap()
    override val argumentsWithConstantConversion: Map<CangJieCallArgument, IntegerValueTypeConstant>
        get() = signedUnsignedConstantConversions ?: emptyMap()

    override fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor) {
        if (newCandidateDescriptor == candidateDescriptor) return
        _candidateDescriptor = newCandidateDescriptor
    }
    fun registerArgumentWithConstantConversion(argument: CangJieCallArgument, convertedConstant: IntegerValueTypeConstant) {
        if (signedUnsignedConstantConversions == null)
            signedUnsignedConstantConversions = hashMapOf()

        signedUnsignedConstantConversions!![argument] = convertedConstant
    }
    fun registerArgumentWithSamConversion(argument: CangJieCallArgument, samConversionDescription: SamConversionDescription) {
        if (samAdapterMap == null)
            samAdapterMap = hashMapOf()

        samAdapterMap!![argument] = samConversionDescription
    }


    fun registerArgumentWithUnitConversion(argument: CangJieCallArgument, convertedType: UnwrappedType) {
        if (unitAdapterMap == null)
            unitAdapterMap = hashMapOf()

        unitAdapterMap!![argument] = convertedType
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


fun CangJieDiagnosticsHolder.addError(error: ConstraintSystemError) {
    addDiagnostic(error.asDiagnostic())
}
fun ResolutionCandidate.markCandidateForCompatibilityResolve(needToReportWarning: Boolean = true) {
//    if (callComponents.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return
//    addDiagnostic(LowerPriorityToPreserveCompatibility(needToReportWarning).asDiagnostic())
}
