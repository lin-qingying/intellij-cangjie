package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.huawei.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.UnwrappedType

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
    fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: CangJieDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType>?

    fun addPartialCallInfo(callInfo: PartialCallInfo)
    fun callCompleted(resolvedAtom: ResolvedAtom): Boolean
    fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean
    fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom): Boolean
    fun addCompletedCallInfo(callInfo: CompletedCallInfo)
    fun computeCompletionMode(candidate: ResolutionCandidate): ConstraintSystemCompletionMode?
    fun resolveReceiverIndependently(): Boolean
    fun currentConstraintSystem(): ConstraintStorage
    fun initializeLambda(lambda: ResolvedLambdaAtom)

    companion object {

        val default = object : InferenceSession {
            override val parentSession: InferenceSession? = null
            override fun addPartialCallInfo(callInfo: PartialCallInfo) {}
            override fun callCompleted(resolvedAtom: ResolvedAtom): Boolean = false
            override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean = false
            override fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean = true
            override fun addErrorCallInfo(callInfo: ErrorCallInfo) {

            }

            override fun inferPostponedVariables(
                lambda: ResolvedLambdaAtom,
                constraintSystemBuilder: ConstraintSystemBuilder,
                completionMode: ConstraintSystemCompletionMode,
                diagnosticsHolder: CangJieDiagnosticsHolder
            ): Map<TypeConstructor, UnwrappedType> = emptyMap()

            override fun resolveReceiverIndependently(): Boolean = false
            override fun currentConstraintSystem(): ConstraintStorage = ConstraintStorage.Empty
            override fun initializeLambda(lambda: ResolvedLambdaAtom) {

            }


            override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true
            override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {}
            override fun computeCompletionMode(
                candidate: ResolutionCandidate
            ): ConstraintSystemCompletionMode? = null


        }
    }
}
