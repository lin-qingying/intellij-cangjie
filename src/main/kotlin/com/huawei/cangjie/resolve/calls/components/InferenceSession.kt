package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.model.*

interface PartialCallInfo {
    val callResolutionResult: PartialCallResolutionResult
}
//interface CompletedCallInfo {
//    val callResolutionResult: CompletedCallResolutionResult
//}

interface ErrorCallInfo {
    val callResolutionResult: CallResolutionResult
}

interface CompletedCallInfo {
    val callResolutionResult: CompletedCallResolutionResult
}

interface InferenceSession {
    val parentSession: InferenceSession?
    fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean
    fun addErrorCallInfo(callInfo: ErrorCallInfo)

    fun addPartialCallInfo(callInfo: PartialCallInfo)
    fun callCompleted(resolvedAtom: ResolvedAtom): Boolean
    fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean
    fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean
    fun addCompletedCallInfo(callInfo: CompletedCallInfo)
    fun computeCompletionMode(candidate: ResolutionCandidate): ConstraintSystemCompletionMode?
    fun resolveReceiverIndependently(): Boolean
    fun currentConstraintSystem(): ConstraintStorage

    companion object {
        val default = object : InferenceSession {
            override val parentSession: InferenceSession? = null
            override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
            override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
            override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false
            override fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean = true
            override fun addErrorCallInfo(callInfo: ErrorCallInfo) {

            }

            override fun resolveReceiverIndependently(): Boolean = false
            override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty


            override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
            override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}
            override fun computeCompletionMode(
                candidate: ResolutionCandidate
            ): ConstraintSystemCompletionMode? = null


        }
    }
}
