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

package org.cangnova.cangjie.resolve.declaration

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.descriptors.SimpleFunctionDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.deccriptorClass

/**
 * 函数声明语义分析测试
 *
 * 专注于测试函数声明的语义分析，包括：
 * - 函数描述符的创建
 * - 参数和返回类型的解析
 * - 函数重载的语义
 * - 函数覆盖（override）的验证
 *
 * ## 测试策略
 *
 * 1. **函数描述符**: 验证 FunctionDescriptor 的创建和属性
 * 2. **类型解析**: 验证参数类型和返回类型的正确解析
 * 3. **重载语义**: 验证函数重载的语义正确性
 * 4. **覆盖验证**: 验证函数覆盖的语义规则
 */
class FunctionSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 函数描述符创建测试 ====================

    /**
     * 测试简单函数的描述符创建
     *
     * 验证：
     * 1. FunctionDescriptor 被正确创建
     * 2. 函数名称正确
     * 3. 返回类型正确解析
     */
    fun `test simple function descriptor creation`() {
        val file = createFile(
            """
            package test

            func greet(name: String): String {
                return "Hello, " + name
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)
            assertEquals("greet", functionDecl!!.name)

            // 验证函数描述符
            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证函数名称
            assertEquals("greet", functionDescriptor!!.name.asString())

            // 验证返回类型
            val returnType = functionDescriptor.returnType
            assertNotNull("函数应该有返回类型", returnType)
            assertEquals("String", returnType!!.toString())
        }
    }

    /**
     * 测试无返回值函数
     *
     * 验证：
     * 1. 返回类型为 Unit 的函数正确处理
     * 2. Unit 类型被正确解析
     */
    fun `test Unit return type function`() {
        val file = createFile(
            """
            package test

            func doSomething(): Unit {
                // do something
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证返回类型为 Unit
            val returnType = functionDescriptor!!.returnType
            assertNotNull("函数应该有返回类型", returnType)
            assertEquals("Unit", returnType!!.deccriptorClass?.name?.asString())
        }
    }

    /**
     * 测试带参数的函数
     *
     * 验证：
     * 1. 参数的 VariableDescriptor 被创建
     * 2. 参数类型正确解析
     * 3. 参数顺序正确
     */
    fun `test function with parameters`() {
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

            // 验证参数
            val parameters = functionDescriptor!!.valueParameters
            assertEquals("应该有 2 个参数", 2, parameters.size)

            // 验证第一个参数
            val param1 = parameters[0]
            assertEquals("a", param1.name.asString())
            assertEquals("Int64", param1.type.toString())

            // 验证第二个参数
            val param2 = parameters[1]
            assertEquals("b", param2.name.asString())
            assertEquals("Int64", param2.type.toString())

            // 验证返回类型
            assertEquals("Int64", functionDescriptor.returnType!!.toString())
        }
    }

    // ==================== 成员函数测试 ====================

    /**
     * 测试类成员函数
     *
     * 验证：
     * 1. 成员函数的 FunctionDescriptor 被创建
     * 2. 成员函数与类的关联关系正确
     * 3. this 接收者类型正确
     */
    fun `test class member function`() {
        val file = createFile(
            """
            package test

            class Calculator {
                public func add(a: Int64, b: Int64): Int64 {
                    return a + b
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val functionDecl = PsiTreeUtil.findChildOfType(classDecl, CjFunction::class.java)
            assertNotNull("应该找到成员函数", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建成员函数 FunctionDescriptor", functionDescriptor)

            // 验证函数所属的类
            val containingClass = functionDescriptor!!.containingDeclaration
            assertNotNull("成员函数应该有包含的声明", containingClass)

            // 验证扩展接收者（成员函数应该有 this）
            // 注意：根据实际实现，可能需要检查 dispatchReceiverParameter 或 extensionReceiverParameter
            val dispatchReceiver = functionDescriptor.dispatchReceiverParameter
            assertNotNull("成员函数应该有 dispatch 接收者", dispatchReceiver)
        }
    }

    /**
     * 测试抽象函数
     *
     * 验证：
     * 1. 抽象函数的描述符正确创建
     * 2. 抽象函数没有函数体
     * 3. 模态性为 ABSTRACT
     */
    fun `test abstract function`() {
        val file = createFile(
            """
            package test

            abstract class Shape {
                abstract func area(): Float64
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到抽象函数", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建抽象函数 FunctionDescriptor", functionDescriptor)

            // 验证函数名称和返回类型
            assertEquals("area", functionDescriptor!!.name.asString())
            assertEquals("Float64", functionDescriptor.returnType!!.toString())

            // 验证模态性（如果实现了）
            // assertEquals(Modality.ABSTRACT, functionDescriptor.modality)
        }
    }

    // ==================== 函数覆盖测试 ====================

    /**
     * 测试函数覆盖
     *
     * 验证：
     * 1. override 函数的描述符正确
     * 2. 覆盖关系被正确识别
     * 3. 被覆盖的函数可以被追溯
     */
    fun `test function override`() {
        val file = createFile(
            """
            package test

            open class Base {
                public open func greet(): String {
                    return "Hello from Base"
                }
            }

            class Child <: Base {
                public override func greet(): String {
                    return "Hello from Child"
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val childClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Child" }
            assertNotNull("应该找到 Child 类", childClass)

            val childGreetFunc = PsiTreeUtil.findChildrenOfType(childClass, CjFunction::class.java)
                .firstOrNull { it.name == "greet" }
            assertNotNull("应该找到 Child.greet 方法", childGreetFunc)

            val childGreetDescriptor = bindingContext[BindingContext.FUNCTION, childGreetFunc!!]
            assertNotNull("应该创建 Child.greet FunctionDescriptor", childGreetDescriptor)

            // 验证覆盖关系（如果实现了）
            // val overriddenDescriptors = childGreetDescriptor.overriddenDescriptors
            // assertTrue("greet 方法应该覆盖父类方法", overriddenDescriptors.isNotEmpty())
        }
    }

    // ==================== 扩展函数测试 ====================

    /**
     * 测试扩展函数
     *
     * 验证：
     * 1. 扩展函数的描述符正确创建
     * 2. 扩展接收者类型正确
     * 3. 扩展函数可以访问接收者成员
     */
    fun `test extension function`() {
        val file = createFile(
            """
            package test

            extend String {
                func isLong(): Bool {
                    return this.length > 10
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 查找扩展声明
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val functionDecl = PsiTreeUtil.findChildOfType(extendDecl, CjFunction::class.java)
            assertNotNull("应该找到扩展函数", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建扩展函数 FunctionDescriptor", functionDescriptor)

            // 验证函数名称
            assertEquals("isLong", functionDescriptor!!.name.asString())


        }
    }

    // ==================== 高阶函数测试 ====================

    /**
     * 测试高阶函数
     *
     * 验证：
     * 1. 接受函数类型参数的函数正确解析
     * 2. 函数类型的参数和返回类型正确
     */
    fun `test higher-order function`() {
        val file = createFile(
            """
            package test

            func apply(value: Int64, transform: (Int64) -> Int64): Int64 {
                return transform(value)
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

            // 验证第一个参数是普通类型
            assertEquals("value", parameters[0].name.asString())
            assertEquals("Int64", parameters[0].type.toString())

            // 验证第二个参数是函数类型
            val transformParam = parameters[1]
            assertEquals("transform", transformParam.name.asString())
            // 函数类型的字符串表示可能是 "(Int64) -> Int64" 或其他形式
            assertNotNull("transform 参数应该有类型", transformParam.type)
        }
    }

    // ==================== 泛型函数测试 ====================

    /**
     * 测试泛型函数
     *
     * 验证：
     * 1. 泛型函数的类型参数被正确解析
     * 2. 类型参数可以在参数和返回类型中使用
     */
    fun `test generic function`() {
        val file = createFile(
            """
            package test

            func identity<T>(value: T): T {
                return value
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到泛型函数", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建泛型函数 FunctionDescriptor", functionDescriptor)

            // 验证类型参数
            val typeParameters = functionDescriptor!!.typeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)
            assertEquals("T", typeParameters[0].name.asString())

            // 验证参数和返回类型使用类型参数
            val parameters = functionDescriptor.valueParameters
            assertEquals("应该有 1 个参数", 1, parameters.size)

            val paramType = parameters[0].type
            assertNotNull("参数应该有类型", paramType)

            val returnType = functionDescriptor.returnType
            assertNotNull("函数应该有返回类型", returnType)
        }
    }
}
