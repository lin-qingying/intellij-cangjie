/*
 * Copyright 2026 LinQingYing. and contributors.
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
package org.cangnova.cangjie.types

import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase

/**
 * OptionTypeUtils 工具类测试
 *
 * 测试 Option 类型的工具方法，包括：
 * - Option 类型识别
 * - Option 类型解包（支持层级控制）
 * - Option 嵌套层级计算
 * - Option 类型创建
 * - 隐式转换检查
 */
class OptionTypeUtilsTest : CangJieAnalysisTestBase() {

    // ==================== Option 类型识别测试 ====================

    /**
     * 测试 isOptionType 方法
     *
     * 验证：
     * 1. 正确识别 Option 类型
     * 2. 正确识别非 Option 类型
     */
    fun `test isOptionType method`() {
        val file = createFile(
            """
            package test

            var a: Int64 = 42
            var b: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns

            // 测试基本类型
            val int64Type = builtIns.int64Type
            assertFalse("Int64 不应该是 Option 类型", int64Type.isOptionType())

            // 测试 Option 类型
            val optionInt64 = int64Type.makeOption()
            assertTrue("Option<Int64> 应该是 Option 类型", optionInt64.isOptionType())

            // 测试嵌套 Option
            val optionOptionInt64 = optionInt64.makeOption()
            assertTrue("Option<Option<Int64>> 应该是 Option 类型", optionOptionInt64.isOptionType())
        }
    }

    // ==================== Option 解包测试 ====================

    /**
     * 测试 unwrapOptionType 基本功能
     *
     * 验证：
     * 1. 解包一层 Option
     * 2. 非 Option 类型返回 null
     */
    fun `test unwrapOptionType basic`() {
        val file = createFile(
            """
            package test

            var x: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 测试基本类型解包
            val unwrapped = int64Type.unwrapOptionType()
            assertNull("Int64 解包应该返回 null", unwrapped)

            // 测试 Option 类型解包
            val optionInt64 = int64Type.makeOption()
            val unwrappedOption = optionInt64.unwrapOptionType()
            assertNotNull("Option<Int64> 解包不应该返回 null", unwrappedOption)
            assertEquals("解包后应该是 Int64", "Int64", unwrappedOption?.toString())
        }
    }

    /**
     * 测试 unwrapOptionType 层级控制
     *
     * 验证：
     * 1. levels = 1: 解包一层
     * 2. levels = 2: 解包两层
     * 3. levels = -1: 解包所有层级
     * 4. levels = 0: 不解包
     */
    fun `test unwrapOptionType with levels`() {
        val file = createFile(
            """
            package test

            var x: Option<Option<Option<Int64>>> = Some(Some(Some(42)))
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 创建 Option<Option<Option<Int64>>>
            val level1 = int64Type.makeOption()                  // Option<Int64>
            val level2 = level1.makeOption()                     // Option<Option<Int64>>
            val level3 = level2.makeOption()                     // Option<Option<Option<Int64>>>

            // 测试 levels = 0（不解包）
            val unwrap0 = level3.unwrapOptionType(0)
            assertEquals("levels=0 应该返回原类型", level3, unwrap0)

            // 测试 levels = 1（解包一层）
            val unwrap1 = level3.unwrapOptionType(1)
            assertTrue("levels=1 应该解包一层", unwrap1?.isOptionType() == true)
            assertEquals(
                "解包一层后应该是 Option<Option<Int64>>",
                2,
                unwrap1?.optionNestedLevel()
            )

            // 测试 levels = 2（解包两层）
            val unwrap2 = level3.unwrapOptionType(2)
            assertTrue("levels=2 应该解包两层", unwrap2?.isOptionType() == true)
            assertEquals(
                "解包两层后应该是 Option<Int64>",
                1,
                unwrap2?.optionNestedLevel()
            )

            // 测试 levels = 3（解包三层）
            val unwrap3 = level3.unwrapOptionType(3)
            assertFalse("levels=3 应该完全解包", unwrap3?.isOptionType() == true)
            assertEquals("完全解包后应该是 Int64", "Int64", unwrap3?.toString())

            // 测试 levels = -1（解包所有层级）
            val unwrapAll = level3.unwrapOptionType(-1)
            assertFalse("levels=-1 应该完全解包", unwrapAll?.isOptionType() == true)
            assertEquals("解包所有层级后应该是 Int64", "Int64", unwrapAll?.toString())
        }
    }

    /**
     * 测试解包超过实际层级
     */
    fun `test unwrapOptionType beyond actual levels`() {
        val file = createFile(
            """
            package test

            var x: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type
            val optionInt64 = int64Type.makeOption()

            // 尝试解包 5 层，但实际只有 1 层
            val unwrapped = optionInt64.unwrapOptionType(5)
            assertFalse("解包超过实际层级应该返回内层类型", unwrapped?.isOptionType() == true)
            assertEquals("应该返回 Int64", "Int64", unwrapped?.toString())
        }
    }

