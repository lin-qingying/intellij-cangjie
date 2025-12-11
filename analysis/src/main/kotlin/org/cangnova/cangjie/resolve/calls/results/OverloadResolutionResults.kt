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
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall

/**
 * 重载解析结果接口
 *
 * 表示函数或操作符重载解析的结果，包含所有候选者、最终选择的调用等信息。
 *
 * @param D 可调用描述符的类型，必须是 CallableDescriptor 的子类型
 */
interface OverloadResolutionResults<D : CallableDescriptor> {
    /**
     * 获取所有候选者
     *
     * 注意：只有在 ResolutionContext.collectAllCandidates 设置为 true 时才会收集所有候选者
     *
     * @return 所有候选调用的集合，如果未收集则返回 null
     */
    val allCandidates: Collection<ResolvedCall<D>>?

    /**
     * 获取结果调用集合
     *
     * 返回解析后的所有有效调用（可能包含多个，表示存在歧义）
     *
     * @return 结果调用的集合（非空）
     */
    val resultingCalls: Collection<ResolvedCall<D>>

    /**
     * 获取单个结果调用
     *
     * 当只有一个有效候选者时返回该调用。如果有多个候选者，会抛出异常或返回第一个。
     *
     * @return 结果调用（非空）
     */
    val resultingCall: ResolvedCall<D>

    /**
     * 获取结果描述符
     *
     * 返回最终选定的可调用描述符
     *
     * @return 可调用描述符（非空）
     */
    val resultingDescriptor: D

    /**
     * 获取结果代码
     *
     * 返回表示解析结果状态的代码（成功、失败原因等）
     *
     * @return 结果代码（非空）
     */
    val resultCode: Code

    /**
     * 判断解析是否成功
     *
     * @return 如果解析成功返回 true，否则返回 false
     */
    val isSuccess: Boolean

    /**
     * 判断是否为单一结果
     *
     * @return 如果只有一个有效候选者返回 true，否则返回 false
     */
    val isSingleResult: Boolean

    /**
     * 判断是否没有找到任何候选者
     *
     * @return 如果没有找到任何候选者返回 true，否则返回 false
     */
    val isNothing: Boolean

    /**
     * 判断是否存在歧义
     *
     * @return 如果有多个同样合适的候选者返回 true，否则返回 false
     */
    val isAmbiguity: Boolean

    /**
     * 判断解析是否不完整
     *
     * @return 如果类型推断不完整返回 true，否则返回 false
     */
    val isIncomplete: Boolean

    /**
     * 替换结果代码
     *
     * 创建一个新的结果对象，使用指定的新代码替换当前代码
     *
     * @param newCode 新的结果代码
     * @return 替换代码后的新结果对象（默认返回当前对象）
     */
    fun replaceCode(newCode: Code): OverloadResolutionResults<D> = this

    /**
     * 重载解析结果代码枚举
     *
     * 表示重载解析过程的各种结果状态
     */
    enum class Code(val isSuccess: Boolean) {
        /** 操作成功 - 找到唯一的匹配候选者 */
        SUCCESS(true),

        /** 未找到名称 - 没有找到指定名称的函数或操作符 */
        NAME_NOT_FOUND(false),

        /** 单一候选者参数不匹配 - 只有一个候选者，但参数类型不匹配 */
        SINGLE_CANDIDATE_ARGUMENT_MISMATCH(false),

        /** 存在歧义 - 有多个同样合适的候选者，无法确定选择哪一个 */
        AMBIGUITY(false),

        /** 多个候选者失败 - 有多个候选者，但都不满足条件 */
        MANY_FAILED_CANDIDATES(false),

        /** 候选者接收者错误 - 候选者的接收者类型不匹配 */
        CANDIDATES_WITH_WRONG_RECEIVER(false),

        /** 类型推断不完整 - 无法完全推断出类型参数 */
        INCOMPLETE_TYPE_INFERENCE(false),

        /** 成功但未找到名称 - 虽然解析成功，但没有找到合适的函数（用于特殊情况） */
        SUCCESS_NAME_NOT_FOUND(true);
    }
}

abstract class AbstractOverloadResolutionResults<D : CallableDescriptor> :
    OverloadResolutionResults<D> {

    override val isSuccess: Boolean
        get() = resultCode.isSuccess

    override val isSingleResult: Boolean
        get() = resultingCalls.size == 1 &&
                resultCode != OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER

    override val isNothing: Boolean
        get() = resultCode == OverloadResolutionResults.Code.NAME_NOT_FOUND

    override val isAmbiguity: Boolean
        get() = resultCode == OverloadResolutionResults.Code.AMBIGUITY

    override val isIncomplete: Boolean
        get() = resultCode == OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE
}


class SingleOverloadResolutionResult<D : CallableDescriptor>(
    val result: ResolvedCall<D>
) : AbstractOverloadResolutionResults<D>() {

    override val allCandidates: Collection<ResolvedCall<D>>?
        get() = null

    override val resultingCalls: Collection<ResolvedCall<D>>
        get() = listOf(result)

    override val resultingCall: ResolvedCall<D>
        get() = result

    override val resultingDescriptor: D
        get() = result.resultingDescriptor

    override val resultCode: OverloadResolutionResults.Code
        get() = when (result.status) {
            ResolutionStatus.SUCCESS ->
                OverloadResolutionResults.Code.SUCCESS

            ResolutionStatus.RECEIVER_TYPE_ERROR ->
                OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER

            ResolutionStatus.INCOMPLETE_TYPE_INFERENCE ->
                OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE

            else ->
                OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH
        }
}


open class NameNotFoundResolutionResult<D : CallableDescriptor> :
    AbstractOverloadResolutionResults<D>() {

    override val allCandidates: Collection<ResolvedCall<D>>?
        get() = null

    override val resultingCalls: Collection<ResolvedCall<D>>
        get() = emptyList()

    override val resultingCall: ResolvedCall<D>
        get() = error("No candidates")

    override val resultingDescriptor: D
        get() = error("No candidates")

    override val resultCode: OverloadResolutionResults.Code
        get() = OverloadResolutionResults.Code.NAME_NOT_FOUND
}

class ManyCandidates<D : CallableDescriptor>(
    val candidates: Collection<ResolvedCall<D>>
) : AbstractOverloadResolutionResults<D>() {

    override val allCandidates: Collection<ResolvedCall<D>>?
        get() = null

    override val resultingCalls: Collection<ResolvedCall<D>>
        get() = candidates

    override val resultingCall: ResolvedCall<D>
        get() = error("Many candidates")

    override val resultingDescriptor: D
        get() = error("Many candidates")

    override val resultCode: OverloadResolutionResults.Code
        get() = when (candidates.first().status) {
            ResolutionStatus.RECEIVER_TYPE_ERROR ->
                OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER

            ResolutionStatus.SUCCESS ->
                OverloadResolutionResults.Code.AMBIGUITY

            ResolutionStatus.INCOMPLETE_TYPE_INFERENCE ->
                OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE

            else ->
                OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES
        }
}


class AllCandidates<D : CallableDescriptor>(
    private val all: Collection<ResolvedCall<D>>
) : NameNotFoundResolutionResult<D>() {

    override val allCandidates: Collection<ResolvedCall<D>>
        get() = all
}
