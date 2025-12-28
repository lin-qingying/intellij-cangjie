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

import org.cangnova.cangjie.CangJieNoPlatformTestBase

/**
 * 区间合并算法测试
 *
 * 测试整数区间检查器中使用的区间合并和空隙查找算法。
 * 这些算法是 IntegerIntervalChecker 的核心逻辑。
 */
class IntervalMergeAlgorithmTest : CangJieNoPlatformTestBase() {

    // ==================== 区间合并测试 ====================

    fun `test mergeIntervals - empty list returns empty`() {
        val intervals = emptyList<LongRange>()

        val merged = mergeIntervals(intervals)

        assertTrue(merged.isEmpty())
    }

    fun `test mergeIntervals - single interval returns unchanged`() {
        val intervals = listOf(5L..10L)

        val merged = mergeIntervals(intervals)

        assertEquals(1, merged.size)
        assertEquals(5L..10L, merged[0])
    }

    fun `test mergeIntervals - non-overlapping intervals unchanged`() {
        val intervals = listOf(1L..5L, 10L..15L, 20L..25L)

        val merged = mergeIntervals(intervals)

        assertEquals(3, merged.size)
        assertEquals(1L..5L, merged[0])
        assertEquals(10L..15L, merged[1])
        assertEquals(20L..25L, merged[2])
    }

    fun `test mergeIntervals - overlapping intervals merge`() {
        val intervals = listOf(1L..10L, 5L..15L)

        val merged = mergeIntervals(intervals)

        assertEquals(1, merged.size)
        assertEquals(1L..15L, merged[0])
    }

    fun `test mergeIntervals - adjacent intervals merge`() {
        val intervals = listOf(1L..5L, 6L..10L)

        val merged = mergeIntervals(intervals)

        assertEquals(1, merged.size)
        assertEquals(1L..10L, merged[0])
    }

    fun `test mergeIntervals - contained intervals merge`() {
        val intervals = listOf(1L..20L, 5L..10L)

        val merged = mergeIntervals(intervals)

        assertEquals(1, merged.size)
        assertEquals(1L..20L, merged[0])
    }

    fun `test mergeIntervals - unsorted intervals are sorted and merged`() {
        val intervals = listOf(10L..15L, 1L..5L, 8L..12L)

        val merged = mergeIntervals(intervals)

        assertEquals(2, merged.size)
        assertEquals(1L..5L, merged[0])
        assertEquals(8L..15L, merged[1])
    }

    fun `test mergeIntervals - all intervals merge into one`() {
        val intervals = listOf(1L..3L, 2L..5L, 4L..7L, 6L..10L)

        val merged = mergeIntervals(intervals)

        assertEquals(1, merged.size)
        assertEquals(1L..10L, merged[0])
    }

    fun `test mergeIntervals - point intervals`() {
        val intervals = listOf(5L..5L, 10L..10L, 15L..15L)

        val merged = mergeIntervals(intervals)

        assertEquals(3, merged.size)
    }

    fun `test mergeIntervals - point intervals adjacent`() {
        val intervals = listOf(5L..5L, 6L..6L, 7L..7L)

        val merged = mergeIntervals(intervals)

        assertEquals(1, merged.size)
        assertEquals(5L..7L, merged[0])
    }

    // ==================== 空隙查找测试 ====================

    fun `test findGaps - empty intervals returns full range`() {
        val intervals = emptyList<LongRange>()
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(0L..255L, gaps[0])
    }

    fun `test findGaps - full coverage returns no gaps`() {
        val intervals = listOf(0L..255L)
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertTrue(gaps.isEmpty())
    }

    fun `test findGaps - gap at beginning`() {
        val intervals = listOf(10L..255L)
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(0L..9L, gaps[0])
    }

    fun `test findGaps - gap at end`() {
        val intervals = listOf(0L..100L)
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(101L..255L, gaps[0])
    }

    fun `test findGaps - gap in middle`() {
        val intervals = listOf(0L..50L, 100L..255L)
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(51L..99L, gaps[0])
    }

