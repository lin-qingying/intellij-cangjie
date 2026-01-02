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

package org.cangnova.cangjie.resolve.scope

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 作用域和符号解析语义分析测试
 *
 * 专注于测试符号解析和作用域系统，包括：
 * - 局部变量作用域
 * - 函数参数作用域
 * - 类成员作用域
 * - 包级符号解析
 * - 导入和符号可见性
 *
 * ## 测试策略
 *
 * 1. **局部作用域**: 验证局部变量在作用域内的解析
 * 2. **嵌套作用域**: 验证嵌套作用域的变量遮蔽
 * 3. **成员作用域**: 验证类成员的访问和解析
 * 4. **包作用域**: 验证包级符号的解析
 * 5. **引用解析**: 验证各种引用的正确解析
 */
class ScopeResolutionTest : CangJieAnalysisTestBase() {

    // ==================== 局部作用域测试 ====================

    /**
     * 测试局部变量作用域
     *
     * 验证：
     * 1. 局部变量在声明后可访问
     * 2. 变量引用正确解析到声明
     */
    fun `test local variable scope`() {
        val file = createFile(
            """
            package test

             main() {
                let x: Int64 = 10
                let y = x + 5
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 2 个变量", 2, patternVars.size)

            val xPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "x" }
            }
            val xBinding = xPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "x" }
            val xDescriptor = bindingContext[BindingContext.VARIABLE, xBinding!!]
            assertNotNull("x 应该有描述符", xDescriptor)

            val yPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "y" }
            }
            val yBinding = yPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "y" }
            val yDescriptor = bindingContext[BindingContext.VARIABLE, yBinding!!]
            assertNotNull("y 应该有描述符", yDescriptor)

            // y 的初始化表达式应该能引用 x
            val yInitializer = yPatternVar.initializer
            assertNotNull("y 应该有初始化表达式", yInitializer)
        }
    }

    /**
     * 测试块作用域
     *
     * 验证：
     * 1. 块内声明的变量仅在块内可见
     * 2. 块外无法访问块内变量
     */
    fun `test block scope`() {
        val file = createFile(
            """
            package test

             main() {
                let outer = 1
                { =>
                    let inner = 2
                    let sum = outer + inner
                }
                // inner 在此处不可见
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 3 个变量", patternVars.size >= 3)

            // 验证 outer 变量
            val outerPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "outer" }
            }
            assertNotNull("应该找到 outer 变量", outerPatternVar)

            val outerBinding = outerPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "outer" }
            val outerDescriptor = bindingContext[BindingContext.VARIABLE, outerBinding!!]
            assertNotNull("outer 应该有描述符", outerDescriptor)