    // ==================== Option 嵌套层级测试 ====================

    /**
     * 测试 optionNestedLevel 方法
     *
     * 验证：
     * 1. 正确计算嵌套层级
     * 2. 非 Option 类型返回 0
     */
    fun `test optionNestedLevel method`() {
        val file = createFile(
            """
            package test

            var a: Int64 = 42
            var b: Option<Int64> = Some(42)
            var c: Option<Option<Int64>> = Some(Some(42))
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 测试基本类型
            assertEquals("Int64 的嵌套层级应该是 0", 0, int64Type.optionNestedLevel())

            // 测试 Option<Int64>
            val level1 = int64Type.makeOption()
            assertEquals("Option<Int64> 的嵌套层级应该是 1", 1, level1.optionNestedLevel())

            // 测试 Option<Option<Int64>>
            val level2 = level1.makeOption()
            assertEquals("Option<Option<Int64>> 的嵌套层级应该是 2", 2, level2.optionNestedLevel())

            // 测试 Option<Option<Option<Int64>>>
            val level3 = level2.makeOption()
            assertEquals("Option<Option<Option<Int64>>> 的嵌套层级应该是 3", 3, level3.optionNestedLevel())
        }
    }

    // ==================== Option 类型创建测试 ====================

    /**
     * 测试 makeOption 方法
     *
     * 验证：
     * 1. 正确创建 Option 类型
     * 2. Option 类型调用 makeOption 返回自身
     */
    fun `test makeOption method`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 测试基本类型创建 Option
            val optionInt64 = int64Type.makeOption()
            assertTrue("应该创建 Option 类型", optionInt64.isOptionType())

            // 测试 Option 类型调用 makeOption
            val optionOption = optionInt64.makeOption()
            assertTrue("应该创建嵌套 Option 类型", optionOption.isOptionType())
            assertEquals("嵌套层级应该是 2", 2, optionOption.optionNestedLevel())
        }
    }

    /**
     * 测试 makeNonOption 方法
     *
     * 验证：
     * 1. Option 类型解包为非 Option 类型
     * 2. 非 Option 类型保持不变
     */
    fun `test makeNonOption method`() {
        val file = createFile(
            """
            package test

            var x: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 测试 Option 类型解包
            val optionInt64 = int64Type.makeOption()
            val unwrapped = optionInt64.makeNonOption()
            assertFalse("解包后不应该是 Option 类型", unwrapped.isOptionType())
            assertEquals("解包后应该是 Int64", "Int64", unwrapped.toString())

            // 测试非 Option 类型
            val nonOption = int64Type.makeNonOption()
            assertEquals("非 Option 类型应该保持不变", int64Type, nonOption)
        }
    }

    // ==================== 完全解包测试 ====================

