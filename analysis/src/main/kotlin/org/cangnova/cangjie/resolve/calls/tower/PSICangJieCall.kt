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
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.CallTransformer
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategyForInvoke
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

/**
 * 安全地将 [CangJieCall] 转换为 [PSICangJieCall]
 *
 * 这个扩展属性用于将通用的 [CangJieCall] 转换为基于 PSI 的具体实现 [PSICangJieCall]。
 * 如果类型转换失败,会抛出断言错误,因为在调用解析系统中,所有调用都应该是 PSI 调用。
 *
 * @throws AssertionError 如果 [CangJieCall] 不是 [PSICangJieCall] 的实例
 * @return 转换后的 [PSICangJieCall] 实例
 */
val CangJieCall.psiCangJieCall: PSICangJieCall
    get() {
        assert(this is PSICangJieCall) {
            "Incorrect ASTCAll: $this. Java class: ${javaClass.canonicalName}"
        }
        return this as PSICangJieCall
    }

/**
 * 基于 PSI 的仓颉调用抽象基类
 *
 * 这个抽象类表示在语法树(PSI)层面上的仓颉语言调用。它扩展了通用的 [CangJieCall] 接口,
 * 添加了与 PSI 元素和数据流分析相关的特定信息。
 *
 * ## 核心职责
 *
 * 1. **PSI 关联**: 维护与 PSI 调用元素的关联 ([psiCall])
 * 2. **数据流跟踪**: 跟踪调用前后的数据流信息,用于类型推导和智能转换
 * 3. **参数数据流**: 为每个参数维护独立的数据流信息
 * 4. **调试追踪**: 通过 [tracingStrategy] 支持调用解析过程的追踪和调试
 *
 * ## 数据流信息
 *
 * - [startingDataFlowInfo]: 调用开始时的数据流状态
 * - [resultDataFlowInfo]: 调用完成后的数据流状态
 * - [dataFlowInfoForArguments]: 每个参数位置的数据流信息
 *
 * ## 子类实现
 *
 * - [PSICangJieCallImpl]: 标准的调用实现
 * - [PSICangJieCallForInvoke]: 用于 invoke 约定的调用
 * - [PSICangJieCallForVariable]: 用于变量/属性访问的调用
 *
 * @see CangJieCall 通用的调用接口
 * @see Call PSI 调用元素
 * @see DataFlowInfo 数据流信息
 */
abstract class PSICangJieCall : CangJieCall {
    /** PSI 调用元素 */
    abstract val psiCall: Call

    /** 调用开始时的数据流信息 */
    abstract val startingDataFlowInfo: DataFlowInfo

    /** 调用结束后的数据流信息 */
    abstract val resultDataFlowInfo: DataFlowInfo

    /** 参数的数据流信息 */
    abstract val dataFlowInfoForArguments: DataFlowInfoForArguments

    /** 追踪策略,用于调试和诊断 */
    abstract val tracingStrategy: TracingStrategy

    override fun toString() = "$psiCall"
}


/**
 * 获取已解析的 PSI 仓颉调用
 *
 * 从绑定上下文中获取已解析的调用实现。这个函数结合了 PSI 调用和绑定追踪,
 * 返回完整的解析结果。
 *
 * @param D 可调用描述符的类型参数
 * @param trace 绑定追踪器,包含解析的绑定信息
 * @return 已解析的调用实现,如果未解析则返回 null
 */
@Suppress("UNCHECKED_CAST")
fun <D : CallableDescriptor> CangJieCall.getResolvedPsiCangJieCall(trace: BindingTrace): ResolvedCallImpl<D>? =
    psiCangJieCall.psiCall.getResolvedCall(trace.bindingContext) as? ResolvedCallImpl<D>


/**
 * 用于 invoke 约定的 PSI 仓颉调用
 *
 * 这个类表示通过 invoke 约定进行的调用。在仓颉语言中,如果一个对象有 `invoke` 方法,
 * 可以像函数一样调用它。例如: `obj()` 实际上会调用 `obj.invoke()`。
 *
 * ## 工作原理
 *
 * 1. **基础调用**: 保持对原始调用 ([baseCall]) 的引用
 * 2. **变量解析**: 存储已解析的变量候选者 ([variableCall])
 * 3. **接收者转换**: 将变量接收者转换为 invoke 方法的接收者
 * 4. **调用转换**: 创建一个新的 PSI 调用,表示 invoke 方法调用
 *
 * ## 接收者处理
 *
 * - [explicitReceiver]: 原始调用的显式接收者
 * - [dispatchReceiverForInvokeExtension]: 用于扩展 invoke 方法的分发接收者
 * - 如果 [dispatchReceiverForInvokeExtension] 为 null,则 [explicitReceiver] 作为变量接收者
 * - 否则,[dispatchReceiverForInvokeExtension] 是变量接收者,[explicitReceiver] 是扩展接收者
 *
 * ## 示例
 *
 * ```cangjie
 * class Adder {
 *     func invoke(x: Int64): Int64 {
 *         return x + 10
 *     }
 * }
 *
 * let adder = Adder()
 * let result = adder(5)  // 通过 invoke 约定调用,等价于 adder.invoke(5)
 * ```
 *
 * @property baseCall 原始的调用实现
 * @property variableCall 已解析的变量候选者
 * @property explicitReceiver 显式接收者参数
 * @property dispatchReceiverForInvokeExtension 用于扩展 invoke 的分发接收者
 *
 * @see PSICangJieCall 基类
 * @see CallTransformer.CallForImplicitInvoke 隐式 invoke 调用的转换器
 * @see TracingStrategyForInvoke invoke 调用的追踪策略
 */
