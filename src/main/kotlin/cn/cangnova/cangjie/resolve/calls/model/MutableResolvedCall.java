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

package cn.cangnova.cangjie.resolve.calls.model;


import cn.cangnova.cangjie.descriptors.CallableDescriptor;
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor;
import cn.cangnova.cangjie.psi.ValueArgument;
import cn.cangnova.cangjie.resolve.DelegatingBindingTrace;
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystem;
import cn.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument;
import cn.cangnova.cangjie.resolve.calls.results.ResolutionStatus;
import cn.cangnova.cangjie.resolve.calls.tasks.TracingStrategy;
import cn.cangnova.cangjie.types.CangJieType;
import cn.cangnova.cangjie.types.TypeSubstitutor;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MutableResolvedCall<D extends CallableDescriptor> extends ResolvedCall<D>  {

    void addStatus(@NotNull ResolutionStatus status);


    void setStatusToSuccess();

    @NotNull
    DelegatingBindingTrace getTrace();

    @NotNull
    TracingStrategy getTracingStrategy();

    void markCallAsCompleted();

    void addRemainingTasks(Function0<Unit> task);

    void performRemainingTasks();

    boolean isCompleted();


    void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument);

    void recordArgumentMatchStatus(@NotNull ValueArgument valueArgument, @NotNull ArgumentMatchStatus matchStatus);

    @Override
    @NotNull
    MutableDataFlowInfoForArguments getDataFlowInfoForArguments();

    @Nullable
    ConstraintSystem getConstraintSystem();

    void setConstraintSystem(@NotNull ConstraintSystem constraintSystem);

    void setSubstitutor(@NotNull TypeSubstitutor substitutor);

    @Nullable
    TypeSubstitutor getKnownTypeParametersSubstitutor();

//    //todo remove: use value to parameter map status
    boolean hasInferredReturnType();

    void setSmartCastDispatchReceiverType(@NotNull CangJieType smartCastDispatchReceiverType);

    void updateExtensionReceiverWithSmartCastIfNeeded(@NotNull CangJieType smartCastExtensionReceiverType);
}
