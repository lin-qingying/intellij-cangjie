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

package cn.cangnova.cangjie.resolve.calls.results;


import com.google.common.collect.Lists;
import cn.cangnova.cangjie.descriptors.CallableDescriptor;
import cn.cangnova.cangjie.descriptors.ClassConstructorDescriptor;
import cn.cangnova.cangjie.resolve.calls.context.ContextDependency;
import cn.cangnova.cangjie.resolve.calls.context.ResolutionContext;
import cn.cangnova.cangjie.resolve.calls.model.MutableResolvedCall;
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall;
import cn.cangnova.cangjie.resolve.calls.tower.NewAbstractResolvedCall;
import cn.cangnova.cangjie.resolve.calls.tower.NewVariableAsFunctionResolvedCallImpl;
import cn.cangnova.cangjie.resolve.calls.util.ResolvedCallUtilKt;
import cn.cangnova.cangjie.types.CangJieType;
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
                if (!ResolvedCallUtilKt.hasInferredReturnType(newResolvedCall)

//                    TODO 这里排除构造函数，是为了对class<T>进行诊断报告
                    && !(resultingCall.getResultingDescriptor() instanceof ClassConstructorDescriptor)

                ) {
                    return null;
                }
            } else if (!((MutableResolvedCall<D>) resultingCall).hasInferredReturnType()) {
                return null;
            }
        }
        return results.isSingleResult() ? results.getResultingCall() : null;
    }
}
