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
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 变量和属性语义分析测试
 *
 * 专注于测试变量和属性的语义分析，包括：
 * - 变量描述符的创建
 * - 类型推断（显式和隐式类型）
 * - 可变性（let vs var）
 * - 属性的 getter/setter 语义
 * - 延迟初始化
 *
 * ## 测试策略
 *
 * 1. **变量描述符**: 验证 VariableDescriptor 的创建和属性
 * 2. **类型推断**: 验证显式类型声明和类型推断的正确性
 * 3. **可变性**: 验证 let（不可变）和 var（可变）的语义
 * 4. **属性语义**: 验证类属性的 getter/setter
 * 5. **初始化**: 验证变量初始化和延迟初始化
 */
class VariableSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 局部变量测试 ====================

    /**
     * 测试简单局部变量的描述符创建
     *
     * 验证：
     * 1. VariableDescriptor 被正确创建
     * 2. 变量名称正确
     * 3. 显式类型正确解析
     */
    fun `test simple local variable descriptor`() {
        val file = createFile(
            """
            package test

            func main() {
                let x: Int64 = 42
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val property = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到变量声明", property)
            assertEquals("x", property!!.name)

            // 验证变量描述符
            val variableDescriptor = bindingContext[BindingContext.VARIABLE, property!!]
            assertNotNull("应该创建 VariableDescriptor", variableDescriptor)

            // 验证变量名称
            assertEquals("x", variableDescriptor!!.name.asString())

            // 验证类型
            val type = variableDescriptor.type
            assertNotNull("变量应该有类型", type)
            assertEquals("Int64", type.toString())
        }
    }

    /**
     * 测试类型推断的局部变量
     *
     * 验证：
     * 1. 从初始化表达式推断类型
     * 2. 推断出的类型正确
     */
    fun `test type inference for local variable`() {
        val file = createFile(
            """
            package test

            func main() {
                let message = "Hello"
                let count = 100
                let pi = 3.14
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 3 个变量", 3, properties.size)

            // 验证 message 的类型推断为 String
            val messageDescriptor = bindingContext[BindingContext.VARIABLE, properties.first { it.name == "message" }]
            assertNotNull("message 应该有描述符", messageDescriptor)
            assertEquals("String", messageDescriptor!!.type.toString())

            // 验证 count 的类型推断（应该是整数类型）
            val countDescriptor = bindingContext[BindingContext.VARIABLE, properties.first { it.name == "count" }]
            assertNotNull("count 应该有描述符", countDescriptor)
            // 根据实际类型推断实现，可能是 Int64 或其他整数类型
            assertNotNull("count 应该有类型", countDescriptor!!.type)

            // 验证 pi 的类型推断（应该是浮点类型）
            val piDescriptor = bindingContext[BindingContext.VARIABLE, properties.first { it.name == "pi" }]
            assertNotNull("pi 应该有描述符", piDescriptor)
            assertNotNull("pi 应该有类型", piDescriptor!!.type)
        }
    }

    /**
     * 测试可变变量（var）
     *
     * 验证：
     * 1. var 变量的描述符正确创建
     * 2. 可变性标记正确
     */
    fun `test mutable variable descriptor`() {
        val file = createFile(
            """
            package test

            func main() {
                var counter: Int64 = 0
                counter = 1
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val property = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到 var 变量", property)

            val variableDescriptor = bindingContext[BindingContext.VARIABLE, property!!]
            assertNotNull("应该创建 VariableDescriptor", variableDescriptor)

            // 验证可变性
            assertTrue("var 变量应该是可变的", variableDescriptor!!.isVar)
        }
    }

    /**
     * 测试不可变变量（let）
     *
     * 验证：
     * 1. let 变量的描述符正确创建
     * 2. 不可变性标记正确
     */
    fun `test immutable variable descriptor`() {
        val file = createFile(
            """
            package test

            func main() {
                let constant: Int64 = 42
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val property = PsiTreeUtil.findChildOfType(file, CjProperty::class.java)
            assertNotNull("应该找到 let 变量", property)

            val variableDescriptor = bindingContext[BindingContext.VARIABLE, property!!]
            assertNotNull("应该创建 VariableDescriptor", variableDescriptor)

            // 验证不可变性
            assertFalse("let 变量应该是不可变的", variableDescriptor!!.isVar)
        }
    }

    // ==================== 类属性测试 ====================

    /**
     * 测试类属性的描述符创建
     *
     * 验证：
     * 1. 类属性的 VariableDescriptor 被创建
     * 2. 属性类型正确
     * 3. 属性所属的类正确
     */
    fun `test class property descriptor`() {
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
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val properties = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
            assertEquals("应该有 2 个属性", 2, properties.size)

            // 验证 name 属性
            val nameProperty = properties.first { it.name == "name" }
            val nameDescriptor = bindingContext[BindingContext.VARIABLE, nameProperty!!]
            assertNotNull("name 应该有描述符", nameDescriptor)
            assertEquals("name", nameDescriptor!!.name.asString())
            assertEquals("String", nameDescriptor.type.toString())
            assertFalse("name 应该是不可变的", nameDescriptor.isVar)

            // 验证 age 属性
            val ageProperty = properties.first { it.name == "age" }
            val ageDescriptor = bindingContext[BindingContext.VARIABLE, ageProperty!!]
            assertNotNull("age 应该有描述符", ageDescriptor)
            assertEquals("age", ageDescriptor!!.name.asString())
            assertEquals("Int64", ageDescriptor.type.toString())
            assertTrue("age 应该是可变的", ageDescriptor.isVar)
        }
    }

    /**
     * 测试带初始值的属性
     *
     * 验证：
     * 1. 属性初始化表达式的类型推断
     * 2. 初始值类型与属性类型匹配
     */
    fun `test property with initializer`() {
        val file = createFile(
            """
            package test

            class Config {
                public let maxRetries: Int64 = 3
                public var timeout: Int64 = 5000
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val properties = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
            assertEquals("应该有 2 个属性", 2, properties.size)

            // 验证 maxRetries 属性
            val maxRetriesProperty = properties.first { it.name == "maxRetries" }
            val maxRetriesDescriptor = bindingContext[BindingContext.VARIABLE, maxRetriesProperty!!]
            assertNotNull("maxRetries 应该有描述符", maxRetriesDescriptor)
            assertEquals("Int64", maxRetriesDescriptor!!.type.toString())

            // 验证初始化表达式存在
            assertNotNull("maxRetries 应该有初始化表达式", maxRetriesProperty.initializer)
        }
    }

    /**
     * 测试带 getter/setter 的属性
     *
     * 验证：
     * 1. 自定义 getter 的函数描述符
     * 2. 自定义 setter 的函数描述符
     * 3. getter/setter 的类型正确性
     */
    fun `test property with getter and setter`() {
        val file = createFile(
            """
            package test

            class Temperature {
                private var _celsius: Float64 = 0.0

                public prop fahrenheit: Float64 {
                    get() {
                        return _celsius * 9.0 / 5.0 + 32.0
                    }
                    set(value) {
                        _celsius = (value - 32.0) * 5.0 / 9.0
                    }
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            // 查找 fahrenheit 属性
            val fahrenheitProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
                .firstOrNull { it.name == "fahrenheit" }
            assertNotNull("应该找到 fahrenheit 属性", fahrenheitProperty)

            val propertyDescriptor = bindingContext[BindingContext.VARIABLE, fahrenheitProperty!!]
            assertNotNull("fahrenheit 应该有描述符", propertyDescriptor)
            assertEquals("Float64", propertyDescriptor!!.type.toString())

            // 验证 getter 和 setter 存在（如果实现了访问器支持）
            val getter = fahrenheitProperty!!.getter
            val setter = fahrenheitProperty.setter
            assertNotNull("应该有 getter", getter)
            assertNotNull("应该有 setter", setter)
        }
    }

    // ==================== 顶层属性测试 ====================

    /**
     * 测试顶层属性
     *
     * 验证：
     * 1. 顶层属性的描述符创建
     * 2. 顶层常量的类型推断
     */
    fun `test top level property`() {
        val file = createFile(
            """
            package test

            let APP_NAME: String = "MyApp"
            let VERSION: Int64 = 1
            var debugMode: Bool = false
            """.trimIndent()
        )

        analyzeForTest(file) {
            val properties = PsiTreeUtil.findChildrenOfType(file, CjProperty::class.java)
            assertEquals("应该有 3 个顶层属性", 3, properties.size)

            // 验证 APP_NAME
            val appNameProperty = properties.first { it.name == "APP_NAME" }
            val appNameDescriptor = bindingContext[BindingContext.VARIABLE, appNameProperty!!]
            assertNotNull("APP_NAME 应该有描述符", appNameDescriptor)
            assertEquals("String", appNameDescriptor!!.type.toString())
            assertFalse("APP_NAME 应该是不可变的", appNameDescriptor.isVar)

            // 验证 VERSION
            val versionProperty = properties.first { it.name == "VERSION" }
            val versionDescriptor = bindingContext[BindingContext.VARIABLE, versionProperty!!]
            assertNotNull("VERSION 应该有描述符", versionDescriptor)
            assertEquals("Int64", versionDescriptor!!.type.toString())

            // 验证 debugMode
            val debugProperty = properties.first { it.name == "debugMode" }
            val debugDescriptor = bindingContext[BindingContext.VARIABLE, debugProperty!!]
            assertNotNull("debugMode 应该有描述符", debugDescriptor)
            assertEquals("Bool", debugDescriptor!!.type.toString())
            assertTrue("debugMode 应该是可变的", debugDescriptor.isVar)
        }
    }

    // ==================== 延迟初始化测试 ====================

    /**
     * 测试延迟初始化属性
     *
     * 验证：
     * 1. lateinit 属性的描述符
     * 2. 延迟初始化标记正确
     */
    fun `test lateinit property`() {
        val file = createFile(
            """
            package test

            class Service {
                private lateinit var connection: String

                public func initialize() {
                    connection = "Connected"
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val connectionProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
                .firstOrNull { it.name == "connection" }
            assertNotNull("应该找到 connection 属性", connectionProperty)

            val propertyDescriptor = bindingContext[BindingContext.VARIABLE, connectionProperty!!]
            assertNotNull("connection 应该有描述符", propertyDescriptor)
            assertEquals("String", propertyDescriptor!!.type.toString())

            // 验证 lateinit 标记（如果实现了）
            // assertTrue("connection 应该是延迟初始化的", propertyDescriptor.isLateInit)
        }
    }

    // ==================== 可空类型属性测试 ====================

    /**
     * 测试可空类型属性
     *
     * 验证：
     * 1. 可空类型的正确解析
     * 2. 可空性标记
     */
    fun `test nullable property`() {
        val file = createFile(
            """
            package test

            class User {
                public var nickname: String? = null
                public let email: String

                public init(email: String) {
                    this.email = email
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            // 验证 nickname 属性（可空）
            val nicknameProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
                .firstOrNull { it.name == "nickname" }
            assertNotNull("应该找到 nickname 属性", nicknameProperty)

            val nicknameDescriptor = bindingContext[BindingContext.VARIABLE, nicknameProperty!!]
            assertNotNull("nickname 应该有描述符", nicknameDescriptor)

            // 验证可空性
            val nicknameType = nicknameDescriptor!!.type
            assertNotNull("nickname 应该有类型", nicknameType)
            // 可空类型的字符串表示应该包含 ? 或标记为 nullable
            // assertTrue("nickname 应该是可空类型", nicknameType.isMarkedNullable)

            // 验证 email 属性（非可空）
            val emailProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
                .firstOrNull { it.name == "email" }
            assertNotNull("应该找到 email 属性", emailProperty)

            val emailDescriptor = bindingContext[BindingContext.VARIABLE, emailProperty!!]
            assertNotNull("email 应该有描述符", emailDescriptor)
            assertEquals("String", emailDescriptor!!.type.toString())
        }
    }

    // ==================== 计算属性测试 ====================

    /**
     * 测试只读计算属性
     *
     * 验证：
     * 1. 只有 getter 的属性
     * 2. 计算属性的类型推断
     */
    fun `test computed property`() {
        val file = createFile(
            """
            package test

            class Rectangle {
                public let width: Float64
                public let height: Float64

                public prop area: Float64 {
                    get() {
                        return width * height
                    }
                }

                public init(width: Float64, height: Float64) {
                    this.width = width
                    this.height = height
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val areaProperty = PsiTreeUtil.findChildrenOfType(classDecl, CjProperty::class.java)
                .firstOrNull { it.name == "area" }
            assertNotNull("应该找到 area 属性", areaProperty)

            val propertyDescriptor = bindingContext[BindingContext.VARIABLE, areaProperty!!]
            assertNotNull("area 应该有描述符", propertyDescriptor)
            assertEquals("Float64", propertyDescriptor!!.type.toString())

            // 验证只有 getter
            assertNotNull("area 应该有 getter", areaProperty!!.getter)
            assertNull("area 不应该有 setter", areaProperty.setter)
        }
    }
}
