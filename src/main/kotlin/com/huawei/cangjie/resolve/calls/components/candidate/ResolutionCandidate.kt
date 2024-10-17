package com.huawei.cangjie.resolve.calls.components.candidate

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.renderer.DescriptorRenderer
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.inference.NewConstraintSystem
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.inference.model.NewConstraintSystemImpl
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.tower.*
import com.huawei.cangjie.types.TypeSubstitutor
import com.huawei.cangjie.types.model.TypeSubstitutorMarker

sealed class ResolutionCandidate : Candidate, CangJieDiagnosticsHolder {
    abstract val resolvedCall: MutableResolvedCallAtom
    abstract val callComponents: CangJieCallComponents
    abstract val variableCandidateIfInvoke: ResolutionCandidate?
    abstract val scopeTower: ImplicitScopeTower
    abstract val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
    abstract val resolutionCallbacks: CangJieResolutionCallbacks
    protected abstract val baseSystem: ConstraintStorage?

    open val resolutionSequence: List<ResolutionPart> get() = resolvedCall.atom.callKind.resolutionSequence

    override val resultingApplicability: CandidateApplicability
        get() {
            processParts(stopOnFirstError = false)
            return resultingApplicabilities.minOrNull() ?: CandidateApplicability.RESOLVED
        }

    override val isSuccessful: Boolean
        get() {
            processParts(stopOnFirstError = true)
            // Note: candidate with  RESOLVED_WITH_ERROR is exceptionally treated as successful
            return resultingApplicabilities.minOrNull()!!.isSuccessOrSuccessWithError && !getSystem().hasContradiction
        }


    private val CandidateApplicability.isSuccessOrSuccessWithError: Boolean
        get() = this >= CandidateApplicability.RESOLVED_LOW_PRIORITY






    protected val mutableDiagnostics: ArrayList<CangJieCallDiagnostic> = arrayListOf()

        val descriptor: CallableDescriptor get() = resolvedCall.candidateDescriptor
    val diagnostics: List<CangJieCallDiagnostic> = mutableDiagnostics
    val resultingApplicabilities: Array<CandidateApplicability>
        get() = arrayOf(currentApplicability, getResultApplicability(getSystem().errors), variableApplicability)

    private val variableApplicability
        get() = variableCandidateIfInvoke?.resultingApplicability ?: CandidateApplicability.RESOLVED
    private val stepCount get() = resolutionSequence.sumOf { it.run { workCount() } }

    private var step = 0

        private var newSystem: NewConstraintSystemImpl? = null
    private var currentApplicability: CandidateApplicability = CandidateApplicability.RESOLVED

    fun getResultingSubstitutor(): TypeSubstitutorMarker? = newSystem?.buildCurrentSubstitutor()

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

        val descriptor = DescriptorRenderer.COMPACT.render(resolvedCall.candidateDescriptor)
        val okOrFail = if (resultingApplicabilities.minOrNull()?.isSuccess != false) "OK" else "FAIL"
        val step = "$step/$stepCount"
        return "$okOrFail($step): $descriptor"
    }

    fun getSystem(): NewConstraintSystem {
        if (newSystem == null) {
            newSystem = NewConstraintSystemImpl(
                callComponents.constraintInjector, callComponents.builtIns,
                callComponents.cangjieTypeRefiner, callComponents.languageVersionSettings
            )
            if (baseSystem != null) {
                newSystem!!.addOtherSystem(baseSystem!!)
            }
        }
        return newSystem!!
    }

    /**
     * 根据给定的错误处理策略处理各个部分。
     *
     * 该函数负责按照分辨率序列处理每个部分，如果需要则在第一个错误时停止处理，或继续直到所有部分都被处理完毕。
     *
     * @param stopOnFirstError 布尔值，指示是否在第一个错误时停止处理。如果为 true，则在发生错误时立即停止处理；如果为 false，则继续处理。
     */
    private fun processParts(stopOnFirstError: Boolean) {
        if (stopOnFirstError && step > 0) return // 错误已经发生，直接返回
        if (step == stepCount) return // 已经处理完所有步骤，直接返回

        var partIndex = 0
        var workStep = step
        while (workStep > 0) {
            val workCount = resolutionSequence[partIndex].run { workCount() }
            if (workStep >= workCount) {
                partIndex++
                workStep -= workCount
            } else {
                break
            }
        }

        // 处理当前部分
        if (partIndex < resolutionSequence.size) {
            if (processPart(resolutionSequence[partIndex], stopOnFirstError, workStep)) return
            partIndex++
        }

        // 继续处理剩余部分
        while (partIndex < resolutionSequence.size) {
            if (processPart(resolutionSequence[partIndex], stopOnFirstError)) return
            partIndex++
        }

        // 如果所有步骤都已处理完成，设置分析结果
        if (step == stepCount) {
            resolvedCall.setAnalyzedResults(getSubResolvedAtoms())
        }
    }


    // true if part was interrupted
    private fun processPart(part: ResolutionPart, stopOnFirstError: Boolean, startWorkIndex: Int = 0): Boolean {
        for (workIndex in startWorkIndex until (part.run { workCount() })) {
            if (stopOnFirstError && !currentApplicability.isSuccess) return true

            part.run { process(workIndex) }
            step++
        }
        return false
    }
}
