/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.resolve.calls.tower.AbstractResolvedCall
import org.cangnova.cangjie.resolve.calls.tower.NewVariableAsFunctionResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.util.hasInferredReturnType
import org.cangnova.cangjie.types.CangJieType

/**
 * 重载解析结果工具类
 *
 * 提供处理函数/方法重载解析结果的工具方法，包括：
 * - 合并多个歧义的解析结果
 * - 获取解析结果的返回类型
 * - 获取最终的调用结果
 */
object OverloadResolutionResultsUtil {
    /**
     * 合并两个重载解析结果为一个歧义结果
     *
     * 当存在多个同样优先级的候选函数时，需要将它们合并为一个歧义结果，
     * 以便后续报告"调用歧义"错误。
     *
     * @param results1 第一个解析结果集
     * @param results2 第二个解析结果集
     * @return 包含所有候选调用的歧义结果
     */
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

    /**
     * 获取重载解析结果的返回类型
     *
     * 从解析结果中提取最终调用的返回类型。如果解析失败或存在歧义，返回 null。
     *
     * @param results 重载解析结果
     * @param context 解析上下文
     * @return 返回类型，如果无法确定则返回 null
     */
    fun <D : CallableDescriptor> getResultingType(
        results: OverloadResolutionResults<D>,
        context: ResolutionContext<*>
    ): CangJieType? {
        val resultingCall = getResultingCall(results, context)
        return resultingCall?.resultingDescriptor?.returnType
    }

    /**
     * 获取重载解析的最终调用结果
     *
     * 从解析结果中提取唯一确定的调用。该方法会进行以下检查：
     * 1. 确保解析结果是唯一的（非歧义）
     * 2. 在独立上下文中，验证返回类型是否已推导
     * 3. 处理"变量作为函数"的特殊情况
     *
     * @param results 重载解析结果
     * @param context 解析上下文，用于判断是否需要类型推导
     * @return 唯一确定的调用结果，如果存在歧义或类型未推导则返回 null
     */
    fun <D : CallableDescriptor> getResultingCall(
        results: OverloadResolutionResults<D>,
        context: ResolutionContext<*>
    ): ResolvedCall<D>? {
        // 仅在结果唯一且上下文独立时进行返回类型检查
        if (results.isSingleResult && context.contextDependency == ContextDependency.INDEPENDENT) {
            val resultingCall = results.resultingCall

            // 提取实际的解析调用对象
            // 对于"变量作为函数"的情况，需要获取其内部的函数调用
            val newResolvedCall: AbstractResolvedCall<*>? = when (resultingCall) {
                is NewVariableAsFunctionResolvedCallImpl -> resultingCall.functionCall
                is AbstractResolvedCall<*> -> resultingCall
                else -> null
            }

            if (newResolvedCall != null) {
                // 检查返回类型是否已推导
                // 构造函数除外，因为需要对 class<T> 进行诊断报告
                if (!newResolvedCall.hasInferredReturnType()
                    // TODO 这里排除构造函数，是为了对class<T>进行诊断报告
                    && resultingCall.resultingDescriptor !is ClassConstructorDescriptor
                ) {
                    return null
                }
            } else if (!(resultingCall as MutableResolvedCall<D>).hasInferredReturnType()) {
                // 对于其他类型的调用，同样检查返回类型是否已推导
                return null
            }
        }

        // 返回唯一结果，如果存在歧义则返回 null
        return if (results.isSingleResult) results.resultingCall else null
    }
}