    /**
     * 测试 fullyUnwrapOption 方法
     *
     * 验证：
     * 1. 完全移除所有 Option 包装
     * 2. 非 Option 类型保持不变
     */
    fun `test fullyUnwrapOption method`() {
        val file = createFile(
            """
            package test

            var x: Option<Option<Option<Int64>>> = Some(Some(Some(42)))
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 创建多层嵌套
            val level3 = int64Type.makeOption().makeOption().makeOption()
            assertEquals("创建的类型应该有 3 层嵌套", 3, level3.optionNestedLevel())

            // 完全解包
            val fullyUnwrapped = level3.fullyUnwrapOption()
            assertFalse("完全解包后不应该是 Option 类型", fullyUnwrapped.isOptionType())
            assertEquals("完全解包后应该是 Int64", "Int64", fullyUnwrapped.toString())

            // 测试非 Option 类型
            val unwrapped = int64Type.fullyUnwrapOption()
            assertEquals("非 Option 类型应该保持不变", int64Type, unwrapped)
        }
    }

    // ==================== 隐式转换测试 ====================

    /**
     * 测试 canImplicitlyConvertToOption 方法
     *
     * 验证：
     * 1. T 可以隐式转换为 Option<T>
     * 2. Option<T> 不能隐式转换为 T
     * 3. 类型不匹配时返回 false
     */
    fun `test canImplicitlyConvertToOption method`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            var y: Option<Int64> = Some(42)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type
            val stringType = builtIns.stdlibTypes.stringType
            val optionInt64 = int64Type.makeOption()

            // 测试 T -> Option<T> 隐式转换
            assertTrue(
                "Int64 应该可以隐式转换为 Option<Int64>",
                OptionTypeUtils.canImplicitlyConvertToOption(int64Type, optionInt64)
            )

            // 测试 Option<T> -> T 不能隐式转换
            assertFalse(
                "Option<Int64> 不应该可以隐式转换为 Int64",
                OptionTypeUtils.canImplicitlyConvertToOption(optionInt64, int64Type)
            )

            // 测试类型不匹配
            val optionString = stringType.makeOption()
            assertFalse(
                "Int64 不应该可以隐式转换为 Option<String>",
                OptionTypeUtils.canImplicitlyConvertToOption(int64Type, optionString)
            )

            // 测试非 Option 到非 Option
            assertFalse(
                "Int64 不应该可以隐式转换为 String",
                OptionTypeUtils.canImplicitlyConvertToOption(int64Type, stringType)
            )
        }
    }

    // ==================== makeOptionalAsSpecified 测试 ====================

    /**
     * 测试 makeOptionalAsSpecified 方法
     *
     * 验证：
     * 1. shouldBeOption = true 时创建 Option 类型
     * 2. shouldBeOption = false 时保持原类型
     * 3. 已经是 Option 类型时不重复包装
     */
    fun `test makeOptionalAsSpecified method`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val int64Type = builtIns.int64Type

            // 测试 shouldBeOption = true
            val madeOption = int64Type.makeOptionalAsSpecified(true)
            assertTrue("shouldBeOption=true 应该创建 Option 类型", madeOption.isOptionType())

            // 测试 shouldBeOption = false
            val notOption = int64Type.makeOptionalAsSpecified(false)
            assertEquals("shouldBeOption=false 应该保持原类型", int64Type, notOption)

            // 测试已经是 Option 类型
            val optionInt64 = int64Type.makeOption()
            val stillOption = optionInt64.makeOptionalAsSpecified(true)
            assertEquals("已经是 Option 类型不应该重复包装", optionInt64, stillOption)
        }
    }

    // ==================== Option<Nothing> 特殊情况测试 ====================

    /**
     * 测试 Option<Nothing> 类型
     *
     * 验证：
     * 1. 正确识别为 Option 类型
     * 2. 解包后是 Nothing 类型
     * 3. 嵌套层级正确
     */
    fun `test Option Nothing type`() {
        val file = createFile(
            """
            package test

            var x: Option<Nothing> = None
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val nothingType = builtIns.nothingType

            // 创建 Option<Nothing>
            val optionNothing = nothingType.makeOption()
            assertTrue("Option<Nothing> 应该是 Option 类型", optionNothing.isOptionType())
            assertEquals("嵌套层级应该是 1", 1, optionNothing.optionNestedLevel())

            // 解包 Option<Nothing>
            val unwrapped = optionNothing.unwrapOptionType()
            assertNotNull("解包应该成功", unwrapped)
            assertTrue("解包后应该是 Nothing 类型", unwrapped?.isNothing() == true)

            // 测试 Option<Option<Nothing>>
            val optionOptionNothing = optionNothing.makeOption()
            assertEquals("嵌套层级应该是 2", 2, optionOptionNothing.optionNestedLevel())

            // 完全解包
            val fullyUnwrapped = optionOptionNothing.fullyUnwrapOption()
            assertTrue("完全解包后应该是 Nothing 类型", fullyUnwrapped.isNothing())
        }
    }
}
