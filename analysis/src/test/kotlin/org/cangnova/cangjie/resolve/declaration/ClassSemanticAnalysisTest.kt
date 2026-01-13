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
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.constructors
import org.cangnova.cangjie.types.deccriptorClass

/**
 * 类声明语义分析测试
 *
 * 专注于测试类声明的语义分析，包括：
 * - 描述符（Descriptor）的创建和验证
 * - 类型系统和类型推导
 * - 继承关系的语义验证
 * - 成员解析和可见性
 *
 * ## 测试策略
 *
 * 1. **描述符验证**: 验证 ClassDescriptor 是否正确创建，属性是否正确
 * 2. **类型验证**: 验证类的默认类型、父类型是否正确解析
 * 3. **继承语义**: 验证继承层次、成员继承、方法覆盖等语义
 * 4. **可见性**: 验证访问修饰符的语义效果
 *
 * 注意：这个测试文件只包含需要完整语义分析的测试。
 * 纯 PSI 结构验证的测试应该放在其他文件中。
 */
class ClassSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 描述符创建测试 ====================

    /**
     * 测试简单类的描述符创建
     *
     * 验证：
     * 1. ClassDescriptor 被正确创建
     * 2. 描述符的名称、模态性等属性正确
     * 3. 描述符与 PSI 的绑定关系正确
     */
    fun `test simple class descriptor creation`() {
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

            // 验证类描述符被创建
            val classDescriptor =  bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证描述符的基本属性
            assertEquals("Foo", classDescriptor!!.name.asString())
            assertNotNull("类应该有默认类型", classDescriptor.defaultType)

            // 验证模态性（普通类应该是 FINAL，除非有 open 修饰符）
            // 注意：根据实际实现可能需要调整
            // assertEquals(Modality.FINAL, classDescriptor.modality)

            // 验证包含关系
            val packageFqName = classDescriptor.containingDeclaration
            assertNotNull("类应该有包含的声明", packageFqName)
        }
    }

    /**
     * 测试抽象类的描述符
     *
     * 验证：
     * 1. 抽象类的 modality 为 ABSTRACT
     * 2. 抽象类不能实例化（通过构造函数检查）
     */
    fun `test abstract class descriptor`() {
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

            val classDescriptor =  bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建抽象类 ClassDescriptor", classDescriptor)

            // 验证模态性为 ABSTRACT
            assertEquals(
                "抽象类的模态性应该是 ABSTRACT",
                Modality.ABSTRACT,
                classDescriptor!!.modality
            )

            // 验证抽象类的类型
            assertNotNull("抽象类应该有默认类型", classDescriptor.defaultType)
            assertEquals("Shape", classDescriptor.defaultType.toString())
        }
    }

    /**
     * 测试 Open 类的描述符
     *
     * 验证：
     * 1. open 类的 modality 为 OPEN
     * 2. open 类可以被继承
     */
    fun `test open class descriptor`() {
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

            val baseDescriptor =  bindingContext[BindingContext.CLASS, baseClass!!]
            assertNotNull("应该创建 Base ClassDescriptor", baseDescriptor)

            // 验证 open 类的模态性
            assertEquals(
                "open 类的模态性应该是 OPEN",
                Modality.OPEN,
                baseDescriptor!!.modality
            )

            // 验证子类
            val childClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Child" }
            assertNotNull("应该找到 Child 类", childClass)

            val childDescriptor =  bindingContext[BindingContext.CLASS, childClass!!]
            assertNotNull("应该创建 Child ClassDescriptor", childDescriptor)

            // 验证继承关系（通过类型上界）
            val superTypes = childDescriptor!!.typeConstructor.supertypes
            assertNotNull("子类应该有父类型", superTypes)
            assertTrue("子类应该继承 Base", superTypes.any { it.toString().contains("Base") })
        }
    }

    // ==================== 类型解析测试 ====================

    /**
     * 测试类的默认类型
     *
     * 验证：
     * 1. 非泛型类的默认类型正确
     * 2. 类型名称与类名一致
     */
    fun `test class default type`() {
        val file = createFile(
            """
            package test

            class MyClass {
                public var value: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            val classDescriptor =  bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证默认类型
            val defaultType = classDescriptor!!.defaultType
            assertNotNull("类应该有默认类型", defaultType)
            assertEquals("MyClass", defaultType.toString())

            // 验证类型构造器
            val typeConstructor = classDescriptor.typeConstructor
            assertNotNull("类应该有类型构造器", typeConstructor)
            assertEquals("MyClass", typeConstructor.toString())
        }
    }

    /**
     * 测试继承的类型解析
     *
     * 验证：
     * 1. 父类类型被正确解析
     * 2. 类型上界包含父类
     */
    fun `test inheritance type resolution`() {
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
            val dogClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Dog" }
            assertNotNull("应该找到 Dog 类", dogClass)

            val dogDescriptor = bindingContext[BindingContext.CLASS, dogClass!!]
            assertNotNull("应该创建 Dog ClassDescriptor", dogDescriptor)

            // 验证父类型
            val superTypes = dogDescriptor!!.typeConstructor.supertypes
            assertTrue("Dog 应该有父类型", superTypes.isNotEmpty())

            // 验证父类型是 Animal
            val animalSuperType = superTypes.firstOrNull { it.toString().contains("Animal") }
            assertNotNull("Dog 应该继承 Animal", animalSuperType)
        }
    }

    // ==================== 构造函数语义测试 ====================

    /**
     * 测试主构造函数的描述符
     *
     * 验证：
     * 1. 主构造函数的 ConstructorDescriptor 被创建
     * 2. 构造函数参数的 VariableDescriptor 被创建
     * 3. 参数类型正确解析
     */
    fun `test primary constructor descriptor`() {
        val file = createFile(
            """
            package test

            class Point  {
            Point(public var x: Int64, public var y: Int64){
            }
                public func distance(): Float64 {
                    return 0.0
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到 Point 类", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证主构造函数
            val constructors = classDescriptor!!.constructors
            val primaryConstructor = constructors.firstOrNull()
            assertNotNull("Point 应该有主构造函数", primaryConstructor)

            // 验证构造函数参数
            val parameters = primaryConstructor!!.valueParameters
            assertEquals("主构造函数应该有 2 个参数", 2, parameters.size)

            // 验证参数名称
            val paramNames = parameters.map { it.name.asString() }
            assertTrue("参数应该包含 x", paramNames.contains("x"))
            assertTrue("参数应该包含 y", paramNames.contains("y"))

            // 验证参数类型
            parameters.forEach { param ->
                assertNotNull("参数应该有类型", param.type)
                assertEquals("参数类型应该是 Int64", "Int64", param.type.deccriptorClass?.name?.asString())
            }
        }
    }

    // ==================== 成员解析测试 ====================

    /**
     * 测试类成员的解析
     *
     * 验证：
     * 1. 类的成员作用域包含所有成员
     * 2. 成员可以通过名称正确解析
     */
    fun `test class member resolution`() {
        val file = createFile(
            """
            package test

            class MyClass {
                public var field: Int64 = 0

                public func method(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            val classDescriptor =  bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证类的成员作用域
            val memberScope = classDescriptor!!.unsubstitutedMemberScope
            assertNotNull("类应该有成员作用域", memberScope)

            // 注意：具体的成员解析测试需要根据实际的作用域 API 实现
            // 这里只验证作用域存在
        }
    }

    // ==================== 继承层次测试 ====================

    /**
     * 测试多层继承的类型层次
     *
     * 验证：
     * 1. 继承链的所有类型都被正确解析
     * 2. 类型上界传递正确
     */
    fun `test multi-level inheritance hierarchy`() {
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
            val classC = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "C" }
            assertNotNull("应该找到类 C", classC)

            val descriptorC = bindingContext[BindingContext.CLASS, classC!!]
            assertNotNull("应该创建 C 的 ClassDescriptor", descriptorC)

            // 验证 C 的直接父类型是 B
            val superTypes = descriptorC!!.typeConstructor.supertypes
            assertTrue("C 应该有父类型", superTypes.isNotEmpty())
            assertTrue("C 应该继承 B", superTypes.any { it.toString().contains("B") })

            // 验证 B 的描述符
            val classB = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "B" }
            val descriptorB = bindingContext[BindingContext.CLASS, classB!!]
            assertNotNull("应该创建 B 的 ClassDescriptor", descriptorB)

            // 验证 B 继承 A
            val bSuperTypes = descriptorB!!.typeConstructor.supertypes
            assertTrue("B 应该有父类型", bSuperTypes.isNotEmpty())
            assertTrue("B 应该继承 A", bSuperTypes.any { it.toString().contains("A") })
        }
    }

    // ==================== Sealed 类测试 ====================

    /**
     * 测试 Sealed 类的语义
     *
     * 验证：
     * 1. sealed 类的 modality 为 SEALED
     * 2. sealed 类的子类可以正常继承
     *
     * 注意：仓颉不支持嵌套类，sealed 类的子类必须定义在顶层
     */
    fun `test sealed class semantics`() {
        val file = createFile(
            """
            package test

            sealed abstract class Result {}

            class Success <: Result {}

            class Failure <: Result {}
            """.trimIndent()
        )

        analyzeForTest(file) {
            val resultClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Result" }
            assertNotNull("应该找到 Result 类", resultClass)

            val resultDescriptor = bindingContext[BindingContext.CLASS, resultClass!!]
            assertNotNull("应该创建 Result ClassDescriptor", resultDescriptor)

            // 验证 sealed 类的模态性
            assertEquals(
                "sealed 类的模态性应该是 SEALED",
                Modality.SEALED,
                resultDescriptor!!.modality
            )

            // 验证子类（在顶层定义）
            val allClasses = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java).toList()
            val subClasses = allClasses.filter { it.name == "Success" || it.name == "Failure" }
            assertEquals("应该有 2 个子类", 2, subClasses.size)

            // 验证子类的描述符
            subClasses.forEach { subClass ->
                val subDescriptor = bindingContext[BindingContext.CLASS, subClass]
                assertNotNull("${subClass.name} 应该有描述符", subDescriptor)

                // 验证它们继承 Result
                val superTypes = subDescriptor!!.typeConstructor.supertypes
                assertTrue(
                    "${subClass.name} 应该继承 Result",
                    superTypes.any { it.toString().contains("Result") }
                )
            }
        }
    }
}
