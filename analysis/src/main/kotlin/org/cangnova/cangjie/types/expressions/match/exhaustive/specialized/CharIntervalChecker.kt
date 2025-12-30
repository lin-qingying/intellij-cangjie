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

package org.cangnova.cangjie.types.expressions.match.exhaustive.specialized

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.constants.RuneValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult

/**
 * 第二层：字符类型区间分析检查器
 *
 * 针对字符（Rune）类型的快速穷举性检查。
 * 使用与整数相同的区间分析算法，但值域是 Unicode 码点范围。
 *
 * ## Unicode 码点范围
 *
 * - 有效范围: [0x0000, 0x10FFFF]（约 110 万个码点）
 * - 无效范围: [0xD800, 0xDFFF]（surrogate 区域，2048 个码点）
 *
 * ```
 * 0x0000 ─────────────── 0xD7FF | 0xE000 ─────────────── 0x10FFFF
 *        ↑ 有效区域 1 ↑         |        ↑ 有效区域 2 ↑
 *                               |
 *                    0xD800 ── 0xDFFF
 *                    ↑ surrogate 无效区域 ↑
 * ```
 *
 * ## 算法原理
 *
 * 与 [IntegerIntervalChecker] 相同，但需要特殊处理 surrogate 区域：
 *
 * 1. 收集字符常量的码点值
 * 2. 合并重叠或相邻的区间
 * 3. 检查是否覆盖了所有有效 Unicode 码点（跳过 surrogate）
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * // 穷举完整（使用通配符）
 * match (ch: Rune) {
 *     case 'a'..'z' => "小写字母"
 *     case 'A'..'Z' => "大写字母"
 *     case '0'..'9' => "数字"
 *     case _ => "其他字符"
 * }
 *
 * // 不穷举
 * match (ch: Rune) {
 *     case 'a' => "a"
 *     case 'b' => "b"
 *     // error: match 表达式不穷举，需要添加通配符分支
 * }
 * ```
 *
 * ## 时间复杂度
 *
 * O(n log n)，其中 n 是分支数量
 *
 * ## 实际应用
 *
 * 由于 Unicode 有超过 100 万个有效码点，实际代码中几乎都需要使用
 * 通配符分支。此检查器主要用于快速识别这种情况并给出适当的错误提示。
 *
 * @see IntegerIntervalChecker
 * @see ExhaustivenessChecker
 * @see CheckSource.CHAR_INTERVAL
 */
class CharIntervalChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.CHAR_INTERVAL

    override val priority: Int = 35

    /**
     * Unicode 码点范围
     */
    private val unicodeMin = 0
    private val unicodeMax = 0x10FFFF

    /**
     * Surrogate 区域（无效的 Unicode 码点）
     */
    private val surrogateStart = 0xD800
    private val surrogateEnd = 0xDFFF

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        return CangJieBuiltIns.isRune(type)
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        if (!CangJieBuiltIns.isRune(type)) {
            return ExhaustivenessResult.Skipped
        }

        // 收集所有区间
        val intervals = mutableListOf<IntRange>()
        var hasWildcard = false

        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue

            when (val kind = pattern.kind) {
                // 通配符覆盖整个范围
                is PatternKind.Wild, is PatternKind.Binding -> {
                    hasWildcard = true
                }

                // 字符常量模式
                is PatternKind.Const -> {
                    val value = kind.value
                    if (value is RuneValue) {
                        val codePoint = value.value.code
                        intervals.add(codePoint..codePoint)
                    }
                }

                // TODO: 支持范围模式
                else -> {}
            }

            // 提前终止
            if (hasWildcard) {
                return ExhaustivenessResult.Exhaustive
            }
        }

        // 如果有通配符，直接返回穷举
        if (hasWildcard) {
            return ExhaustivenessResult.Exhaustive
        }

        // 合并区间并检查覆盖
        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged)

        return if (gaps.isEmpty()) {
            ExhaustivenessResult.Exhaustive
        } else {
            // 字符类型有太多可能值，只报告需要通配符
            val wildPattern = Pattern.wild(type)
            ExhaustivenessResult.NonExhaustive(listOf(wildPattern), source)
        }
    }

    /**
     * 合并重叠或相邻的区间
     */
    private fun mergeIntervals(intervals: List<IntRange>): List<IntRange> {
        if (intervals.isEmpty()) return emptyList()

        val sorted = intervals.sortedBy { it.first }
        val result = mutableListOf<IntRange>()

        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.last + 1) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)

        return result
    }

    /**
     * 找出未覆盖的空隙
     *
     * 注意：跳过 surrogate 区域
     */
    private fun findGaps(intervals: List<IntRange>): List<IntRange> {
        // 有效的 Unicode 区间（排除 surrogate）
        val validRanges = listOf(
            unicodeMin until surrogateStart,
            (surrogateEnd + 1)..unicodeMax
        )

        val gaps = mutableListOf<IntRange>()

        for (validRange in validRanges) {
            // 找出在有效范围内且未被覆盖的区间
            var pos = validRange.first
            for (interval in intervals) {
                if (interval.last < validRange.first) continue
                if (interval.first > validRange.last) break

                val effectiveStart = maxOf(interval.first, validRange.first)
                val effectiveEnd = minOf(interval.last, validRange.last)

                if (pos < effectiveStart) {
                    gaps.add(pos until effectiveStart)
                }
                pos = effectiveEnd + 1
            }
            if (pos <= validRange.last) {
                gaps.add(pos..validRange.last)
            }
        }

        return gaps
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = CharIntervalChecker()
    }
}
