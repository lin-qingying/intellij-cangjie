package com.huawei.cangjie.resolve.calls.results;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
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
