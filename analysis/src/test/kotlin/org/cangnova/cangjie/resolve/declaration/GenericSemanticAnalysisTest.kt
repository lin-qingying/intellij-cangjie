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
 * 泛型语义分析测试
 *
 * 专注于测试泛型系统的语义分析，包括：
 * - 泛型类和接口的类型参数
 * - 类型参数的上界约束
 * - 泛型函数的类型参数
 * - 泛型类型实例化
 * - 型变（协变、逆变、不变）
 *
 * ## 测试策略
 *
 * 1. **类型参数**: 验证 TypeParameterDescriptor 的创建
 * 2. **类型约束**: 验证类型参数的上界约束
 * 3. **泛型实例化**: 验证泛型类型的具体化
 * 4. **型变**: 验证协变、逆变的语义
 * 5. **类型推断**: 验证泛型调用时的类型推断
 */
class GenericSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 泛型类测试 ====================

    /**
     * 测试简单泛型类
     *
     * 验证：
     * 1. 泛型类的 ClassDescriptor 创建
     * 2. 类型参数的 TypeParameterDescriptor 创建
     * 3. 类型参数在类体中可用
     */
    fun `test simple generic class`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                private var value: T

                public init(value: T) {
                    this.value = value
                }

                public func getValue(): T {
                    return value
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)
            assertEquals("Box", classDecl!!.name)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证类型参数
            val typeParameters = classDescriptor!!.declaredTypeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)

            val typeParam = typeParameters[0]
            assertEquals("T", typeParam.name.asString())

            // 验证类型参数的上界（默认应该是 Any?）
            val upperBounds = typeParam.upperBounds
            assertFalse("类型参数应该有上界", upperBounds.isEmpty())
        }
    }

    /**
     * 测试多个类型参数的泛型类
     *
     * 验证：
     * 1. 多个类型参数的解析
     * 2. 类型参数的顺序正确
     */
    fun `test generic class with multiple type parameters`() {
        val file = createFile(
            """
            package test

            class Pair<K, V> {
                public let first: K
                public let second: V

                public init(first: K, second: V) {
                    this.first = first
                    this.second = second
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
            assertNotNull("应该找到类声明", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 ClassDescriptor", classDescriptor)

            // 验证类型参数
            val typeParameters = classDescriptor!!.declaredTypeParameters
            assertEquals("应该有 2 个类型参数", 2, typeParameters.size)

            assertEquals("K", typeParameters[0].name.asString())
            assertEquals("V", typeParameters[1].name.asString())
        }
    }

    /**
     * 测试带上界约束的泛型类
     *
     * 验证：
     * 1. 类型参数的上界约束解析
     * 2. 上界类型正确
     */
    fun `test generic class with upper bound`() {
        val file = createFile(
            """
            package test

            class Animal {
                public func makeSound(): String {
                    return "Some sound"
                }
            }

            class Zoo<T > where T <: Animal{
                private var animals: Array<T>

                public init(animals: Array<T>) {
                    this.animals = animals
                }

                public func allMakeSounds() {
                    // T 保证有 makeSound 方法
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val zooClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "Zoo" }
            assertNotNull("应该找到 Zoo 类", zooClass)

            val zooDescriptor = bindingContext[BindingContext.CLASS, zooClass!!]
            assertNotNull("应该创建 Zoo ClassDescriptor", zooDescriptor)

            // 验证类型参数
            val typeParameters = zooDescriptor!!.declaredTypeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)

            val typeParam = typeParameters[0]
            assertEquals("T", typeParam.name.asString())

            // 验证上界约束
            val upperBounds = typeParam.upperBounds
            assertFalse("类型参数应该有上界", upperBounds.isEmpty())

            // 第一个上界应该是 Animal
            val firstBound = upperBounds.first()
            assertTrue("上界应该包含 Animal", firstBound.toString().contains("Animal"))
        }
    }

    /**
     * 测试多个上界约束
     *
     * 验证：
     * 1. 类型参数可以有多个上界
     * 2. 所有上界都被正确解析
     */
    fun `test generic class with multiple upper bounds`() {
        val file = createFile(
            """
            package test

            interface Comparable<T> {
                func compareTo(other: T): Int64
            }

            interface Serializable {
                func serialize(): String
            }

            class SortedList<T> where T <: Comparable<T>, T <: Serializable {
                private var items: Array<T>

                public init() {
                    items = Array<T>()
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val sortedListClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "SortedList" }
            assertNotNull("应该找到 SortedList 类", sortedListClass)

            val classDescriptor = bindingContext[BindingContext.CLASS, sortedListClass!!]
            assertNotNull("应该创建 SortedList ClassDescriptor", classDescriptor)

            // 验证类型参数
            val typeParameters = classDescriptor!!.declaredTypeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)

            val typeParam = typeParameters[0]
            assertEquals("T", typeParam.name.asString())

            // 验证多个上界
            val upperBounds = typeParam.upperBounds
            assertTrue("类型参数应该有多个上界", upperBounds.size >= 2)
        }
    }

    // ==================== 泛型接口测试 ====================

    /**
     * 测试泛型接口
     *
     * 验证：
     * 1. 泛型接口的 ClassDescriptor 创建
     * 2. 类型参数在接口方法中可用
     */
    fun `test generic interface`() {
        val file = createFile(
            """
            package test

            interface Container<T> {
                func add(item: T): Unit
                func get(index: Int64): T
                func size(): Int64
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val interfaceDecl = PsiTreeUtil.findChildOfType(file, CjInterface::class.java)
            assertNotNull("应该找到接口声明", interfaceDecl)

            val interfaceDescriptor = bindingContext[BindingContext.CLASS, interfaceDecl!!]
            assertNotNull("应该创建接口 ClassDescriptor", interfaceDescriptor)

            // 验证是接口类型
            assertEquals("应该是接口类型", ClassKind.INTERFACE, interfaceDescriptor!!.kind)

            // 验证类型参数
            val typeParameters = interfaceDescriptor.declaredTypeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)
            assertEquals("T", typeParameters[0].name.asString())
        }
    }

    /**
     * 测试类实现泛型接口
     *
     * 验证：
     * 1. 泛型接口的实例化
     * 2. 类型参数的具体化
     */
    fun `test class implementing generic interface`() {
        val file = createFile(
            """
            package test

            interface Comparable<T> {
                func compareTo(other: T): Int64
            }

            class IntComparable <: Comparable<Int64> {
                private let value: Int64

                public init(value: Int64) {
                    this.value = value
                }

                public func compareTo(other: Int64): Int64 {
                    return value - other
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val classDecl = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "IntComparable" }
            assertNotNull("应该找到 IntComparable 类", classDecl)

            val classDescriptor = bindingContext[BindingContext.CLASS, classDecl!!]
            assertNotNull("应该创建 IntComparable ClassDescriptor", classDescriptor)

            // 验证实现了 Comparable 接口
            val superTypes = classDescriptor!!.typeConstructor.supertypes
            assertTrue("应该实现 Comparable 接口",
                superTypes.any { it.toString().contains("Comparable") })

            // 验证泛型参数被具体化为 Int64
            val comparableType = superTypes.first { it.toString().contains("Comparable") }
            assertTrue("Comparable 应该被实例化为 Int64",
                comparableType.toString().contains("Int64"))
        }
    }

    // ==================== 泛型函数测试 ====================

    /**
     * 测试泛型函数
     *
     * 验证：
     * 1. 函数的类型参数
     * 2. 类型参数在参数和返回类型中使用
     */
    fun `test generic function`() {
        val file = createFile(
            """
            package test

            func swap<T>(a: T, b: T): (T, T) {
                return (b, a)
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildOfType(file, CjFunction::class.java)
            assertNotNull("应该找到函数声明", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 FunctionDescriptor", functionDescriptor)

            // 验证类型参数
            val typeParameters = functionDescriptor!!.typeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)
            assertEquals("T", typeParameters[0].name.asString())

            // 验证参数使用类型参数
            val valueParameters = functionDescriptor.valueParameters
            assertEquals("应该有 2 个参数", 2, valueParameters.size)
            assertNotNull("参数应该有类型", valueParameters[0].type)
            assertNotNull("参数应该有类型", valueParameters[1].type)

            // 验证返回类型（元组类型）
            val returnType = functionDescriptor.returnType
            assertNotNull("应该有返回类型", returnType)
        }
    }

    /**
     * 测试带约束的泛型函数
     *
     * 验证：
     * 1. 函数类型参数的上界约束
     * 2. 约束类型正确解析
     */
    fun `test generic function with constraint`() {
        val file = createFile(
            """
            package test

            interface Comparable<T> {
                func compareTo(other: T): Int64
            }

            func findMax<T>(items: Array<T>): T where T <: Comparable<T> {
                var max = items[0]
                for (item in items) {
                    if (item.compareTo(max) > 0) {
                        max = item
                    }
                }
                return max
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val functionDecl = PsiTreeUtil.findChildrenOfType(file, CjFunction::class.java)
                .firstOrNull { it.name == "findMax" }
            assertNotNull("应该找到 findMax 函数", functionDecl)

            val functionDescriptor = bindingContext[BindingContext.FUNCTION, functionDecl!!]
            assertNotNull("应该创建 findMax FunctionDescriptor", functionDescriptor)

            // 验证类型参数
            val typeParameters = functionDescriptor!!.typeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)

            val typeParam = typeParameters[0]
            assertEquals("T", typeParam.name.asString())

            // 验证上界约束
            val upperBounds = typeParam.upperBounds
            assertFalse("类型参数应该有上界", upperBounds.isEmpty())
            assertTrue("上界应该包含 Comparable",
                upperBounds.any { it.toString().contains("Comparable") })
        }
    }

    // ==================== 泛型继承测试 ====================

    /**
     * 测试泛型类继承
     *
     * 验证：
     * 1. 子类继承泛型父类
     * 2. 类型参数的传递
     */
    fun `test generic class inheritance`() {
        val file = createFile(
            """
            package test

            open class Container<T> {
                protected var items: Array<T>

                public init() {
                    items = Array<T>()
                }

                public func add(item: T) {
                    // add implementation
                }
            }

            class StringContainer <: Container<String> {
                public func addString(str: String) {
                    add(str)
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val stringContainerClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "StringContainer" }
            assertNotNull("应该找到 StringContainer 类", stringContainerClass)

            val classDescriptor = bindingContext[BindingContext.CLASS, stringContainerClass!!]
            assertNotNull("应该创建 StringContainer ClassDescriptor", classDescriptor)

            // 验证继承了 Container
            val superTypes = classDescriptor!!.typeConstructor.supertypes
            assertTrue("应该继承 Container",
                superTypes.any { it.toString().contains("Container") })

            // 验证泛型参数被实例化为 String
            val containerType = superTypes.first { it.toString().contains("Container") }
            assertTrue("Container 应该被实例化为 String",
                containerType.toString().contains("String"))
        }
    }

    /**
     * 测试泛型类型参数传递
     *
     * 验证：
     * 1. 子类的类型参数传递给父类
     * 2. 类型参数的正确映射
     */
    fun `test generic type parameter forwarding`() {
        val file = createFile(
            """
            package test

            open class Box<T> {
                protected var value: T?

                public init() {
                    value = null
                }

                public func set(newValue: T) {
                    value = newValue
                }
            }

            class GenericBox<E> <: Box<E> {
                public func get(): E? {
                    return value
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val genericBoxClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "GenericBox" }
            assertNotNull("应该找到 GenericBox 类", genericBoxClass)

            val classDescriptor = bindingContext[BindingContext.CLASS, genericBoxClass!!]
            assertNotNull("应该创建 GenericBox ClassDescriptor", classDescriptor)

            // 验证自己的类型参数
            val ownTypeParams = classDescriptor!!.declaredTypeParameters
            assertEquals("应该有 1 个类型参数", 1, ownTypeParams.size)
            assertEquals("E", ownTypeParams[0].name.asString())

            // 验证继承了 Box
            val superTypes = classDescriptor.typeConstructor.supertypes
            assertTrue("应该继承 Box",
                superTypes.any { it.toString().contains("Box") })
        }
    }

    // ==================== 型变测试 ====================



    // ==================== 泛型嵌套测试 ====================

    /**
     * 测试嵌套泛型类型
     *
     * 验证：
     * 1. 泛型类型作为另一个泛型的类型参数
     * 2. 嵌套类型的正确解析
     */
    fun `test nested generic types`() {
        val file = createFile(
            """
            package test

            class Box<T> {
                private var value: T

                public init(value: T) {
                    this.value = value
                }
            }

            class BoxOfBoxes<T> {
                private var boxes: Array<Box<T>>

                public init() {
                    boxes = Array<Box<T>>()
                }

                public func addBox(box: Box<T>) {
                    // add implementation
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val boxOfBoxesClass = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
                .firstOrNull { it.name == "BoxOfBoxes" }
            assertNotNull("应该找到 BoxOfBoxes 类", boxOfBoxesClass)

            val classDescriptor = bindingContext[BindingContext.CLASS, boxOfBoxesClass!!]
            assertNotNull("应该创建 BoxOfBoxes ClassDescriptor", classDescriptor)

            // 验证类型参数
            val typeParameters = classDescriptor!!.declaredTypeParameters
            assertEquals("应该有 1 个类型参数", 1, typeParameters.size)
            assertEquals("T", typeParameters[0].name.asString())

            // 验证字段的嵌套泛型类型
            val fields = PsiTreeUtil.findChildrenOfType(boxOfBoxesClass, CjFieldVariable::class.java)
            val boxesField = fields.firstOrNull { it.name == "boxes" }
            assertNotNull("应该找到 boxes 字段", boxesField)

            val fieldDescriptor = bindingContext[BindingContext.VARIABLE, boxesField!!]
            assertNotNull("boxes 应该有描述符", fieldDescriptor)
            assertNotNull("boxes 应该有类型", fieldDescriptor!!.type)
        }
    }
}
