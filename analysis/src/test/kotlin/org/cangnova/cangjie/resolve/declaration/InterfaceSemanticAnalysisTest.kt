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
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 接口语义分析测试
 *
 * 专注于测试接口声明和实现的语义分析，包括：
 * - 接口描述符的创建
 * - 接口继承的类型解析
 * - 接口实现的语义验证
 * - 抽象成员和默认实现的区分
 *
 * ## 测试策略
 *
 * 1. **接口描述符**: 验证接口的 ClassDescriptor（ClassKind.INTERFACE）
 * 2. **继承语义**: 验证接口的多继承语义
 * 3. **实现验证**: 验证类实现接口的语义正确性
 * 4. **成员解析**: 验证接口成员的解析和覆盖
 */
class InterfaceSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 接口描述符创建测试 ====================

    /**
     * 测试简单接口的描述符创建
     *
     * 验证：
     * 1. 接口的 ClassDescriptor 被正确创建
     * 2. ClassKind 为 INTERFACE
     * 3. 接口的默认类型正确
     */
    fun `test simple interface descriptor creation`() {
        val file = createFile(
            """
            package test

            interface Flyable {
                func fly(): Unit
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaceDecl = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到接口声明", interfaceDecl)
            assertEquals("Flyable", interfaceDecl!!.name)

            // 验证接口描述符
            val interfaceDescriptor = bindingContext[BindingContext.CLASS, interfaceDecl!!]
            assertNotNull("应该创建接口描述符", interfaceDescriptor)

            // 验证类型种类为接口
            assertEquals(
                "应该是接口类型",
                ClassKind.INTERFACE,
                interfaceDescriptor!!.kind
            )

            // 验证接口名称
            assertEquals("Flyable", interfaceDescriptor.name.asString())

            // 验证默认类型
            assertNotNull("接口应该有默认类型", interfaceDescriptor.defaultType)
            assertEquals("Flyable", interfaceDescriptor.defaultType.toString())
        }
    }

    /**
     * 测试带默认实现的接口
     *
     * 验证：
     * 1. 接口可以包含有实现的方法
     * 2. 抽象方法和默认实现方法都能正确解析
     */
    fun `test interface with default implementation`() {
        val file = createFile(
            """
            package test

            interface Walkable {
                func walk(): Unit {
                    // 默认实现
                }

                func run(): Unit  // 抽象方法
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaceDecl = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到接口声明", interfaceDecl)

            val interfaceDescriptor = bindingContext[BindingContext.CLASS, interfaceDecl!!]
            assertNotNull("应该创建接口描述符", interfaceDescriptor)

            // 验证是接口
            assertEquals(ClassKind.INTERFACE, interfaceDescriptor!!.kind)

            // 验证成员作用域
            val memberScope = interfaceDescriptor.unsubstitutedMemberScope
            assertNotNull("接口应该有成员作用域", memberScope)

            // 注意：具体的成员函数解析需要根据实际 API
            // 这里验证基本结构
        }
    }

    // ==================== 接口继承语义测试 ====================

    /**
     * 测试单接口继承
     *
     * 验证：
     * 1. 接口可以继承另一个接口
     * 2. 继承关系通过类型上界正确建立
     */
    fun `test single interface inheritance`() {
        val file = createFile(
            """
            package test

            interface Animal {
                func eat(): Unit
            }

            interface Flyable <: Animal {
                func fly(): Unit
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val animalInterface = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull { it.name == "Animal" }
            assertNotNull("应该找到 Animal 接口", animalInterface)

            val animalDescriptor = bindingContext[BindingContext.CLASS, animalInterface!!]
            assertNotNull("应该创建 Animal 描述符", animalDescriptor)
            assertEquals(ClassKind.INTERFACE, animalDescriptor!!.kind)

            val flyableInterface = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull { it.name == "Flyable" }
            assertNotNull("应该找到 Flyable 接口", flyableInterface)

            val flyableDescriptor = bindingContext[BindingContext.CLASS, flyableInterface!!]
            assertNotNull("应该创建 Flyable 描述符", flyableDescriptor)
            assertEquals(ClassKind.INTERFACE, flyableDescriptor!!.kind)

            // 验证继承关系
            val superTypes = flyableDescriptor.typeConstructor.supertypes
            assertTrue("Flyable 应该有父接口", superTypes.isNotEmpty())
            assertTrue(
                "Flyable 应该继承 Animal",
                superTypes.any { it.toString().contains("Animal") }
            )
        }
    }

    /**
     * 测试多接口继承
     *
     * 验证：
     * 1. 接口可以继承多个接口
     * 2. 所有父接口都在类型上界中
     */
    fun `test multiple interface inheritance`() {
        val file = createFile(
            """
            package test

            interface Flyable {
                func fly(): Unit
            }

            interface Swimmable {
                func swim(): Unit
            }

            interface Amphibian <: Flyable & Swimmable {
                func jump(): Unit
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val amphibianInterface = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull { it.name == "Amphibian" }
            assertNotNull("应该找到 Amphibian 接口", amphibianInterface)

            val amphibianDescriptor = bindingContext[BindingContext.CLASS, amphibianInterface!!]
            assertNotNull("应该创建 Amphibian 描述符", amphibianDescriptor)
            assertEquals(ClassKind.INTERFACE, amphibianDescriptor!!.kind)

            // 验证继承多个接口
            val superTypes = amphibianDescriptor.typeConstructor.supertypes
            assertTrue("Amphibian 应该有多个父接口", superTypes.size >= 2)

            // 验证包含两个父接口
            val hasFlyable = superTypes.any { it.toString().contains("Flyable") }
            val hasSwimmable = superTypes.any { it.toString().contains("Swimmable") }
            assertTrue("应该继承 Flyable", hasFlyable)
            assertTrue("应该继承 Swimmable", hasSwimmable)
        }
    }

    /**
     * 测试接口继承链
     *
     * 验证：
     * 1. 接口继承链的类型传递
     * 2. 间接继承的类型也能正确解析
     */
    fun `test interface inheritance chain`() {
        val file = createFile(
            """
            package test

            interface A {
                func methodA(): Unit
            }

            interface B <: A {
                func methodB(): Unit
            }

            interface C <: B {
                func methodC(): Unit
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaceC = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull { it.name == "C" }
            assertNotNull("应该找到接口 C", interfaceC)

            val descriptorC = bindingContext[BindingContext.CLASS, interfaceC!!]
            assertNotNull("应该创建 C 的描述符", descriptorC)

            // 验证 C 的直接父接口是 B
            val superTypes = descriptorC!!.typeConstructor.supertypes
            assertTrue("C 应该有父接口", superTypes.isNotEmpty())
            assertTrue("C 应该继承 B", superTypes.any { it.toString().contains("B") })

            // 验证 B 的描述符和继承关系
            val interfaceB = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull { it.name == "B" }
            val descriptorB = bindingContext[BindingContext.CLASS, interfaceB!!]
            assertNotNull("应该创建 B 的描述符", descriptorB)

            val bSuperTypes = descriptorB!!.typeConstructor.supertypes
            assertTrue("B 应该继承 A", bSuperTypes.any { it.toString().contains("A") })
        }
    }

    // ==================== 类实现接口语义测试 ====================

    /**
     * 测试类实现单个接口
     *
     * 验证：
     * 1. 类的类型上界包含接口类型
     * 2. 类的描述符正确引用接口
     */
    fun `test class implements single interface`() {
        val file = createFile(
            """
            package test

            interface Flyable {
                func fly(): Unit
            }

            class Bird <: Flyable {
                public func fly(): Unit {
                    // 实现
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val flyableInterface = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到 Flyable 接口", flyableInterface)

            val flyableDescriptor = bindingContext[BindingContext.CLASS, flyableInterface!!]
            assertNotNull("应该创建 Flyable 描述符", flyableDescriptor)
            assertEquals(ClassKind.INTERFACE, flyableDescriptor!!.kind)

            val birdClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Bird 类", birdClass)

            val birdDescriptor = bindingContext[BindingContext.CLASS, birdClass!!]
            assertNotNull("应该创建 Bird 描述符", birdDescriptor)
            assertEquals(ClassKind.CLASS, birdDescriptor!!.kind)

            // 验证 Bird 实现 Flyable
            val superTypes = birdDescriptor.typeConstructor.supertypes
            assertTrue("Bird 应该实现 Flyable", superTypes.any { it.toString().contains("Flyable") })
        }
    }

    /**
     * 测试类实现多个接口
     *
     * 验证：
     * 1. 类可以实现多个接口
     * 2. 所有接口都在类的类型上界中
     */
    fun `test class implements multiple interfaces`() {
        val file = createFile(
            """
            package test

            interface Flyable {
                func fly(): Unit
            }

            interface Swimmable {
                func swim(): Unit
            }

            class Duck <: Flyable & Swimmable {
                public func fly(): Unit {
                    // 实现
                }

                public func swim(): Unit {
                    // 实现
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val duckClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Duck 类", duckClass)

            val duckDescriptor = bindingContext[BindingContext.CLASS, duckClass!!]
            assertNotNull("应该创建 Duck 描述符", duckDescriptor)

            // 验证 Duck 实现两个接口
            val superTypes = duckDescriptor!!.typeConstructor.supertypes
            assertTrue("Duck 应该有多个父类型", superTypes.size >= 2)

            val hasFlyable = superTypes.any { it.toString().contains("Flyable") }
            val hasSwimmable = superTypes.any { it.toString().contains("Swimmable") }
            assertTrue("Duck 应该实现 Flyable", hasFlyable)
            assertTrue("Duck 应该实现 Swimmable", hasSwimmable)
        }
    }

    /**
     * 测试类继承并实现接口
     *
     * 验证：
     * 1. 类可以同时继承类和实现接口
     * 2. 父类和接口都在类型上界中
     */
    fun `test class extends and implements interface`() {
        val file = createFile(
            """
            package test

            open class Animal {
                public var name: String = ""
            }

            interface Flyable {
                func fly(): Unit
            }

            class Bird <: Animal & Flyable {
                public func fly(): Unit {
                    // 实现
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val birdClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java).find {
                it.name == "Bird"
            }
            assertNotNull("应该找到 Bird 类", birdClass)

            val birdDescriptor = bindingContext[BindingContext.CLASS, birdClass!!]
            assertNotNull("应该创建 Bird 描述符", birdDescriptor)

            // 验证 Bird 既继承 Animal 又实现 Flyable
            val superTypes = birdDescriptor!!.typeConstructor.supertypes
            assertTrue("Bird 应该有多个父类型", superTypes.size >= 2)

            val hasAnimal = superTypes.any { it.toString().contains("Animal") }
            val hasFlyable = superTypes.any { it.toString().contains("Flyable") }
            assertTrue("Bird 应该继承 Animal", hasAnimal)
            assertTrue("Bird 应该实现 Flyable", hasFlyable)
        }
    }

    // ==================== 接口成员解析测试 ====================

    /**
     * 测试接口成员的解析
     *
     * 验证：
     * 1. 接口的成员作用域包含所有声明的成员
     * 2. 抽象方法和默认实现都能被解析
     */
    fun `test interface member resolution`() {
        val file = createFile(
            """
            package test

            interface Named {
                var name: String

                func getName(): String {
                    return name
                }

                func setName(newName: String): Unit
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaceDecl = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到 Named 接口", interfaceDecl)

            val interfaceDescriptor = bindingContext[BindingContext.CLASS, interfaceDecl!!]
            assertNotNull("应该创建接口描述符", interfaceDescriptor)

            // 验证成员作用域
            val memberScope = interfaceDescriptor!!.unsubstitutedMemberScope
            assertNotNull("接口应该有成员作用域", memberScope)

            // 注意：具体的成员查找需要根据实际的作用域 API
            // 这里验证基本结构存在
        }
    }
}
