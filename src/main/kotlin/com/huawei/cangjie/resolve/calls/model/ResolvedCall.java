package com.huawei.cangjie.resolve.calls.model;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus;
import org.jetbrains.annotations.NotNull;

public interface ResolvedCall<D extends CallableDescriptor>{

    @NotNull
    ResolutionStatus getStatus();

    /**
     * A target callable descriptor with all type arguments substituted.
     * <p>
     * The resulting descriptor must not have any unsubstituted type. However, the descriptor's
     * {@link CallableDescriptor#getTypeParameters()} are unchanged and still refer to the declaration's declared type parameters.
     *
     * @see ResolvedCall#getTypeArguments()
     */
    @NotNull
    D getResultingDescriptor();
}