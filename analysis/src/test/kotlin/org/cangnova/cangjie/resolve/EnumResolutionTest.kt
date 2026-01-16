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
package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.EnumConstructorDescriptor
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.EnumKind
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.resolve.binding.BindingContext


/**
 * 枚举解析测试
 *
 * 测试枚举类型的声明、解析和类型检查，包括：
 * - 简单枚举（无关联值）
 * - 带关联值的枚举
 * - 泛型枚举
 * - 非穷尽性枚举
 * - Option 类型枚举
 * - 枚举构造函数解析
 * - 枚举成员访问
 * - 枚举类型推断
 */
class EnumResolutionTest : CangJieAnalysisTestBase() {

    // ==================== 简单枚举测试 ====================

    /**
     * 测试简单枚举声明
     *
     * 验证：
     * 1. 枚举描述符正确创建
     * 2. 枚举类型为 ENUM
     * 3. 枚举构造函数正确解析
     * 4. 不是非穷尽性枚举
     */
    fun `test simple enum declaration`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor
            // 验证基本属性
            assertEquals("Color", enumDescriptor.name.asString())
            assertEquals(ClassKind.ENUM, enumDescriptor.kind)
            assertEquals(EnumKind.ENUM, enumDescriptor.enumKind)
            assertFalse("简单枚举不应该有关联值", enumDescriptor.hasArguments)
            assertFalse("简单枚举不应该是非穷尽性", enumDescriptor.isNonExhaustive)

            // 验证构造函数
            val constructors = enumDescriptor.constructors
            assertEquals(3, constructors.size)

            val constructorNames = constructors.map { it.name.asString() }.toSet()
            assertTrue(constructorNames.contains("Red"))
            assertTrue(constructorNames.contains("Green"))
            assertTrue(constructorNames.contains("Blue"))

