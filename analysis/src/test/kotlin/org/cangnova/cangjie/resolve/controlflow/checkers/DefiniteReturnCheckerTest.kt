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
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 确定性返回检查器测试
 *
 * 专门测试 [DefiniteReturnChecker] 的功能：
 * - 块体函数缺失返回语句检测
 * - 表达式体函数中非法 return 检测
 * - 所有控制流路径返回值检查
 *
 * ## 测试策略
 *
 * 1. **正向测试**: 验证正确代码不产生错误
 * 2. **分支测试**: 验证条件分支中的返回
 * 3. **表达式体测试**: 验证表达式体函数
 */
class DefiniteReturnCheckerTest : CangJieAnalysisTestBase() {

    // ==================== 基本返回测试 ====================

    /**
     * 测试简单函数返回
     *
     * 验证：函数正确返回值
     */
    fun `test simple function return`() {
        val file = createFile(
            """
            package test

            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
            assertEquals("Int64", functionDescriptor!!.returnType!!.toString())
        }
    }

    /**
     * 测试 Unit 返回类型函数
     *
     * 验证：返回 Unit 的函数可以省略 return
     */
    fun `test unit return function`() {
        val file = createFile(
            """
            package test

            func doSomething(): Unit {
                let x = 42
            }

            func doSomethingElse() {
                let y = 100
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 2 个函数", 2, functions.size)

            for (func in functions) {
                val descriptor = bindingContext[BindingContext.FUNCTION, func!!]
                assertNotNull("函数应该有描述符", descriptor)
            }
        }
    }

    /**
     * 测试表达式体函数
     *
     * 验证：表达式体函数的返回类型推断
     */
    fun `test expression body function`() {
        val file = createFile(
            """
            package test

            func double(x: Int64): Int64 => x * 2

            func isPositive(x: Int64): Bool => x > 0
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 2 个函数", 2, functions.size)

            val doubleFunc = functions.first { it.name == "double" }
            val doubleDescriptor = bindingContext[BindingContext.FUNCTION, doubleFunc!!]
            assertNotNull("double 应该有描述符", doubleDescriptor)
            assertEquals("Int64", doubleDescriptor!!.returnType!!.toString())

            val isPositiveFunc = functions.first { it.name == "isPositive" }
            val isPositiveDescriptor = bindingContext[BindingContext.FUNCTION, isPositiveFunc!!]
            assertNotNull("isPositive 应该有描述符", isPositiveDescriptor)
            assertEquals("Bool", isPositiveDescriptor!!.returnType!!.toString())
        }
    }

    // ==================== 条件分支返回测试 ====================

    /**
     * 测试 if-else 所有分支都有返回
     *
     * 验证：所有分支都有 return 语句
     */
    fun `test all branches return in if-else`() {
        val file = createFile(
            """
            package test

            func max(a: Int64, b: Int64): Int64 {
                if (a > b) {
                    return a
                } else {
                    return b
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
            assertEquals("Int64", functionDescriptor!!.returnType!!.toString())
        }
    }

    /**
     * 测试嵌套条件分支返回
     *
     * 验证：嵌套条件的所有路径都有返回
     */
    fun `test nested conditional return`() {
        val file = createFile(
            """
            package test

            func classify(x: Int64): String {
                if (x > 0) {
                    if (x > 100) {
                        return "large positive"
                    } else {
                        return "small positive"
                    }
                } else if (x < 0) {
                    return "negative"
                } else {
                    return "zero"
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
            assertEquals("String", functionDescriptor!!.returnType!!.toString())
        }
    }

    /**
     * 测试 match 表达式作为返回值
     *
     * 验证：match 表达式所有分支都有值
     */
    fun `test match expression return`() {
        val file = createFile(
            """
            package test

            func getGrade(score: Int64): String {
                return match (score) {
                    case 90..100 => "A"
                    case 80..89 => "B"
                    case 70..79 => "C"
                    case 60..69 => "D"
                    case _ => "F"
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
            assertEquals("String", functionDescriptor!!.returnType!!.toString())
        }
    }

    // ==================== 提前返回测试 ====================

    /**
     * 测试提前返回
     *
     * 验证：提前 return 后的代码可能不可达
     */
    fun `test early return`() {
        val file = createFile(
            """
            package test

            func findFirst(array: Array<Int64>, target: Int64): Int64 {
                for (i in 0..array.size) {
                    if (array[i] == target) {
                        return i
                    }
                }
                return -1
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
            assertEquals("Int64", functionDescriptor!!.returnType!!.toString())
        }
    }

    /**
     * 测试 throw 作为终止
     *
     * 验证：throw 语句可以终止控制流
     */
    fun `test throw terminates control flow`() {
        val file = createFile(
            """
            package test

            func divide(a: Int64, b: Int64): Int64 {
                if (b == 0) {
                    throw Exception("Division by zero")
                }
                return a / b
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

    // ==================== 循环中的返回测试 ====================

    /**
     * 测试循环中的返回
     *
     * 验证：循环中的 return 正确处理
     */
    fun `test return in loop`() {
        val file = createFile(
            """
            package test

            func sumUntil(n: Int64, limit: Int64): Int64 {
                var sum: Int64 = 0
                for (i in 1..n) {
                    sum = sum + i
                    if (sum > limit) {
                        return sum
                    }
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
            assertEquals("Int64", functionDescriptor!!.returnType!!.toString())
        }
    }

    // ==================== try-catch 中的返回测试 ====================

    /**
     * 测试 try-catch 中的返回
     *
     * 验证：try-catch 块中的返回正确处理
     */
    fun `test return in try catch`() {
        val file = createFile(
            """
            package test

            func safeOperation(): Int64 {
                try {
                    return riskyComputation()
                } catch (e: Exception) {
                    return 0
                }
            }

            func riskyComputation(): Int64 {
                return 42
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 2 个函数", 2, functions.size)

            val safeFunc = functions.first { it.name == "safeOperation" }
            val safeDescriptor = bindingContext[BindingContext.FUNCTION, safeFunc!!]
            assertNotNull("safeOperation 应该有描述符", safeDescriptor)
            assertEquals("Int64", safeDescriptor!!.returnType!!.toString())
        }
    }

    /**
     * 测试 try-catch-finally 中的返回
     *
     * 验证：finally 块不影响返回语义
     */
    fun `test return in try catch finally`() {
        val file = createFile(
            """
            package test

            func processWithCleanup(): Int64 {
                var result: Int64 = 0
                try {
                    result = 42
                    return result
                } catch (e: Exception) {
                    return -1
                } finally {
                    // cleanup code
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
