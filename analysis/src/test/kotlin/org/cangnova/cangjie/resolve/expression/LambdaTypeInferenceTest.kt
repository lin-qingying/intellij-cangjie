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
 * Lambda表达式类型推断测试
 *
 * 测试lambda表达式在不同场景下的类型推断，包括：
 * - 简单lambda变量赋值
 * - 带参数的lambda
 * - 带返回类型的lambda
 * - 作为函数参数的lambda
 * - 高阶函数中的lambda
 */
class LambdaTypeInferenceTest : CangJieAnalysisTestBase() {

    /**
     * 测试简单lambda变量赋值
     *
     * 验证：
     * 1. lambda表达式可以赋值给变量
     * 2. 变量类型推断正确
     */
    fun `test simple lambda variable assignment`() {
        val file = createFile(
            """
            package test

            main() {
                let f = { => 42 }
                let result = f()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 2 个变量", patternVars.size >= 2)

            // 验证 f 的类型推断
            val fPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "f" }
            }
            assertNotNull("应该找到 f 变量", fPatternVar)

            val fBinding = fPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "f" }
            val fDescriptor = bindingContext[BindingContext.VARIABLE, fBinding!!]
            assertNotNull("f 应该有描述符", fDescriptor)
            assertNotNull("f 应该有类型", fDescriptor!!.type)

            // 验证 result 的类型推断
            val resultPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "result" }
            }
            if (resultPatternVar != null) {
                val resultBinding = resultPatternVar.pattern.getAllBindings().firstOrNull { it.name == "result" }
                val resultDescriptor = bindingContext[BindingContext.VARIABLE, resultBinding!!]
                assertNotNull("result 应该有描述符", resultDescriptor)
                assertNotNull("result 应该有类型", resultDescriptor!!.type)
            }
        }
    }

    /**
     * 测试带参数的lambda
     *
     * 验证：
     * 1. lambda参数类型推断
     * 2. lambda返回类型推断
     */
    fun `test lambda with parameters`() {
        val file = createFile(
            """
            package test

            main() {
                let add = { x: Int64, y: Int64 => x + y }
                let sum = add(10, 20)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 2 个变量", patternVars.size >= 2)

            // 验证 add 的类型
            val addPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "add" }
            }
            assertNotNull("应该找到 add 变量", addPatternVar)

            val addBinding = addPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "add" }
            val addDescriptor = bindingContext[BindingContext.VARIABLE, addBinding!!]
            assertNotNull("add 应该有描述符", addDescriptor)
            assertNotNull("add 应该有类型", addDescriptor!!.type)

            // 验证 sum 的类型
            val sumPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "sum" }
            }
            if (sumPatternVar != null) {
                val sumBinding = sumPatternVar.pattern.getAllBindings().firstOrNull { it.name == "sum" }
                val sumDescriptor = bindingContext[BindingContext.VARIABLE, sumBinding!!]
                assertNotNull("sum 应该有描述符", sumDescriptor)
                assertNotNull("sum 应该有类型", sumDescriptor!!.type)
            }
        }
    }

    /**
     * 测试带显式返回类型的lambda
     *
     * 验证：
     * 1. lambda显式返回类型正确解析
     * 2. 类型检查正确
     */
    fun `test lambda with explicit return type`() {
        val file = createFile(
            """
            package test

            main() {
                let multiply = { x: Int64, y: Int64 : Int64 => x * y }
                let product = multiply(5, 6)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 2 个变量", patternVars.size >= 2)

            // 验证 multiply 的类型
            val multiplyPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "multiply" }
            }
            assertNotNull("应该找到 multiply 变量", multiplyPatternVar)

            val multiplyBinding = multiplyPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "multiply" }
            val multiplyDescriptor = bindingContext[BindingContext.VARIABLE, multiplyBinding!!]
            assertNotNull("multiply 应该有描述符", multiplyDescriptor)
            assertNotNull("multiply 应该有类型", multiplyDescriptor!!.type)

            // 验证 product 的类型
            val productPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "product" }
            }
            if (productPatternVar != null) {
                val productBinding = productPatternVar.pattern.getAllBindings().firstOrNull { it.name == "product" }
                val productDescriptor = bindingContext[BindingContext.VARIABLE, productBinding!!]
                assertNotNull("product 应该有描述符", productDescriptor)
                assertEquals("Int64", productDescriptor!!.type.toString())
            }
        }
    }

    /**
     * 测试lambda作为函数参数
     *
     * 验证：
     * 1. lambda可以作为参数传递
     * 2. 高阶函数的类型推断
     */
    fun `test lambda as function parameter`() {
        val file = createFile(
            """
            package test

            func applyFunc(x: Int64, f: (Int64) -> Int64): Int64 {
                return f(x)
            }

            main() {
                let result = applyFunc(10, { n: Int64 => n * 2 })
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
     * 测试无参数lambda
     *
     * 验证：
     * 1. 无参数lambda的类型推断
     * 2. 调用无参数lambda
     */
    fun `test lambda without parameters`() {
        val file = createFile(
            """
            package test

            main() {
                let getMessage = { => "Hello, World!" }
                let message = getMessage()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 2 个变量", 2, patternVars.size)

            // 验证 getMessage 的类型
            val getMessagePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "getMessage" }
            }
            assertNotNull("应该找到 getMessage 变量", getMessagePatternVar)

            val getMessageBinding = getMessagePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "getMessage" }
            val getMessageDescriptor = bindingContext[BindingContext.VARIABLE, getMessageBinding!!]
            assertNotNull("getMessage 应该有描述符", getMessageDescriptor)
            assertNotNull("getMessage 应该有类型", getMessageDescriptor!!.type)

            // 验证 message 的类型
            val messagePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "message" }
            }
            if (messagePatternVar != null) {
                val messageBinding = messagePatternVar.pattern.getAllBindings().firstOrNull { it.name == "message" }
                val messageDescriptor = bindingContext[BindingContext.VARIABLE, messageBinding!!]
                assertNotNull("message 应该有描述符", messageDescriptor)
                assertEquals("String", messageDescriptor!!.type.toString())
            }
        }
    }

    /**
     * 测试带多个参数的lambda
     *
     * 验证：
     * 1. 多参数lambda的类型推断
     * 2. 参数顺序正确
     */
    fun `test lambda with multiple parameters`() {
        val file = createFile(
            """
            package test

            main() {
                let combine = { a: Int64, b: Int64, c: Int64 => (a + b) * c }
                let result = combine(1, 2, 3)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 2 个变量", 2, patternVars.size)

            // 验证 combine 的类型
            val combinePatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "combine" }
            }
            assertNotNull("应该找到 combine 变量", combinePatternVar)

            val combineBinding = combinePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "combine" }
            val combineDescriptor = bindingContext[BindingContext.VARIABLE, combineBinding!!]
            assertNotNull("combine 应该有描述符", combineDescriptor)
            assertNotNull("combine 应该有类型", combineDescriptor!!.type)

            // 验证 result 的类型
            val resultPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "result" }
            }
            if (resultPatternVar != null) {
                val resultBinding = resultPatternVar.pattern.getAllBindings().firstOrNull { it.name == "result" }
                val resultDescriptor = bindingContext[BindingContext.VARIABLE, resultBinding!!]
                assertNotNull("result 应该有描述符", resultDescriptor)
                assertNotNull("result 应该有类型", resultDescriptor!!.type)
            }
        }
    }
}
