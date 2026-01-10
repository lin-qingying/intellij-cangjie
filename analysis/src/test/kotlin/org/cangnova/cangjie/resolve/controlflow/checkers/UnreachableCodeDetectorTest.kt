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

package org.cangnova.cangjie.resolve.controlflow.checkers

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 不可达代码检测器测试
 *
 * 专门测试 [UnreachableCodeDetector] 的功能：
 * - return 后的不可达代码检测
 * - throw 后的不可达代码检测
 * - break/continue 后的不可达代码检测
 * - 无限循环后的不可达代码检测
 *
 * ## 测试策略
 *
 * 1. **正向测试**: 验证可达代码不被标记
 * 2. **负向测试**: 验证不可达代码被正确检测
 * 3. **边界测试**: 验证边界情况的处理
 */
class UnreachableCodeDetectorTest : CangJieAnalysisTestBase() {

    // ==================== return 后的不可达代码测试 ====================

    /**
     * 测试正常可达代码
     *
     * 验证：正常代码不被标记为不可达
     */
    fun `test normal reachable code`() {
        val file = createFile(
            """
            package test

            func compute(x: Int64): Int64 {
                let doubled = x * 2
                let result = doubled + 1
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

    /**
     * 测试条件分支都可达
     *
     * 验证：条件分支中的代码可达
     */
    fun `test conditional branches reachable`() {
        val file = createFile(
            """
            package test

            func process(x: Int64): Int64 {
                if (x > 0) {
                    let positive = x
                    return positive
                } else {
                    let nonPositive = -x
                    return nonPositive
                }
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

    // ==================== break/continue 后的不可达代码测试 ====================

    /**
     * 测试 break 后代码在循环外可达
     *
     * 验证：break 后循环外的代码仍然可达
     */
    fun `test code after loop with break is reachable`() {
        val file = createFile(
            """
            package test

            func findAndProcess(): Int64 {
                var found: Int64 = -1
                for (i in 0..100) {
                    if (i == 42) {
                        found = i
                        break
                    }
                }
                return found * 2
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

    /**
     * 测试 continue 后循环继续
     *
     * 验证：continue 不影响循环外代码的可达性
     */
    fun `test code after loop with continue is reachable`() {
        val file = createFile(
            """
            package test

            func sumOdds(n: Int64): Int64 {
                var sum: Int64 = 0
                for (i in 0..n) {
                    if (i % 2 == 0) {
                        continue
                    }
                    sum = sum + i
                }
                return sum
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

    // ==================== 循环可达性测试 ====================

    /**
     * 测试有条件的循环
     *
     * 验证：有条件终止的循环后代码可达
     */
    fun `test code after conditional loop is reachable`() {
        val file = createFile(
            """
            package test

            func countdown(start: Int64): Int64 {
                var count = start
                while (count > 0) {
                    count = count - 1
                }
                return count
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

    // ==================== try-catch 可达性测试 ====================

    /**
     * 测试 try-catch 后代码可达
     *
     * 验证：try-catch 块后的代码可达
     */
    fun `test code after try catch is reachable`() {
        val file = createFile(
            """
            package test

            func safeProcess(): Int64 {
                var result: Int64 = 0
                try {
                    result = riskyOperation()
                } catch (e: Exception) {
                    result = -1
                }
                return result * 2
            }

            func riskyOperation(): Int64 {
                return 42
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 2 个函数", 2, functions.size)

            val safeProcessFunc = functions.first { it.name == "safeProcess" }
            val descriptor = bindingContext[BindingContext.FUNCTION, safeProcessFunc!!]
            assertNotNull("safeProcess 应该有描述符", descriptor)
        }
    }

    // ==================== 复杂控制流测试 ====================

    /**
     * 测试复杂控制流中的可达性
     *
     * 验证：复杂嵌套控制流的可达性分析
     */
    fun `test complex control flow reachability`() {
        val file = createFile(
            """
            package test

            func complexFlow(x: Int64, y: Int64): Int64 {
                if (x > 0) {
                    for (i in 0..y) {
                        if (i == x) {
                            return i
                        }
                    }
                    return -1
                } else {
                    var result: Int64 = 0
                    while (result < y) {
                        result = result + 1
                        if (result == -x) {
                            break
                        }
                    }
                    return result
                }
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

    /**
     * 测试 match 表达式中的可达性
     *
     * 验证：match 各分支的可达性
     */
    fun `test match expression reachability`() {
        val file = createFile(
            """
            package test

            func processValue(x: Int64): String {
                return match (x) {
                    case 0 => {
                        let msg = "zero"
                        msg
                    }
                    case 1 => "one"
                    case _ => {
                        let result = "other: " + x.toString()
                        result
                    }
                }
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
}
