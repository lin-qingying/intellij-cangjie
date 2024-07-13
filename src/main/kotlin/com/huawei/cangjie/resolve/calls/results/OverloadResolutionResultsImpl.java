package com.huawei.cangjie.resolve.calls.results;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class OverloadResolutionResultsImpl <D extends CallableDescriptor> implements OverloadResolutionResults<D>{
    private final Code resultCode;

    private final Collection<MutableResolvedCall<D>> results;
    private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<MutableResolvedCall<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }
    private Collection<ResolvedCall<D>> allCandidates;
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<MutableResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<>(Code.AMBIGUITY, candidates);
    }
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound() {
        OverloadResolutionResultsImpl<D> results = new OverloadResolutionResultsImpl<>(
                Code.NAME_NOT_FOUND, Collections.<MutableResolvedCall<D>>emptyList());
        results.setAllCandidates(Collections.emptyList());
        return results;
    }
    public void setAllCandidates(@Nullable Collection<ResolvedCall<D>> allCandidates) {
        this.allCandidates = allCandidates;
    }
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> success(@NotNull MutableResolvedCall<D> candidate) {
        return new OverloadResolutionResultsImpl<>(Code.SUCCESS, Collections.singleton(candidate));
    }
    @Override
    public @Nullable Collection<ResolvedCall<D>> getAllCandidates() {
        return allCandidates;

    }

    @Override
    public @NotNull Collection<? extends ResolvedCall<D>> getResultingCalls() {
        return results;

    }

    @Override
    public @NotNull ResolvedCall<D> getResultingCall() {
//        assert isSingleResult();
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
