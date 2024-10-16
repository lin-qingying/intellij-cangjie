package com.huawei.cangjie.resolve.calls.results;


import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface OverloadResolutionResults<D extends CallableDescriptor> {
    /* All candidates are collected only if ResolutionContext.collectAllCandidates is set to true */
    @Nullable
    Collection<ResolvedCall<D>> getAllCandidates();

    @NotNull
    Collection<? extends ResolvedCall<D>> getResultingCalls();

    @NotNull
    ResolvedCall<D> getResultingCall();

    @NotNull
    D getResultingDescriptor();

    @NotNull
    Code getResultCode();

    boolean isSuccess();

    boolean isSingleResult();

    boolean isNothing();

    boolean isAmbiguity();

    boolean isIncomplete();

    default OverloadResolutionResults<D> replaceCode(@NotNull Code newCode) {
        return this;
    }


    enum Code {
        SUCCESS(true),
        NAME_NOT_FOUND(false),
        SINGLE_CANDIDATE_ARGUMENT_MISMATCH(false),
        AMBIGUITY(false),
        MANY_FAILED_CANDIDATES(false),
        CANDIDATES_WITH_WRONG_RECEIVER(false),
        INCOMPLETE_TYPE_INFERENCE(false),
        SUCCESS_NAME_NOT_FOUND(true) //虽然是成功，但是没有找到合适的函数
        ;

        private final boolean success;

        Code(boolean success) {
            this.success = success;
        }

        boolean isSuccess() {
            return success;
        }
    }
}
