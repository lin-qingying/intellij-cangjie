/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults.Code
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall

class OverloadResolutionResultsImpl<D : CallableDescriptor> private constructor(
    private val resultCode: OverloadResolutionResults.Code,
    private val results: Collection<MutableResolvedCall<D>>
) : OverloadResolutionResults<D> {

    private var allCandidates: Collection<ResolvedCall<D>>? = null

    override fun replaceCode(newCode: Code): OverloadResolutionResultsImpl<D> =
        OverloadResolutionResultsImpl(newCode, results)

    override fun getAllCandidates(): Collection<ResolvedCall<D>>? = allCandidates

    fun setAllCandidates(allCandidates: Collection<ResolvedCall<D>>?) {
        this.allCandidates = allCandidates
    }

    override fun getResultingCalls(): Collection<MutableResolvedCall<D>> = results

    override fun getResultingCall(): MutableResolvedCall<D> {
        check(isSingleResult())
        return results.iterator().next()
    }

    override fun getResultingDescriptor(): D = getResultingCall().resultingDescriptor

    override fun getResultCode(): Code = resultCode

    override fun isSuccess(): Boolean = resultCode.isSuccess

    override fun isSingleResult(): Boolean =
        results.size == 1 && getResultCode() != Code.CANDIDATES_WITH_WRONG_RECEIVER

    override fun isNothing(): Boolean = resultCode == Code.NAME_NOT_FOUND

    override fun isAmbiguity(): Boolean = resultCode == Code.AMBIGUITY

    override fun isIncomplete(): Boolean = resultCode == Code.INCOMPLETE_TYPE_INFERENCE

    companion object {
        fun <D : CallableDescriptor> incompleteTypeInference(candidate: MutableResolvedCall<D>): OverloadResolutionResultsImpl<D> =
            incompleteTypeInference(setOf(candidate))

        fun <D : CallableDescriptor> incompleteTypeInference(candidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.INCOMPLETE_TYPE_INFERENCE, candidates)

        fun <D : CallableDescriptor> ambiguity(candidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.AMBIGUITY, candidates)

        fun <D : CallableDescriptor> candidatesWithWrongReceiver(failedCandidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.CANDIDATES_WITH_WRONG_RECEIVER, failedCandidates)

        fun <D : CallableDescriptor> singleFailedCandidate(candidate: MutableResolvedCall<D>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(
                OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH,
                setOf(candidate)
            )

        fun <D : CallableDescriptor> manyFailedCandidates(failedCandidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES, failedCandidates)

        fun <D : CallableDescriptor> nameNotFound(): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl<D>(OverloadResolutionResults.Code.NAME_NOT_FOUND, emptyList()).apply {
                setAllCandidates(emptyList())
            }

        fun <D : CallableDescriptor> success(candidate: MutableResolvedCall<D>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.SUCCESS, setOf(candidate))
    }
}
