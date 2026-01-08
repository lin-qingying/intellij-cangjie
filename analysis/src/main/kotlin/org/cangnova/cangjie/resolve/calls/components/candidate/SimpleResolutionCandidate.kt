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

package org.cangnova.cangjie.resolve.calls.components.candidate

import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.model.CangJieCallComponents
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCallAtom
import org.cangnova.cangjie.resolve.calls.model.ResolvedAtom
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower
import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * 简单解析候选 (Simple Resolution Candidate)
 *
 * 这是 [ResolutionCandidate] 的标准实现,用于大多数普通的调用解析场景。
 * 相比抽象父类,它提供了具体的子原子管理和变量候选获取实现。
 *
 * ## 基础系统约定
 * [baseSystem] 包含了所有参数的信息,即所有参数约束系统的并集。
 * **按照约定,我们假设 baseSystem 不包含矛盾**,这意味着参数本身的解析是成功的。
 * 如果参数解析失败,应该使用特殊的错误候选而不是普通候选。
 *
 * ## 使用场景
 * 此类适用于:
 * - 普通函数调用
 * - 构造器调用
 * - 属性访问
 * - 操作符重载调用
 *
 * 对于错误描述符(解析失败的情况),应该使用 [SimpleErrorResolutionCandidate]。
 *
 * ## 子原子管理
 * 此类维护一个可变列表来存储嵌套的解析原子,如:
 * - Lambda 表达式的解析结果
 * - 嵌套调用的解析结果
 * - 属性委托的解析结果
 *
 * @property callComponents 调用组件,提供解析所需的工具和服务
 * @property resolutionCallbacks 解析回调,用于报告诊断和记录决策
 * @property scopeTower 作用域塔,提供符号查找能力
 * @property baseSystem 基础约束系统,包含参数的类型约束
 * @property resolvedCall 已解析的调用原子,存储解析结果
 * @property knownTypeParametersResultingSubstitutor 已知类型参数的替换器(可选)
 *
 * @see ResolutionCandidate 抽象父类
 * @see SimpleErrorResolutionCandidate 错误场景的特化版本
 */
open class SimpleResolutionCandidate(
    override val callComponents: CangJieCallComponents,
    override val resolutionCallbacks: CangJieResolutionCallbacks,
    override val scopeTower: ImplicitScopeTower,
    override val baseSystem: ConstraintStorage,
    override val resolvedCall: MutableResolvedCallAtom,
    override val knownTypeParametersResultingSubstitutor: TypeSubstitutor? = null,
) : ResolutionCandidate() {
    /**
     * invoke 调用时的变量候选
     *
     * 当解析 `obj()` 这种调用时,首先需要将 `obj` 解析为变量,
     * 然后在该变量的类型上查找 `invoke` 操作符。
     *
     * 此属性通过无状态回调来查询是否存在对应的变量候选。
     * 如果当前调用不是 invoke 调用,则返回 null。
     */
    override val variableCandidateIfInvoke: ResolutionCandidate?
        get() = callComponents.statelessCallbacks.getVariableCandidateIfInvoke(resolvedCall.atom)

    /**
     * 获取子解析原子列表
     *
     * 返回该候选内部所有嵌套的解析原子。这些原子在解析过程中被添加,
     * 例如当处理 lambda 参数或嵌套调用时。
     *
     * @return 子原子的不可变视图
     */
    override fun getSubResolvedAtoms(): List<ResolvedAtom> = subResolvedAtoms

    /**
     * 添加已解析的仓颉原语
     *
     * 将一个已解析的原子添加到子原子列表中。通常在以下场景调用:
     * - 解析 lambda 表达式时,添加 lambda 的解析结果
     * - 解析嵌套调用时,添加内层调用的解析结果
     * - 解析复杂参数时,添加参数表达式的解析结果
     *
     * @param resolvedAtom 要添加的已解析原子
     */
    override fun addResolvedCjPrimitive(resolvedAtom: ResolvedAtom) {
        subResolvedAtoms.add(resolvedAtom)
    }

    /**
     * 子解析原子存储
     *
     * 可变列表,用于存储嵌套的解析原子。这些原子代表了当前候选内部的
     * 子表达式或嵌套调用的解析结果。
     *
     * 使用 ArrayList 以支持高效的添加操作。
     */
    private var subResolvedAtoms: MutableList<ResolvedAtom> = arrayListOf()
}
