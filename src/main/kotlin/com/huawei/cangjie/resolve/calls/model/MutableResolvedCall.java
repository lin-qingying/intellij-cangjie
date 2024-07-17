package com.huawei.cangjie.resolve.calls.model;


import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.psi.ValueArgument;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeSubstitutor;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MutableResolvedCall<D extends CallableDescriptor> extends ResolvedCall<D>  {
//
//    void addStatus(@NotNull ResolutionStatus status);
//
//    void setStatusToSuccess();
//
//    @NotNull
//    DelegatingBindingTrace getTrace();
//
//    @NotNull
//    TracingStrategy getTracingStrategy();
//
//    void markCallAsCompleted();
//
//    void addRemainingTasks(Function0<Unit> task);
//
//    void performRemainingTasks();
//
//    bool isCompleted();
//
//
//    void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument);
//
//    void recordArgumentMatchStatus(@NotNull ValueArgument valueArgument, @NotNull ArgumentMatchStatus matchStatus);
//
//    @Override
//    @NotNull
//    MutableDataFlowInfoForArguments getDataFlowInfoForArguments();
//
//    @Nullable
//    ConstraintSystem getConstraintSystem();
//
//    void setConstraintSystem(@NotNull ConstraintSystem constraintSystem);
//
//    void setSubstitutor(@NotNull TypeSubstitutor substitutor);
//
//    @Nullable
//    TypeSubstitutor getKnownTypeParametersSubstitutor();
//
//    //todo remove: use value to parameter map status
    boolean hasInferredReturnType();
//
//    void setSmartCastDispatchReceiverType(@NotNull CangJieType smartCastDispatchReceiverType);
//
//    void updateExtensionReceiverWithSmartCastIfNeeded(@NotNull CangJieType smartCastExtensionReceiverType);
}