class PSICangJieCallForInvoke(
    val baseCall: PSICangJieCallImpl,
    val variableCall: ResolutionCandidate,
    override val explicitReceiver: ReceiverCangJieCallArgument,
    override val dispatchReceiverForInvokeExtension: SimpleCangJieCallArgument?
) : PSICangJieCall() {
    override val callKind: CangJieCallKind get() = CangJieCallKind.FUNCTION
    override val name: Name get() = OperatorNameConventions.INVOKE
    override val typeArguments: List<TypeArgument> get() = baseCall.typeArguments
    override val topTypeArguments: List<TypeArgument> = baseCall.topTypeArguments
    override val argumentsInParenthesis: List<CangJieCallArgument> get() = baseCall.argumentsInParenthesis
    override val externalArgument: CangJieCallArgument? get() = baseCall.externalArgument

    override val startingDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val resultDataFlowInfo: DataFlowInfo get() = baseCall.resultDataFlowInfo
    override val dataFlowInfoForArguments: DataFlowInfoForArguments get() = baseCall.dataFlowInfoForArguments
    override val psiCall: Call
    override val tracingStrategy: TracingStrategy
    override val isForImplicitInvoke: Boolean = true

    init {
        // 确定变量接收者和扩展接收者
        val variableReceiver = dispatchReceiverForInvokeExtension ?: explicitReceiver
        val explicitExtensionReceiver = if (dispatchReceiverForInvokeExtension == null) null else explicitReceiver
        val calleeExpression = baseCall.psiCall.calleeExpression!!

        // 创建隐式 invoke 调用的 PSI 表示
        psiCall = CallTransformer.CallForImplicitInvoke(
            explicitExtensionReceiver?.receiverValue,
            variableReceiver.receiverValue as ExpressionReceiver, baseCall.psiCall, true
        )

        // 创建 invoke 调用的追踪策略
        tracingStrategy =
            TracingStrategyForInvoke(
                calleeExpression,
                psiCall,
                variableReceiver.receiverValue!!.type
            ) // check for type parameters
    }
}

/**
 * 获取接收者的接收者值
 *
 * 从接收者调用参数中提取实际的接收者值。这个扩展属性处理不同类型的接收者参数,
 * 并返回对应的 [ReceiverValue]。
 *
 * @return 接收者值,如果无法提取则返回 null
 */
val ReceiverCangJieCallArgument.receiverValue: ReceiverValue?
    get() = when (this) {
        is SimpleCangJieCallArgument -> this.receiver.receiverValue
//        is QualifierReceiverCangJieCallArgument -> this.receiver.classValueReceiver
        else -> null
    }

/**
 * 用于变量/属性访问的 PSI 仓颉调用
 *
 * 这个类表示对变量或属性的访问调用。它基于一个函数调用,但去除了参数和类型参数,
 * 将其转换为简单的变量访问。
 *
 * ## 工作原理
 *
 * 1. **基于函数调用**: 从函数调用 ([baseCall]) 派生
 * 2. **去除参数**: 调用没有参数(空的 [argumentsInParenthesis])
 * 3. **去除类型参数**: 调用没有类型参数(空的 [typeArguments])
 * 4. **简化接收者**: 如果没有显式接收者,也会去除接收者
 *
 * ## 使用场景
 *
 * 当 invoke 约定的第一步是解析变量时,需要将原始的函数调用转换为变量访问调用。
 * 例如: `obj()` 首先解析 `obj` 变量,然后再调用其 `invoke` 方法。
 *
 * ## 示例
 *
 * ```cangjie
 * let func = { x: Int64 -> x + 1 }
 * func(5)  // 第一步: 访问变量 func (PSICangJieCallForVariable)
 *          // 第二步: 调用 invoke 方法 (PSICangJieCallForInvoke)
 * ```
 *
 * @property baseCall 原始的函数调用实现
 * @property explicitReceiver 显式接收者,如果没有则为 null
 * @property name 变量或属性的名称
 *
 * @see PSICangJieCall 基类
 * @see CallTransformer.stripCallArguments 去除调用参数的转换器
 * @see CallTransformer.stripReceiver 去除接收者的转换器
 */
