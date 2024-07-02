package com.huawei.cangjie.resolve.calls.components.candidate

import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage

import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.Candidate
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability
import com.huawei.cangjie.resolve.calls.tower.ImplicitScopeTower
import com.huawei.cangjie.resolve.calls.tower.getResultApplicability
import com.huawei.cangjie.types.TypeSubstitutor
import java.util.ArrayList

sealed class ResolutionCandidate : Candidate, CangJieDiagnosticsHolder {
    abstract val resolvedCall: MutableResolvedCallAtom
    abstract val callComponents: CangJieCallComponents
    abstract val variableCandidateIfInvoke: ResolutionCandidate?
    abstract val scopeTower: ImplicitScopeTower
    abstract val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
    abstract val resolutionCallbacks: CangJieResolutionCallbacks
protected abstract val baseSystem: ConstraintStorage?
    private val variableApplicability
        get() = variableCandidateIfInvoke?.resultingApplicability ?: CandidateApplicability.RESOLVED
val resultingApplicabilities: Array<CandidateApplicability>
    get() = arrayOf(currentApplicability,/* getResultApplicability(getSystem().errors), variableApplicability*/)

    override val resultingApplicability: CandidateApplicability
        get() {
            processParts(stopOnFirstError = false)
            return resultingApplicabilities.minOrNull() ?: CandidateApplicability.RESOLVED
        }
    override val isSuccessful: Boolean
        get() = true
//    override val isSuccessful: Boolean
//        get() {
//            processParts(stopOnFirstError = true)
//            // Note: candidate with K1_RESOLVED_WITH_ERROR is exceptionally treated as successful
//            return resultingApplicabilities.minOrNull()!!.isSuccessOrSuccessWithError && !getSystem().hasContradiction
//        }
//    private var newSystem: NewConstraintSystemImpl? = null

    private val CandidateApplicability.isSuccessOrSuccessWithError: Boolean
        get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY

//    override val resultingApplicability: CandidateApplicability
//        get() {
//            processParts(stopOnFirstError = false)
//            return resultingApplicabilities.minOrNull() ?: CandidateApplicability.RESOLVED
//        }

//    open val resolutionSequence: List<ResolutionPart> get() = resolvedCall.atom.callKind.resolutionSequence
//fun getSystem(): NewConstraintSystem {
//    if (newSystem == null) {
//        newSystem = NewConstraintSystemImpl(
//            callComponents.constraintInjector, callComponents.builtIns,
//            callComponents.kotlinTypeRefiner, callComponents.languageVersionSettings
//        )
//        if (baseSystem != null) {
//            newSystem!!.addOtherSystem(baseSystem!!)
//        }
//    }
//    return newSystem!!
//}


    protected val mutableDiagnostics: ArrayList<CangJieCallDiagnostic> = arrayListOf()

//    val descriptor: CallableDescriptor get() = resolvedCall.candidateDescriptor
    val diagnostics: List<CangJieCallDiagnostic> = mutableDiagnostics
//    val resultingApplicabilities: Array<CandidateApplicability>
//        get() = arrayOf(currentApplicability, getResultApplicability(getSystem().errors), variableApplicability)

//    private val variableApplicability
//        get() = variableCandidateIfInvoke?.resultingApplicability ?: CandidateApplicability.RESOLVED
//    private val stepCount get() = resolutionSequence.sumOf { it.run { workCount() } }

    private var step = 0
//    private var newSystem: NewConstraintSystemImpl? = null
    private var currentApplicability: CandidateApplicability = CandidateApplicability.RESOLVED

//    fun getResultingSubstitutor(): TypeSubstitutorMarker? = newSystem?.buildCurrentSubstitutor()

    abstract fun getSubResolvedAtoms(): List<ResolvedAtom>
    abstract fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom)

    override fun addDiagnostic(diagnostic: CangJieCallDiagnostic) {
        mutableDiagnostics.add(diagnostic)
        currentApplicability = minOf(diagnostic.candidateApplicability, currentApplicability)
    }

    override fun addCompatibilityWarning(other: Candidate) {
//        if (other is ResolutionCandidate && this !== other && this::class == other::class) {
//            addDiagnostic(CompatibilityWarning(other.descriptor))
//        }
    }

    override fun toString(): String {
        return "带实现"
//        val descriptor = DescriptorRenderer.COMPACT.render(resolvedCall.candidateDescriptor)
//        val okOrFail = if (resultingApplicabilities.minOrNull()?.isSuccess != false) "OK" else "FAIL"
//        val step = "$step/$stepCount"
//        return "$okOrFail($step): $descriptor"
    }

//    fun getSystem(): NewConstraintSystem {
//        if (newSystem == null) {
//            newSystem = NewConstraintSystemImpl(
//                callComponents.constraintInjector, callComponents.builtIns,
//                callComponents.kotlinTypeRefiner, callComponents.languageVersionSettings
//            )
//            if (baseSystem != null) {
//                newSystem!!.addOtherSystem(baseSystem!!)
//            }
//        }
//        return newSystem!!
//    }

    private fun processParts(stopOnFirstError: Boolean) {
        if (stopOnFirstError && step > 0) return // error already happened
//        if (step == stepCount) return
//
//        var partIndex = 0
//        var workStep = step
//        while (workStep > 0) {
//            val workCount = resolutionSequence[partIndex].run { workCount() }
//            if (workStep >= workCount) {
//                partIndex++
//                workStep -= workCount
//            } else {
//                break
//            }
//        }
//        if (partIndex < resolutionSequence.size) {
//            if (processPart(resolutionSequence[partIndex], stopOnFirstError, workStep)) return
//            partIndex++
//        }
//
//        while (partIndex < resolutionSequence.size) {
//            if (processPart(resolutionSequence[partIndex], stopOnFirstError)) return
//            partIndex++
//        }
//        if (step == stepCount) {
//            resolvedCall.setAnalyzedResults(getSubResolvedAtoms())
//        }
    }

    // true if part was interrupted
//    private fun processPart(part: ResolutionPart, stopOnFirstError: Boolean, startWorkIndex: Int = 0): Boolean {
//        for (workIndex in startWorkIndex until (part.run { workCount() })) {
//            if (stopOnFirstError && !currentApplicability.isSuccess) return true
//
//            part.run { process(workIndex) }
//            step++
//        }
//        return false
//    }
}
