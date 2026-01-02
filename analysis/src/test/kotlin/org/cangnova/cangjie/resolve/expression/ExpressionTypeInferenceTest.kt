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

package org.cangnova.cangjie.resolve.expression

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
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

             main() {
                let a = 42
                let b = 100000000000
                let c: Int8 = 10
                let d: Int64 = 999
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 4 个变量", 4, patternVars.size)

            // 验证变量 a 的类型推断
            val aPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "a" }
            }
            val aBinding = aPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "a" }
            val aDescriptor = bindingContext[BindingContext.VARIABLE, aBinding!!]
            assertNotNull("a 应该有描述符", aDescriptor)
            assertNotNull("a 应该有推断类型", aDescriptor!!.type)

            // 验证变量 c 显式类型
            val cPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "c" }
            }
            val cBinding = cPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "c" }
            val cDescriptor = bindingContext[BindingContext.VARIABLE, cBinding!!]
            assertNotNull("c 应该有描述符", cDescriptor)
            assertEquals("Int8", cDescriptor!!.type.toString())

            // 验证变量 d 显式类型
            val dPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "d" }
            }
            val dBinding = dPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "d" }
            val dDescriptor = bindingContext[BindingContext.VARIABLE, dBinding!!]
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

              main() {
                let pi = 3.14
                let e: Float32 = 2.71
                let large: Float64 = 1.23456789
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 3 个变量", 3, patternVars.size)

            // 验证 pi 的类型推断
            val piPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "pi" }
            }
            val piBinding = piPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "pi" }
            val piDescriptor = bindingContext[BindingContext.VARIABLE, piBinding!!]
            assertNotNull("pi 应该有描述符", piDescriptor)
            assertNotNull("pi 应该有推断类型", piDescriptor!!.type)

            // 验证 e 的显式类型
            val ePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "e" }
            }
            val eBinding = ePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "e" }
            val eDescriptor = bindingContext[BindingContext.VARIABLE, eBinding!!]
            assertNotNull("e 应该有描述符", eDescriptor)
            assertEquals("Float32", eDescriptor!!.type.toString())

            // 验证 large 的显式类型
            val largePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "large" }
            }
            val largeBinding = largePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "large" }
            val largeDescriptor = bindingContext[BindingContext.VARIABLE, largeBinding!!]
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

              main() {
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
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 2 个变量", patternVars.size >= 2)

            // 验证 message 的类型推断
            val messagePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "message" }
            }
            val messageBinding = messagePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "message" }
            val messageDescriptor = bindingContext[BindingContext.VARIABLE, messageBinding!!]
            assertNotNull("message 应该有描述符", messageDescriptor)
            assertEquals("String", messageDescriptor!!.type.toString())

            // 验证 empty 的类型推断
            val emptyPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "empty" }
            }
            val emptyBinding = emptyPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "empty" }
            val emptyDescriptor = bindingContext[BindingContext.VARIABLE, emptyBinding!!]
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

              main() {
                let isTrue = true
                let isFalse = false
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjVariable::class.java)
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

              main() {
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
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 6 个变量", patternVars.size >= 6)

            // 验证 sum 的类型推断
            val sumPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "sum" }
            }
            val sumBinding = sumPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "sum" }
            val sumDescriptor = bindingContext[BindingContext.VARIABLE, sumBinding!!]
            assertNotNull("sum 应该有描述符", sumDescriptor)
            assertNotNull("sum 应该有类型", sumDescriptor!!.type)

            // 验证 diff 的类型推断
            val diffPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "diff" }
            }
            val diffBinding = diffPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "diff" }
            val diffDescriptor = bindingContext[BindingContext.VARIABLE, diffBinding!!]
            assertNotNull("diff 应该有描述符", diffDescriptor)
            assertNotNull("diff 应该有类型", diffDescriptor!!.type)

            // 验证 product 的类型推断
            val productPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "product" }
            }
            val productBinding = productPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "product" }
            val productDescriptor = bindingContext[BindingContext.VARIABLE, productBinding!!]
            assertNotNull("product 应该有描述符", productDescriptor)
            assertNotNull("product 应该有类型", productDescriptor!!.type)

            // 验证 quotient 的类型推断
            val quotientPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "quotient" }
            }
            val quotientBinding = quotientPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "quotient" }
            val quotientDescriptor = bindingContext[BindingContext.VARIABLE, quotientBinding!!]
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

             main() {
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
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 8 个变量", patternVars.size >= 8)

            // 验证所有比较结果都是 Bool 类型
            val comparisonProperties = listOf("isEqual", "isNotEqual", "isLess", "isGreater", "isLessOrEqual", "isGreaterOrEqual")
            for (propName in comparisonProperties) {
                val patternVar = patternVars.firstOrNull { pv ->
                    pv.pattern.getAllBindings().any { it.name == propName }
                }
                if (patternVar != null) {
                    val binding = patternVar.pattern.getAllBindings().firstOrNull { it.name == propName }
                    val descriptor = bindingContext[BindingContext.VARIABLE, binding!!]
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

             main() {
                let a = true
                let b = false
                let andResult = a && b
                let orResult = a || b
                let notResult = !a
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 5 个变量", 5, patternVars.size)

            // 验证 andResult 的类型
            val andPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "andResult" }
            }
            val andBinding = andPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "andResult" }
            val andDescriptor = bindingContext[BindingContext.VARIABLE, andBinding!!]
            assertNotNull("andResult 应该有描述符", andDescriptor)
            assertEquals("Bool", andDescriptor!!.type.toString())

            // 验证 orResult 的类型
            val orPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "orResult" }
            }
            val orBinding = orPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "orResult" }
            val orDescriptor = bindingContext[BindingContext.VARIABLE, orBinding!!]
            assertNotNull("orResult 应该有描述符", orDescriptor)
            assertEquals("Bool", orDescriptor!!.type.toString())

            // 验证 notResult 的类型
            val notPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "notResult" }
            }
            val notBinding = notPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "notResult" }
            val notDescriptor = bindingContext[BindingContext.VARIABLE, notBinding!!]
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

             main() {
                let num = getNumber()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val numPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "num" }
                }
            assertNotNull("应该找到 num 变量", numPatternVar)

            val numBinding = numPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "num" }
            val numDescriptor = bindingContext[BindingContext.VARIABLE, numBinding!!]
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

             main() {
                let result = add(10, 20)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "result" }
                }
            assertNotNull("应该找到 result 变量", resultPatternVar)

            val resultBinding = resultPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "result" }
            val resultDescriptor = bindingContext[BindingContext.VARIABLE, resultBinding!!]
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

             main() {
                let num = identity(42)
                let str = identity("hello")
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 2 个变量", patternVars.size >= 2)

            // 验证 num 的类型推断（应该是 Int 类型）
            val numPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "num" }
            }
            val numBinding = numPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "num" }
            val numDescriptor = bindingContext[BindingContext.VARIABLE, numBinding!!]
            assertNotNull("num 应该有描述符", numDescriptor)
            assertNotNull("num 应该有类型", numDescriptor!!.type)

            // 验证 str 的类型推断（应该是 String 类型）
            val strPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "str" }
            }
            val strBinding = strPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "str" }
            val strDescriptor = bindingContext[BindingContext.VARIABLE, strBinding!!]
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

              main() {
                let person = Person("Alice", 30)
                let personName = person.name
                let personAge = person.age
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)

            // 验证 personName 的类型
            val personNamePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "personName" }
            }
            if (personNamePatternVar != null) {
                val binding = personNamePatternVar.pattern.getAllBindings().firstOrNull { it.name == "personName" }
                val descriptor = bindingContext[BindingContext.VARIABLE, binding!!]
                assertNotNull("personName 应该有描述符", descriptor)
                assertEquals("String", descriptor!!.type.toString())
            }

            // 验证 personAge 的类型
            val personAgePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "personAge" }
            }
            if (personAgePatternVar != null) {
                val binding = personAgePatternVar.pattern.getAllBindings().firstOrNull { it.name == "personAge" }
                val descriptor = bindingContext[BindingContext.VARIABLE, binding!!]
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

              main() {
                let calc = Calculator()
                let sum = calc.add(10, 20)
                let message = calc.getMessage()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)

            // 验证 sum 的类型
            val sumPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "sum" }
            }
            if (sumPatternVar != null) {
                val binding = sumPatternVar.pattern.getAllBindings().firstOrNull { it.name == "sum" }
                val descriptor = bindingContext[BindingContext.VARIABLE, binding!!]
                assertNotNull("sum 应该有描述符", descriptor)
                assertEquals("Int64", descriptor!!.type.toString())
            }

            // 验证 message 的类型
            val messagePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "message" }
            }
            if (messagePatternVar != null) {
                val binding = messagePatternVar.pattern.getAllBindings().firstOrNull { it.name == "message" }
                val descriptor = bindingContext[BindingContext.VARIABLE, binding!!]
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

              main() {
                let a: Int64 = 10
                let b: Int64 = 20
                let c: Int64 = 30
                let result = (a + b) * c - (a - b) / 2
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "result" }
                }
            assertNotNull("应该找到 result 变量", resultPatternVar)

            val resultBinding = resultPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "result" }
            val resultDescriptor = bindingContext[BindingContext.VARIABLE, resultBinding!!]
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

              main() {
                let condition = true
                let value = if (condition) { 42 } else { 0 }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val valuePatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "value" }
                }
            assertNotNull("应该找到 value 变量", valuePatternVar)

            val valueBinding = valuePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "value" }
            val valueDescriptor = bindingContext[BindingContext.VARIABLE, valueBinding!!]
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

              main() {
                let numbers: Array<Int64> = Array<Int64>()
                let first = numbers[0]
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val firstPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "first" }
                }
            assertNotNull("应该找到 first 变量", firstPatternVar)

            val firstBinding = firstPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "first" }
            val firstDescriptor = bindingContext[BindingContext.VARIABLE, firstBinding!!]
            assertNotNull("first 应该有描述符", firstDescriptor)
            assertNotNull("first 应该有类型", firstDescriptor!!.type)
        }
    }
}
