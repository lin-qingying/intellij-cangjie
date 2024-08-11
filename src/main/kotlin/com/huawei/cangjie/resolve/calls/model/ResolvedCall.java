package com.huawei.cangjie.resolve.calls.model;

import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.TypeParameterDescriptor;
import com.huawei.cangjie.descriptors.ValueParameterDescriptor;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.ValueArgument;
import com.huawei.cangjie.resolve.calls.inference.model.ResolvedValueArgument;
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus;
import com.huawei.cangjie.resolve.calls.tasks.ExplicitReceiverKind;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface ResolvedCall<D extends CallableDescriptor> {
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

    /**
     * Values (arguments) for value parameters indexed by parameter index
     */
    @Nullable
    List<ResolvedValueArgument> getValueArgumentsByIndex();

    @NotNull
    Call getCall();

    @Nullable
    CangJieType getSmartCastDispatchReceiverType();

    /**
     * Determines whether receiver argument or this object is substituted for explicit receiver
     */
    @NotNull
    ExplicitReceiverKind getExplicitReceiverKind();

    /**
     * The result of mapping the value argument to a parameter
     */
    @NotNull
    ArgumentMapping getArgumentMapping(@NotNull ValueArgument valueArgument);

    /**
     * Values (arguments) for value parameters
     */
    @NotNull
    Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments();

    /**
     * If the target was an extension function or property, this is the value for its receiver parameter
     */
    @Nullable
    ReceiverValue getExtensionReceiver();

    @NotNull
    ResolutionStatus getStatus();

    /**
     * If the target was a function or property with context receivers, this is the value for its context receiver parameters
     */
    @NotNull
    List<ReceiverValue> getContextReceivers();

    /**
     * Data flow info for each argument and the result data flow info
     */
    @NotNull
    DataFlowInfoForArguments getDataFlowInfoForArguments();

    /**
     * What's substituted for type parameters
     */
    @NotNull
    Map<TypeParameterDescriptor, CangJieType> getTypeArguments();

    /**
     * If the target was a member of a class, this is the object of that class to call it on
     */
    @Nullable
    ReceiverValue getDispatchReceiver();
    /** If the target was an extension function or property, this is the value for its receiver parameter */
//    @Nullable
//    ReceiverValue getExtensionReceiver();

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
