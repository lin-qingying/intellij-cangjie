package com.huawei.cangjie.resolve.calls.components

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
    fun addPartialCallInfo(callInfo: PartialCallInfo)
    fun callCompleted(resolvedAtom: ResolvedAtom): Boolean
    fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean
    fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean
    fun addCompletedCallInfo(callInfo: CompletedCallInfo)

    companion object {
        val default = object : InferenceSession {
            override val parentSession: InferenceSession? = null
            override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
            override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
            override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false


            override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
            override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}



        }
    }
}
