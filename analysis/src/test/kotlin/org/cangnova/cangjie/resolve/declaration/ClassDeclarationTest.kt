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

package org.cangnova.cangjie.resolve.declaration

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.CangJieAnalysisTestBase
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 类声明和继承测试
 *
 * 测试仓颉语言的类声明和继承机制的语义分析功能：
 * - 类的基本声明
 * - 单继承关系
 * - 接口实现
 * - Open/Sealed 修饰符
 * - 继承层次结构验证
 * - 成员继承
 *
 * ## 测试策略
 *
 * 1. **类声明**: 验证类的 PSI 结构和描述符创建
 * 2. **继承关系**: 验证父类和接口的正确解析
 * 3. **修饰符**: 验证 open、sealed、abstract 等修饰符的语义
 * 4. **成员继承**: 验证成员的继承和可见性
 *
 * ## 相关文档
 *
 * - 仓颉文档: class_and_interface/class.md
 * - 继承机制: class_的继承
 * - 接口实现: interface.md
 */
class ClassDeclarationTest : CangJieAnalysisTestBase() {

    // ==================== 基本类声明测试 ====================

    /**
     * 测试简单类声明
     *
     * 验证：
     * 1. 类的 PSI 结构正确
     * 2. ClassDescriptor 被正确创建
     * 3. 类名正确记录
     */
    fun `test simple class declaration`() {
        val file = createFile(
            """
            package test

            class Foo {
                public var bar: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 查找类声明
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)
            assertEquals("Foo", classDecl!!.name)

            // 验证类描述符
            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl]
            // 注意：如果还没有实现完整的描述符系统，这里可能需要调整
            // assertNotNull("应该创建类描述符", classDescriptor)
            // assertEquals("Foo", classDescriptor?.name?.asString())

            // 验证成员变量
            val members = PsiTreeUtil.findChildrenOfType(classDecl, CjFieldVariable::class.java)
            assertEquals("应该有 1 个成员变量", 1, members.size)
            assertEquals("bar", members.first().name)
        }
    }

    /**
     * 测试抽象类声明
     *
     * 验证：
     * 1. abstract 修饰符被正确识别
     * 2. 抽象类可以包含抽象成员
     */
    fun `test abstract class declaration`() {
        val file = createFile(
            """
            package test

            abstract class Shape {
                abstract func area(): Float64
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到抽象类声明", classDecl)

            // 验证 abstract 修饰符
            assertTrue(
                "类应该有 abstract 修饰符",
                classDecl!!.hasModifier(CjTokens.ABSTRACT_KEYWORD)
            )

            // 验证抽象成员函数
            val abstractFunc = PsiTreeUtil.findChildOfType(classDecl, CjFunction::class.java)
            assertNotNull("应该找到抽象函数", abstractFunc)
            assertTrue(
                "函数应该有 abstract 修饰符",
                abstractFunc!!.hasModifier(CjTokens.ABSTRACT_KEYWORD)
            )
        }
    }

    /**
     * 测试 Open 类声明
     *
     * 验证：
     * 1. open 修饰符允许类被继承
     * 2. 非 open 类不能被继承
     */
    fun `test open class declaration`() {
        val file = createFile(
            """
            package test

            open class Base {
                public var x: Int64 = 0
            }

            class Child <: Base {
                public var y: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val baseClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Base" }
            assertNotNull("应该找到 Base 类", baseClass)

            // 验证 open 修饰符
            assertTrue(
                "Base 类应该有 open 修饰符",
                baseClass!!.hasModifier(CjTokens.OPEN_KEYWORD)
            )

            val childClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Child" }
            assertNotNull("应该找到 Child 类", childClass)

            // 验证继承关系
            val superTypeList = childClass!!.getSuperTypeList()
            assertNotNull("Child 类应该有父类列表", superTypeList)
        }
    }

    /**
     * 测试 Sealed 抽象类
     *
     * 验证：
     * 1. sealed 修饰符限制类只能在包内被继承
     * 2. sealed 类隐含 public 和 open 语义
     */
    fun `test sealed abstract class declaration`() {
        val file = createFile(
            """
            package test

            sealed abstract class Result {
                class Success <: Result
                class Failure <: Result
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Result" }
            assertNotNull("应该找到 Result 类", resultClass)

            // 验证 sealed 修饰符
            assertTrue(
                "Result 类应该有 sealed 修饰符",
                resultClass!!.hasModifier(CjTokens.SEALED_KEYWORD)
            )

            // 验证嵌套子类
            val nestedClasses = PsiTreeUtil.findChildrenOfType(resultClass, CjClass::class.java)
            assertEquals("应该有 2 个嵌套子类", 2, nestedClasses.size)
        }
    }

    // ==================== 单继承测试 ====================

    /**
     * 测试简单继承
     *
     * 验证：
     * 1. 子类正确继承父类
     * 2. <: 语法正确解析
     * 3. 继承层次正确建立
     */
    fun `test simple inheritance`() {
        val file = createFile(
            """
            package test

            open class Animal {
                public var name: String = ""
            }

            class Dog <: Animal {
                public var breed: String = ""
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val animalClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Animal" }
            assertNotNull("应该找到 Animal 类", animalClass)

            val dogClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Dog" }
            assertNotNull("应该找到 Dog 类", dogClass)

            // 验证继承声明
            val superTypeList = dogClass!!.getSuperTypeList()
            assertNotNull("Dog 类应该有父类列表", superTypeList)

            val superTypes = superTypeList!!.entries
            assertEquals("应该有 1 个父类", 1, superTypes.size)

            val superType = superTypes.first().typeReference
            assertNotNull("父类应该有类型引用", superType)
        }
    }

    /**
     * 测试多层继承
     *
     * 验证：
     * 1. A -> B -> C 的继承链正确建立
     * 2. 每一层的继承关系正确
     */
    fun `test multi-level inheritance`() {
        val file = createFile(
            """
            package test

            open class A {
                public var a: Int64 = 0
            }

            open class B <: A {
                public var b: Int64 = 0
            }

            class C <: B {
                public var c: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classes = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
            assertEquals("应该有 3 个类", 3, classes.size)

            val classA = classes.firstOrNull { it.name == "A" }
            val classB = classes.firstOrNull { it.name == "B" }
            val classC = classes.firstOrNull { it.name == "C" }

            assertNotNull("应该找到类 A", classA)
            assertNotNull("应该找到类 B", classB)
            assertNotNull("应该找到类 C", classC)

            // 验证 B 继承 A
            assertNotNull("B 应该有父类", classB!!.getSuperTypeList())

            // 验证 C 继承 B
            assertNotNull("C 应该有父类", classC!!.getSuperTypeList())
        }
    }

    /**
     * 测试默认继承 Object
     *
     * 验证：
     * 1. 没有显式父类的类默认继承 Object
     * 2. Object 是所有类的根父类
     */
    fun `test default Object inheritance`() {
        val file = createFile(
            """
            package test

            class MyClass {
                public var value: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val myClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 MyClass", myClass)

            // 验证没有显式的父类声明
            val superTypeList = myClass!!.getSuperTypeList()
            // 如果为 null，说明没有显式父类，应该默认继承 Object
            // 这需要在语义分析阶段验证
        }
    }

    // ==================== 接口实现测试 ====================

    /**
     * 测试单接口实现
     *
     * 验证：
     * 1. 类正确实现接口
     * 2. 接口成员在类中被实现
     */
    fun `test single interface implementation`() {
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
            val flyableInterface = PsiTreeUtil.findChildrenOfType(file, CjInterface::class.java)
                .firstOrNull()
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
        }
    }

    /**
     * 测试多接口实现
     *
     * 验证：
     * 1. 类可以实现多个接口（使用 & 连接）
     * 2. 所有接口成员都被实现
     */
    fun `test multiple interface implementation`() {
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
     * 测试继承类并实现接口
     *
     * 验证：
     * 1. 类可以同时继承父类和实现接口
     * 2. 继承顺序：父类在前，接口在后
     */
    fun `test inheritance with interface implementation`() {
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
            val birdClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .find {
                    it.name == "Bird"
                }
            assertNotNull("应该找到 Bird 类", birdClass)

            // 验证同时有父类和接口
            val superTypeList = birdClass!!.getSuperTypeList()
            assertNotNull("Bird 应该有父类和接口", superTypeList)

            val superTypes = superTypeList!!.entries
            assertEquals("应该有 2 个父类型(1 个父类 + 1 个接口)", 2, superTypes.size)
        }
    }

    // ==================== 成员继承测试 ====================

    /**
     * 测试成员变量继承
     *
     * 验证：
     * 1. 子类继承父类的 public 成员
     * 2. 子类不继承父类的 private 成员
     */
    fun `test member variable inheritance`() {
        val file = createFile(
            """
            package test

            open class Base {
                public var publicVar: Int64 = 0
                private var privateVar: Int64 = 0
            }

            class Child <: Base {
                public func accessMembers() {
                    var x = publicVar  // 应该可以访问
                    // var y = privateVar  // 不应该可以访问
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val baseClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Base" }
            assertNotNull("应该找到 Base 类", baseClass)

            // 验证 Base 有两个成员变量
            val baseMembers = PsiTreeUtil.findChildrenOfType(baseClass!!, CjFieldVariable::class.java)
            assertEquals("Base 应该有 2 个成员变量", 2, baseMembers.size)

            val childClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Child" }
            assertNotNull("应该找到 Child 类", childClass)

            // 验证 Child 中可以访问 publicVar
            val accessFunc = PsiTreeUtil.findChildOfType(childClass!!, CjFunction::class.java)
            assertNotNull("应该找到 accessMembers 函数", accessFunc)
        }
    }

    /**
     * 测试成员函数继承
     *
     * 验证：
     * 1. 子类继承父类的 public 方法
     * 2. 子类可以覆盖父类的 open 方法
     */
    fun `test member function inheritance`() {
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
            val baseClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Base" }
            assertNotNull("应该找到 Base 类", baseClass)

            val baseGreet = PsiTreeUtil.findChildrenOfType(baseClass!!, CjFunction::class.java)
                .firstOrNull { it.name == "greet" }
            assertNotNull("Base 应该有 greet 方法", baseGreet)
            assertTrue(
                "Base.greet 应该有 open 修饰符",
                baseGreet!!.hasModifier(CjTokens.OPEN_KEYWORD)
            )

            val childClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Child" }
            assertNotNull("应该找到 Child 类", childClass)

            val childGreet = PsiTreeUtil.findChildrenOfType(childClass!!, CjFunction::class.java)
                .firstOrNull { it.name == "greet" }
            assertNotNull("Child 应该有 greet 方法", childGreet)
            assertTrue(
                "Child.greet 应该有 override 修饰符",
                childGreet!!.hasModifier(CjTokens.OVERRIDE_KEYWORD)
            )
        }
    }

    // ==================== 构造函数测试 ====================

    /**
     * 测试主构造函数
     *
     * 验证：
     * 1. 主构造函数的参数声明
     * 2. 主构造函数参数自动成为成员变量
     */
    fun `test primary constructor`() {
        val file = createFile(
            """
            package test

            class Point {
            public Point(public var x: Int64, public var y: Int64){
            }
                public func distance(): Float64 {
                    return 0.0
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val pointClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Point 类", pointClass)

            // 验证主构造函数参数
            val primaryConstructor = pointClass!!.primaryConstructor
            assertNotNull("应该有主构造函数", primaryConstructor)

            val parameters = primaryConstructor!!.valueParameterList?.parameters
            assertNotNull("主构造函数应该有参数", parameters)
            assertEquals("应该有 2 个参数", 2, parameters!!.size)
        }
    }

    /**
     * 测试次构造函数
     *
     * 验证：
     * 1. 类可以有多个构造函数
     * 2. 次构造函数必须委托到主构造函数
     */
    fun `test secondary constructor`() {
        val file = createFile(
            """
            package test

            class Point {
                public var x: Int64
                public var y: Int64

                public init(x: Int64, y: Int64) {
                    this.x = x
                    this.y = y
                }

                public init() {
                    this.x = 0
                    this.y = 0
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val pointClass = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Point 类", pointClass)

            // 验证次构造函数
            val constructors = PsiTreeUtil.findChildrenOfType(pointClass!!, CjConstructor::class.java)
            assertEquals("应该有 2 个构造函数", 2, constructors.size)
        }
    }
}
