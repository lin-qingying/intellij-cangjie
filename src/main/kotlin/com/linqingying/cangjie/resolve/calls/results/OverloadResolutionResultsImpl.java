/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.resolve.calls.results;

import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.resolve.calls.model.MutableResolvedCall;
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class OverloadResolutionResultsImpl<D extends CallableDescriptor> implements OverloadResolutionResults<D> {
    private final Code resultCode;

    private final Collection<MutableResolvedCall<D>> results;
    private Collection<ResolvedCall<D>> allCandidates;

    private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<MutableResolvedCall<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(MutableResolvedCall<D> candidate) {
        return incompleteTypeInference(Collections.singleton(candidate));
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(Collection<MutableResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<>(Code.INCOMPLETE_TYPE_INFERENCE, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<MutableResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<>(Code.AMBIGUITY, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> candidatesWithWrongReceiver(Collection<MutableResolvedCall<D>> failedCandidates) {
        return new OverloadResolutionResultsImpl<>(Code.CANDIDATES_WITH_WRONG_RECEIVER, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> singleFailedCandidate(MutableResolvedCall<D> candidate) {
        return new OverloadResolutionResultsImpl<>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> manyFailedCandidates(Collection<MutableResolvedCall<D>> failedCandidates) {
        return new OverloadResolutionResultsImpl<>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound() {
        OverloadResolutionResultsImpl<D> results = new OverloadResolutionResultsImpl<>(
                Code.NAME_NOT_FOUND, Collections.emptyList());
        results.setAllCandidates(Collections.emptyList());
        return results;
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> success(@NotNull MutableResolvedCall<D> candidate) {
        return new OverloadResolutionResultsImpl<>(Code.SUCCESS, Collections.singleton(candidate));
    }
@Override
    public OverloadResolutionResultsImpl<D> replaceCode(@NotNull Code newCode) {
        return new OverloadResolutionResultsImpl<>(newCode, results);
    }

    @Override
    public @Nullable Collection<ResolvedCall<D>> getAllCandidates() {
        return allCandidates;

    }

    public void setAllCandidates(@Nullable Collection<ResolvedCall<D>> allCandidates) {
        this.allCandidates = allCandidates;
    }

    @Override
    @NotNull
    public Collection<MutableResolvedCall<D>> getResultingCalls() {
        return results;
    }

    @Override
    @NotNull
    public MutableResolvedCall<D> getResultingCall() {
        assert isSingleResult();
        return results.iterator().next();
    }

    @Override
    public @NotNull D getResultingDescriptor() {
        return getResultingCall().getResultingDescriptor();

    }

    @Override
    public @NotNull Code getResultCode() {
        return resultCode;

    }

    @Override
    public boolean isSuccess() {
        return resultCode.isSuccess();

    }

    @Override
    public boolean isSingleResult() {
        return results.size() == 1 && getResultCode() != Code.CANDIDATES_WITH_WRONG_RECEIVER;

    }

    @Override
    public boolean isNothing() {
        return resultCode == Code.NAME_NOT_FOUND;

    }

    @Override
    public boolean isAmbiguity() {
        return resultCode == Code.AMBIGUITY;

    }

    @Override
    public boolean isIncomplete() {
        return resultCode == Code.INCOMPLETE_TYPE_INFERENCE;

    }
}