    fun `test findGaps - multiple gaps`() {
        val intervals = listOf(10L..20L, 50L..60L, 100L..110L)
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(4, gaps.size)
        assertEquals(0L..9L, gaps[0])
        assertEquals(21L..49L, gaps[1])
        assertEquals(61L..99L, gaps[2])
        assertEquals(111L..255L, gaps[3])
    }

    fun `test findGaps - adjacent intervals no gap`() {
        val intervals = listOf(0L..100L, 101L..255L)
        val typeRange = 0L..255L

        val gaps = findGaps(intervals, typeRange)

        assertTrue(gaps.isEmpty())
    }

    fun `test findGaps - single point coverage`() {
        val intervals = listOf(5L..5L)
        val typeRange = 0L..10L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(2, gaps.size)
        assertEquals(0L..4L, gaps[0])
        assertEquals(6L..10L, gaps[1])
    }

    // ==================== 边界条件测试 ====================

    fun `test findGaps - type range starts at minimum Long`() {
        val intervals = listOf(Long.MIN_VALUE..(Long.MIN_VALUE + 10))
        val typeRange = Long.MIN_VALUE..(Long.MIN_VALUE + 100)

        val gaps = findGaps(intervals, typeRange)

        assertEquals(1, gaps.size)
        assertEquals((Long.MIN_VALUE + 11)..(Long.MIN_VALUE + 100), gaps[0])
    }

    fun `test findGaps - large range with small coverage`() {
        val intervals = listOf(0L..0L)
        val typeRange = 0L..1000000L

        val gaps = findGaps(intervals, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(1L..1000000L, gaps[0])
    }

    // ==================== 穷尽性判断测试 ====================

    fun `test exhaustiveness - UInt8 fully covered by values`() {
        val typeRange = 0L..255L
        val intervals = (0L..255L).map { it..it }

        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged, typeRange)

        assertTrue("所有 UInt8 值覆盖应该没有空隙", gaps.isEmpty())
    }

    fun `test exhaustiveness - UInt8 missing one value`() {
        val typeRange = 0L..255L
        val intervals = (0L..254L).map { it..it } // 缺少 255

        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(255L..255L, gaps[0])
    }

    fun `test exhaustiveness - Bool type exhaustive`() {
        val typeRange = 0L..1L // Bool: false=0, true=1
        val intervals = listOf(0L..0L, 1L..1L)

        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged, typeRange)

        assertTrue("布尔类型完全覆盖", gaps.isEmpty())
    }

    fun `test exhaustiveness - Bool type missing false`() {
        val typeRange = 0L..1L
        val intervals = listOf(1L..1L) // 只有 true

        val merged = mergeIntervals(intervals)
        val gaps = findGaps(merged, typeRange)

        assertEquals(1, gaps.size)
        assertEquals(0L..0L, gaps[0]) // 缺少 false
    }

    // ==================== 辅助函数实现 ====================

    /**
     * 合并重叠或相邻的区间
     */
    private fun mergeIntervals(intervals: List<LongRange>): List<LongRange> {
        if (intervals.isEmpty()) return emptyList()

        val sorted = intervals.sortedBy { it.first }
        val result = mutableListOf<LongRange>()

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
     */
    private fun findGaps(intervals: List<LongRange>, typeRange: LongRange): List<LongRange> {
        if (intervals.isEmpty()) {
            return listOf(typeRange)
        }

        val gaps = mutableListOf<LongRange>()

        // 开头空隙
        if (intervals.first().first > typeRange.first) {
            gaps.add(typeRange.first until intervals.first().first)
        }

        // 中间空隙
        for (i in 0 until intervals.size - 1) {
            val gapStart = intervals[i].last + 1
            val gapEnd = intervals[i + 1].first - 1
            if (gapStart <= gapEnd) {
                gaps.add(gapStart..gapEnd)
            }
        }

        // 结尾空隙
        if (intervals.last().last < typeRange.last) {
            gaps.add((intervals.last().last + 1)..typeRange.last)
        }

        return gaps
    }
}
