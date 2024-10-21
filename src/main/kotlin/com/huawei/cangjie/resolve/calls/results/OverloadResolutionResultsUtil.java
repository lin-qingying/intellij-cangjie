package com.huawei.cangjie.resolve.calls.results;


import com.google.common.collect.Lists;
import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.descriptors.ClassConstructorDescriptor;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall;
import com.huawei.cangjie.resolve.calls.model.ResolvedCall;
import com.huawei.cangjie.resolve.calls.tower.NewAbstractResolvedCall;
import com.huawei.cangjie.resolve.calls.tower.NewVariableAsFunctionResolvedCallImpl;
import com.huawei.cangjie.resolve.calls.util.ResolvedCallUtilKt;
import com.huawei.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class OverloadResolutionResultsUtil {
    @NotNull
    @SuppressWarnings("unchecked")
    public static <D extends CallableDescriptor> OverloadResolutionResults<D> ambiguity(OverloadResolutionResults<D> results1, OverloadResolutionResults<D> results2) {
        Collection<MutableResolvedCall<D>> resultingCalls = Lists.newArrayList();
        resultingCalls.addAll((Collection<MutableResolvedCall<D>>) results1.getResultingCalls());
        resultingCalls.addAll((Collection<MutableResolvedCall<D>>) results2.getResultingCalls());
        return OverloadResolutionResultsImpl.ambiguity(resultingCalls);
    }

    @Nullable
    public static <D extends CallableDescriptor> CangJieType getResultingType(
            @NotNull OverloadResolutionResults<D> results,
            @NotNull ResolutionContext<?> context
    ) {
        ResolvedCall<D> resultingCall = getResultingCall(results, context);
        return resultingCall != null ? resultingCall.getResultingDescriptor().getReturnType() : null;
    }

    @Nullable
    public static <D extends CallableDescriptor> ResolvedCall<D> getResultingCall(
            @NotNull OverloadResolutionResults<D> results,
            @NotNull ResolutionContext<?> context
    ) {
        if (results.isSingleResult() && context.contextDependency == ContextDependency.INDEPENDENT) {
            ResolvedCall<D> resultingCall = results.getResultingCall();
            NewAbstractResolvedCall<?> newResolvedCall;
            if (resultingCall instanceof NewVariableAsFunctionResolvedCallImpl) {
                newResolvedCall = ((NewVariableAsFunctionResolvedCallImpl) resultingCall).getFunctionCall();
            }
            else if (resultingCall instanceof NewAbstractResolvedCall<?>) {
                newResolvedCall = (NewAbstractResolvedCall<?>) resultingCall;
            } else {
                newResolvedCall = null;
            }
            if (newResolvedCall != null) {
                if (!ResolvedCallUtilKt.hasInferredReturnType(newResolvedCall) /*&& !(resultingCall.getResultingDescriptor() instanceof ClassConstructorDescriptor)*/) {
                    return null;
                }
            } else if (!((MutableResolvedCall<D>) resultingCall).hasInferredReturnType()) {
                return null;
            }
        }
        return results.isSingleResult() ? results.getResultingCall() : null;
    }
}
