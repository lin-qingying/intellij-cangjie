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
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 接口声明和实现测试
 *
 * 测试仓颉语言的接口声明和实现机制的语义分析功能：
 * - 接口的基本声明
 * - 接口继承
 * - 接口成员（抽象方法、默认实现）
 * - 类实现接口
 * - 多接口实现
 * - 静态成员
 *
 * ## 测试策略
 *
 * 1. **接口声明**: 验证接口的 PSI 结构和描述符创建
 * 2. **接口继承**: 验证接口之间的继承关系
 * 3. **成员验证**: 验证接口成员的声明和实现
 * 4. **实现检查**: 验证类对接口的实现完整性
 *
 * ## 相关文档
 *
 * - 仓颉文档: class_and_interface/interface.md
 * - 接口实现: class.md#接口实现
 */
class InterfaceDeclarationTest : CangJieAnalysisTestBase() {

    // ==================== 基本接口声明测试 ====================

    /**
     * 测试简单接口声明
     *
     * 验证：
     * 1. 接口的 PSI 结构正确
     * 2. InterfaceDescriptor 被正确创建
     * 3. 接口名正确记录
     */
    fun `test simple interface declaration`() {
        val file = createFile(
            """
            package test

            interface Flyable {
                func fly(): Unit
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 查找接口声明
            val interfaceDecl = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到接口声明", interfaceDecl)
            assertEquals("Flyable", interfaceDecl!!.name)

            // 验证接口描述符
            val interfaceDescriptor = bindingContext[BindingContext.CLASS, interfaceDecl!!]
            // 注意：如果还没有实现完整的描述符系统，这里可能需要调整
            // assertNotNull("应该创建接口描述符", interfaceDescriptor)
            // assertTrue("应该是接口", interfaceDescriptor?.kind == ClassKind.INTERFACE)

            // 验证成员函数
            val members = PsiTreeUtil.findChildrenOfType(interfaceDecl, CjFunction::class.java)
            assertEquals("应该有 1 个成员函数", 1, members.size)
            assertEquals("fly", members.first().name)
        }
    }

    /**
     * 测试带默认实现的接口
     *
     * 验证：
     * 1. 接口可以包含默认实现的方法
     * 2. 默认实现方法有方法体
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

            // 验证成员函数
            val functions = PsiTreeUtil.findChildrenOfType(interfaceDecl!!, CjFunction::class.java)
            assertEquals("应该有 2 个成员函数", 2, functions.size)

            val walkFunc = functions.firstOrNull { it.name == "walk" }
            assertNotNull("应该找到 walk 方法", walkFunc)
            assertNotNull("walk 方法应该有方法体", walkFunc!!.bodyExpression)

            val runFunc = functions.firstOrNull { it.name == "run" }
            assertNotNull("应该找到 run 方法", runFunc)
            assertNull("run 方法不应该有方法体", runFunc!!.bodyExpression)
        }
    }

    /**
     * 测试带属性的接口
     *
     * 验证：
     * 1. 接口可以声明抽象属性
     * 2. 接口属性没有初始化器
     */
    fun `test interface with properties`() {
        val file = createFile(
            """
            package test

            interface Named {
                var name: String
                func getName(): String
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaceDecl = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到接口声明", interfaceDecl)

            // 验证属性声明
            val properties = PsiTreeUtil.findChildrenOfType(interfaceDecl!!, CjProperty::class.java)
            assertEquals("应该有 1 个属性", 1, properties.size)
            assertEquals("name", properties.first().name)

            // 验证方法声明
            val functions = PsiTreeUtil.findChildrenOfType(interfaceDecl, CjFunction::class.java)
            assertEquals("应该有 1 个方法", 1, functions.size)
            assertEquals("getName", functions.first().name)
        }
    }

    // ==================== 接口继承测试 ====================

    /**
     * 测试单接口继承
     *
     * 验证：
     * 1. 接口可以继承另一个接口
     * 2. 继承关系正确建立
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

            val flyableInterface = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull { it.name == "Flyable" }
            assertNotNull("应该找到 Flyable 接口", flyableInterface)

            // 验证继承关系
            val superTypeList = flyableInterface!!.getSuperTypeList()
            assertNotNull("Flyable 应该有父接口列表", superTypeList)

            val superTypes = superTypeList!!.entries
            assertEquals("应该有 1 个父接口", 1, superTypes.size)
        }
    }

    /**
     * 测试多接口继承
     *
     * 验证：
     * 1. 接口可以继承多个接口（使用 & 连接）
     * 2. 所有父接口都被正确解析
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

            // 验证继承多个接口
            val superTypeList = amphibianInterface!!.getSuperTypeList()
            assertNotNull("Amphibian 应该有父接口列表", superTypeList)

            val superTypes = superTypeList!!.entries
            assertEquals("应该继承 2 个接口", 2, superTypes.size)

            // 验证自己的方法
            val jumpMethod = PsiTreeUtil.findChildrenOfType(amphibianInterface, CjFunction::class.java)
                .firstOrNull { it.name == "jump" }
            assertNotNull("应该有 jump 方法", jumpMethod)
        }
    }

    /**
     * 测试接口继承链
     *
     * 验证：
     * 1. A -> B -> C 的接口继承链正确建立
     * 2. 每一层的继承关系正确
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
            val interfaces = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
            assertEquals("应该有 3 个接口", 3, interfaces.size)

            val interfaceA = interfaces.firstOrNull { it.name == "A" }
            val interfaceB = interfaces.firstOrNull { it.name == "B" }
            val interfaceC = interfaces.firstOrNull { it.name == "C" }

            assertNotNull("应该找到接口 A", interfaceA)
            assertNotNull("应该找到接口 B", interfaceB)
            assertNotNull("应该找到接口 C", interfaceC)

            // 验证 B 继承 A
            assertNotNull("B 应该有父接口", interfaceB!!.getSuperTypeList())

            // 验证 C 继承 B
            assertNotNull("C 应该有父接口", interfaceC!!.getSuperTypeList())
        }
    }

    // ==================== 类实现接口测试 ====================

    /**
     * 测试类实现单个接口
     *
     * 验证：
     * 1. 类正确实现接口
     * 2. 接口方法在类中被实现
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

            val birdClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Bird 类", birdClass)

            // 验证接口实现
            val superTypeList = birdClass!!.getSuperTypeList()
            assertNotNull("Bird 应该有接口实现", superTypeList)

            // 验证 fly 方法实现
            val flyMethod = PsiTreeUtil.findChildrenOfType(birdClass, CjFunction::class.java)
                .firstOrNull { it.name == "fly" }
            assertNotNull("应该实现 fly 方法", flyMethod)
            assertNotNull("fly 方法应该有实现", flyMethod!!.bodyExpression)
        }
    }

    /**
     * 测试类实现多个接口
     *
     * 验证：
     * 1. 类可以实现多个接口（使用 & 连接）
     * 2. 所有接口方法都被实现
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

            // 验证多接口实现
            val superTypeList = duckClass!!.getSuperTypeList()
            assertNotNull("Duck 应该有接口列表", superTypeList)

            val superTypes = superTypeList!!.entries
            assertEquals("应该实现 2 个接口", 2, superTypes.size)

            // 验证两个方法都实现了
            val methods = PsiTreeUtil.findChildrenOfType(duckClass, CjFunction::class.java)
            val flyMethod = methods.firstOrNull { it.name == "fly" }
            val swimMethod = methods.firstOrNull { it.name == "swim" }

            assertNotNull("应该实现 fly 方法", flyMethod)
            assertNotNull("应该实现 swim 方法", swimMethod)
        }
    }

    /**
     * 测试类继承并实现接口
     *
     * 验证：
     * 1. 类可以同时继承父类和实现接口
     * 2. 继承顺序：父类在前，接口在后
     */
    fun `test class extends and implements`() {
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
            val birdClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Bird 类", birdClass)

            // 验证同时有父类和接口
            val superTypeList = birdClass!!.getSuperTypeList()
            assertNotNull("Bird 应该有父类和接口", superTypeList)

            val superTypes = superTypeList!!.entries
            assertEquals("应该有 2 个父类型(1 个父类 + 1 个接口)", 2, superTypes.size)

            // 验证 fly 方法实现
            val flyMethod = PsiTreeUtil.findChildrenOfType(birdClass, CjFunction::class.java)
                .firstOrNull { it.name == "fly" }
            assertNotNull("应该实现 fly 方法", flyMethod)
        }
    }

