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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.calls.inference.model.ResolvedValueArgument
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType

/**
 * 已解析的调用
 *
 * 表示一个已解析的函数或属性调用，包含所有解析信息，如类型参数、值参数、接收者等。
 *
 * @param D 可调用描述符类型
 */
interface ResolvedCall<out D : CallableDescriptor> {

    /**
     * 候选描述符
     *
     * 目标可调用描述符在相应作用域中的可访问形式，即类型参数未被替换的形式。
     *
     * **注意**：只有声明本身的类型参数（即声明的类型参数）保持未替换状态。
     *
     * 来自其他声明的类型参数（例如包含类的类型参数）会像在 [resultingDescriptor] 中一样被替换。
     */
    val candidateDescriptor: D

    /**
     * 结果描述符
     *
     * 目标可调用描述符，所有类型参数都已替换。
     *
     * 结果描述符不能有任何未替换的类型。但是，描述符的 [CallableDescriptor.getTypeParameters]
     * 保持不变，仍然引用声明的声明类型参数。
     *
     * @see typeArguments
     */
    val resultingDescriptor: D

    /**
     * 调用对象
     *
     * 表示此调用的 PSI 元素
     */
    val call: Call

    /**
     * 解析状态
     *
     * 表示调用解析的状态（成功、失败等）
     */
    val status: ResolutionStatus

    /**
     * 按索引排列的值参数
     *
     * 值参数（实参）按参数索引排列的列表
     */
    val valueArgumentsByIndex: List<ResolvedValueArgument>?

    /**
     * 值参数映射
     *
     * 值参数（实参）到值参数描述符的映射
     */
    val valueArguments: Map<ValueParameterDescriptor, ResolvedValueArgument>

    /**
     * 类型参数映射
     *
     * 类型参数到类型的替换映射
     */
    val typeArguments: Map<TypeParameterDescriptor, CangJieType>

    /**
     * 分发接收者
     *
     * 如果目标是类的成员，这是调用它的对象
     */
    val dispatchReceiver: ReceiverValue?

    /**
     * 扩展接收者
     *
     * 如果目标是扩展函数或属性，这是其接收者参数的值
     */
    val extensionReceiver: ReceiverValue?

    /**
     * 上下文接收者列表
     *
     * 如果目标是具有上下文接收者的函数或属性，这是其上下文接收者参数的值列表
     */
    val contextReceivers: List<ReceiverValue>

    /**
     * 显式接收者类型
     *
     * 确定是用接收者参数还是 this 对象替换显式接收者
     */
    val explicitReceiverKind: ExplicitReceiverKind

    /**
     * 智能转换分发接收者类型
     *
     * 如果分发接收者发生了智能转换，返回智能转换后的类型
     */
    val smartCastDispatchReceiverType: CangJieType?

    /**
     * 参数的数据流信息
     *
     * 每个参数的数据流信息以及结果数据流信息
     */
    val dataFlowInfoForArguments: DataFlowInfoForArguments

    /**
     * 获取参数映射
     *
     * 值参数到参数的映射结果
     *
     * @param valueArgument 值参数
     * @return 参数映射
     */
    fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping
}
