package com.huawei.cangjie.resolve.calls.results

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults.Code


abstract class AbstractOverloadResolutionResults<D : CallableDescriptor> : OverloadResolutionResults<D> {
    override fun isSuccess() = resultCode.isSuccess
    override fun isSingleResult() = resultingCalls.size == 1 && resultCode != OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER
    override fun isNothing() = resultCode == OverloadResolutionResults.Code.NAME_NOT_FOUND
    override fun isAmbiguity() = resultCode == OverloadResolutionResults.Code.AMBIGUITY
    override fun isIncomplete() = resultCode == OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE
}

class SingleOverloadResolutionResult<D : CallableDescriptor>(val result: ResolvedCall<D>) : AbstractOverloadResolutionResults<D>() {
    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = null
    override fun getResultingCalls(): Collection<ResolvedCall<D>> = listOf(result)
    override fun getResultingCall() = result

    override fun getResultingDescriptor(): D = result.resultingDescriptor

    override fun getResultCode(): Code = when (result.status) {
        ResolutionStatus.SUCCESS -> Code.SUCCESS
        ResolutionStatus.RECEIVER_TYPE_ERROR -> Code.CANDIDATES_WITH_WRONG_RECEIVER
        ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> Code.INCOMPLETE_TYPE_INFERENCE
        else -> Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH
    }
}

open class NameNotFoundResolutionResult<D : CallableDescriptor> : AbstractOverloadResolutionResults<D>() {
    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = null
    override fun getResultingCalls(): Collection<ResolvedCall<D>> = emptyList()
    override fun getResultingCall() = error("No candidates")
    override fun getResultingDescriptor() = error("No candidates")
    override fun getResultCode() = Code.NAME_NOT_FOUND
}

class ManyCandidates<D : CallableDescriptor>(
    val candidates: Collection<ResolvedCall<D>>
) : AbstractOverloadResolutionResults<D>() {
    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = null
    override fun getResultingCalls(): Collection<ResolvedCall<D>> = candidates
    override fun getResultingCall() = error("Many candidates")
    override fun getResultingDescriptor() = error("Many candidates")
    override fun getResultCode() =
        when (candidates.first().status) {
            ResolutionStatus.RECEIVER_TYPE_ERROR -> Code.CANDIDATES_WITH_WRONG_RECEIVER
            ResolutionStatus.SUCCESS -> Code.AMBIGUITY
            ResolutionStatus.INCOMPLETE_TYPE_INFERENCE -> Code.INCOMPLETE_TYPE_INFERENCE
            else -> Code.MANY_FAILED_CANDIDATES
        }
}


class AllCandidates<D : CallableDescriptor>(private val allCandidates: Collection<ResolvedCall<D>>) : NameNotFoundResolutionResult<D>() {
    override fun getAllCandidates() = allCandidates
}