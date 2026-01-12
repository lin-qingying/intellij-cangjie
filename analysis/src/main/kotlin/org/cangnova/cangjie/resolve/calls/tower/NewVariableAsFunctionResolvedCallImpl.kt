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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.model.CangJieCallDiagnostic
import org.cangnova.cangjie.resolve.calls.model.ResolvedCallAtom
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.TypeApproximator

/**
 * 变量作为函数的已解析调用实现
 *
 * 这个类处理将变量当作函数调用的特殊情况，例如调用重载了 invoke 操作符的对象。
 * 它包含两个调用：
 * 1. variableCall - 解析变量本身的调用
 * 2. functionCall - 解析 invoke 函数的调用
 *
 * 这个类使用委托模式，将大部分调用委托给 functionCall。
 *
 * @property variableCall 变量调用，解析变量本身
 * @property functionCall 函数调用，解析 invoke 操作符
 */
class NewVariableAsFunctionResolvedCallImpl(
    override val variableCall: AbstractResolvedCall<VariableDescriptor>,
    override val functionCall: AbstractResolvedCall<FunctionDescriptor>,
) : VariableAsFunctionResolvedCall, AbstractResolvedCall<FunctionDescriptor>() {

    /** 基础调用，从 invoke 调用中提取的原始 PSI 调用 */
    val baseCall: PSICangJieCallImpl = (functionCall.psiCangJieCall as PSICangJieCallForInvoke).baseCall

    // ========== 从父类实现的属性（委托给 functionCall）==========

    override val resolvedCallAtom: ResolvedCallAtom? = functionCall.resolvedCallAtom
    override val psiCangJieCall: PSICangJieCall = functionCall.psiCangJieCall
    override val typeApproximator: TypeApproximator = functionCall.typeApproximator
    override val freshSubstitutor: ComposableTypeSubstitutor? = functionCall.freshSubstitutor
    override val cangjieCall = functionCall.cangjieCall
    override val languageVersionSettings = functionCall.languageVersionSettings
    override val argumentMappingByOriginal = functionCall.argumentMappingByOriginal
    override val diagnostics: Collection<CangJieCallDiagnostic> = functionCall.diagnostics

    // ========== 从接口实现的属性（委托给 functionCall）==========

    /**
     * 解析状态
     * 委托给函数调用的状态
     */
    override val status
        get() = functionCall.status


    /**
     * 类型参数映射
     * 委托给函数调用的类型参数
     */
    override val typeArguments
        get() = functionCall.typeArguments

    /**
     * 候选描述符
     * 委托给函数调用的候选描述符
     */
    override val candidateDescriptor
        get() = functionCall.candidateDescriptor

    /**
     * 智能转换调度接收者类型
     * 委托给函数调用的智能转换类型
     */
    override val smartCastDispatchReceiverType
        get() = functionCall.smartCastDispatchReceiverType

    /**
     * 显式接收者类型
     * 委托给函数调用的显式接收者类型
     */
    override val explicitReceiverKind
        get() = functionCall.explicitReceiverKind



    /**
     * 结果描述符
     * 委托给函数调用的结果描述符
     */
    override val resultingDescriptor
        get() = functionCall.resultingDescriptor

    /**
     * 调度接收者
     * 委托给函数调用的调度接收者
     */
    override val dispatchReceiver
        get() = functionCall.dispatchReceiver

    // ========== 方法实现（委托给 functionCall）==========

    /**
     * 更新调度接收者类型
     * 委托给函数调用
     */
    override fun updateDispatchReceiverType(newType: CangJieType) = functionCall.updateDispatchReceiverType(newType)

    /**
     * 构建参数到参数描述符的映射
     * 委托给函数调用
     */
    override fun argumentToParameterMap(
        resultingDescriptor: CallableDescriptor,
        valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>
    ) = functionCall.argumentToParameterMap(resultingDescriptor, valueArguments)


    /**
     * 设置结果替换器
     * 同时更新函数调用和变量调用的替换器
     */
    override fun setResultingSubstitutor(substitutor: ComposableTypeSubstitutor?) {
        functionCall.setResultingSubstitutor(substitutor)
        variableCall.setResultingSubstitutor(substitutor)
    }
}

