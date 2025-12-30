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
 */

package org.cangnova.cangjie.types.expressions.match.exhaustive

import org.cangnova.cangjie.types.expressions.match.Pattern

/**
 * 穷举性检查结果
 *
 * 表示模式匹配穷举性检查的所有可能结果。这是一个密封类层次结构，
 * 用于在类型安全的方式下表达检查结果。
 *
 * ## 结果类型
 *
 * | 类型 | 含义 | 使用场景 |
 * |------|------|----------|
 * | [Exhaustive] | 穷举完整 | 所有可能的值都被覆盖 |
 * | [NonExhaustive] | 不穷举 | 存在未覆盖的值，包含缺失模式 |
 * | [Error] | 检查错误 | 类型不一致、内部错误等 |
 * | [Skipped] | 跳过检查 | 类型无法确定、算法不适用 |
 *
 * ## 使用示例
 *
 * ```kotlin
 * val result = ExhaustivenessAnalyzer.checkMatch(matchExpr, context)
 * when (result) {
 *     is ExhaustivenessResult.Exhaustive -> {
 *         // 穷举完整，无需报告
 *     }
 *     is ExhaustivenessResult.NonExhaustive -> {
 *         // 报告缺失的模式
 *         val missing = result.getMissingPatternTexts()
 *         reportError("Match 不穷举，缺失: ${missing.joinToString()}")
 *     }
 *     is ExhaustivenessResult.Error -> {
 *         // 内部错误，记录日志
 *         log.warn("穷举性检查失败: ${result.reason}")
 *     }
 *     is ExhaustivenessResult.Skipped -> {
 *         // 无法检查，静默跳过
 *     }
 * }
 * ```
 *
 * @see ExhaustivenessAnalyzer
 * @see CheckSource
 */
sealed class ExhaustivenessResult {

    /**
     * 穷举完整
     *
     * 表示 match 表达式覆盖了所有可能的情况，符合仓颉语言的穷举性要求。
     */
    data object Exhaustive : ExhaustivenessResult()

    /**
     * 不穷举
     *
     * 表示 match 表达式没有覆盖所有可能的情况，包含缺失的模式列表。
     *
     * @property missingPatterns 缺失的模式列表，用于生成诊断消息
     * @property source 检查来源，标识使用了哪种算法
     *
     * ## 示例
     *
     * 对于以下不穷举的 match：
     * ```cangjie
     * match (opt) {
     *     case Some(x) => ...
     *     // 缺少 None 分支
     * }
     * ```
     *
     * `missingPatterns` 将包含表示 `None` 的 Pattern。
     */
    data class NonExhaustive(
        val missingPatterns: List<Pattern>,
        val source: CheckSource = CheckSource.UNKNOWN
    ) : ExhaustivenessResult() {

        /**
         * 获取缺失模式的文本表示
         *
         * 用于生成用户友好的诊断消息。
         *
         * @return 缺失模式的字符串列表
         */
        fun getMissingPatternTexts(): List<String> =
            missingPatterns.map { it.text(null) }
    }

    /**
     * 检查错误
     *
     * 表示检查过程中发生了错误，无法确定穷举性。
     * 这通常是内部错误，不应该向用户报告。
     *
     * @property reason 错误原因描述
     */
    data class Error(val reason: String) : ExhaustivenessResult()

    /**
     * 检查被跳过
     *
     * 表示检查无法执行，例如：
     * - 类型无法确定
     * - 算法不适用于该类型
     * - 模式矩阵为空
     */
    data object Skipped : ExhaustivenessResult()

    /**
     * 是否穷举完整
     */
    val isExhaustive: Boolean
        get() = this is Exhaustive

    /**
     * 是否不穷举
     */
    val isNonExhaustive: Boolean
        get() = this is NonExhaustive
}

/**
 * 检查来源
 *
 * 标识穷举性检查使用了哪种算法。用于调试和性能分析。
 *
 * ## 算法层次
 *
 * 混合模式检查器按以下优先级选择算法：
 *
 * ```
 * 第一层 (O(1)):
 *   TRIVIAL     - 空匹配、单通配符等琐碎情况
 *
 * 第二层 (O(n)):
 *   BOOLEAN_FLAG    - 布尔类型，两个标记
 *   ENUM_BITVECTOR  - 小枚举（≤64变体），位向量
 *   INTEGER_INTERVAL- 整数类型，区间分析
 *   CHAR_INTERVAL   - 字符类型，区间分析
 *
 * 第三层 (O(n*m)):
 *   TUPLE_COMPONENT - 元组类型，分量独立检查
 *   NESTED_FLATTEN  - 嵌套枚举，扁平化后位向量
 *
 * 第四层 (O(2^n)):
 *   MARANGET        - 完整 Maranget 算法
 * ```
 */
enum class CheckSource {
    /** 未知来源 */
    UNKNOWN,

    /** 第一层：琐碎情况快速检查 */
    TRIVIAL,

    /** 第二层：布尔类型标记检查 */
    BOOLEAN_FLAG,

    /** 第二层：小枚举位向量检查 */
    ENUM_BITVECTOR,

    /** 第二层：整数区间分析 */
    INTEGER_INTERVAL,

    /** 第二层：字符区间分析 */
    CHAR_INTERVAL,

    /** 第三层：元组分量独立检查 */
    TUPLE_COMPONENT,

    /** 第三层：嵌套结构扁平化检查 */
    NESTED_FLATTEN,

    /** 第四层：完整 Maranget 算法 */
    MARANGET
}
