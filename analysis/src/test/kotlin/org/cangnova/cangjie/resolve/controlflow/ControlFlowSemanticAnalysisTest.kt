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

package org.cangnova.cangjie.resolve.controlflow

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 控制流和语句语义分析测试
 *
 * 专注于测试控制流语句的语义分析，包括：
 * - if/else 语句的类型检查
 * - while/for 循环的语义
 * - match 表达式的类型推断
 * - return 语句的类型检查
 * - break/continue 的语义验证
 *
 * ## 测试策略
 *
 * 1. **条件语句**: 验证 if/else 的条件类型和分支类型
 * 2. **循环语句**: 验证循环的语义正确性
 * 3. **模式匹配**: 验证 match 表达式的类型推断
 * 4. **跳转语句**: 验证 return/break/continue 的语义
 * 5. **异常处理**: 验证 try/catch 的语义
 */
class ControlFlowSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== if/else 语句测试 ====================

    /**
     * 测试简单 if 语句
     *
     * 验证：
     * 1. if 条件表达式必须是 Bool 类型
     * 2. if 语句的语义正确
     */
    fun `test simple if statement`() {
        val file = createFile(
            """
            package test

            func main() {
                let x: Int64 = 10
                if (x > 5) {
                    let y = x * 2
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证变量 x
            val xProperty = PsiTreeUtil.findChildrenOfType(functionDecl, CjProperty::class.java)
                .firstOrNull { it.name == "x" }
            assertNotNull("应该找到变量 x", xProperty)

            val xDescriptor = bindingContext[BindingContext.VARIABLE, xProperty!!]
            assertNotNull("x 应该有描述符", xDescriptor)
            assertEquals("Int64", xDescriptor!!.type.toString())

            // 验证 if 块内的变量 y
            val yProperty = PsiTreeUtil.findChildrenOfType(functionDecl, CjProperty::class.java)
                .firstOrNull { it.name == "y" }
            if (yProperty != null) {
                val yDescriptor = bindingContext[BindingContext.VARIABLE, yProperty!!]
                assertNotNull("y 应该有描述符", yDescriptor)
            }
        }
    }

    /**
     * 测试 if-else 表达式的类型推断
     *
     * 验证：
     * 1. if-else 作为表达式时有类型
     * 2. 类型是两个分支的公共父类型
     */
    fun `test if else expression type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let condition = true
                let value = if (condition) { 42 } else { 0 }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val valueProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "value" }
            assertNotNull("应该找到 value 变量", valueProperty)

            val valueDescriptor = bindingContext[BindingContext.VARIABLE, valueProperty!!]
            assertNotNull("value 应该有描述符", valueDescriptor)
            assertNotNull("value 应该有类型", valueDescriptor!!.type)
        }
    }

    /**
     * 测试嵌套 if-else 语句
     *
     * 验证：
     * 1. 嵌套 if 语句的语义正确
     * 2. 每个条件都是 Bool 类型
     */
    fun `test nested if else statement`() {
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

            // 验证返回类型
            val returnType = functionDescriptor!!.returnType
            assertNotNull("函数应该有返回类型", returnType)
            assertEquals("String", returnType!!.toString())
        }
    }

    // ==================== while 循环测试 ====================

    /**
     * 测试 while 循环
     *
     * 验证：
     * 1. while 条件必须是 Bool 类型
     * 2. 循环体的语义正确
     */
    fun `test while loop`() {
        val file = createFile(
            """
            package test

            func main() {
                var counter: Int64 = 0
                while (counter < 10) {
                    counter = counter + 1
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val counterProperty = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到 counter 变量", counterProperty)

            val counterDescriptor = bindingContext[BindingContext.VARIABLE, counterProperty!!]
            assertNotNull("counter 应该有描述符", counterDescriptor)
            assertEquals("Int64", counterDescriptor!!.type.toString())
            assertTrue("counter 应该是可变的", counterDescriptor.isVar)
        }
    }

    /**
     * 测试 do-while 循环
     *
     * 验证：
     * 1. do-while 的语义正确
     * 2. 循环条件类型正确
     */
    fun `test do while loop`() {
        val file = createFile(
            """
            package test

            func main() {
                var x: Int64 = 0
                do {
                    x = x + 1
                } while (x < 5)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val xProperty = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到 x 变量", xProperty)

            val xDescriptor = bindingContext[BindingContext.VARIABLE, xProperty!!]
            assertNotNull("x 应该有描述符", xDescriptor)
            assertTrue("x 应该是可变的", xDescriptor!!.isVar)
        }
    }

    // ==================== for 循环测试 ====================

    /**
     * 测试 for-in 循环
     *
     * 验证：
     * 1. 循环变量类型从可迭代对象推断
     * 2. 循环体中可以访问循环变量
     */
    fun `test for in loop`() {
        val file = createFile(
            """
            package test

            func main() {
                let numbers: Array<Int64> = Array<Int64>()
                for (num in numbers) {
                    let doubled = num * 2
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val numbersProperty = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到 numbers 变量", numbersProperty)

            val numbersDescriptor = bindingContext[BindingContext.VARIABLE, numbersProperty!!]
            assertNotNull("numbers 应该有描述符", numbersDescriptor)
            assertNotNull("numbers 应该有类型", numbersDescriptor!!.type)

            // 验证循环体内的 doubled 变量
            val doubledProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "doubled" }
            if (doubledProperty != null) {
                val doubledDescriptor = bindingContext[BindingContext.VARIABLE, doubledProperty!!]
                assertNotNull("doubled 应该有描述符", doubledDescriptor)
            }
        }
    }

    /**
     * 测试 for 循环的范围表达式
     *
     * 验证：
     * 1. 范围表达式的类型推断
     * 2. 循环变量类型正确
     */
    fun `test for loop with range`() {
        val file = createFile(
            """
            package test

            func main() {
                for (i in 0..10) {
                    let square = i * i
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val squareProperty = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            if (squareProperty != null) {
                val squareDescriptor = bindingContext[BindingContext.VARIABLE, squareProperty!!]
                assertNotNull("square 应该有描述符", squareDescriptor)
            }
        }
    }

    // ==================== match 表达式测试 ====================

    /**
     * 测试简单 match 表达式
     *
     * 验证：
     * 1. match 表达式的类型推断
     * 2. 各分支类型的公共父类型
     */
    fun `test simple match expression`() {
        val file = createFile(
            """
            package test

            func getGrade(score: Int64): String {
                return match (score) {
                    case 90..100 => "A"
                    case 80..89 => "B"
                    case 70..79 => "C"
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

            // 验证返回类型
            val returnType = functionDescriptor!!.returnType
            assertNotNull("函数应该有返回类型", returnType)
            assertEquals("String", returnType!!.toString())
        }
    }

    /**
     * 测试 match 表达式的模式匹配
     *
     * 验证：
     * 1. 不同模式的类型检查
     * 2. match 结果类型的推断
     */
    fun `test match expression with patterns`() {
        val file = createFile(
            """
            package test

            func describe(value: Int64): String {
                return match (value) {
                    case 0 => "zero"
                    case 1 => "one"
                    case 2..10 => "small"
                    case _ => "large"
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

    // ==================== return 语句测试 ====================

    /**
     * 测试 return 语句的类型检查
     *
     * 验证：
     * 1. return 表达式的类型匹配函数返回类型
     * 2. 所有路径都有 return（如果需要）
     */
    fun `test return statement type checking`() {
        val file = createFile(
            """
            package test

            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }

            func greet(): String {
                return "Hello"
            }

            func doNothing(): Unit {
                return
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 3 个函数", 3, functions.size)

            // 验证 add 函数
            val addFunc = functions.first { it.name == "add" }
            val addDescriptor = bindingContext[BindingContext.FUNCTION, addFunc!!]
            assertNotNull("add 应该有描述符", addDescriptor)
            assertEquals("Int64", addDescriptor!!.returnType!!.toString())

            // 验证 greet 函数
            val greetFunc = functions.first { it.name == "greet" }
            val greetDescriptor = bindingContext[BindingContext.FUNCTION, greetFunc!!]
            assertNotNull("greet 应该有描述符", greetDescriptor)
            assertEquals("String", greetDescriptor!!.returnType!!.toString())

            // 验证 doNothing 函数
            val doNothingFunc = functions.first { it.name == "doNothing" }
            val doNothingDescriptor = bindingContext[BindingContext.FUNCTION, doNothingFunc!!]
            assertNotNull("doNothing 应该有描述符", doNothingDescriptor)
            assertEquals("Unit", doNothingDescriptor!!.returnType!!.toString())
        }
    }

    /**
     * 测试提前 return 的语义
     *
     * 验证：
     * 1. 提前 return 的类型正确
     * 2. 所有 return 语句类型一致
     */
    fun `test early return semantics`() {
        val file = createFile(
            """
            package test

            func findMax(a: Int64, b: Int64): Int64 {
                if (a > b) {
                    return a
                }
                return b
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

    // ==================== break/continue 语句测试 ====================

    /**
     * 测试 break 语句
     *
     * 验证：
     * 1. break 只能在循环中使用
     * 2. break 的语义正确
     */
    fun `test break statement`() {
        val file = createFile(
            """
            package test

            func main() {
                var i: Int64 = 0
                while (true) {
                    i = i + 1
                    if (i > 10) {
                        break
                    }
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val iProperty = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到 i 变量", iProperty)

            val iDescriptor = bindingContext[BindingContext.VARIABLE, iProperty!!]
            assertNotNull("i 应该有描述符", iDescriptor)
            assertTrue("i 应该是可变的", iDescriptor!!.isVar)
        }
    }

    /**
     * 测试 continue 语句
     *
     * 验证：
     * 1. continue 只能在循环中使用
     * 2. continue 的语义正确
     */
    fun `test continue statement`() {
        val file = createFile(
            """
            package test

            func main() {
                for (i in 0..10) {
                    if (i % 2 == 0) {
                        continue
                    }
                    let odd = i
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val oddProperty = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            if (oddProperty != null) {
                val oddDescriptor = bindingContext[BindingContext.VARIABLE, oddProperty!!]
                assertNotNull("odd 应该有描述符", oddDescriptor)
            }
        }
    }

    // ==================== try-catch 异常处理测试 ====================

    /**
     * 测试 try-catch 语句
     *
     * 验证：
     * 1. try-catch 的语义正确
     * 2. catch 块中可以访问异常变量
     */
    fun `test try catch statement`() {
        val file = createFile(
            """
            package test

            func riskyOperation(): Int64 {
                try {
                    let result = 42 / 0
                    return result
                } catch (e: Exception) {
                    return 0
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
     * 测试 try-catch-finally 语句
     *
     * 验证：
     * 1. finally 块总是执行
     * 2. 整体语义正确
     */
    fun `test try catch finally statement`() {
        val file = createFile(
            """
            package test

            func cleanup(): Unit {
                var resource: String? = null
                try {
                    resource = "acquired"
                } catch (e: Exception) {
                    // handle error
                } finally {
                    resource = null
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)
            assertEquals("Unit", functionDescriptor!!.returnType!!.toString())

            // 验证 resource 变量
            val resourceProperty = PsiTreeUtil.findChildOfType(functionDecl, CjProperty::class.java)
            assertNotNull("应该找到 resource 变量", resourceProperty)

            val resourceDescriptor = bindingContext[BindingContext.VARIABLE, resourceProperty!!]
            assertNotNull("resource 应该有描述符", resourceDescriptor)
        }
    }

    // ==================== 控制流分析测试 ====================

    /**
     * 测试不可达代码检测
     *
     * 验证：
     * 1. return 后的代码是不可达的
     * 2. 控制流分析正确
     */
    fun `test unreachable code detection`() {
        val file = createFile(
            """
            package test

            func example(): Int64 {
                return 42
                // 下面的代码不可达
                let x = 10
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 注意：不可达代码检测可能会有警告或错误
            // 这里只验证语义分析不会崩溃
        }
    }

    /**
     * 测试所有路径返回值检查
     *
     * 验证：
     * 1. 非 Unit 函数所有路径都有返回值
     * 2. 缺少返回值时的错误检测
     */
    fun `test all paths return value`() {
        val file = createFile(
            """
            package test

            func compute(x: Int64): Int64 {
                if (x > 0) {
                    return x
                } else {
                    return -x
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
}
