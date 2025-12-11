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

import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults.Code
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall

/**
 * 重载解析结果实现类
 *
 * 该类是 [OverloadResolutionResults] 接口的具体实现，用于封装函数或操作符重载解析的结果。
 * 它包含了解析状态码、候选调用集合以及所有候选者信息。
 *
 * 该类通过私有构造函数确保只能通过伴生对象的工厂方法创建实例，提供了类型安全和
 * 不可变性保证。
 *
 * @param D 可调用描述符的类型，必须是 [CallableDescriptor] 的子类型
 * @property _resultCode 解析结果状态码
 * @property results 解析后得到的候选调用集合
 *
 * @see OverloadResolutionResults
 * @see MutableResolvedCall
 */
class OverloadResolutionResultsImpl<D : CallableDescriptor> private constructor(
    private val _resultCode: OverloadResolutionResults.Code,
    private val results: Collection<MutableResolvedCall<D>>
) : OverloadResolutionResults<D> {

    /**
     * 所有候选者的内部存储
     *
     * 注意：该字段为可变的，可以通过 [setAllCandidates] 方法设置
     */
    private var _allCandidates: Collection<ResolvedCall<D>>? = null

    /**
     * 替换结果代码
     *
     * 创建一个新的 [OverloadResolutionResultsImpl] 实例，使用指定的新代码替换当前结果代码，
     * 但保留原有的候选调用集合。
     *
     * @param newCode 新的结果代码
     * @return 使用新代码的结果对象
     */
    override fun replaceCode(newCode: Code): OverloadResolutionResultsImpl<D> =
        OverloadResolutionResultsImpl(newCode, results)

    /**
     * 获取所有候选者
     *
     * 返回所有参与解析的候选调用。只有在解析上下文设置了收集所有候选者时，
     * 该属性才会有值。
     *
     * @return 所有候选调用的集合，如果未收集则返回 null
     */
    override val allCandidates: Collection<ResolvedCall<D>>?
        get() = _allCandidates

    /**
     * 设置所有候选者
     *
     * 用于在解析完成后设置所有参与解析的候选者集合。通常在收集所有候选者模式下使用。
     *
     * @param allCandidates 所有候选调用的集合，可以为 null
     */
    fun setAllCandidates(allCandidates: Collection<ResolvedCall<D>>?) {
        this._allCandidates = allCandidates
    }

    /**
     * 获取结果调用集合
     *
     * 返回解析后的有效候选调用集合。如果解析成功且无歧义，集合只包含一个元素；
     * 如果存在歧义，集合包含多个同样合适的候选者。
     *
     * @return 结果调用的可变集合（非空，但可以为空集合）
     */
    override val resultingCalls: Collection<MutableResolvedCall<D>>
        get() = results

    /**
     * 获取单个结果调用
     *
     * 当解析结果为单一候选者时，返回该候选调用。如果有多个候选者或没有候选者，
     * 会抛出 [IllegalStateException] 异常。
     *
     * @return 单个结果调用
     * @throws IllegalStateException 如果不是单一结果
     */
    override val resultingCall: MutableResolvedCall<D>
        get() {
            check(isSingleResult)
            return results.iterator().next()
        }

    /**
     * 获取结果描述符
     *
     * 返回最终选定的可调用描述符。该属性依赖于 [resultingCall]，
     * 因此只能在单一结果的情况下使用。
     *
     * @return 可调用描述符
     * @throws IllegalStateException 如果不是单一结果
     */
    override val resultingDescriptor: D
        get() = resultingCall.resultingDescriptor

    /**
     * 获取结果代码
     *
     * 返回表示解析结果状态的代码，如成功、失败原因等。
     *
     * @return 结果状态码
     * @see OverloadResolutionResults.Code
     */
    override val resultCode: Code
        get() = _resultCode

    /**
     * 判断解析是否成功
     *
     * 基于结果代码判断重载解析是否成功找到了合适的候选者。
     *
     * @return 如果解析成功返回 true，否则返回 false
     */
    override val isSuccess: Boolean
        get() = _resultCode.isSuccess

    /**
     * 判断是否为单一结果
     *
     * 检查是否只有一个有效的候选者，且该候选者不是因为接收者错误而被选中的。
     *
     * @return 如果只有一个有效候选者返回 true，否则返回 false
     */
    override val isSingleResult: Boolean
        get() = results.size == 1 && resultCode != Code.CANDIDATES_WITH_WRONG_RECEIVER

    /**
     * 判断是否没有找到任何候选者
     *
     * 检查解析结果是否为未找到名称，即没有找到任何匹配的函数或操作符。
     *
     * @return 如果没有找到任何候选者返回 true，否则返回 false
     */
    override val isNothing: Boolean
        get() = _resultCode == Code.NAME_NOT_FOUND

    /**
     * 判断是否存在歧义
     *
     * 检查是否有多个同样合适的候选者，导致无法确定应该选择哪一个。
     *
     * @return 如果存在歧义返回 true，否则返回 false
     */
    override val isAmbiguity: Boolean
        get() = _resultCode == Code.AMBIGUITY

    /**
     * 判断解析是否不完整
     *
     * 检查类型推断是否不完整，即无法完全推断出所有类型参数。
     *
     * @return 如果类型推断不完整返回 true，否则返回 false
     */
    override val isIncomplete: Boolean
        get() = _resultCode == Code.INCOMPLETE_TYPE_INFERENCE

    companion object {
        fun <D : CallableDescriptor> incompleteTypeInference(candidate: MutableResolvedCall<D>): OverloadResolutionResultsImpl<D> =
            incompleteTypeInference(setOf(candidate))

        fun <D : CallableDescriptor> incompleteTypeInference(candidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.INCOMPLETE_TYPE_INFERENCE, candidates)

        fun <D : CallableDescriptor> ambiguity(candidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.AMBIGUITY, candidates)

        fun <D : CallableDescriptor> candidatesWithWrongReceiver(failedCandidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.CANDIDATES_WITH_WRONG_RECEIVER, failedCandidates)

        fun <D : CallableDescriptor> singleFailedCandidate(candidate: MutableResolvedCall<D>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(
                OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH,
                setOf(candidate)
            )

        fun <D : CallableDescriptor> manyFailedCandidates(failedCandidates: Collection<MutableResolvedCall<D>>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES, failedCandidates)

        fun <D : CallableDescriptor> nameNotFound(): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl<D>(OverloadResolutionResults.Code.NAME_NOT_FOUND, emptyList()).apply {
                setAllCandidates(emptyList())
            }

        fun <D : CallableDescriptor> success(candidate: MutableResolvedCall<D>): OverloadResolutionResultsImpl<D> =
            OverloadResolutionResultsImpl(Code.SUCCESS, setOf(candidate))
    }
}
