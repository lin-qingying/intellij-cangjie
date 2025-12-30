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

package org.cangnova.cangjie.resolve.expression

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 表达式类型推断语义分析测试
 *
 * 专注于测试各种表达式的类型推断，包括：
 * - 字面量表达式的类型
 * - 二元运算符表达式的类型推断
 * - 函数调用表达式的返回类型
 * - 成员访问表达式的类型
 * - 类型转换表达式
 *
 * ## 测试策略
 *
 * 1. **字面量类型**: 验证各种字面量的推断类型
 * 2. **运算符类型**: 验证算术、比较、逻辑运算符的类型
 * 3. **调用表达式**: 验证函数调用的返回类型推断
 * 4. **成员访问**: 验证属性访问和方法调用的类型
 * 5. **复合表达式**: 验证复杂表达式的类型推断
 */
class ExpressionTypeInferenceTest : CangJieAnalysisTestBase() {

    // ==================== 字面量类型推断测试 ====================

    /**
     * 测试整数字面量类型推断
     *
     * 验证：
     * 1. 整数字面量被推断为正确的整数类型
     * 2. 不同大小的整数字面量类型正确
     */
    fun `test integer literal type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let a = 42
                let b = 100000000000
                let c: Int8 = 10
                let d: Int64 = 999
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 4 个变量", 4, properties.size)

            // 验证变量 a 的类型推断
            val aProperty = properties.first { it.name == "a" }
            val aDescriptor = bindingContext[BindingContext.VARIABLE, aProperty!!]
            assertNotNull("a 应该有描述符", aDescriptor)
            assertNotNull("a 应该有推断类型", aDescriptor!!.type)

            // 验证变量 c 显式类型
            val cProperty = properties.first { it.name == "c" }
            val cDescriptor = bindingContext[BindingContext.VARIABLE, cProperty!!]
            assertNotNull("c 应该有描述符", cDescriptor)
            assertEquals("Int8", cDescriptor!!.type.toString())

            // 验证变量 d 显式类型
            val dProperty = properties.first { it.name == "d" }
            val dDescriptor = bindingContext[BindingContext.VARIABLE, dProperty!!]
            assertNotNull("d 应该有描述符", dDescriptor)
            assertEquals("Int64", dDescriptor!!.type.toString())
        }
    }

    /**
     * 测试浮点数字面量类型推断
     *
     * 验证：
     * 1. 浮点数字面量被推断为正确的浮点类型
     */
    fun `test floating point literal type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let pi = 3.14
                let e: Float32 = 2.71
                let large: Float64 = 1.23456789
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 3 个变量", 3, properties.size)

            // 验证 pi 的类型推断
            val piProperty = properties.first { it.name == "pi" }
            val piDescriptor = bindingContext[BindingContext.VARIABLE, piProperty!!]
            assertNotNull("pi 应该有描述符", piDescriptor)
            assertNotNull("pi 应该有推断类型", piDescriptor!!.type)

            // 验证 e 的显式类型
            val eProperty = properties.first { it.name == "e" }
            val eDescriptor = bindingContext[BindingContext.VARIABLE, eProperty!!]
            assertNotNull("e 应该有描述符", eDescriptor)
            assertEquals("Float32", eDescriptor!!.type.toString())

