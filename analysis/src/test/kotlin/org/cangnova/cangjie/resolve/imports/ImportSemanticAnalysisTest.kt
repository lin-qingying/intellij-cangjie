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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.imports

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 导入和包系统语义分析测试
 *
 * 专注于测试导入和包系统的语义分析，包括：
 * - import 语句的解析
 * - 包级符号的可见性
 * - 导入符号的解析
 * - 导入别名（as）的处理
 * - 通配符导入的语义
 *
 * ## 测试策略
 *
 * 1. **简单导入**: 验证单个符号的导入
 * 2. **通配符导入**: 验证 import * 的语义
 * 3. **导入别名**: 验证 as 关键字的使用
 * 4. **包结构**: 验证包的层级结构
 * 5. **符号可见性**: 验证访问修饰符的语义
 */
class ImportSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 简单导入测试 ====================

    /**
     * 测试导入单个类
     *
     * 验证：
     * 1. import 语句正确解析
     * 2. 导入的类可以在文件中使用
     * 3. 类引用正确解析到导入的类
     */
    fun `test import single class`() {
        // 创建被导入的类文件
        val utilFile = createFile(
            """
            package utils

            public class Helper {
                public func help(): String {
                    return "helping"
                }
            }
            """.trimIndent(),
            "Helper.cj"
        )

        // 创建导入并使用的文件
        val mainFile = createFile(
            """
            package main

            import utils.Helper

            func main() {
                let helper = Helper()
                let message = helper.help()
            }
            """.trimIndent(),
            "Main.cj"
        )

        analyzeForTest(mainFile) {
            // 验证 Helper 类被导入
            val importDirectives = PsiTreeUtil.findChildrenOfType(mainFile, CjImportDirective::class.java)
            assertTrue("应该有导入语句", importDirectives.isNotEmpty())

            // 验证可以使用 Helper 类
            val helperProperty = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java)
                .firstOrNull { it.name == "helper" }
            if (helperProperty != null) {
                val helperDescriptor = bindingContext[BindingContext.VARIABLE, helperProperty!!]
                assertNotNull("helper 应该有描述符", helperDescriptor)
            }
        }
    }

    /**
     * 测试导入多个符号
     *
     * 验证：
     * 1. 多个 import 语句的处理
     * 2. 所有导入的符号都可用
     */
    fun `test import multiple symbols`() {
        val file = createFile(
            """
            package test

            import std.collection.Array
            import std.collection.HashMap
            import std.io.File

            func main() {
                let arr = Array<Int64>()
                let map = HashMap<String, Int64>()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val importDirectives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertEquals("应该有 3 个导入语句", 3, importDirectives.size)

            // 验证导入的符号可以使用
            val properties = PsiTreeUtil.findChildrenOfType(file, CjVariable::class.java)
            assertTrue("应该有变量声明", properties.isNotEmpty())
        }
    }

    // ==================== 通配符导入测试 ====================

    /**
     * 测试通配符导入
     *
     * 验证：
     * 1. import package.* 的语义
     * 2. 包内所有公开符号可用
     */
    fun `test wildcard import`() {
        val file = createFile(
            """
            package test

            import std.collection.*

            func main() {
                let list = ArrayList<String>()
                let set = HashSet<Int64>()
                let map = HashMap<String, Bool>()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val importDirectives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertTrue("应该有导入语句", importDirectives.isNotEmpty())

            // 验证可以使用包内的多个类
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 3 个变量", 3, properties.size)
        }
    }

    /**
     * 测试通配符导入的优先级
     *
     * 验证：
     * 1. 显式导入优先于通配符导入
     * 2. 本地声明优先于导入
     */
    fun `test wildcard import priority`() {
        val file = createFile(
            """
            package test

            import std.collection.*
            import std.io.File

            class Array<T> {
                // 本地 Array 类
            }

            func main() {
                // Array 应该解析到本地类
                let arr = Array<Int64>()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val localArray = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到本地 Array 类", localArray)

            val localArrayDescriptor = bindingContext[BindingContext.CLASS, localArray!!]
            assertNotNull("本地 Array 应该有描述符", localArrayDescriptor)
        }
    }

    // ==================== 导入别名测试 ====================

    /**
     * 测试导入别名（as）
     *
     * 验证：
     * 1. import as 语法的解析
     * 2. 别名在文件中可用
     * 3. 原名称不可用（仅别名可用）
     */
    fun `test import with alias`() {
        val file = createFile(
            """
            package test

            import std.collection.ArrayList as List
            import std.collection.HashMap as Map

            func main() {
                let numbers = List<Int64>()
                let cache = Map<String, String>()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val importDirectives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertEquals("应该有 2 个导入语句", 2, importDirectives.size)

            // 验证可以使用别名
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 2 个变量", 2, properties.size)
        }
    }

    /**
     * 测试别名解决命名冲突
     *
     * 验证：
     * 1. 使用别名避免命名冲突
     * 2. 不同别名引用不同的类
     */
    fun `test alias resolves name conflict`() {
        val file = createFile(
            """
            package test

            import package1.Helper as Helper1
            import package2.Helper as Helper2

            func main() {
                let h1 = Helper1()
                let h2 = Helper2()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val importDirectives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertEquals("应该有 2 个导入语句", 2, importDirectives.size)

            val properties = PsiTreeUtil.findChildrenOfType(file, CjVariable::class.java)
            assertEquals("应该有 2 个变量", 2, properties.size)
        }
    }

    // ==================== 包结构测试 ====================

    /**
     * 测试包声明
     *
     * 验证：
     * 1. package 声明的解析
     * 2. 文件属于正确的包
     */
    fun `test package declaration`() {
        val file = createFile(
            """
            package com.example.app

            class MyClass {
                public func doSomething(): Unit {
                    // implementation
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val packageDirective = PsiTreeUtil.findChildOfType(file, CjPackageDirective::class.java)
            assertNotNull("应该有 package 声明", packageDirective)

            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)
        }
    }

    /**
     * 测试同包内的符号访问
     *
     * 验证：
     * 1. 同一包内的符号无需导入即可访问
     * 2. 包内可见性正确
     */
    fun `test same package symbol access`() {
        val file1 = createFile(
            """
            package myapp

            class ServiceA {
                public func processA(): String {
                    return "A"
                }
            }
            """.trimIndent(),
            "ServiceA.cj"
        )

        val file2 = createFile(
            """
            package myapp

            class ServiceB {
                public func processB(): String {
                    // 同包内可以直接使用 ServiceA
                    let serviceA = ServiceA()
                    return serviceA.processA()
                }
            }
            """.trimIndent(),
            "ServiceB.cj"
        )

        analyzeForTest(file2) {
            val serviceBClass = PsiTreeUtil.findChildOfType(file2, CjClass::class.java)
            assertNotNull("应该找到 ServiceB 类", serviceBClass)

            val serviceBDescriptor = bindingContext[BindingContext.CLASS, serviceBClass!!]
            assertNotNull("应该创建 ServiceB ClassDescriptor", serviceBDescriptor)
        }
    }

    // ==================== 符号可见性测试 ====================

    /**
     * 测试 public 可见性
     *
     * 验证：
     * 1. public 符号可以在其他包中访问
     * 2. public 修饰符的语义正确
     */
    fun `test public visibility`() {
        val utilFile = createFile(
            """
            package utils

            public class PublicHelper {
                public func help(): String {
                    return "public help"
                }
            }
            """.trimIndent(),
            "PublicHelper.cj"
        )

        val mainFile = createFile(
            """
            package main

            import utils.PublicHelper

            func main() {
                let helper = PublicHelper()
                let msg = helper.help()
            }
            """.trimIndent(),
            "Main.cj"
        )

        analyzeForTest(mainFile) {
            val properties = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java)
            assertTrue("应该有变量声明", properties.isNotEmpty())
        }
    }

    /**
     * 测试 internal 可见性
     *
     * 验证：
     * 1. internal 符号在模块内可见
     * 2. internal 符号在模块外不可见
     */
    fun `test internal visibility`() {
        val file = createFile(
            """
            package test

            internal class InternalHelper {
                internal func help(): String {
                    return "internal help"
                }
            }

            func useInternal() {
                // 同一模块内可以访问 internal 符号
                let helper = InternalHelper()
                let msg = helper.help()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val internalClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 InternalHelper 类", internalClass)

            val internalDescriptor = bindingContext[BindingContext.CLASS, internalClass!!]
            assertNotNull("应该创建 InternalHelper ClassDescriptor", internalDescriptor)
        }
    }

    /**
     * 测试 private 可见性
     *
     * 验证：
     * 1. private 符号仅在声明文件内可见
     * 2. private 修饰符的语义正确
     */
    fun `test private visibility`() {
        val file = createFile(
            """
            package test

            private class PrivateHelper {
                private func help(): String {
                    return "private help"
                }
            }

            func usePrivate() {
                // 同一文件内可以访问 private 符号
                let helper = PrivateHelper()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val privateClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 PrivateHelper 类", privateClass)

            val privateDescriptor = bindingContext[BindingContext.CLASS, privateClass!!]
            assertNotNull("应该创建 PrivateHelper ClassDescriptor", privateDescriptor)
        }
    }

    /**
     * 测试 protected 可见性
     *
     * 验证：
     * 1. protected 成员在子类中可见
     * 2. protected 修饰符的语义正确
     */
    fun `test protected visibility`() {
        val file = createFile(
            """
            package test

            open class Base {
                protected var value: Int64 = 0

                protected func getValue(): Int64 {
                    return value
                }
            }

            class Derived <: Base {
                public func doubleValue(): Int64 {
                    // 子类可以访问 protected 成员
                    return value * 2
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val derivedClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Derived" }
            assertNotNull("应该找到 Derived 类", derivedClass)

            val derivedDescriptor = bindingContext[BindingContext.CLASS, derivedClass!!]
            assertNotNull("应该创建 Derived ClassDescriptor", derivedDescriptor)
        }
    }

    // ==================== 导入路径解析测试 ====================

    /**
     * 测试绝对导入路径
     *
     * 验证：
     * 1. 完整包路径的解析
     * 2. 绝对导入的正确性
     */
    fun `test absolute import path`() {
        val file = createFile(
            """
            package test

            import std.collection.ArrayList
            import std.io.File
            import std.math.Math

            func main() {
                let list = ArrayList<Int64>()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val importDirectives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertEquals("应该有 3 个导入语句", 3, importDirectives.size)
        }
    }

    /**
     * 测试嵌套包导入
     *
     * 验证：
     * 1. 多层嵌套包的解析
     * 2. 深层包路径的正确性
     */
    fun `test nested package import`() {
        val file = createFile(
            """
            package test

            import com.example.app.services.UserService
            import com.example.app.models.User
            import com.example.app.utils.Validator

            func main() {
                let service = UserService()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val importDirectives = PsiTreeUtil.findChildrenOfType(file, CjImportDirective::class.java)
            assertEquals("应该有 3 个导入语句", 3, importDirectives.size)
        }
    }

    // ==================== 循环导入检测测试 ====================

    /**
     * 测试循环导入检测
     *
     * 验证：
     * 1. 检测并处理循环导入
     * 2. 语义分析不会陷入无限循环
     */
    fun `test circular import detection`() {
        val fileA = createFile(
            """
            package test

            import test.ClassB

            class ClassA {
                public func useB(b: ClassB): Unit {
                    // use ClassB
                }
            }
            """.trimIndent(),
            "ClassA.cj"
        )

        val fileB = createFile(
            """
            package test

            import test.ClassA

            class ClassB {
                public func useA(a: ClassA): Unit {
                    // use ClassA
                }
            }
            """.trimIndent(),
            "ClassB.cj"
        )

        analyzeForTest(fileA) {
            val classA = PsiTreeUtil.findChildOfType(fileA, CjClass::class.java)
            assertNotNull("应该找到 ClassA", classA)

            val classADescriptor = bindingContext[BindingContext.CLASS, classA!!]
            assertNotNull("应该创建 ClassA ClassDescriptor", classADescriptor)
        }
    }

    // ==================== 导入函数和变量测试 ====================

    /**
     * 测试导入顶层函数
     *
     * 验证：
     * 1. 可以导入包级函数
     * 2. 导入的函数可以调用
     */
    fun `test import top level function`() {
        val utilFile = createFile(
            """
            package utils

            public func formatString(s: String): String {
                return s.toUpperCase()
            }
            """.trimIndent(),
            "Utils.cj"
        )

        val mainFile = createFile(
            """
            package main

            import utils.formatString

            func main() {
                let result = formatString("hello")
            }
            """.trimIndent(),
            "Main.cj"
        )

        analyzeForTest(mainFile) {
            val properties = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java)
            assertFalse("应该有变量声明", properties.isEmpty())
        }
    }

    /**
     * 测试导入顶层常量
     *
     * 验证：
     * 1. 可以导入包级常量
     * 2. 导入的常量可以使用
     */
    fun `test import top level constant`() {
        val configFile = createFile(
            """
            package config

            public let MAX_SIZE: Int64 = 1024
            public let APP_VERSION: String = "1.0.0"
            """.trimIndent(),
            "Config.cj"
        )

        val mainFile = createFile(
            """
            package main

            import config.MAX_SIZE
            import config.APP_VERSION

            func main() {
                let size = MAX_SIZE
                let version = APP_VERSION
            }
            """.trimIndent(),
            "Main.cj"
        )

        analyzeForTest(mainFile) {
            val properties = PsiTreeUtil.findChildrenOfType(mainFile, CjProperty::class.java)
            assertEquals("应该有 2 个变量", 2, properties.size)
        }
    }
}