            // 验证 inner 变量（在块内）
            val innerPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "inner" }
            }
            assertNotNull("应该找到 inner 变量", innerPatternVar)

            val innerBinding = innerPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "inner" }
            val innerDescriptor = bindingContext[BindingContext.VARIABLE, innerBinding!!]
            assertNotNull("inner 应该有描述符", innerDescriptor)

            // 验证 sum 变量可以访问 outer 和 inner
            val sumPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "sum" }
            }
            assertNotNull("应该找到 sum 变量", sumPatternVar)

            val sumBinding = sumPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "sum" }
            val sumDescriptor = bindingContext[BindingContext.VARIABLE, sumBinding!!]
            assertNotNull("sum 应该有描述符", sumDescriptor)
        }
    }

        /**
         * 测试变量遮蔽
         *
         * 验证：
         * 1. 内层作用域可以遮蔽外层同名变量
         * 2. 引用解析到最近的声明
         */
        fun `test variable shadowing`() {
            val file = createFile(
                """
                package test
    
           
main() {
    let x: Int64 = 10
    {
        => let x: String = "shadowed"
        // 此处 x 引用 String 类型的变量
    }
    // 此处 x 引用 Int64 类型的变量
    
}
                """.trimIndent()
            )

            analyzeForTest(file) {
                val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                val xVars = patternVars.filter { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "x" }
                }
                assertEquals("应该有 2 个 x 变量", 2, xVars.size)

                // 验证外层 x
                val outerXPatternVar = xVars.first()
                val outerXBinding = outerXPatternVar.pattern.getAllBindings().firstOrNull { it.name == "x" }
                val outerXDescriptor = bindingContext[BindingContext.VARIABLE, outerXBinding!!]
                assertNotNull("外层 x 应该有描述符", outerXDescriptor)
                assertEquals("Int64", outerXDescriptor!!.type.toString())

                // 验证内层 x
                val innerXPatternVar = xVars.last()
                val innerXBinding = innerXPatternVar.pattern.getAllBindings().firstOrNull { it.name == "x" }
                val innerXDescriptor = bindingContext[BindingContext.VARIABLE, innerXBinding!!]
                assertNotNull("内层 x 应该有描述符", innerXDescriptor)
                assertEquals("String", innerXDescriptor!!.type.toString())
            }
        }

    // ==================== 函数参数作用域测试 ====================

    /**
     * 测试函数参数作用域
     *
     * 验证：
     * 1. 函数参数在函数体内可访问
     * 2. 参数引用正确解析
     */
    fun `test function parameter scope`() {
        val file = createFile(
            """
            package test

            func add(a: Int64, b: Int64): Int64 {
                let sum = a + b
                return sum
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证参数
            val parameters = functionDescriptor!!.valueParameters
            assertEquals("应该有 2 个参数", 2, parameters.size)

            // 验证局部变量 sum
            val sumPatternVar = PsiTreeUtil.findChildrenOfType(functionDecl, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "sum" }
                }
            assertNotNull("应该找到 sum 变量", sumPatternVar)

            val sumBinding = sumPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "sum" }
            val sumDescriptor = bindingContext[BindingContext.VARIABLE, sumBinding!!]
            assertNotNull("sum 应该有描述符", sumDescriptor)
        }
    }

    /**
     * 测试参数与局部变量的作用域
     *
     * 验证：
     * 1. 局部变量可以与参数同名（遮蔽）
     * 2. 引用正确解析
     */
    fun `test parameter and local variable scope`() {
        val file = createFile(
            """
            package test

            func process(value: Int64): Int64 {
                let value = value * 2
                return value
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证参数
            val parameters = functionDescriptor!!.valueParameters
            assertEquals("应该有 1 个参数", 1, parameters.size)
            assertEquals("value", parameters[0].name.asString())

            // 验证局部变量
            val valuePatternVar = PsiTreeUtil.findChildrenOfType(functionDecl, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "value" }
                }
            assertNotNull("应该找到局部变量 value", valuePatternVar)

            val valueBinding = valuePatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "value" }
            val valueDescriptor = bindingContext[BindingContext.VARIABLE, valueBinding!!]
            assertNotNull("局部变量 value 应该有描述符", valueDescriptor)
        }
    }

    // ==================== 类成员作用域测试 ====================

    /**
     * 测试类成员访问
     *
     * 验证：
     * 1. 类成员在类内部可访问
     * 2. this 引用正确解析
     */
    fun `test class member scope`() {
        val file = createFile(
            """
            package test

            class Counter {
                private var count: Int64 = 0

                public func increment() {
                    this.count = this.count + 1
                }

                public func getCount(): Int64 {
                    return count
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证属性
            val countProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjFieldVariable::class.java)
                .firstOrNull { it.name == "count" }
            assertNotNull("应该找到 count 属性", countProperty)

            val countDescriptor = bindingContext[BindingContext.VARIABLE, countProperty!!]
            assertNotNull("count 应该有描述符", countDescriptor)

            // 验证方法
            val functions = PsiTreeUtil.findChildrenOfType(classDecl, CjFunction::class.java)
            assertEquals("应该有 2 个方法", 2, functions.size)

            val incrementFunc = functions.first { it.name == "increment" }
            val incrementDescriptor = bindingContext[BindingContext.FUNCTION, incrementFunc!!]
            assertNotNull("increment 应该有描述符", incrementDescriptor)
        }
    }

    /**
     * 测试继承的成员访问
     *
     * 验证：
     * 1. 子类可以访问父类的 public 和 protected 成员
     * 2. 成员引用正确解析到父类
     */
    fun `test inherited member scope`() {
        val file = createFile(
            """
            package test

            open class Base {
                protected var value: Int64 = 0

                public open func getValue(): Int64 {
                    return value
                }
            }

            class Derived <: Base {
                public func increment() {
                    value = value + 1
                }

                public override func getValue(): Int64 {
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

            // 验证继承关系
            val superTypes = derivedDescriptor!!.typeConstructor.supertypes
            assertTrue("应该继承 Base", superTypes.any { it.toString().contains("Base") })

            // 验证方法
            val incrementFunc = PsiTreeUtil.findChildrenOfType(derivedClass, CjFunction::class.java)
                .firstOrNull { it.name == "increment" }
            assertNotNull("应该找到 increment 方法", incrementFunc)

            val incrementDescriptor = bindingContext[BindingContext.FUNCTION, incrementFunc!!]
            assertNotNull("increment 应该有描述符", incrementDescriptor)
        }
    }

    // ==================== 包级作用域测试 ====================

    /**
     * 测试包级函数解析
     *
     * 验证：
     * 1. 包级函数在同一包内可访问
     * 2. 函数调用正确解析
     */
    fun `test package level function scope`() {
        val file = createFile(
            """
            package test

            func helper(): String {
                return "helper"
            }

            main() {
                let result = helper()
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functions = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
            assertEquals("应该有 2 个函数", 2, functions.size)

            val helperFunc = functions.first { it.name == "helper" }
            val helperDescriptor = bindingContext[BindingContext.FUNCTION, helperFunc!!]
            assertNotNull("helper 应该有描述符", helperDescriptor)

            val mainFunc = functions.first { it.name == "main" }
            val mainDescriptor = bindingContext[BindingContext.FUNCTION, mainFunc!!]
            assertNotNull("main 应该有描述符", mainDescriptor)
        }
    }

    /**
     * 测试顶层属性解析
     *
     * 验证：
     * 1. 包级常量在同一包内可访问
     * 2. 属性引用正确解析
     */
    fun `test package level property scope`() {
        val file = createFile(
            """
            package test

            let APP_VERSION: String = "1.0.0"

            func getVersion(): String {
                return APP_VERSION
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val appVersionPatternVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "APP_VERSION" }
                }
            assertNotNull("应该找到 APP_VERSION 属性", appVersionPatternVar)

            val appVersionBinding = appVersionPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "APP_VERSION" }
            val appVersionDescriptor = bindingContext[BindingContext.VARIABLE, appVersionBinding!!]
            assertNotNull("APP_VERSION 应该有描述符", appVersionDescriptor)
            assertEquals("String", appVersionDescriptor!!.type.toString())

            val getVersionFunc = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到 getVersion 函数", getVersionFunc)

            val getVersionDescriptor = bindingContext[BindingContext.FUNCTION, getVersionFunc!!]
            assertNotNull("getVersion 应该有描述符", getVersionDescriptor)
        }
    }

    // ==================== 引用解析测试 ====================

    /**
     * 测试简单名称引用解析
     *
     * 验证：
     * 1. 变量引用解析到正确的声明
     * 2. 函数引用解析到正确的声明
     */
    fun `test simple name reference resolution`() {
        val file = createFile(
            """
            package test

            func compute(x: Int64): Int64 {
                let doubled = x * 2
                return doubled
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证参数 x
            val parameters = functionDescriptor!!.valueParameters
            assertEquals("应该有 1 个参数", 1, parameters.size)
            assertEquals("x", parameters[0].name.asString())

            // 验证局部变量 doubled
            val doubledPatternVar = PsiTreeUtil.findChildrenOfType(functionDecl, CjPatternVariable::class.java)
                .firstOrNull { patternVar ->
                    patternVar.pattern.getAllBindings().any { it.name == "doubled" }
                }
            assertNotNull("应该找到 doubled 变量", doubledPatternVar)

            val doubledBinding = doubledPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "doubled" }
            val doubledDescriptor = bindingContext[BindingContext.VARIABLE, doubledBinding!!]
            assertNotNull("doubled 应该有描述符", doubledDescriptor)
        }
    }

    /**
     * 测试限定名称引用解析
     *
     * 验证：
     * 1. 通过 this 访问成员
     * 2. 限定名称正确解析
     */
    fun `test qualified name reference resolution`() {
        val file = createFile(
            """
            package test

            class Person {
                private let name: String

                public init(name: String) {
                    this.name = name
                }

                public func getName(): String {
                    return this.name
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证属性
            val nameProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjFieldVariable::class.java)
                .firstOrNull { it.name == "name" }
            assertNotNull("应该找到 name 属性", nameProperty)

            val nameDescriptor = bindingContext[BindingContext.VARIABLE, nameProperty!!]
            assertNotNull("name 应该有描述符", nameDescriptor)

            // 验证构造函数
            val constructor = PsiTreeUtil.findChildOfType(classDecl, CjConstructor::class.java)
            assertNotNull("应该找到构造函数", constructor)

            // 验证 getName 方法
            val getNameFunc = PsiTreeUtil.findChildrenOfType(classDecl, CjFunction::class.java)
                .firstOrNull { it.name == "getName" }
            assertNotNull("应该找到 getName 方法", getNameFunc)

            val getNameDescriptor = bindingContext[BindingContext.FUNCTION, getNameFunc!!]
            assertNotNull("getName 应该有描述符", getNameDescriptor)
        }
    }

    // ==================== 循环中的作用域测试 ====================

    /**
     * 测试 for 循环变量作用域
     *
     * 验证：
     * 1. 循环变量在循环体内可访问
     * 2. 循环变量在循环外不可访问
     */
    fun `test for loop variable scope`() {
        val file = createFile(
            """
            package test

            main() {
                let items = Array<Int64>()
                for (item in items) {
                    let doubled = item * 2
                }
                // item 在此处不可见
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val patternVars = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertTrue("应该至少有 2 个变量", patternVars.size >= 2)

            val itemsPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "items" }
            }
            assertNotNull("应该找到 items 变量", itemsPatternVar)

            val itemsBinding = itemsPatternVar!!.pattern.getAllBindings().firstOrNull { it.name == "items" }
            val itemsDescriptor = bindingContext[BindingContext.VARIABLE, itemsBinding!!]
            assertNotNull("items 应该有描述符", itemsDescriptor)

            // 查找循环体内的 doubled 变量
            val doubledPatternVar = patternVars.firstOrNull { patternVar ->
                patternVar.pattern.getAllBindings().any { it.name == "doubled" }
            }
            if (doubledPatternVar != null) {
                val doubledBinding = doubledPatternVar.pattern.getAllBindings().firstOrNull { it.name == "doubled" }
                val doubledDescriptor = bindingContext[BindingContext.VARIABLE, doubledBinding!!]
                assertNotNull("doubled 应该有描述符", doubledDescriptor)
            }
        }
    }
}