            // 验证 large 的显式类型
            val largeProperty = properties.first { it.name == "large" }
            val largeDescriptor = bindingContext[BindingContext.VARIABLE, largeProperty!!]
            assertNotNull("large 应该有描述符", largeDescriptor)
            assertEquals("Float64", largeDescriptor!!.type.toString())
        }
    }

    /**
     * 测试字符串字面量类型推断
     *
     * 验证：
     * 1. 字符串字面量被推断为 String 类型
     */
    fun `test string literal type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let message = "Hello, World!"
                let empty = ""
                let multiline = \"\"\"
                    This is
                    a multiline
                    string
                \"\"\"
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertTrue("应该至少有 2 个变量", properties.size >= 2)

            // 验证 message 的类型推断
            val messageProperty = properties.first { it.name == "message" }
            val messageDescriptor = bindingContext[BindingContext.VARIABLE, messageProperty!!]
            assertNotNull("message 应该有描述符", messageDescriptor)
            assertEquals("String", messageDescriptor!!.type.toString())

            // 验证 empty 的类型推断
            val emptyProperty = properties.first { it.name == "empty" }
            val emptyDescriptor = bindingContext[BindingContext.VARIABLE, emptyProperty!!]
            assertNotNull("empty 应该有描述符", emptyDescriptor)
            assertEquals("String", emptyDescriptor!!.type.toString())
        }
    }

    /**
     * 测试布尔字面量类型推断
     *
     * 验证：
     * 1. 布尔字面量被推断为 Bool 类型
     */
    fun `test boolean literal type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let isTrue = true
                let isFalse = false
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 2 个变量", 2, properties.size)

            // 验证 isTrue 的类型推断
            val isTrueProperty = properties.first { it.name == "isTrue" }
            val isTrueDescriptor = bindingContext[BindingContext.VARIABLE, isTrueProperty!!]
            assertNotNull("isTrue 应该有描述符", isTrueDescriptor)
            assertEquals("Bool", isTrueDescriptor!!.type.toString())

            // 验证 isFalse 的类型推断
            val isFalseProperty = properties.first { it.name == "isFalse" }
            val isFalseDescriptor = bindingContext[BindingContext.VARIABLE, isFalseProperty!!]
            assertNotNull("isFalse 应该有描述符", isFalseDescriptor)
            assertEquals("Bool", isFalseDescriptor!!.type.toString())
        }
    }

    // ==================== 二元运算符类型推断测试 ====================

    /**
     * 测试算术运算符类型推断
     *
     * 验证：
     * 1. 加法、减法、乘法、除法的类型推断
     * 2. 运算结果类型正确
     */
    fun `test arithmetic operator type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let a: Int64 = 10
                let b: Int64 = 20
                let sum = a + b
                let diff = a - b
                let product = a * b
                let quotient = a / b
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertTrue("应该至少有 6 个变量", properties.size >= 6)

            // 验证 sum 的类型推断
            val sumProperty = properties.first { it.name == "sum" }
            val sumDescriptor = bindingContext[BindingContext.VARIABLE, sumProperty!!]
            assertNotNull("sum 应该有描述符", sumDescriptor)
            assertNotNull("sum 应该有类型", sumDescriptor!!.type)

            // 验证 diff 的类型推断
            val diffProperty = properties.first { it.name == "diff" }
            val diffDescriptor = bindingContext[BindingContext.VARIABLE, diffProperty!!]
            assertNotNull("diff 应该有描述符", diffDescriptor)
            assertNotNull("diff 应该有类型", diffDescriptor!!.type)

            // 验证 product 的类型推断
            val productProperty = properties.first { it.name == "product" }
            val productDescriptor = bindingContext[BindingContext.VARIABLE, productProperty!!]
            assertNotNull("product 应该有描述符", productDescriptor)
            assertNotNull("product 应该有类型", productDescriptor!!.type)

            // 验证 quotient 的类型推断
            val quotientProperty = properties.first { it.name == "quotient" }
            val quotientDescriptor = bindingContext[BindingContext.VARIABLE, quotientProperty!!]
            assertNotNull("quotient 应该有描述符", quotientDescriptor)
            assertNotNull("quotient 应该有类型", quotientDescriptor!!.type)
        }
    }

    /**
     * 测试比较运算符类型推断
     *
     * 验证：
     * 1. 比较运算符返回 Bool 类型
     */
    fun `test comparison operator type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let a: Int64 = 10
                let b: Int64 = 20
                let isEqual = a == b
                let isNotEqual = a != b
                let isLess = a < b
                let isGreater = a > b
                let isLessOrEqual = a <= b
                let isGreaterOrEqual = a >= b
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertTrue("应该至少有 8 个变量", properties.size >= 8)

            // 验证所有比较结果都是 Bool 类型
            val comparisonProperties = listOf("isEqual", "isNotEqual", "isLess", "isGreater", "isLessOrEqual", "isGreaterOrEqual")
            for (propName in comparisonProperties) {
                val property = properties.firstOrNull { it.name == propName }
                if (property != null) {
                    val descriptor = bindingContext[BindingContext.VARIABLE, property!!]
                    assertNotNull("$propName 应该有描述符", descriptor)
                    assertEquals("Bool", descriptor!!.type.toString())
                }
            }
        }
    }

    /**
     * 测试逻辑运算符类型推断
     *
     * 验证：
     * 1. 逻辑与、或、非运算符返回 Bool 类型
     */
    fun `test logical operator type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let a = true
                let b = false
                let andResult = a && b
                let orResult = a || b
                let notResult = !a
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 5 个变量", 5, properties.size)

            // 验证 andResult 的类型
            val andProperty = properties.first { it.name == "andResult" }
            val andDescriptor = bindingContext[BindingContext.VARIABLE, andProperty!!]
            assertNotNull("andResult 应该有描述符", andDescriptor)
            assertEquals("Bool", andDescriptor!!.type.toString())

            // 验证 orResult 的类型
            val orProperty = properties.first { it.name == "orResult" }
            val orDescriptor = bindingContext[BindingContext.VARIABLE, orProperty!!]
            assertNotNull("orResult 应该有描述符", orDescriptor)
            assertEquals("Bool", orDescriptor!!.type.toString())

            // 验证 notResult 的类型
            val notProperty = properties.first { it.name == "notResult" }
            val notDescriptor = bindingContext[BindingContext.VARIABLE, notProperty!!]
            assertNotNull("notResult 应该有描述符", notDescriptor)
            assertEquals("Bool", notDescriptor!!.type.toString())
        }
    }

    // ==================== 函数调用表达式类型推断测试 ====================

    /**
     * 测试简单函数调用的类型推断
     *
     * 验证：
     * 1. 函数调用表达式的类型等于函数返回类型
     */
    fun `test function call type inference`() {
        val file = createFile(
            """
            package test

            func getNumber(): Int64 {
                return 42
            }

            func main() {
                let num = getNumber()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val numProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "num" }
            assertNotNull("应该找到 num 变量", numProperty)

            val numDescriptor = bindingContext[BindingContext.VARIABLE, numProperty!!]
            assertNotNull("num 应该有描述符", numDescriptor)
            assertEquals("Int64", numDescriptor!!.type.toString())
        }
    }

    /**
     * 测试带参数的函数调用类型推断
     *
     * 验证：
     * 1. 参数类型检查
     * 2. 返回类型推断
     */
    fun `test function call with parameters type inference`() {
        val file = createFile(
            """
            package test

            func add(a: Int64, b: Int64): Int64 {
                return a + b
            }

            func main() {
                let result = add(10, 20)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "result" }
            assertNotNull("应该找到 result 变量", resultProperty)

            val resultDescriptor = bindingContext[BindingContext.VARIABLE, resultProperty!!]
            assertNotNull("result 应该有描述符", resultDescriptor)
            assertEquals("Int64", resultDescriptor!!.type.toString())
        }
    }

    /**
     * 测试泛型函数调用的类型推断
     *
     * 验证：
     * 1. 泛型类型参数的推断
     * 2. 返回类型的具体化
     */
    fun `test generic function call type inference`() {
        val file = createFile(
            """
            package test

            func identity<T>(value: T): T {
                return value
            }

            func main() {
                let num = identity(42)
                let str = identity("hello")
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertTrue("应该至少有 2 个变量", properties.size >= 2)

            // 验证 num 的类型推断（应该是 Int 类型）
            val numProperty = properties.first { it.name == "num" }
            val numDescriptor = bindingContext[BindingContext.VARIABLE, numProperty!!]
            assertNotNull("num 应该有描述符", numDescriptor)
            assertNotNull("num 应该有类型", numDescriptor!!.type)

            // 验证 str 的类型推断（应该是 String 类型）
            val strProperty = properties.first { it.name == "str" }
            val strDescriptor = bindingContext[BindingContext.VARIABLE, strProperty!!]
            assertNotNull("str 应该有描述符", strDescriptor)
            assertEquals("String", strDescriptor!!.type.toString())
        }
    }

    // ==================== 成员访问表达式类型推断测试 ====================

    /**
     * 测试属性访问的类型推断
     *
     * 验证：
     * 1. 成员属性访问的类型正确
     */
    fun `test property access type inference`() {
        val file = createFile(
            """
            package test

            class Person {
                public let name: String
                public var age: Int64

                public init(name: String, age: Int64) {
                    this.name = name
                    this.age = age
                }
            }

            func main() {
                let person = Person("Alice", 30)
                let personName = person.name
                let personAge = person.age
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)

            // 验证 personName 的类型
            val personNameProperty = properties.firstOrNull { it.name == "personName" }
            if (personNameProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, personNameProperty!!]
                assertNotNull("personName 应该有描述符", descriptor)
                assertEquals("String", descriptor!!.type.toString())
            }

            // 验证 personAge 的类型
            val personAgeProperty = properties.firstOrNull { it.name == "personAge" }
            if (personAgeProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, personAgeProperty!!]
                assertNotNull("personAge 应该有描述符", descriptor)
                assertEquals("Int64", descriptor!!.type.toString())
            }
        }
    }

    /**
     * 测试方法调用的类型推断
     *
     * 验证：
     * 1. 成员方法调用的返回类型
     */
    fun `test method call type inference`() {
        val file = createFile(
            """
            package test

            class Calculator {
                public func add(a: Int64, b: Int64): Int64 {
                    return a + b
                }

                public func getMessage(): String {
                    return "Calculator"
                }
            }

            func main() {
                let calc = Calculator()
                let sum = calc.add(10, 20)
                let message = calc.getMessage()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)

            // 验证 sum 的类型
            val sumProperty = properties.firstOrNull { it.name == "sum" }
            if (sumProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, sumProperty!!]
                assertNotNull("sum 应该有描述符", descriptor)
                assertEquals("Int64", descriptor!!.type.toString())
            }

            // 验证 message 的类型
            val messageProperty = properties.firstOrNull { it.name == "message" }
            if (messageProperty != null) {
                val descriptor = bindingContext[BindingContext.VARIABLE, messageProperty!!]
                assertNotNull("message 应该有描述符", descriptor)
                assertEquals("String", descriptor!!.type.toString())
            }
        }
    }

    // ==================== 复合表达式类型推断测试 ====================

    /**
     * 测试嵌套表达式的类型推断
     *
     * 验证：
     * 1. 复杂嵌套表达式的类型推断
     */
    fun `test nested expression type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let a: Int64 = 10
                let b: Int64 = 20
                let c: Int64 = 30
                let result = (a + b) * c - (a - b) / 2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "result" }
            assertNotNull("应该找到 result 变量", resultProperty)

            val resultDescriptor = bindingContext[BindingContext.VARIABLE, resultProperty!!]
            assertNotNull("result 应该有描述符", resultDescriptor)
            assertNotNull("result 应该有类型", resultDescriptor!!.type)
        }
    }

    /**
     * 测试条件表达式的类型推断
     *
     * 验证：
     * 1. 三元运算符或 if 表达式的类型推断
     * 2. 分支类型的公共父类型
     */
    fun `test conditional expression type inference`() {
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
     * 测试数组/集合元素访问的类型推断
     *
     * 验证：
     * 1. 数组元素访问的类型
     * 2. 泛型集合元素的类型
     */
    fun `test array element access type inference`() {
        val file = createFile(
            """
            package test

            func main() {
                let numbers: Array<Int64> = Array<Int64>()
                let first = numbers[0]
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val firstProperty = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
                .firstOrNull { it.name == "first" }
            assertNotNull("应该找到 first 变量", firstProperty)

            val firstDescriptor = bindingContext[BindingContext.VARIABLE, firstProperty!!]
            assertNotNull("first 应该有描述符", firstDescriptor)
            assertNotNull("first 应该有类型", firstDescriptor!!.type)
        }
    }
}
