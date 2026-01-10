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

package org.cangnova.cangjie.resolve.calls.components.candidate


import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.resolve.calls.components.CallableReceiver
import org.cangnova.cangjie.resolve.calls.components.CallableReferenceAdaptation
import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.inference.components.FreshVariableTypeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.model.*
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.types.DefaultTypeSubstitutor
import org.cangnova.cangjie.types.UnwrappedType


/**
 * 可调用引用解析候选
 *
 * 表示可调用引用（如函数引用、属性引用）的解析候选项。
 * 可调用引用是一种特殊的表达式，它引用一个函数或属性而不立即调用它。
 *
 * 例如：
 * - `::functionName` - 函数引用
 * - `ClassName::memberFunction` - 成员函数引用
 * - `instance::memberFunction` - 绑定引用
 * - `::propertyName` - 属性引用
 *
 * 此类封装了解析可调用引用所需的所有信息，包括：
 * - 被引用的可调用对象（函数、属性等）
 * - 接收者信息（调度接收者和显式接收者）
 * - 反射类型（KFunction、KProperty 等）
 * - 适配信息（用于处理参数默认值等）
 *
 * @property candidate 被引用的可调用描述符（函数、属性等）
 * @property dispatchReceiver 调度接收者（对于成员引用，这是所属类的实例）
 * @property explicitReceiverKind 显式接收者的种类（无、分发、扩展等）
 * @property reflectionCandidateType 反射候选类型（KFunction、KProperty 等类型）
 * @property callableReferenceAdaptation 可调用引用适配信息（如默认参数、协变返回类型等）
 * @property cangjieCall 仓颉可调用引用调用的解析原子
 * @property expectedType 期望类型（从上下文推断的类型）
 * @property callComponents 调用组件，提供解析所需的各种服务
 * @property scopeTower 隐式作用域塔，用于名称查找
 * @property resolutionCallbacks 解析回调，用于报告解析进度和结果
 * @property baseSystem 基础约束存储系统
 */
class CallableReferenceResolutionCandidate(
    val candidate: CallableDescriptor,
    val dispatchReceiver: CallableReceiver?,
    val explicitReceiverKind: ExplicitReceiverKind,
    val reflectionCandidateType: UnwrappedType,
    val callableReferenceAdaptation: CallableReferenceAdaptation?,
    val cangjieCall: CallableReferenceResolutionAtom,
    val expectedType: UnwrappedType?,
    override val callComponents: CangJieCallComponents,
    override val scopeTower: ImplicitScopeTower,
    override val resolutionCallbacks: CangJieResolutionCallbacks,
    override val baseSystem: ConstraintStorage?
) : ResolutionCandidate() {
    /**
     * invoke 的变量候选
     *
     * 对于可调用引用，不存在 invoke 的变量候选，始终为 null。
     * 此属性用于区分直接调用和通过变量的 invoke 操作符调用。
     */
    override val variableCandidateIfInvoke: ResolutionCandidate? = null

    /**
     * 已知类型参数的结果替换器
     *
     * 可调用引用的右侧不包含类型参数，因此此属性始终为 null。
     * 类型参数的推断发生在引用本身的使用点，而不是在被引用的可调用对象上。
     */
    override val knownTypeParametersResultingSubstitutor: DefaultTypeSubstitutor? =
        null // 可调用引用的右侧没有类型参数

    /**
     * 已解析的调用原子
     *
     * 创建一个已解析的可调用引用调用原子，包含：
     * - 原始调用信息
     * - 解析得到的候选描述符
     * - 接收者信息
     * - 反射类型
     * - 对此候选的引用
     */
    override val resolvedCall = ResolvedCallableReferenceCallAtom(
        cangjieCall.call, candidate, explicitReceiverKind,
        if (dispatchReceiver != null) ReceiverExpressionCangJieCallArgument(dispatchReceiver.receiver) else null,
        reflectionCandidateType,
        candidate = this
    )

    /**
     * 添加已解析的仓颉原语
     *
     * 可调用引用没有嵌套的已解析原语，因此此方法为空实现。
     * 这与普通函数调用不同，后者可能包含 Lambda 参数等嵌套的解析原语。
     *
     * @param resolvedAtom 已解析的原子（未使用）
     */
    override fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom) {}
        // 可调用引用没有嵌套的已解析原语


    /**
     * 获取子解析原子
     *
     * 可调用引用不包含子解析原子（如 Lambda、内部调用等），返回空列表。
     *
     * @return 空列表
     */
    override fun getSubResolvedAtoms(): List<ResolvedAtom> = emptyList()

    /**
     * 新鲜类型变量替换器
     *
     * 用于类型推断过程中的类型变量替换。
     * 当解析可调用引用时，可能需要引入新的类型变量来表示泛型类型参数。
     *
     * 此属性由类型推断系统内部设置。
     */
    var freshVariablesSubstitutor: FreshVariableTypeSubstitutor? = null
        internal set

    /**
     * 默认参数的数量
     *
     * 返回此可调用引用适配中使用的默认参数数量。
     * 当可调用引用被适配以匹配期望的函数类型时，可能会使用默认参数。
     *
     * 例如，如果引用一个有3个参数的函数，但期望类型是只接受2个参数的函数类型，
     * 可能会使用默认参数来适配。
     *
     * @return 默认参数数量，如果没有适配则返回 0
     */
    val numDefaults get() = callableReferenceAdaptation?.defaults ?: 0
}