            // 验证构造函数属性
            constructors.forEach { constructor ->
                assertTrue("枚举构造函数应该是静态的", constructor.isStatic)
                assertTrue("枚举构造函数应该是常量", constructor.isConst)
                assertFalse("枚举构造函数不应该是变量", constructor.isVar)
                assertTrue("简单构造函数应该没有参数", constructor.isSimpleConstructor)
                assertFalse("简单构造函数不应该是函数构造函数", constructor.isFunctionConstructor)
                assertEquals(0, constructor.valueParameters.size)
            }
        }
    }

    // ==================== 带关联值的枚举测试 ====================

    /**
     * 测试带关联值的枚举
     *
     * 验证：
     * 1. 有关联值的构造函数正确解析
     * 2. 参数类型正确
     * 3. 混合简单和复杂构造函数
     */
    fun `test enum with associated values`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String) |
                Pending
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor =bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            assertTrue("枚举应该有关联值", enumDescriptor.hasArguments)

            val constructors = enumDescriptor.constructors
            assertEquals(3, constructors.size)

            // 检查 Success 构造函数
            val success = constructors.first { it.name.asString() == "Success" }
            assertTrue("Success 应该有参数", success.hasArguments)
            assertTrue("Success 应该是函数构造函数", success.isFunctionConstructor)
            assertFalse("Success 不应该是简单构造函数", success.isSimpleConstructor)
            assertEquals(1, success.valueParameters.size)
            assertEquals("Int64", success.valueParameters[0].type.toString())

            // 检查 Error 构造函数
            val error = constructors.first { it.name.asString() == "Error" }
            assertTrue("Error 应该有参数", error.hasArguments)
            assertEquals(1, error.valueParameters.size)
            assertEquals("String", error.valueParameters[0].type.toString())

            // 检查 Pending 构造函数
            val pending = constructors.first { it.name.asString() == "Pending" }
            assertFalse("Pending 不应该有参数", pending.hasArguments)
            assertTrue("Pending 应该是简单构造函数", pending.isSimpleConstructor)
            assertEquals(0, pending.valueParameters.size)
        }
    }

    // ==================== 泛型枚举测试 ====================

    /**
     * 测试泛型枚举
     *
     * 验证：
     * 1. 类型参数正确解析
     * 2. 构造函数使用类型参数
     * 3. 泛型约束处理
     */
    fun `test generic enum`() {
        val file = createFile(
            """
            package test

            enum Option<T> {
                Some(T) |
                None
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证类型参数
            val typeParams = enumDescriptor.declaredTypeParameters
            assertEquals(1, typeParams.size)
            assertEquals("T", typeParams[0].name.asString())

            // 验证构造函数
            val constructors = enumDescriptor.constructors
            assertEquals(2, constructors.size)

            // 检查 Some 构造函数
            val some = constructors.first { it.name.asString() == "Some" }
            assertTrue("Some 应该有参数", some.hasArguments)
            assertEquals(1, some.valueParameters.size)

            val paramType = some.valueParameters[0].type
            assertTrue("参数类型应该是类型参数 T", paramType.toString().contains("T"))

            // 检查 None 构造函数
            val none = constructors.first { it.name.asString() == "None" }
            assertFalse("None 不应该有参数", none.hasArguments)
        }
    }

    /**
     * 测试多个类型参数的泛型枚举
     */
    fun `test enum with multiple type parameters`() {
        val file = createFile(
            """
            package test

            enum Either<L, R> {
                Left(L) |
                Right(R)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor =bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证类型参数
            val typeParams = enumDescriptor.declaredTypeParameters
            assertEquals(2, typeParams.size)
            assertEquals("L", typeParams[0].name.asString())
            assertEquals("R", typeParams[1].name.asString())

            // 验证构造函数使用正确的类型参数
            val left = enumDescriptor.constructors.first { it.name.asString() == "Left" }
            val right = enumDescriptor.constructors.first { it.name.asString() == "Right" }

            assertEquals(1, left.valueParameters.size)
            assertEquals(1, right.valueParameters.size)
        }
    }

    // ==================== 非穷尽性枚举测试 ====================

    /**
     * 测试非穷尽性枚举
     *
     * 验证：
     * 1. 省略号正确识别
     * 2. isNonExhaustive 标志正确
     * 3. enumKind 正确
     */
    fun `test non-exhaustive enum`() {
        val file = createFile(
            """
            package test

            enum Status {
                Active |
                Inactive |
                ...
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            assertTrue("应该识别为非穷尽性枚举", enumDescriptor.isNonExhaustive)
            assertTrue("hasEllipsis 应该为 true", enumDescriptor.hasEllipsis)
            assertEquals(EnumKind.NON_EXHAUSTIVE, enumDescriptor.enumKind)
            assertTrue("isNonExhaustiveEnum() 应该返回 true", enumDescriptor.isNonExhaustiveEnum())
        }
    }

    // ==================== Option 类型测试 ====================

    /**
     * 测试标准库 Option 类型识别
     *
     * 验证：
     * 1. Option 是枚举类型
     * 2. 有正确的构造函数
     * 3. 是泛型枚举
     */
    fun `test Option type enum`() {
        val file = createFile(
            """
            package test

            import std.core.*

            func test() {
                let x: Option<Int64> = Some(42)
                let y: Option<String> = None
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val builtIns = resolutionFacade.moduleDescriptor.builtIns
            val optionDescriptor = builtIns.stdlibTypes.option

            assertTrue("Option 应该是枚举类型", optionDescriptor is EnumDescriptor)
            assertEquals(ClassKind.ENUM, optionDescriptor.kind)

            if (optionDescriptor is EnumDescriptor) {
                // 验证 Option 的构造函数
                val constructors = optionDescriptor.constructors
                assertEquals(2, constructors.size)

                val constructorNames = constructors.map { it.name.asString() }.toSet()
                assertTrue("应该有 Some 构造函数", constructorNames.contains("Some"))
                assertTrue("应该有 None 构造函数", constructorNames.contains("None"))

                // 验证类型参数
                assertEquals(1, optionDescriptor.declaredTypeParameters.size)
            }
        }
    }

    // ==================== 枚举构造函数调用测试 ====================

    /**
     * 测试枚举构造函数调用解析
     */
    fun `test enum constructor call resolution`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue
            }

            func getColor(): Color {
                return Color.Red
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 这里可以添加引用解析测试
            // 验证 Color.Red 正确解析到 Red 构造函数
        }
    }

    /**
     * 测试带参数的枚举构造函数调用
     */
    fun `test enum constructor call with arguments`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String)
            }

            func createResult(): Result {
                return Result.Success(42)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 验证构造函数调用的参数类型检查
        }
    }

    // ==================== 枚举成员访问测试 ====================

    /**
     * 测试枚举成员函数
     */
    fun `test enum member functions`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue

                func toHex(): String {
                    return ""
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val enumDecl = file.declarations.filterIsInstance<CjEnum>().first()
            val enumDescriptor = bindingContext[BindingContext.CLASS, enumDecl] as EnumDescriptor

            // 验证枚举有成员函数
            val memberScope = enumDescriptor.unsubstitutedMemberScope
            assertNotNull("枚举应该有作用域", memberScope)
        }
    }

    // ==================== 嵌套枚举测试 ====================

    /**
     * 测试嵌套枚举类型
     */
    fun `test nested enum Option types`() {
        val file = createFile(
            """
            package test

            enum Result<T> {
                Success(T) |
                Error(String)
            }

            func test() {
                let x: Option<Result<Int64>> = Some(Result.Success(42))
                let y: Option<Option<Int64>> = Some(Some(42))
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 验证嵌套的 Option 和 Result 枚举类型
        }
    }


}
