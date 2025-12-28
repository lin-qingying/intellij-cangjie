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
import org.cangnova.cangjie.types.expressions.match.exhaustive.inria.customDistinct

/**
 * 矩阵工具函数测试
 *
 * 测试 MatrixUtils.kt 中提供的工具函数，主要测试 customDistinct 去重函数。
 */
class MatrixUtilsTest : CangJieNoPlatformTestBase() {

    // ==================== customDistinct 测试 ====================

    fun `test customDistinct - empty list returns empty`() {
        val list = emptyList<Int>()

        val result = list.customDistinct { a, b -> a == b }

        assertTrue(result.isEmpty())
    }

    fun `test customDistinct - single element returns single element`() {
        val list = listOf(1)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(1, result.size)
        assertEquals(1, result[0])
    }

    fun `test customDistinct - no duplicates returns unchanged`() {
        val list = listOf(1, 2, 3, 4, 5)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(5, result.size)
        assertEquals(listOf(1, 2, 3, 4, 5), result)
    }

    fun `test customDistinct - removes consecutive duplicates`() {
        val list = listOf(1, 1, 2, 2, 3, 3)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(3, result.size)
        assertEquals(listOf(1, 2, 3), result)
    }

    fun `test customDistinct - removes non-consecutive duplicates`() {
        val list = listOf(1, 2, 1, 3, 2, 4)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(4, result.size)
        assertEquals(listOf(1, 2, 3, 4), result)
    }

    fun `test customDistinct - preserves first occurrence`() {
        val list = listOf("a", "b", "A", "B")

        // 使用忽略大小写的比较器
        val result = list.customDistinct { a, b -> a.equals(b, ignoreCase = true) }

        assertEquals(2, result.size)
        assertEquals("a", result[0])  // 保留第一个出现的 "a"
        assertEquals("b", result[1])  // 保留第一个出现的 "b"
    }

    fun `test customDistinct - custom comparator for modulo`() {
        val list = listOf(1, 4, 7, 2, 5, 8, 3)

        // 模 3 相等的元素视为相同
        val result = list.customDistinct { a, b -> a % 3 == b % 3 }

        assertEquals(3, result.size)
        assertEquals(1, result[0])  // 1 % 3 = 1
        assertEquals(2, result[1])  // 2 % 3 = 2
        assertEquals(3, result[2])  // 3 % 3 = 0
    }

    fun `test customDistinct - all same elements returns single element`() {
        val list = listOf(5, 5, 5, 5, 5)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(1, result.size)
        assertEquals(5, result[0])
    }

    fun `test customDistinct - preserves order`() {
        val list = listOf(3, 1, 4, 1, 5, 9, 2, 6)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(listOf(3, 1, 4, 5, 9, 2, 6), result)
    }

    // ==================== 复杂类型去重测试 ====================

    fun `test customDistinct - with nullable values`() {
        val list = listOf(1, null, 2, null, 3)

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(4, result.size)
        assertEquals(listOf(1, null, 2, 3), result)
    }

    fun `test customDistinct - with data class instances`() {
        data class Point(val x: Int, val y: Int)

        val list = listOf(
            Point(1, 2),
            Point(3, 4),
            Point(1, 2),  // 重复
            Point(5, 6)
        )

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(3, result.size)
        assertEquals(Point(1, 2), result[0])
        assertEquals(Point(3, 4), result[1])
        assertEquals(Point(5, 6), result[2])
    }

    fun `test customDistinct - with partial equality`() {
        data class Person(val name: String, val age: Int)

        val list = listOf(
            Person("Alice", 25),
            Person("Bob", 30),
            Person("Alice", 35),  // 同名不同年龄
            Person("Carol", 25)
        )

        // 只比较名字
        val result = list.customDistinct { a, b -> a.name == b.name }

        assertEquals(3, result.size)
        assertEquals("Alice", result[0].name)
        assertEquals(25, result[0].age)  // 保留第一个 Alice
        assertEquals("Bob", result[1].name)
        assertEquals("Carol", result[2].name)
    }

    // ==================== 边界条件测试 ====================

    fun `test customDistinct - large list performance`() {
        val list = (1..1000).toList() + (1..1000).toList()  // 2000 个元素，各重复一次

        val result = list.customDistinct { a, b -> a == b }

        assertEquals(1000, result.size)
    }

    fun `test customDistinct - comparator always returns true`() {
        val list = listOf(1, 2, 3, 4, 5)

        // 所有元素都"相等"
        val result = list.customDistinct { _, _ -> true }

        assertEquals(1, result.size)
        assertEquals(1, result[0])  // 只保留第一个
    }

    fun `test customDistinct - comparator always returns false`() {
        val list = listOf(1, 1, 1, 1, 1)

        // 所有元素都"不相等"
        val result = list.customDistinct { _, _ -> false }

        assertEquals(5, result.size)  // 所有都保留
    }
}
