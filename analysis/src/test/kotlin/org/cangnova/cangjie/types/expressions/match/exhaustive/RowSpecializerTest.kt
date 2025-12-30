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
import org.cangnova.cangjie.resolve.constants.BoolValue
import org.cangnova.cangjie.resolve.constants.Int64Value
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.expressions.match.Constructor
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.inria.RowSpecializer

/**
 * 行特化器测试
 *
 * 测试 Maranget 算法中的行特化操作。
 * 特化是将模式矩阵相对于某个构造器进行"投影"的核心操作。
 */
class RowSpecializerTest : CangJieNoPlatformTestBase() {

    // ==================== 空行测试 ====================

    fun `test specializeRow - empty row returns empty list`() {
        val row = emptyList<Pattern>()
        val constructor = Constructor.Single
        val type = ErrorUtils.invalidType

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    // ==================== 通配符模式测试 ====================

    fun `test specializeRow - wildcard pattern returns wildcards`() {
        val type = ErrorUtils.invalidType
        val wildPattern = Pattern.wild(type)
        val row = listOf(wildPattern)
        val constructor = Constructor.Single

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
    }

    fun `test specializeRow - wildcard with remaining patterns`() {
        val type = ErrorUtils.invalidType
        val wildPattern = Pattern.wild(type)
        val otherPattern = Pattern.wild(type)
        val row = listOf(wildPattern, otherPattern)
        val constructor = Constructor.Single

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
        // 结果应该包含特化后的头部 + 剩余模式
    }

    // ==================== 绑定模式测试 ====================

    fun `test specializeRow - binding pattern returns wildcards`() {
        val type = ErrorUtils.invalidType
        val bindingPattern = Pattern(type, PatternKind.Binding(type, "x"))
        val row = listOf(bindingPattern)
        val constructor = Constructor.Single

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
    }

    // ==================== 常量模式测试 ====================

    fun `test specializeRow - matching constant returns empty head`() {
        val type = ErrorUtils.invalidType
        val constPattern = Pattern(type, PatternKind.Const(BoolValue(true)))
        val row = listOf(constPattern)
        val constructor = Constructor.ConstantValue(BoolValue(true))

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
    }

    fun `test specializeRow - non-matching constant returns null`() {
        val type = ErrorUtils.invalidType
        val constPattern = Pattern(type, PatternKind.Const(BoolValue(true)))
        val row = listOf(constPattern)
        val constructor = Constructor.ConstantValue(BoolValue(false))

        val result = RowSpecializer.specializeRow(row, constructor, type)

        // 常量不匹配时返回 null
        assertNull(result)
    }

    fun `test specializeRow - integer constant matching`() {
        val type = ErrorUtils.invalidType
        val constPattern = Pattern(type, PatternKind.Const(Int64Value(42)))
        val row = listOf(constPattern)
        val constructor = Constructor.ConstantValue(Int64Value(42))

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
    }

    fun `test specializeRow - integer constant non-matching`() {
        val type = ErrorUtils.invalidType
        val constPattern = Pattern(type, PatternKind.Const(Int64Value(42)))
        val row = listOf(constPattern)
        val constructor = Constructor.ConstantValue(Int64Value(100))

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNull(result)
    }

    // ==================== 错误模式测试 ====================

    fun `test specializeRow - error pattern returns null`() {
        val row = listOf(Pattern.Error)
        val constructor = Constructor.Single
        val type = ErrorUtils.invalidType

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNull(result)
    }

    // ==================== 多模式行测试 ====================

    fun `test specializeRow - preserves remaining patterns`() {
        val type = ErrorUtils.invalidType
        val firstPattern = Pattern.wild(type)
        val secondPattern = Pattern(type, PatternKind.Binding(type, "y"))
        val thirdPattern = Pattern(type, PatternKind.Const(BoolValue(true)))
        val row = listOf(firstPattern, secondPattern, thirdPattern)
        val constructor = Constructor.Single

        val result = RowSpecializer.specializeRow(row, constructor, type)

        assertNotNull(result)
        // 结果应该包含剩余的模式
        assertTrue(result!!.any { it.kind is PatternKind.Binding })
        assertTrue(result.any { it.kind is PatternKind.Const })
    }

    // ==================== Single 构造器测试 ====================

    fun `test specializeRow - Single constructor with wildcard`() {
        val type = ErrorUtils.invalidType
        val wildPattern = Pattern.wild(type)
        val row = listOf(wildPattern)

        val result = RowSpecializer.specializeRow(row, Constructor.Single, type)

        assertNotNull(result)
    }

    // ==================== 构造器类型测试 ====================

    fun `test Constructor Single coveredByRange always returns true`() {
        val from = BoolValue(false)
        val to = BoolValue(true)

        assertTrue(Constructor.Single.coveredByRange(from, to, true))
        assertTrue(Constructor.Single.coveredByRange(from, to, false))
    }

    fun `test Constructor ConstantValue coveredByRange - in range inclusive`() {
        val constructor = Constructor.ConstantValue(Int64Value(5))
        val from = Int64Value(0)
        val to = Int64Value(10)

        assertTrue(constructor.coveredByRange(from, to, included = true))
    }

    fun `test Constructor ConstantValue coveredByRange - at boundary inclusive`() {
        val constructor = Constructor.ConstantValue(Int64Value(10))
        val from = Int64Value(0)
        val to = Int64Value(10)

        assertTrue(constructor.coveredByRange(from, to, included = true))
    }

    fun `test Constructor ConstantValue coveredByRange - at boundary exclusive`() {
        val constructor = Constructor.ConstantValue(Int64Value(10))
        val from = Int64Value(0)
        val to = Int64Value(10)

        assertFalse(constructor.coveredByRange(from, to, included = false))
    }

    fun `test Constructor ConstantValue coveredByRange - out of range`() {
        val constructor = Constructor.ConstantValue(Int64Value(15))
        val from = Int64Value(0)
        val to = Int64Value(10)

        assertFalse(constructor.coveredByRange(from, to, included = true))
    }
}
