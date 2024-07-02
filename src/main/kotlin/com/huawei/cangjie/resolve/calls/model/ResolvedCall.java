package com.huawei.cangjie.resolve.calls.model;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResolvedCall<D extends CallableDescriptor>{
    /**
     * A target callable descriptor as it was accessible in the corresponding scope, i.e. with type arguments not substituted.
     * <p>
     * Note that <b>only type parameters from the declaration itself (i.e. declared type parameters) remain unsubstituted</b>.
     * <p>
     * Type parameters from the other declarations (e.g. a containing class' type parameters) are <b>substituted</b> just as in
     * {@link  ResolvedCall#getResultingDescriptor()}.
     */
    @NotNull
    D getCandidateDescriptor();
    @NotNull
    Call getCall();
    @NotNull
    ResolutionStatus getStatus();

    /** If the target was a member of a class, this is the object of that class to call it on */
    @Nullable
    ReceiverValue getDispatchReceiver();
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
