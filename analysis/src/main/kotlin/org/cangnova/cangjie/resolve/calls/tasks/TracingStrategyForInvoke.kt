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

package org.cangnova.cangjie.resolve.calls.tasks

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.types.CangJieType
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.infos.errors.FUNCTION_EXPECTED
import org.cangnova.cangjie.diagnostics.infos.errors.NO_RECEIVER_ALLOWED
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.isFunctionType

/**
 * Invoke 调用的追踪策略
 *
 * 用于处理函数类型变量的调用（invoke 调用）的绑定和错误报告。
 * 例如：当 `foo` 是一个函数类型的变量时，`foo(a, b)` 实际上是 `foo.invoke(a, b)` 的语法糖。
 *
 * 主要职责：
 * - 区分"变量作为函数调用"和普通的 invoke 调用
 * - 绑定调用信息到 BindingContext
 * - 处理未解析引用的错误报告
 *
 * @property calleeType 被调用对象的类型
 */
class TracingStrategyForInvoke(
    reference: CjExpression,
    call: Call,
    private val calleeType: CangJieType
) : AbstractTracingStrategy(reference, call) {
    /**
     * 绑定调用信息
     *
     * 将调用信息记录到 BindingTrace 中。
     * 特殊处理：如果引用是简单名称表达式（如 `foo(a, b)` 中的 `foo`），
     * 则不绑定 invoke 调用，因为外层调用已经被绑定。
     *
     * @param trace 绑定追踪器
     * @param call 调用对象
     */
    override fun bindCall(trace: BindingTrace, call: Call) {
        // 如果引用是简单名称，这是"变量作为函数调用"的情况（例如 `foo(a, b)`，其中 `foo` 是变量）
        // 外层调用已被绑定（`foo(a, b)`），而此情况下的 invoke 调用是 `foo.invoke(a, b)`，不应该被绑定
        if (reference is CjSimpleNameExpression) return
        trace.record(BindingContext.CALL, reference, call)
    }

    /**
     * 绑定引用目标
     *
     * 将引用表达式绑定到其解析的候选描述符。
     * 仅当调用元素是引用表达式时才进行绑定。
     *
     * @param trace 绑定追踪器
     * @param resolvedCall 已解析的调用
     */
    override fun <D : CallableDescriptor> bindReference(
        trace: BindingTrace, resolvedCall: ResolvedCall<D>
    ) {
        val callElement: PsiElement = call.callElement
        if (callElement is CjReferenceExpression) {
            trace.record(BindingContext.REFERENCE_TARGET, callElement, resolvedCall.candidateDescriptor)
        }
    }

    /**
     * 绑定已解析的调用
     *
     * 将已解析的调用信息记录到 BindingTrace 中。
     * 特殊处理：如果引用是简单名称表达式，则不绑定，因为外层调用已经被绑定。
     *
     * @param trace 绑定追踪器
     * @param resolvedCall 已解析的调用
     */
    override fun <D : CallableDescriptor> bindResolvedCall(
        trace: BindingTrace, resolvedCall: ResolvedCall<D>
    ) {
        if (reference is CjSimpleNameExpression) return
        trace.record(BindingContext.RESOLVED_CALL, call, resolvedCall)
    }

    /**
     * 处理未解析的引用
     *
     * 当引用无法解析时，报告相应的错误。
     * 根据被调用对象的类型，报告"期望函数类型"或"不允许接收者"错误。
     *
     * @param trace 绑定追踪器
     */
    override fun unresolvedReference(trace: BindingTrace) {
        functionExpectedOrNoReceiverAllowed(trace)
    }

    /**
     * 处理接收者错误的未解析引用
     *
     * 当存在候选项但接收者类型不匹配时，报告相应的错误。
     * 根据被调用对象的类型，报告"期望函数类型"或"不允许接收者"错误。
     *
     * @param trace 绑定追踪器
     * @param candidates 候选的已解析调用集合
     */
    override fun <D : CallableDescriptor> unresolvedReferenceWrongReceiver(
        trace: BindingTrace, candidates: Collection<ResolvedCall<D>>
    ) {
        functionExpectedOrNoReceiverAllowed(trace)
    }


    /**
     * 报告"期望函数类型"或"不允许接收者"错误
     *
     * 根据被调用对象的类型决定报告哪种错误：
     * - 如果是函数类型：报告"不允许接收者"错误（仓颉没有扩展函数类型）
     * - 如果不是函数类型：报告"期望函数类型"错误
     *
     * @param trace 绑定追踪器
     */
    private fun functionExpectedOrNoReceiverAllowed(trace: BindingTrace) {
        // 仓颉没有扩展函数类型，所有函数类型都是非扩展的
        if (calleeType.isFunctionType) {
            trace.report(NO_RECEIVER_ALLOWED.on(reference))
        } else {
            trace.report(FUNCTION_EXPECTED.on(reference, reference, calleeType))
        }
    }
}
