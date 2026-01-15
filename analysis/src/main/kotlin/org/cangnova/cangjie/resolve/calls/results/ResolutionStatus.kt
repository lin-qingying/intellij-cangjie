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

import java.util.EnumSet

/**
 * 解析状态枚举
 *
 * 表示函数/方法调用解析过程中的各种状态，包括成功和各类错误。
 * 每个状态都有一个严重程度级别，用于在多个候选项中选择最合适的错误报告。
 *
 * @property success 是否为成功状态
 */
enum class ResolutionStatus(private val success: Boolean = false) {
    /** 未知状态，通常用作初始值 */
    UNKNOWN_STATUS,

    /** 不安全调用错误（例如对可空类型的不安全访问） */
    UNSAFE_CALL_ERROR,

    /** 类型参数数量错误 */
    WRONG_NUMBER_OF_TYPE_ARGUMENTS_ERROR,

    /** 接收者的智能转换不稳定 */
    UNSTABLE_SMARTCAST_FOR_RECEIVER_ERROR,

    /** 不可见成员错误（访问了私有/内部成员） */
    INVISIBLE_MEMBER_ERROR,

    /** 可空参数类型不匹配 */
    NULLABLE_ARGUMENT_TYPE_MISMATCH,

    /** 其他错误 */
    OTHER_ERROR,

    /** 参数映射错误（参数与形参无法匹配） */
    ARGUMENTS_MAPPING_ERROR,

    /**
     * 接收者类型错误
     *
     * 例如：'1.foo()' 不应该解析到 'fun String.foo()'
     * 带有此错误的候选项会被特殊处理（如果没有其他选项，会在"未解析"错误中提及）
     */
    RECEIVER_TYPE_ERROR,

    /**
     * 接收者存在性错误
     *
     * 例如：'a.foo()' 不应该解析到包级别的非扩展函数 'fun foo()'
     * 带有此错误的候选项会被完全丢弃
     */
    RECEIVER_PRESENCE_ERROR,

    /** 类型推导不完整 */
    INCOMPLETE_TYPE_INFERENCE,

    /** 解析成功 */
    SUCCESS(true);

    /** 严重程度索引缓存，-1 表示未初始化 */
    private var severityIndex: Int = -1

    /** 是否为成功状态 */
    val isSuccess: Boolean get() = success

    /**
     * 判断当前状态是否可能转换为成功状态
     *
     * 只有未知状态、类型推导不完整和成功状态本身可以转换为成功。
     *
     * @return true 如果可能转换为成功状态
     */
    fun possibleTransformToSuccess(): Boolean =
        this == UNKNOWN_STATUS || this == INCOMPLETE_TYPE_INFERENCE || this == SUCCESS

    /**
     * 合并两个解析状态
     *
     * 合并规则：
     * 1. 如果当前状态是 UNKNOWN_STATUS，返回另一个状态
     * 2. 如果其中一个是 SUCCESS，返回另一个（非成功的状态）
     * 3. 如果其中一个是 INCOMPLETE_TYPE_INFERENCE，返回另一个
     * 4. 否则返回严重程度更高的状态
     *
     * @param other 另一个解析状态
     * @return 合并后的状态
     */
    fun combine(other: ResolutionStatus): ResolutionStatus {
        // 未知状态直接返回另一个状态
        if (this == UNKNOWN_STATUS) return other

        // 如果其中一个是成功，返回非成功的那个
        if (SUCCESS.among(this, other)) {
            return SUCCESS.chooseDifferent(this, other)
        }

        // 如果其中一个是类型推导不完整，返回另一个
        if (INCOMPLETE_TYPE_INFERENCE.among(this, other)) {
            return INCOMPLETE_TYPE_INFERENCE.chooseDifferent(this, other)
        }

        // 返回严重程度更高的状态
        if (this.getSeverityIndex() < other.getSeverityIndex()) return other
        return this
    }

    /**
     * 判断当前状态是否在给定的两个状态中
     *
     * @param first 第一个状态
     * @param second 第二个状态
     * @return true 如果当前状态等于 first 或 second
     */
    private fun among(first: ResolutionStatus, second: ResolutionStatus): Boolean =
        this == first || this == second

    /**
     * 从两个状态中选择与当前状态不同的那个
     *
     * 前提条件：当前状态必须是 first 或 second 之一
     *
     * @param first 第一个状态
     * @param second 第二个状态
     * @return 与当前状态不同的那个状态
     */
    private fun chooseDifferent(first: ResolutionStatus, second: ResolutionStatus): ResolutionStatus {
        assert(among(first, second))
        return if (this == first) second else first
    }

    /**
     * 获取当前状态的严重程度索引
     *
     * 索引值越大，错误越严重。使用懒加载方式计算并缓存索引值。
     * 索引值对应 SEVERITY_LEVELS 数组中的位置。
     *
     * @return 严重程度索引（0 表示最轻微，值越大越严重）
     */
    private fun getSeverityIndex(): Int {
        // 懒加载：首次调用时计算索引
        if (severityIndex == -1) {
            for (i in SEVERITY_LEVELS.indices) {
                if (SEVERITY_LEVELS[i].contains(this)) {
                    severityIndex = i
                    break
                }
            }
        }
        assert(severityIndex >= 0)

        return severityIndex
    }

    companion object {
        /**
         * 错误严重程度级别数组
         *
         * 按照从轻微到严重的顺序排列各种错误状态。
         * 索引越大，错误越严重。用于在多个候选项中选择最严重的错误进行报告。
         *
         * 严重程度排序（从轻到重）：
         * 0. UNSAFE_CALL_ERROR - 不安全调用（最轻微）
         * 1. WRONG_NUMBER_OF_TYPE_ARGUMENTS_ERROR - 类型参数数量错误
         * 2. UNSTABLE_SMARTCAST_FOR_RECEIVER_ERROR - 智能转换不稳定
         * 3. INVISIBLE_MEMBER_ERROR - 不可见成员
         * 4. NULLABLE_ARGUMENT_TYPE_MISMATCH - 可空参数类型不匹配
         * 5. OTHER_ERROR - 其他错误
         * 6. ARGUMENTS_MAPPING_ERROR - 参数映射错误
         * 7. RECEIVER_TYPE_ERROR - 接收者类型错误
         * 8. RECEIVER_PRESENCE_ERROR - 接收者存在性错误（最严重）
         */
        @Suppress("UNCHECKED_CAST")
        val SEVERITY_LEVELS: Array<EnumSet<ResolutionStatus>> = arrayOf(
            EnumSet.of(UNSAFE_CALL_ERROR), // 最轻微
            EnumSet.of(WRONG_NUMBER_OF_TYPE_ARGUMENTS_ERROR),
            EnumSet.of(UNSTABLE_SMARTCAST_FOR_RECEIVER_ERROR),
            EnumSet.of(INVISIBLE_MEMBER_ERROR),
            EnumSet.of(NULLABLE_ARGUMENT_TYPE_MISMATCH),
            EnumSet.of(OTHER_ERROR),
            EnumSet.of(ARGUMENTS_MAPPING_ERROR),
            EnumSet.of(RECEIVER_TYPE_ERROR),
            EnumSet.of(RECEIVER_PRESENCE_ERROR), // 最严重
        )
    }
}