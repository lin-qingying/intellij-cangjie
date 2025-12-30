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
import org.cangnova.cangjie.resolve.constants.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult

/**
 * 第二层：整数区间分析检查器
 *
 * 针对整数类型的快速穷举性检查。
 * 将整数的所有可能值看作一条数轴，模式匹配就是在数轴上标记已覆盖的区间。
 *
 * ## 算法原理
 *
 * ```
 * 类型: UInt8 → 值范围 [0, 255]
 *
 * 步骤 1: 收集模式区间
 *   case 0      => ... → [0, 0]
 *   case 1..10  => ... → [1, 10]
 *   case 100    => ... → [100, 100]
 *
 * 步骤 2: 合并重叠区间
 *   [0, 0] + [1, 10] = [0, 10]（相邻合并）
 *   结果: [[0, 10], [100, 100]]
 *
 * 步骤 3: 找出空隙
 *   类型范围 [0, 255]
 *   已覆盖: [0, 10], [100, 100]
 *   空隙: [11, 99], [101, 255]
 *
 * 步骤 4: 报告缺失
 *   error: match 表达式不穷举，缺少: 11, 99, 101, 255, ...
 * ```
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * // 穷举完整（使用通配符）
 * match (n: Int32) {
 *     case 0 => "零"
 *     case 1..9 => "个位数"
 *     case _ => "其他"
 * }
 *
 * // 不穷举
 * match (n: UInt8) {
 *     case 0 => "零"
 *     case 1..100 => "一到百"
 *     // error: match 表达式不穷举，缺少: 101, 255, ...
 * }
 * ```
 *
 * ## 时间复杂度
 *
 * O(n log n)，其中 n 是分支数量（主要是区间排序的开销）
 *
 * ## 为什么快
 *
 * - 避免枚举所有整数值（不可能枚举 2³² 个值）
 * - 区间合并操作是线性的
 * - 实际代码中很少有超过 10 个整数模式分支
 *
 * @see ExhaustivenessChecker
 * @see CheckSource.INTEGER_INTERVAL
 */
class IntegerIntervalChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.INTEGER_INTERVAL

    override val priority: Int = 30

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        return CangJieBuiltIns.isIntegral(type)
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        if (!CangJieBuiltIns.isIntegral(type)) {
            return ExhaustivenessResult.Skipped
        }

        // 获取类型的值范围
        val typeRange = getTypeRange(type) ?: return ExhaustivenessResult.Skipped

        // 收集所有区间
        val intervals = mutableListOf<LongRange>()
        var hasWildcard = false

        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue

            when (val kind = pattern.kind) {
                // 通配符覆盖整个范围
                is PatternKind.Wild, is PatternKind.Binding -> {
                    hasWildcard = true
                }

                // 常量模式
                is PatternKind.Const -> {
                    val value = kind.value
                    val longValue = toLong(value)
                    if (longValue != null) {
                        intervals.add(longValue..longValue)
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
        val gaps = findGaps(merged, typeRange)

        return if (gaps.isEmpty()) {
            ExhaustivenessResult.Exhaustive
        } else {
            // 生成缺失模式（最多报告前几个）
            val missingPatterns = gaps.take(3).flatMap { gap ->
                // 对于大范围，只报告边界值
                if (gap.last - gap.first > 10) {
                    listOf(
                        createIntegerPattern(type, gap.first),
                        createIntegerPattern(type, gap.last)
                    )
                } else {
                    (gap.first..gap.last).map { createIntegerPattern(type, it) }
                }
            }.take(5)

            ExhaustivenessResult.NonExhaustive(missingPatterns, source)
        }
    }

    /**
     * 获取整数类型的值范围
     */
    private fun getTypeRange(type: CangJieType): LongRange? {
        return when {
            CangJieBuiltIns.isInt8(type) -> Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
            CangJieBuiltIns.isInt16(type) -> Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()
            CangJieBuiltIns.isInt32(type) -> Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
            CangJieBuiltIns.isInt64(type) -> Long.MIN_VALUE..Long.MAX_VALUE
            CangJieBuiltIns.isUInt8(type) -> 0L..255L
            CangJieBuiltIns.isUInt16(type) -> 0L..65535L
            CangJieBuiltIns.isUInt32(type) -> 0L..4294967295L
            CangJieBuiltIns.isUInt64(type) -> 0L..Long.MAX_VALUE // 近似，无法完全表示
            else -> null
        }
    }

    /**
     * 将常量值转换为 Long
     */
    private fun toLong(value: ConstantValue<*>): Long? {
        return when (value) {
            is Int8Value -> value.value.toLong()
            is Int16Value -> value.value.toLong()
            is Int32Value -> value.value.toLong()
            is Int64Value -> value.value
            is UInt8Value -> value.value.toLong()
            is UInt16Value -> value.value.toLong()
            is UInt32Value -> value.value.toLong()
            is UInt64Value -> value.value.toLong()
            else -> null
        }
    }

    /**
     * 合并重叠或相邻的区间
     */
    private fun mergeIntervals(intervals: List<LongRange>): List<LongRange> {
        if (intervals.isEmpty()) return emptyList()

        // 按起始点排序
        val sorted = intervals.sortedBy { it.first }
        val result = mutableListOf<LongRange>()

        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.last + 1) {
                // 重叠或相邻，合并
                current = current.first..maxOf(current.last, next.last)
            } else {
                // 不重叠，保存当前区间
                result.add(current)
                current = next
            }
        }
        result.add(current)

        return result
    }

    /**
     * 找出未覆盖的空隙
     */
    private fun findGaps(intervals: List<LongRange>, typeRange: LongRange): List<LongRange> {
        if (intervals.isEmpty()) {
            return listOf(typeRange)
        }

        val gaps = mutableListOf<LongRange>()

        // 检查开头的空隙
        if (intervals.first().first > typeRange.first) {
            gaps.add(typeRange.first until intervals.first().first)
        }

        // 检查中间的空隙
        for (i in 0 until intervals.size - 1) {
            val gapStart = intervals[i].last + 1
            val gapEnd = intervals[i + 1].first - 1
            if (gapStart <= gapEnd) {
                gaps.add(gapStart..gapEnd)
            }
        }

        // 检查结尾的空隙
        if (intervals.last().last < typeRange.last) {
            gaps.add((intervals.last().last + 1)..typeRange.last)
        }

        return gaps
    }

    /**
     * 创建整数常量模式
     */
    private fun createIntegerPattern(type: CangJieType, value: Long): Pattern {
        val constValue: ConstantValue<*> = when {
            CangJieBuiltIns.isInt8(type) -> Int8Value(value.toByte())
            CangJieBuiltIns.isInt16(type) -> Int16Value(value.toShort())
            CangJieBuiltIns.isInt32(type) -> Int32Value(value.toInt())
            CangJieBuiltIns.isInt64(type) -> Int64Value(value)
            CangJieBuiltIns.isUInt8(type) -> UInt8Value(value.toByte())
            CangJieBuiltIns.isUInt16(type) -> UInt16Value(value.toShort())
            CangJieBuiltIns.isUInt32(type) -> UInt32Value(value.toInt())
            CangJieBuiltIns.isUInt64(type) -> UInt64Value(value.toLong())
            else -> Int64Value(value)
        }
        return Pattern(type, PatternKind.Const(constValue))
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = IntegerIntervalChecker()
    }
}