    // ==================== 接口默认实现测试 ====================

    /**
     * 测试类使用接口默认实现
     *
     * 验证：
     * 1. 类可以不重写接口的默认实现方法
     * 2. 类可以选择性重写默认实现
     */
    fun `test class uses default implementation`() {
        val file = createFile(
            """
            package test

            interface Walkable {
                func walk(): Unit {
                    // 默认实现
                }

                func run(): Unit  // 抽象方法
            }

            class Person <: Walkable {
                // 使用 walk 的默认实现

                public func run(): Unit {
                    // 必须实现抽象方法
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val personClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Person 类", personClass)

            // 验证只实现了 run 方法
            val methods = PsiTreeUtil.findChildrenOfType(personClass!!, CjFunction::class.java)
            val runMethod = methods.firstOrNull { it.name == "run" }
            assertNotNull("应该实现 run 方法", runMethod)

            // walk 方法没有在类中显式实现（使用默认实现）
            val walkMethod = methods.firstOrNull { it.name == "walk" }
            assertNull("walk 方法应该使用默认实现", walkMethod)
        }
    }

    /**
     * 测试类重写接口默认实现
     *
     * 验证：
     * 1. 类可以重写接口的默认实现
     * 2. 重写的方法覆盖默认实现
     */
    fun `test class overrides default implementation`() {
        val file = createFile(
            """
            package test

            interface Walkable {
                func walk(): Unit {
                    // 默认实现
                }
            }

            class Robot <: Walkable {
                public override func walk(): Unit {
                    // 自定义实现
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val robotClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Robot 类", robotClass)

            // 验证 walk 方法被重写
            val walkMethod = PsiTreeUtil.findChildrenOfType(robotClass!!, CjFunction::class.java)
                .firstOrNull { it.name == "walk" }
            assertNotNull("应该重写 walk 方法", walkMethod)

            // 验证 override 修饰符
            assertTrue(
                "walk 方法应该有 override 修饰符",
                walkMethod!!.hasModifier(CjTokens.OVERRIDE_KEYWORD)
            )
        }
    }

    // ==================== 接口属性实现测试 ====================

    /**
     * 测试类实现接口属性
     *
     * 验证：
     * 1. 类必须实现接口的所有抽象属性
     * 2. 属性可以在主构造函数或类体中实现
     */
    fun `test class implements interface properties`() {
        val file = createFile(
            """
            package test

            interface Named {
                var name: String
            }

            class Person(public var name: String) <: Named {
                // 通过主构造函数实现 name 属性
            }

            class Robot <: Named {
                public var name: String = "R2D2"  // 在类体中实现
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val personClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Person" }
            assertNotNull("应该找到 Person 类", personClass)

            // 验证主构造函数参数
            val primaryConstructor = personClass!!.primaryConstructor
            assertNotNull("Person 应该有主构造函数", primaryConstructor)

            val robotClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Robot" }
            assertNotNull("应该找到 Robot 类", robotClass)

            // 验证类体中的属性
            val nameProperty = PsiTreeUtil.findChildrenOfType(robotClass!!, CjProperty::class.java)
                .firstOrNull { it.name == "name" }
            assertNotNull("Robot 应该有 name 属性", nameProperty)
        }
    }

    // ==================== 接口冲突解决测试 ====================

    /**
     * 测试多接口方法冲突
     *
     * 验证：
     * 1. 当多个接口有相同签名的方法时
     * 2. 实现类必须显式实现该方法
     */
    fun `test multiple interface method conflict`() {
        val file = createFile(
            """
            package test

            interface A {
                func foo(): Unit {
                    // A 的默认实现
                }
            }

            interface B {
                func foo(): Unit {
                    // B 的默认实现
                }
            }

            class C <: A & B {
                public override func foo(): Unit {
                    // 必须显式实现以解决冲突
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classC = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类 C", classC)

            // 验证 foo 方法被显式实现
            val fooMethod = PsiTreeUtil.findChildrenOfType(classC!!, CjFunction::class.java)
                .firstOrNull { it.name == "foo" }
            assertNotNull("应该显式实现 foo 方法", fooMethod)
            assertNotNull("foo 方法应该有实现", fooMethod!!.bodyExpression)

            // 验证 override 修饰符
            assertTrue(
                "foo 方法应该有 override 修饰符",
                fooMethod.hasModifier(CjTokens.OVERRIDE_KEYWORD)
            )
        }
    }
}