class PSICangJieCallForVariable(
    val baseCall: PSICangJieCallImpl,
    override val explicitReceiver: ReceiverCangJieCallArgument?,
    override val name: Name
) : PSICangJieCall() {
    override val callKind: CangJieCallKind get() = CangJieCallKind.VARIABLE
    override val typeArguments: List<TypeArgument> get() = emptyList()
    override val topTypeArguments: List<TypeArgument> = emptyList()
    override val argumentsInParenthesis: List<CangJieCallArgument> get() = emptyList()
    override val externalArgument: CangJieCallArgument? get() = null

    override val startingDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val resultDataFlowInfo: DataFlowInfo get() = baseCall.startingDataFlowInfo
    override val dataFlowInfoForArguments: DataFlowInfoForArguments get() = baseCall.dataFlowInfoForArguments

    override val tracingStrategy: TracingStrategy get() = baseCall.tracingStrategy

    // 转换 PSI 调用:去除参数,如果没有显式接收者也去除接收者
    override val psiCall: Call = CallTransformer.stripCallArguments(baseCall.psiCall).let {
        if (explicitReceiver == null) CallTransformer.stripReceiver(it) else it
    }

    override val isForImplicitInvoke: Boolean get() = false
}

/**
 * 标准的 PSI 仓颉调用实现
 *
 * 这是 [PSICangJieCall] 的标准实现,表示一个完整的函数或方法调用。
 * 它包含了调用解析所需的所有信息:接收者、参数、类型参数、数据流信息等。
 *
 * ## 包含的信息
 *
 * 1. **调用类型**: [callKind] 指示是函数调用、变量访问还是其他类型
 * 2. **PSI 元素**: [psiCall] 关联到语法树中的调用节点
 * 3. **接收者**: [explicitReceiver] 和 [dispatchReceiverForInvokeExtension]
 * 4. **参数**: [argumentsInParenthesis] 和 [externalArgument]
 * 5. **类型参数**: [typeArguments] 和 [topTypeArguments]
 * 6. **数据流**: [startingDataFlowInfo]、[resultDataFlowInfo]、[dataFlowInfoForArguments]
 * 7. **追踪**: [tracingStrategy] 用于调试
 *
 * ## 使用场景
 *
 * 这是最常见的调用表示,用于标准的函数调用、方法调用、构造器调用等。
 *
 * ## 示例
 *
 * ```cangjie
 * // 标准函数调用
 * foo(1, 2)
 *
 * // 方法调用
 * obj.method(arg)
 *
 * // 构造器调用
 * MyClass(param)
 *
 * // 带类型参数的调用
 * genericFunc<Int64>(value)
 * ```
 *
 * @property callKind 调用类型(函数、变量、属性等)
 * @property psiCall PSI 调用元素
 * @property tracingStrategy 追踪策略
 * @property explicitReceiver 显式接收者
 * @property dispatchReceiverForInvokeExtension invoke 扩展的分发接收者
 * @property name 被调用的函数或变量名称
 * @property typeArguments 类型参数列表
 * @property topTypeArguments 顶层类型参数列表
 * @property argumentsInParenthesis 括号内的参数列表
 * @property externalArgument 外部参数(如 lambda 参数)
 * @property startingDataFlowInfo 调用开始时的数据流信息
 * @property resultDataFlowInfo 调用结束后的数据流信息
 * @property dataFlowInfoForArguments 参数的数据流信息
 * @property isForImplicitInvoke 是否用于隐式 invoke 调用
 *
 * @see PSICangJieCall 基类
 * @see CangJieCallKind 调用类型枚举
 */
class PSICangJieCallImpl(
    override val callKind: CangJieCallKind,
    override val psiCall: Call,
    override val tracingStrategy: TracingStrategy,
    override val explicitReceiver: ReceiverCangJieCallArgument?,
    override val dispatchReceiverForInvokeExtension: ReceiverCangJieCallArgument?,
    override val name: Name,
    override val typeArguments: List<TypeArgument>,
    override val topTypeArguments: List<TypeArgument>,
    override val argumentsInParenthesis: List<CangJieCallArgument>,
    override val externalArgument: CangJieCallArgument?,
    override val startingDataFlowInfo: DataFlowInfo,
    override val resultDataFlowInfo: DataFlowInfo,
    override val dataFlowInfoForArguments: DataFlowInfoForArguments,
    override val isForImplicitInvoke: Boolean
) : PSICangJieCall()
