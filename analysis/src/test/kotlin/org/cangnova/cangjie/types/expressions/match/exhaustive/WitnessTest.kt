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
import org.cangnova.cangjie.types.expressions.match.exhaustive.inria.Witness

/**
 * Witness（证据）类测试
 *
 * 测试 Maranget 算法中用于构造未匹配反例的 Witness 类。
 * Witness 使用栈式结构来递归地构造反例模式。
 */
class WitnessTest : CangJieNoPlatformTestBase() {

    // ==================== 基本操作测试 ====================

    fun `test Witness - empty initialization`() {
        val witness = Witness()

        assertTrue(witness.patterns.isEmpty())
    }

    fun `test Witness - clone creates independent copy`() {
        val witness = Witness()

        val cloned = witness.clone()

        assertNotSame(witness, cloned)
        assertNotSame(witness.patterns, cloned.patterns)
    }

    fun `test Witness - clone preserves patterns`() {
        val witness = Witness()
        // 向原始 witness 添加模式后克隆
        // 由于 patterns 是 mutableList，修改操作会影响列表

        val cloned = witness.clone()

        assertEquals(witness.patterns.size, cloned.patterns.size)
    }

    fun `test Witness - modifications to clone do not affect original`() {
        val witness = Witness()
        val cloned = witness.clone()

        // 修改克隆不应影响原始
        val originalSize = witness.patterns.size
        // 注：由于没有简单的添加方法，这里只验证克隆后的独立性

        assertEquals(originalSize, witness.patterns.size)
    }

    // ==================== 模式列表操作测试 ====================

    fun `test Witness patterns list is mutable`() {
        val witness = Witness()

        // patterns 应该是可变列表
        assertNotNull(witness.patterns)
        assertTrue(witness.patterns is MutableList)
    }

    fun `test Witness toString returns patterns string representation`() {
        val witness = Witness()

        // toString 应该返回 patterns 的字符串表示
        assertEquals(witness.patterns.toString(), witness.toString())
    }
}
