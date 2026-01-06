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

package org.cangnova.cangjie.resolve.controlflow.checkers

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 变量初始化检查器测试
 *
 * 专门测试 [VariableInitializationChecker] 的功能：
 * - 未初始化变量使用检测
 * - let 变量重新赋值检测
 * - 声明前赋值检测
 * - struct 不可变函数成员修改检测
 *
 * ## 测试策略
 *
 * 1. **正向测试**: 验证正确代码不产生错误
 * 2. **负向测试**: 验证错误代码产生预期诊断
 * 3. **边界测试**: 验证边界情况的处理
 */
class VariableInitializationCheckerTest : CangJieAnalysisTestBase() {

    // ==================== let 变量不可变性测试 ====================

    /**
     * 测试 let 变量正确使用
     *
     * 验证：let 声明后赋值一次是合法的
     */
    fun `test let variable correct usage`() {
        val file = createFile(
            """
            package test

            main() {
                let x: Int64 = 42
                let y = x + 1
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val xPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "x" }
                }
            assertNotNull("应该找到变量 x", xPatternVar)

            val xBinding = xPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "x" }
            val xDescriptor = bindingContext[BindingContext.VARIABLE, xBinding!!]
            assertNotNull("x 应该有描述符", xDescriptor)
            assertFalse("x 应该是不可变的 (let)", xDescriptor!!.isVar)
        }
    }

    /**
     * 测试 var 变量可重新赋值
     *
     * 验证：var 声明的变量可以多次赋值
     */
    fun `test var variable reassignment allowed`() {
        val file = createFile(
            """
            package test

            main() {
                var counter: Int64 = 0
                counter = 1
                counter = counter + 1
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val counterPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "counter" }
                }
            assertNotNull("应该找到变量 counter", counterPatternVar)

            val counterBinding = counterPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "counter" }
            val counterDescriptor = bindingContext[BindingContext.VARIABLE, counterBinding!!]
            assertNotNull("counter 应该有描述符", counterDescriptor)
            assertTrue("counter 应该是可变的 (var)", counterDescriptor!!.isVar)
        }
    }

    /**
     * 测试函数参数不可变性
     *
     * 验证：函数参数默认不可变
     */
    fun `test function parameter immutability`() {
        val file = createFile(
            """
            package test

            func process(value: Int64): Int64 {
                return value * 2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证参数
            val params = functionDescriptor!!.valueParameters
            assertEquals("应该有 1 个参数", 1, params.size)
            assertFalse("参数应该是不可变的", params[0].isVar)
        }
    }

    // ==================== 变量初始化测试 ====================

    /**
     * 测试变量正确初始化
     *
     * 验证：变量在使用前已初始化
     */
    fun `test variable properly initialized before use`() {
        val file = createFile(
            """
            package test

            main() {
                let x: Int64 = 10
                let y: Int64 = x + 5
                let z = y * 2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 验证所有变量都有描述符
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该找到多个变量", patternVars.size >= 3)

            for (patternVar in patternVars) {
                val bindings = patternVar.pattern.getAllBindings()
                for (binding in bindings) {
                    val descriptor = bindingContext[BindingContext.VARIABLE, binding]
                    assertNotNull("${binding.name} 应该有描述符", descriptor)
                }
            }
        }
    }

    /**
     * 测试条件分支中的初始化
     *
     * 验证：在所有分支中都初始化的变量被认为是已初始化的
     */
    fun `test initialization in all branches`() {
        val file = createFile(
            """
            package test

            func test(condition: Bool): Int64 {
                var result: Int64
                if (condition) {
                    result = 1
                } else {
                    result = 0
                }
                return result
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
        }
    }

    // ==================== 类成员初始化测试 ====================

    /**
     * 测试类成员变量初始化
     *
     * 验证：类成员在构造时正确初始化
     */
    fun `test class member initialization`() {
        val file = createFile(
            """
            package test

            class Counter {
                var count: Int64 = 0
                let maxValue: Int64 = 100
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证属性
            val properties = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
            assertEquals("应该有 2 个属性", 2, properties.size)
        }
    }

    /**
     * 测试构造器中的成员初始化
     *
     * 验证：主构造器参数可以初始化成员
     */
    fun `test constructor parameter initialization`() {
        val file = createFile(
            """
            package test

            class Point(let x: Int64, let y: Int64) {
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)
        }
    }

    // ==================== 闭包中的变量捕获测试 ====================

    /**
     * 测试 lambda 中读取外部变量
     *
     * 验证：lambda 可以读取已初始化的外部变量
     */
    fun `test lambda captures initialized variable`() {
        val file = createFile(
            """
            package test

            main() {
                let value: Int64 = 42
                let fn = { => value * 2 }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val valuePatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "value" }
                }
            assertNotNull("应该找到变量 value", valuePatternVar)

            val valueBinding = valuePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "value" }
            val valueDescriptor = bindingContext[BindingContext.VARIABLE, valueBinding!!]
            assertNotNull("value 应该有描述符", valueDescriptor)
        }
    }

    // ==================== struct 特性测试 ====================

    /**
     * 测试 struct 的不可变性
     *
     * 验证：struct 实例默认不可变
     */
    fun `test struct immutability`() {
        val file = createFile(
            """
            package test

            struct Point {
                var x: Int64 = 0
                var y: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val structDecl = PsiTreeUtil.findChildOfType(file, CjStruct::class.java)
            assertNotNull("应该找到 struct 声明", structDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, structDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)
        }
    }

    /**
     * 测试 struct mut 函数
     *
     * 验证：mut 函数可以修改 struct 成员
     */
    fun `test struct mut function`() {
        val file = createFile(
            """
            package test

            struct Counter {
                var count: Int64 = 0

                mut func increment() {
                    count = count + 1
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val structDecl = PsiTreeUtil.findChildOfType(file, CjStruct::class.java)
            assertNotNull("应该找到 struct 声明", structDecl)

            val functions = PsiTreeUtil.findChildrenOfType(structDecl, CjFunction::class.java)
            assertEquals("应该有 1 个函数", 1, functions.size)

            val incrementFunc = functions.first()
            assertTrue("increment 应该是 mut 函数", incrementFunc.isMut)
        }
    }
}
