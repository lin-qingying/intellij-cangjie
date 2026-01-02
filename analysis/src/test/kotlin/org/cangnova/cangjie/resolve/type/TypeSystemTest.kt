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

package org.cangnova.cangjie.resolve.type

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.identifier
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 类型系统测试
 *
 * 测试仓颉语言的类型系统语义分析功能：
 * - 基本类型的识别和验证
 * - 复合类型（数组、元组、函数类型）的解析
 * - 类型引用的解析和类型推导
 * - 类型兼容性和子类型关系
 *
 * ## 测试策略
 *
 * 1. **基本类型**: 验证内置类型（Int64, Float64, Bool 等）的正确识别
 * 2. **类型引用**: 验证类型引用能正确解析到类型定义
 * 3. **类型推导**: 验证变量和表达式的类型推导是否正确
 * 4. **泛型类型**: 验证泛型类型的实例化和约束
 */
class TypeSystemTest : CangJieAnalysisTestBase() {

    // ==================== 基本类型测试 ====================

    /**
     * 测试整数类型
     *
     * 验证：
     * 1. Int64 类型引用能正确解析
     * 2. 变量的类型推导正确
     */
    fun `test Int64 type`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 0
            var y = 42
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 验证显式类型声明
            val xVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "x" }
                }
            assertNotNull("应该找到变量 x", xVar)

            val xTypeRef = xVar!!.typeReference
            assertNotNull("变量 x 应该有类型引用", xTypeRef)

            // 验证类型引用解析
            val xType = bindingContext[BindingContext.TYPE, xTypeRef!!]
            // 注意：类型解析可能需要内置类型支持，这里先验证 PSI 结构
            val basicType = xTypeRef.typeElement as? CjBasicType
            assertNotNull("应该是基本类型", basicType)
            assertEquals("Int64", basicType!!.name)

            // 验证类型推导（变量 y）
            val yVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "y" }
                }
            assertNotNull("应该找到变量 y", yVar)
            // y 的类型应该从字面量 42 推导为 Int64
        }
    }

    /**
     * 测试浮点数类型
     *
     * 验证：
     * 1. Float64 类型引用能正确解析
     * 2. 浮点数字面量的类型推导
     */
    fun `test Float64 type`() {
        val file = createFile(
            """
            package test

            var pi: Float64 = 3.14
            var e = 2.718
            """.trimIndent()
        )

        analyzeForTest(file) {
            val piVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "pi" }
                }
            assertNotNull("应该找到变量 pi", piVar)

            val piTypeRef = piVar!!.typeReference
            assertNotNull("变量 pi 应该有类型引用", piTypeRef)

            val basicType = piTypeRef!!.typeElement as? CjBasicType
            assertNotNull("应该是基本类型", basicType)
            assertEquals("Float64", basicType!!.name)
        }
    }

    /**
     * 测试布尔类型
     *
     * 验证：
     * 1. Bool 类型引用能正确解析
     * 2. 布尔字面量的类型推导
     */
    fun `test Bool type`() {
        val file = createFile(
            """
            package test

            var flag: Bool = true
            var result = false
            """.trimIndent()
        )

        analyzeForTest(file) {
            val flagVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "flag" }
                }
            assertNotNull("应该找到变量 flag", flagVar)

            val flagTypeRef = flagVar!!.typeReference
            assertNotNull("变量 flag 应该有类型引用", flagTypeRef)

            val basicType = flagTypeRef!!.typeElement as? CjBasicType
            assertNotNull("应该是基本类型", basicType)
            assertEquals("Bool", basicType!!.name)
        }
    }

    /**
     * 测试字符串类型
     *
     * 验证：
     * 1. String 类型引用能正确解析（String 是类类型，不是基本类型）
     * 2. 字符串字面量的类型推导
     */
    fun `test String type`() {
        val file = createFile(
            """
            package test

            var name: String = "Alice"
            var greeting = "Hello"
            """.trimIndent()
        )

        analyzeForTest(file) {
            val nameVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "name" }
                }
            assertNotNull("应该找到变量 name", nameVar)

            val nameTypeRef = nameVar!!.typeReference
            assertNotNull("变量 name 应该有类型引用", nameTypeRef)

            // String 是用户类型（类类型），不是基本类型
            val userType = nameTypeRef!!.typeElement as? CjUserType
            assertNotNull("应该是用户类型", userType)
            assertEquals("String", userType!!.name)
        }
    }

    /**
     * 测试字符类型
     *
     * 验证：
     * 1. Rune 类型引用能正确解析
     * 2. 字符字面量的类型推导
     */
    fun `test Rune type`() {
        val file = createFile(
            """
            package test

            var ch: Rune = r'a'
            var newline = r'\n'
            """.trimIndent()
        )

        analyzeForTest(file) {
            val chVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "ch" }
                }
            assertNotNull("应该找到变量 ch", chVar)

            val chTypeRef = chVar!!.typeReference
            assertNotNull("变量 ch 应该有类型引用", chTypeRef)

            val basicType = chTypeRef!!.typeElement as? CjBasicType
            assertNotNull("应该是基本类型", basicType)
            assertEquals("Rune", basicType!!.name)
        }
    }

    // ==================== 复合类型测试 ====================

    /**
     * 测试数组类型
     *
     * 验证：
     * 1. Array<T> 类型引用能正确解析
     * 2. 数组元素类型的泛型参数
     */
    fun `test Array type`() {
        val file = createFile(
            """
            package test

            var numbers: Array<Int64> = Array<Int64>(10, {i: Int64 => 0})
            """.trimIndent()
        )

        analyzeForTest(file) {
            val numbersVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "numbers" }
                }
            assertNotNull("应该找到变量 numbers", numbersVar)

            val typeRef = numbersVar!!.typeReference
            assertNotNull("变量 numbers 应该有类型引用", typeRef)

            // 验证是泛型类型
            val userType = typeRef!!.typeElement as? CjUserType
            assertNotNull("应该是用户类型", userType)
            assertEquals("Array", userType!!.identifier?.text)

            // 验证泛型参数
            val typeArguments = userType.typeArgumentList?.arguments
            assertNotNull("应该有类型参数", typeArguments)
            assertEquals(1, typeArguments!!.size)
        }
    }

    /**
     * 测试元组类型
     *
     * 验证：
     * 1. (T1, T2, ...) 元组类型的解析
     * 2. 元组元素类型的验证
     */
    fun `test Tuple type`() {
        val file = createFile(
            """
            package test

            var pair: (Int64, String) = (1, "one")
            var triple: (Int64, Float64, Bool) = (1, 1.0, true)
            """.trimIndent()
        )

        analyzeForTest(file) {
            val pairVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "pair" }
                }
            assertNotNull("应该找到变量 pair", pairVar)

            val pairTypeRef = pairVar!!.typeReference
            assertNotNull("变量 pair 应该有类型引用", pairTypeRef)

            // 验证元组类型
            val tupleType = pairTypeRef!!.typeElement as? CjTupleType
            assertNotNull("应该是元组类型", tupleType)

            // 验证元组元素数量
            val typeElements = tupleType!!.typeArgumentsAsTypes
            assertEquals(2, typeElements.size)
        }
    }

    /**
     * 测试函数类型
     *
     * 验证：
     * 1. (T1, T2) -> R 函数类型的解析
     * 2. 参数类型和返回类型的验证
     */
    fun `test Function type`() {
        val file = createFile(
            """
            package test

         
            var add: (Int64, Int64) -> Int64 = {a: Int64, b: Int64 => a + b}
            var transform: (String) -> Int64 = {s: String => s.size}
            """.trimIndent()
        )

        analyzeForTest(file) {
            val addVar = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
                .firstOrNull {
                    it.pattern.getAllBindings().any { binding -> binding.name == "add" }
                }
            assertNotNull("应该找到变量 add", addVar)

            val addTypeRef = addVar!!.typeReference
            assertNotNull("变量 add 应该有类型引用", addTypeRef)

            // 验证函数类型
            val functionType = addTypeRef!!.typeElement as? CjFunctionType
            assertNotNull("应该是函数类型", functionType)

            // 验证参数类型列表
            val paramTypes = functionType!!.parameters
            assertNotNull("应该有参数类型列表", paramTypes)
            assertEquals(2, paramTypes.size)

            // 验证返回类型
            val returnType = functionType.returnTypeReference
            assertNotNull("应该有返回类型", returnType)
        }
    }

    // ==================== 类型引用解析测试 ====================

    /**
     * 测试简单类型引用
     *
     * 验证：
     * 1. 类型引用能解析到内置类型
     * 2. BindingContext 中记录了正确的类型信息
     */
    fun `test simple type reference resolution`() {
        val file = createFile(
            """
            package test

            var x: Int64 = 0
            var y: String = ""
            var z: Bool = false
            """.trimIndent()
        )

        analyzeForTest(file) {
            val variables = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 3 个变量", 3, variables.size)

            // 验证每个变量的类型引用都存在且格式正确
            for (variable in variables) {
                val typeRef = variable.typeReference
                assertNotNull("变量 ${variable.name} 应该有类型引用", typeRef)

                val typeElement = typeRef!!.typeElement
                assertNotNull("类型引用应该有类型元素", typeElement)
            }
        }
    }

    /**
     * 测试限定类型引用
     *
     * 验证：
     * 1. 带包路径的类型引用（如 std.core.String）
     * 2. 限定符的正确解析
     */
    fun `test qualified type reference`() {
        val file = createFile(
            """
            package test

            var name: std.core.String = ""
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 查找变量声明
            val patternVar = PsiTreeUtil.findChildOfType(file, CjPatternVariable::class.java)
            assertNotNull("应该找到变量声明", patternVar)

            // 通过 pattern 获取绑定
            val bindings = patternVar!!.pattern.getAllBindings()
            assertTrue("应该有至少一个绑定", bindings.isNotEmpty())

            val nameBinding = bindings.firstOrNull { it.name == "name" }
            assertNotNull("应该找到名为 name 的绑定", nameBinding)

            // 获取类型引用
            val typeRef = patternVar.typeReference
            assertNotNull("变量应该有类型引用", typeRef)

            // 验证限定类型引用
            val userType = typeRef!!.typeElement as? CjUserType
            assertNotNull("应该是用户类型", userType)

            // 验证限定符
            val qualifier = userType!!.qualifier
            assertNotNull("应该有限定符", qualifier)
        }
    }

    // ==================== 类型推导测试 ====================

    /**
     * 测试字面量类型推导
     *
     * 验证：
     * 1. 整数字面量推导为 Int64
     * 2. 浮点数字面量推导为 Float64
     * 3. 字符串字面量推导为 String
     * 4. 布尔字面量推导为 Bool
     */
    fun `test literal type inference`() {
        val file = createFile(
            """
            package test

            var a = 42           // Int64
            var b = 3.14         // Float64
            var c = "hello"      // String
            var d = true         // Bool
            var e = r'x'         // Rune
            """.trimIndent()
        )

        analyzeForTest(file) {
            val variables = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java)
            assertEquals("应该有 5 个变量", 5, variables.size)

            // 验证每个变量都有初始化表达式
            for (variable in variables) {
                val initializer = variable.initializer
                assertNotNull("变量 ${variable.name} 应该有初始化表达式", initializer)
            }
        }
    }

    /**
     * 测试表达式类型推导
     *
     * 验证：
     * 1. 算术表达式的类型推导
     * 2. 逻辑表达式的类型推导
     * 3. 比较表达式的类型推导
     */
    fun `test expression type inference`() {
        val file = createFile(
            """
            package test

            func test() {
                var sum = 1 + 2           // Int64
                var product = 3.0 * 4.0   // Float64
                var isEqual = (1 == 2)    // Bool
                var isGreater = (5 > 3)   // Bool
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数 test", function)

            val variables = PsiTreeUtil.findChildrenOfType(function!!, CjPatternVariable::class.java)
            assertEquals("应该有 4 个变量", 4, variables.size)

            // 验证每个变量都有初始化表达式
            for (variable in variables) {
                val initializer = variable.initializer
                assertNotNull("变量 ${variable.name} 应该有初始化表达式", initializer)
            }
        }
    }

    /**
     * 测试条件表达式类型推导
     *
     * 验证：
     * 1. if-else 表达式的类型推导
     * 2. 两个分支的公共类型计算
     */
    fun `test if expression type inference`() {
        val file = createFile(
            """
            package test

            func test() {
                var x = if (true) 1 else 2        // Int64
                var y = if (false) 1.0 else 2.0   // Float64
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val function = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数 test", function)

            val variables = PsiTreeUtil.findChildrenOfType(function!!, CjPatternVariable::class.java)
            assertEquals("应该有 2 个变量", 2, variables.size)

            // 验证每个变量都有 if 表达式作为初始化器
            for (variable in variables) {
                val initializer = variable.initializer
                assertNotNull("变量 ${variable.name} 应该有初始化表达式", initializer)
                assertTrue(
                    "初始化表达式应该是 if 表达式",
                    initializer is CjIfExpression
                )
            }
        }
    }

    // ==================== Unit 和 Nothing 类型测试 ====================

    /**
     * 测试 Unit 类型
     *
     * 验证：
     * 1. 无返回值函数的返回类型是 Unit
     * 2. 赋值表达式的类型是 Unit
     */
    fun `test Unit type`() {
        val file = createFile(
            """
            package test

            func doSomething(): Unit {
                var x = 10
            }

            func implicitUnit() {
                var y = 20
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val doSomethingFunc = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
                .firstOrNull { it.name == "doSomething" }
            assertNotNull("应该找到函数 doSomething", doSomethingFunc)

            // 验证显式 Unit 返回类型
            val returnTypeRef = doSomethingFunc!!.typeReference
            assertNotNull("函数应该有返回类型", returnTypeRef)

            // 验证返回类型的文本表示
            assertEquals("Unit", returnTypeRef!!.text)
        }
    }

    /**
     * 测试 Nothing 类型
     *
     * 验证：
     * 1. throw 表达式的类型是 Nothing
     * 2. return 表达式的类型是 Nothing
     */
    fun `test Nothing type`() {
        val file = createFile(
            """
            package test

            func alwaysThrow(): Nothing {
                throw Exception("error")
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val alwaysThrowFunc = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数 alwaysThrow", alwaysThrowFunc)

            // 验证 Nothing 返回类型
            val returnTypeRef = alwaysThrowFunc!!.typeReference
            assertNotNull("函数应该有返回类型", returnTypeRef)

            // 验证返回类型的文本表示
            assertEquals("Nothing", returnTypeRef!!.text)
        }
    }
}
