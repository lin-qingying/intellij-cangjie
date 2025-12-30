/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ClassConstructorDescriptor
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.tower.NewAbstractResolvedCall
import org.cangnova.cangjie.resolve.calls.tower.NewVariableAsFunctionResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.util.hasInferredReturnType
import org.cangnova.cangjie.types.CangJieType

object OverloadResolutionResultsUtil {
    @Suppress("UNCHECKED_CAST")
    fun <D : CallableDescriptor> ambiguity(
        results1: OverloadResolutionResults<D>,
        results2: OverloadResolutionResults<D>
    ): OverloadResolutionResults<D> {
        val resultingCalls = mutableListOf<MutableResolvedCall<D>>()
        resultingCalls.addAll(results1.resultingCalls as Collection<MutableResolvedCall<D>>)
        resultingCalls.addAll(results2.resultingCalls as Collection<MutableResolvedCall<D>>)
        return OverloadResolutionResultsImpl.ambiguity(resultingCalls)
    }

    fun <D : CallableDescriptor> getResultingType(
        results: OverloadResolutionResults<D>,
        context: ResolutionContext<*>
    ): CangJieType? {
        val resultingCall = getResultingCall(results, context)
        return resultingCall?.resultingDescriptor?.returnType
    }

    fun <D : CallableDescriptor> getResultingCall(
        results: OverloadResolutionResults<D>,
        context: ResolutionContext<*>
    ): ResolvedCall<D>? {
        if (results.isSingleResult && context.contextDependency == ContextDependency.INDEPENDENT) {
            val resultingCall = results.resultingCall
            val newResolvedCall: NewAbstractResolvedCall<*>? = when (resultingCall) {
                is NewVariableAsFunctionResolvedCallImpl -> resultingCall.functionCall
                is NewAbstractResolvedCall<*> -> resultingCall
                else -> null
            }

            if (newResolvedCall != null) {
                if (!newResolvedCall.hasInferredReturnType()
                    // TODO 这里排除构造函数，是为了对class<T>进行诊断报告
                    && resultingCall.resultingDescriptor !is ClassConstructorDescriptor
                ) {
                    return null
                }
            } else if (!(resultingCall as MutableResolvedCall<D>).hasInferredReturnType()) {
                return null
            }
        }
        return if (results.isSingleResult) results.resultingCall else null
    }
}
