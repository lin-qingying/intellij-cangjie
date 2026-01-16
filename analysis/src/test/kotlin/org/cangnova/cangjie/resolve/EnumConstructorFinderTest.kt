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
import org.cangnova.cangjie.name.Name
import kotlin.system.measureNanoTime

/**
 * 枚举构造器查找器测试
 *
 * 测试混合查找策略的正确性和性能。
 */
class EnumConstructorFinderTest : CangJieAnalysisTestBase() {

    // ==================== 功能测试 ====================

    /**
     * 测试快速路径：局部枚举
     */
    fun `test find local enum constructor`() {
        val file = createFile(
            """
            package test

            enum Color {
                Red |
                Green |
                Blue
            }

            func test(): Color {
                return Red
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 获取函数内部的作用域
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            // 查找 Red 构造器
            val constructors = EnumConstructorFinder.findConstructor(
                Name.identifier("Red"),
                arity = 0,
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            // 验证
            assertEquals(1, constructors.size)
            assertEquals("Red", constructors[0].name.asString())
            assertEquals(0, constructors[0].valueParameters.size)

            val enumDescriptor = constructors[0].containingDeclaration
            assertEquals("Color", enumDescriptor.name.asString())
        }
    }

    /**
     * 测试带参数的构造器
     */
    fun `test find constructor with parameters`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String)
            }

            func test(): Result {
                return Success(42)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            // 查找 Success 构造器（参数数量 = 1）
            val constructors = EnumConstructorFinder.findConstructor(
                Name.identifier("Success"),
                arity = 1,
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            assertEquals(1, constructors.size)
            assertEquals("Success", constructors[0].name.asString())
            assertEquals(1, constructors[0].valueParameters.size)
            assertEquals("Int64", constructors[0].valueParameters[0].type.toString())
        }
    }

    /**
     * 测试导入的枚举
     */
    fun `test find imported enum constructor`() {
        // 创建枚举定义文件
        val colorFile = createFile(
            """
            package com.example.colors

            public enum Color {
                Red |
                Green |
                Blue
            }
            """.trimIndent(),
            "Color.cj"
        )

        // 创建使用文件
        val mainFile = createFile(
            """
            package test

            import com.example.colors.Color

            func test(): Color {
                return Red
            }
            """.trimIndent(),
            "Main.cj"
        )

        analyzeForTest(mainFile) {
            val function = mainFile.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            val constructors = EnumConstructorFinder.findConstructor(
                Name.identifier("Red"),
                arity = 0,
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            assertEquals(1, constructors.size)
            assertEquals("Red", constructors[0].name.asString())
        }
    }

    /**
     * 测试歧义情况：多个同名构造器
     */
    fun `test ambiguous constructor names`() {
        val file = createFile(
            """
            package test

            import std.core.*

            enum Color { Red | Green }
            enum Status { Red | Yellow }

            func test1(): Color {
                return Red  // 应该找到两个 Red，需要类型推断消歧
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            val constructors = EnumConstructorFinder.findConstructor(
                Name.identifier("Red"),
                arity = 0,
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            // 应该找到两个 Red 构造器
            assertEquals(2, constructors.size)
            val names = constructors.map {
                (it.containingDeclaration as org.cangnova.cangjie.descriptors.EnumDescriptor).name.asString()
            }.toSet()
            assertTrue(names.contains("Color"))
            assertTrue(names.contains("Status"))
        }
    }

    /**
     * 测试不存在的构造器
     */
    fun `test non-existent constructor`() {
        val file = createFile(
            """
            package test

            enum Color { Red | Green | Blue }

            func test() {
                // 查找不存在的构造器
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            val constructors = EnumConstructorFinder.findConstructor(
                Name.identifier("Purple"),  // 不存在
                arity = 0,
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            assertEquals(0, constructors.size)
        }
    }

    // ==================== 性能测试 ====================

    /**
     * 性能对比：快速路径 vs 慢速路径
     */
    fun `test performance comparison`() {
        val file = createFile(
            """
            package test

            enum Color { Red | Green | Blue }
            enum Status { Active | Inactive }
            enum Result { Success(Int64) | Error(String) }

            func test(): Color {
                return Red
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!
            val searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)

            // 预热
            repeat(10) {
                EnumConstructorFinder.findConstructor(
                    Name.identifier("Red"),
                    0,
                    scope,
                    project,
                    searchScope
                )
            }

            // 测试快速路径（作用域查找）
            val scopeTime = measureNanoTime {
                repeat(100) {
                    EnumConstructorFinder.findInScopeOnly(
                        Name.identifier("Red"),
                        0,
                        scope
                    )
                }
            } / 100 / 1_000_000.0  // 转换为毫秒

            // 测试慢速路径（索引查找）
            val indexTime = measureNanoTime {
                repeat(100) {
                    EnumConstructorFinder.findByIndexOnly(
                        Name.identifier("Red"),
                        0,
                        scope,
                        project,
                        searchScope
                    )
                }
            } / 100 / 1_000_000.0  // 转换为毫秒

            println("作用域查找平均耗时: ${scopeTime}ms")
            println("索引查找平均耗时: ${indexTime}ms")

            // 验证两种方式结果一致
            val scopeResults = EnumConstructorFinder.findInScopeOnly(
                Name.identifier("Red"),
                0,
                scope
            )
            val indexResults = EnumConstructorFinder.findByIndexOnly(
                Name.identifier("Red"),
                0,
                scope,
                project,
                searchScope
            )

            assertEquals(scopeResults.size, indexResults.size)
        }
    }

    /**
     * 测试混合策略的性能
     */
    fun `test hybrid strategy performance`() {
        val file = createFile(
            """
            package test

            enum LocalEnum { Value1 | Value2 | Value3 }

            func test(): LocalEnum {
                return Value1
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!
            val searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)

            // 预热
            repeat(10) {
                EnumConstructorFinder.findConstructor(
                    Name.identifier("Value1"),
                    0,
                    scope,
                    project,
                    searchScope
                )
            }

            // 测试混合策略
            val hybridTime = measureNanoTime {
                repeat(100) {
                    EnumConstructorFinder.findConstructor(
                        Name.identifier("Value1"),
                        0,
                        scope,
                        project,
                        searchScope
                    )
                }
            } / 100 / 1_000_000.0

            println("混合策略平均耗时: ${hybridTime}ms")

            // 混合策略应该使用快速路径（因为是局部枚举）
            assertTrue("混合策略应该很快（< 2ms）", hybridTime < 2.0)
        }
    }

    // ==================== 边界情况测试 ====================

    /**
     * 测试参数数量不匹配
     */
    fun `test arity mismatch`() {
        val file = createFile(
            """
            package test

            enum Result {
                Success(Int64) |
                Error(String)
            }

            func test() {
                // 查找参数数量不匹配的构造器
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            // Success 有 1 个参数，但我们查找 0 个参数的
            val constructors = EnumConstructorFinder.findConstructor(
                Name.identifier("Success"),
                arity = 0,  // 错误的参数数量
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            assertEquals(0, constructors.size)
        }
    }

    /**
     * 测试 arity 为 null（不限制参数数量）
     */
    fun `test arity null matches all`() {
        val file = createFile(
            """
            package test

            enum Test {
                A |
                B(Int64) |
                C(String, Int64)
            }

            func test() {
                // 查找所有名为特定的构造器，不限制参数数量
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = file.declarations.filterIsInstance<org.cangnova.cangjie.psi.CjFunction>().first()
            val scope = bindingContext[org.cangnova.cangjie.resolve.binding.BindingContext.LEXICAL_SCOPE, function]!!

            // 查找 A（arity = null，应该匹配）
            val constructorsA = EnumConstructorFinder.findConstructor(
                Name.identifier("A"),
                arity = null,
                scope = scope,
                project = project,
                searchScope = com.intellij.psi.search.GlobalSearchScope.allScope(project)
            )

            assertEquals(1, constructorsA.size)
            assertEquals(0, constructorsA[0].valueParameters.size)
        }
    }
}
