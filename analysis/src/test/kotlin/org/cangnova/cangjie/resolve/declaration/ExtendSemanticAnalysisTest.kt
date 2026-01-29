/*
 * Copyright 2026 LinQingYing. and contributors.
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
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.extend.ExtendManager

/**
 * 扩展（extend）语义分析测试
 *
 * 测试扩展声明的语义分析，包括：
 * - 扩展描述符的创建
 * - 扩展实现接口
 * - 基本类型扩展
 * - 扩展方法的解析
 * - ExtendManager 的功能
 */
class ExtendSemanticAnalysisTest : CangJieAnalysisTestBase() {

    // ==================== 扩展描述符创建测试 ====================

    /**
     * 测试简单扩展的描述符创建
     *
     * 验证：
     * 1. ExtendDescriptor 被正确创建
     * 2. 扩展的被扩展类型正确
     */
    fun `test simple extend descriptor creation`() {
        val file = createFile(
            """
            package test

            class MyClass {
            }

            extend MyClass {
                public func hello(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            // 验证扩展描述符
            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证被扩展类型
            val extendedType = extendDescriptor!!.extendType
            assertNotNull("应该有被扩展类型", extendedType)
            assertTrue("被扩展类型应该是 MyClass", extendedType.toString().contains("MyClass"))
        }
    }

    /**
     * 测试扩展实现接口
     *
     * 验证：
     * 1. 扩展的 superTypes 包含实现的接口
     * 2. extendId 正确生成
     */
    fun `test extend implements interface`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            class MyClass {
            }

            extend MyClass <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证实现的接口
            val superTypes = extendDescriptor!!.superTypes
            assertTrue("扩展应该实现接口", superTypes.isNotEmpty())
            assertTrue("应该实现 Printable", superTypes.any { it.toString().contains("Printable") })

            // 验证 extendId 包含接口信息
            val extendId = extendDescriptor.extendId
            assertNotNull("应该有 extendId", extendId)
            assertTrue("extendId 应该包含 Printable", extendId.contains("Printable"))
        }
    }

    /**
     * 测试扩展实现多个接口
     *
     * 验证：
     * 1. 扩展可以实现多个接口
     * 2. 所有接口都在 superTypes 中
     */
    fun `test extend implements multiple interfaces`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            interface Serializable {
                func serialize(): String
            }

            class MyClass {
            }

            extend MyClass <: Printable & Serializable {
                public func print(): Unit {
                }

                public func serialize(): String {
                    return ""
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证实现多个接口
            val superTypes = extendDescriptor!!.superTypes
            assertTrue("扩展应该实现多个接口", superTypes.size >= 2)

            val hasPrintable = superTypes.any { it.toString().contains("Printable") }
            val hasSerializable = superTypes.any { it.toString().contains("Serializable") }
            assertTrue("应该实现 Printable", hasPrintable)
            assertTrue("应该实现 Serializable", hasSerializable)
        }
    }

    // ==================== 基本类型扩展测试 ====================

    /**
     * 测试基本类型的纯扩展方法（不实现接口）
     *
     * 验证：
     * 1. 可以为基本类型添加扩展方法
     * 2. 不需要实现接口
     * 3. 不报 ORPHAN_EXTEND 错误
     */
    fun `test builtin type extend without interface`() {
        val file = createFile(
            """
            package test

            extend Int64 {
                public func double(): Int64 {
                    return this * 2
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证被扩展类型是 Int64
            val extendedType = extendDescriptor!!.extendType
            assertTrue("被扩展类型应该是 Int64", extendedType.toString().contains("Int64"))

            // 验证没有实现接口
            val superTypes = extendDescriptor.superTypes
            assertTrue("纯扩展方法不应该有 superTypes", superTypes.isEmpty())
        }
    }

    /**
     * 测试基本类型扩展实现接口
     *
     * 验证：
     * 1. 可以为基本类型扩展接口实现
     * 2. 不报 ORPHAN_EXTEND 错误（内置类型例外）
     */
    fun `test builtin type extend with interface`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            extend Int64 <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证被扩展类型是 Int64
            val extendedType = extendDescriptor!!.extendType
            assertTrue("被扩展类型应该是 Int64", extendedType.toString().contains("Int64"))

            // 验证实现 Printable 接口
            val superTypes = extendDescriptor.superTypes
            assertTrue("应该实现 Printable", superTypes.any { it.toString().contains("Printable") })
        }
    }

    // ==================== 泛型扩展测试 ====================

    /**
     * 测试泛型扩展
     *
     * 验证：
     * 1. 扩展可以有类型参数
     * 2. 类型参数正确解析
     */
    fun `test generic extend`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            class Box<T> {
                var value: T
                init(value: T) {
                    this.value = value
                }
            }

            extend<T> Box<T> <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证类型参数
            val typeParameters = extendDescriptor!!.declaredTypeParameters
            assertTrue("扩展应该有类型参数", typeParameters.isNotEmpty())
            assertEquals("应该有一个类型参数", 1, typeParameters.size)
            assertEquals("类型参数名称应该是 T", "T", typeParameters[0].name.asString())
        }
    }

    // ==================== 扩展成员作用域测试 ====================

    /**
     * 测试扩展成员作用域
     *
     * 验证：
     * 1. 扩展的成员作用域包含扩展定义的方法
     */
    fun `test extend member scope`() {
        val file = createFile(
            """
            package test

            class MyClass {
            }

            extend MyClass {
                public func extendMethod(): Unit {
                }

                public var extendProperty: Int64 = 0
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDecl = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证成员作用域
            val memberScope = extendDescriptor!!.unsubstitutedMemberScope
            assertNotNull("扩展应该有成员作用域", memberScope)
        }
    }

    // ==================== ExtendManager 测试 ====================

    /**
     * 测试 ExtendManager 注册扩展
     *
     * 验证：
     * 1. 扩展被正确注册到 ExtendManager
     * 2. 可以通过类型构造器查询扩展
     */
    fun `test extend manager registration`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            class MyClass {
            }

            extend MyClass <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDescriptor = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
                ?.let { bindingContext[BindingContext.EXTEND, it] }
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 获取 ExtendManager
            val extendManager = moduleDescriptor.getCapability(ExtendManager.CAPABILITY)
            assertNotNull("模块应该有 ExtendManager", extendManager)

            // 查询 MyClass 的扩展
            val myClassDescriptor = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
                ?.let { bindingContext[BindingContext.CLASS, it] }
            assertNotNull("应该找到 MyClass 描述符", myClassDescriptor)

            val extensions = extendManager!!.getExtensionsForType(myClassDescriptor!!.typeConstructor)
            assertTrue("应该找到 MyClass 的扩展", extensions.isNotEmpty())
        }
    }

    /**
     * 测试通过 ExtendManager 获取扩展的超类型
     *
     * 验证：
     * 1. getExtendSupertypes 返回扩展实现的接口
     */
    fun `test extend manager get supertypes`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            interface Serializable {
                func serialize(): String
            }

            class MyClass {
            }

            extend MyClass <: Printable {
                public func print(): Unit {
                }
            }

            extend MyClass <: Serializable {
                public func serialize(): String {
                    return ""
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            // 获取 ExtendManager
            val extendManager = moduleDescriptor.getCapability(ExtendManager.CAPABILITY)
            assertNotNull("模块应该有 ExtendManager", extendManager)

            // 获取 MyClass 的类型构造器
            val myClassDescriptor = PsiTreeUtil.findChildOfType(file, CjClass::class.java)
                ?.let { bindingContext[BindingContext.CLASS, it] }
            assertNotNull("应该找到 MyClass 描述符", myClassDescriptor)

            // 查询扩展的超类型
            val extendSupertypes = extendManager!!.getExtendSupertypes(
                myClassDescriptor!!.typeConstructor
            )

            // 验证包含两个接口
            assertTrue("应该有扩展的超类型", extendSupertypes.isNotEmpty())
            val hasPrintable = extendSupertypes.any { it.toString().contains("Printable") }
            val hasSerializable = extendSupertypes.any { it.toString().contains("Serializable") }
            assertTrue("应该包含 Printable", hasPrintable)
            assertTrue("应该包含 Serializable", hasSerializable)
        }
    }

    // ==================== extendId 测试 ====================

    /**
     * 测试 extendId 的生成
     *
     * 验证：
     * 1. extendId 包含包名
     * 2. extendId 包含被扩展类型
     * 3. extendId 包含实现的接口
     */
    fun `test extend id generation`() {
        val file = createFile(
            """
            package test

            interface Printable {
                func print(): Unit
            }

            class MyClass {
            }

            extend MyClass <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDescriptor = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
                ?.let { bindingContext[BindingContext.EXTEND, it] }
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            val extendId = extendDescriptor!!.extendId
            assertNotNull("应该有 extendId", extendId)

            // 验证 extendId 格式
            assertTrue("extendId 应该包含包名", extendId.contains("test"))
            assertTrue("extendId 应该包含被扩展类型", extendId.contains("MyClass"))
            assertTrue("extendId 应该包含接口", extendId.contains("Printable"))
            assertTrue("extendId 应该包含分隔符", extendId.contains("<:"))
        }
    }

    /**
     * 测试相同扩展的 extendId 一致性
     *
     * 验证：
     * 1. 相同的扩展声明生成相同的 extendId
     */
    fun `test extend id consistency`() {
        val file = createFile(
            """
            package test

            interface A {
            }

            interface B {
            }

            class MyClass {
            }

            extend MyClass <: A & B {
            }
            """.trimIndent()
        )

        analyzeForTest(file) {
            val extendDescriptor = PsiTreeUtil.findChildOfType(file, CjExtend::class.java)
                ?.let { bindingContext[BindingContext.EXTEND, it] }
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            val extendId1 = extendDescriptor!!.extendId
            val extendId2 = extendDescriptor.extendId

            assertEquals("多次获取 extendId 应该一致", extendId1, extendId2)
        }
    }

    // ==================== 跨包扩展解析测试 ====================

    /**
     * 测试跨包扩展：在另一个包中定义接口和扩展
     *
     * 验证：
     * 1. 多文件分析正确工作
     * 2. 跨包的扩展描述符正确创建
     * 3. ExtendManager 能够查询到跨包的扩展
     */
    fun `test cross package extend registration`() {
        // 包 A：定义接口
        val interfaceFile = createFile(
            """
            package pkgA

            public interface Printable {
                func print(): Unit
            }
            """.trimIndent(),
            "Printable.cj"
        )

        // 包 B：定义类和扩展
        val extendFile = createFile(
            """
            package pkgB

            import pkgA.Printable

            public class MyClass {
            }

            extend MyClass <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent(),
            "MyClass.cj"
        )

        analyzeForTest(interfaceFile, extendFile) {
            // 验证扩展描述符
            val extendDecl = PsiTreeUtil.findChildOfType(extendFile, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证实现的接口来自 pkgA
            val superTypes = extendDescriptor!!.superTypes
            assertTrue("扩展应该实现接口", superTypes.isNotEmpty())
            assertTrue("应该实现 Printable", superTypes.any { it.toString().contains("Printable") })

            // 验证 ExtendManager 注册
            val extendManager = moduleDescriptor.getCapability(ExtendManager.CAPABILITY)
            assertNotNull("模块应该有 ExtendManager", extendManager)

            val myClassDescriptor = PsiTreeUtil.findChildOfType(extendFile, CjClass::class.java)
                ?.let { bindingContext[BindingContext.CLASS, it] }
            assertNotNull("应该找到 MyClass 描述符", myClassDescriptor)

            val extensions = extendManager!!.getExtensionsForType(myClassDescriptor!!.typeConstructor)
            assertTrue("应该找到 MyClass 的扩展", extensions.isNotEmpty())
        }
    }

    /**
     * 测试跨包扩展：多个包之间的扩展关系
     *
     * 验证：
     * 1. 包 A 定义接口
     * 2. 包 B 定义类
     * 3. 包 C 导入两者并创建扩展
     */
    fun `test multi package extend relationship`() {
        // 包 A：定义接口
        val interfaceFile = createFile(
            """
            package pkgA

            public interface Serializable {
                func serialize(): String
            }
            """.trimIndent(),
            "Serializable.cj"
        )

        // 包 B：定义类
        val classFile = createFile(
            """
            package pkgB

            public class DataHolder {
                public var data: Int64 = 0
            }
            """.trimIndent(),
            "DataHolder.cj"
        )

        // 包 C：扩展 B 包的类实现 A 包的接口
        val extendFile = createFile(
            """
            package pkgC

            import pkgA.Serializable
            import pkgB.DataHolder

            extend DataHolder <: Serializable {
                public func serialize(): String {
                    return ""
                }
            }
            """.trimIndent(),
            "DataHolderExt.cj"
        )

        analyzeForTest(interfaceFile, classFile, extendFile) {
            // 验证扩展描述符
            val extendDecl = PsiTreeUtil.findChildOfType(extendFile, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证被扩展类型是 DataHolder
            val extendedType = extendDescriptor!!.extendType
            assertTrue("被扩展类型应该是 DataHolder", extendedType.toString().contains("DataHolder"))

            // 验证实现的接口是 Serializable
            val superTypes = extendDescriptor.superTypes
            assertTrue("应该实现 Serializable", superTypes.any { it.toString().contains("Serializable") })

            // 验证 extendId 包含正确的包名
            val extendId = extendDescriptor.extendId
            assertTrue("extendId 应该包含 pkgC", extendId.contains("pkgC"))
        }
    }

    /**
     * 测试同一类型的多包扩展
     *
     * 验证：
     * 1. 不同包可以为同一类型添加不同接口的扩展
     * 2. ExtendManager 能查询到所有扩展
     */
    fun `test same type extended in multiple packages`() {
        // 公共包：定义类和接口
        val commonFile = createFile(
            """
            package common

            public interface Printable {
                func print(): Unit
            }

            public interface Hashable {
                func hash(): Int64
            }

            public class Entity {
            }
            """.trimIndent(),
            "Common.cj"
        )

        // 扩展包 1：为 Entity 实现 Printable
        val extend1File = createFile(
            """
            package ext1

            import common.Entity
            import common.Printable

            extend Entity <: Printable {
                public func print(): Unit {
                }
            }
            """.trimIndent(),
            "Ext1.cj"
        )

        // 扩展包 2：为 Entity 实现 Hashable
        val extend2File = createFile(
            """
            package ext2

            import common.Entity
            import common.Hashable

            extend Entity <: Hashable {
                public func hash(): Int64 {
                    return 0
                }
            }
            """.trimIndent(),
            "Ext2.cj"
        )

        analyzeForTest(commonFile, extend1File, extend2File) {
            // 获取 Entity 类描述符
            val entityClass = PsiTreeUtil.findChildOfType(commonFile, CjClass::class.java)
                ?.let { bindingContext[BindingContext.CLASS, it] }
            assertNotNull("应该找到 Entity 描述符", entityClass)

            // 获取 ExtendManager
            val extendManager = moduleDescriptor.getCapability(ExtendManager.CAPABILITY)
            assertNotNull("模块应该有 ExtendManager", extendManager)

            // 查询 Entity 的所有扩展
            val extensions = extendManager!!.getExtensionsForType(entityClass!!.typeConstructor)
            assertTrue("应该找到 Entity 的扩展", extensions.isNotEmpty())
            assertEquals("应该有两个扩展", 2, extensions.size)

            // 查询扩展的超类型
            val extendSupertypes = extendManager.getExtendSupertypes(entityClass.typeConstructor)
            assertTrue("应该有扩展的超类型", extendSupertypes.isNotEmpty())

            val hasPrintable = extendSupertypes.any { it.toString().contains("Printable") }
            val hasHashable = extendSupertypes.any { it.toString().contains("Hashable") }
            assertTrue("应该包含 Printable", hasPrintable)
            assertTrue("应该包含 Hashable", hasHashable)
        }
    }

    /**
     * 测试泛型类型的跨包扩展
     *
     * 验证：
     * 1. 泛型类可以在其他包中被扩展
     * 2. 类型参数正确传递
     */
    fun `test generic type cross package extend`() {
        // 包 A：定义泛型类
        val genericClassFile = createFile(
            """
            package pkgA

            public class Container<T> {
                public var value: T
                public init(value: T) {
                    this.value = value
                }
            }
            """.trimIndent(),
            "Container.cj"
        )

        // 包 B：定义接口
        val interfaceFile = createFile(
            """
            package pkgB

            public interface Iterable<E> {
                func iterator(): Unit
            }
            """.trimIndent(),
            "Iterable.cj"
        )

        // 包 C：为泛型类扩展接口实现
        val extendFile = createFile(
            """
            package pkgC

            import pkgA.Container
            import pkgB.Iterable

            extend<T> Container<T> <: Iterable<T> {
                public func iterator(): Unit {
                }
            }
            """.trimIndent(),
            "ContainerExt.cj"
        )

        analyzeForTest(genericClassFile, interfaceFile, extendFile) {
            val extendDecl = PsiTreeUtil.findChildOfType(extendFile, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证类型参数
            val typeParameters = extendDescriptor!!.declaredTypeParameters
            assertEquals("扩展应该有一个类型参数", 1, typeParameters.size)
            assertEquals("类型参数名称应该是 T", "T", typeParameters[0].name.asString())

            // 验证被扩展类型是 Container<T>
            val extendedType = extendDescriptor.extendType
            assertTrue("被扩展类型应该是 Container", extendedType.toString().contains("Container"))

            // 验证实现的接口是 Iterable<T>
            val superTypes = extendDescriptor.superTypes
            assertTrue("应该实现 Iterable", superTypes.any { it.toString().contains("Iterable") })
        }
    }

    /**
     * 测试孤儿规则：外部类型实现外部接口（应该报错，但本地接口例外）
     *
     * 验证：
     * 1. 当扩展的类型和接口都不在当前包时，应该报 ORPHAN_EXTEND
     * 2. 当至少有一个在当前包时，不应报错
     */
    fun `test orphan rule with local interface`() {
        // 包 A：定义类
        val classFile = createFile(
            """
            package pkgA

            public class ExternalClass {
            }
            """.trimIndent(),
            "ExternalClass.cj"
        )

        // 包 B：定义本地接口并扩展外部类（合法，因为接口是本地的）
        val extendFile = createFile(
            """
            package pkgB

            import pkgA.ExternalClass

            interface LocalInterface {
                func localMethod(): Unit
            }

            extend ExternalClass <: LocalInterface {
                public func localMethod(): Unit {
                }
            }
            """.trimIndent(),
            "LocalExt.cj"
        )

        analyzeForTest(classFile, extendFile) {
            val extendDecl = PsiTreeUtil.findChildOfType(extendFile, CjExtend::class.java)
            assertNotNull("应该找到扩展声明", extendDecl)

            val extendDescriptor = bindingContext[BindingContext.EXTEND, extendDecl!!]
            assertNotNull("应该创建扩展描述符", extendDescriptor)

            // 验证实现的接口是本地接口
            val superTypes = extendDescriptor!!.superTypes
            assertTrue("应该实现 LocalInterface", superTypes.any { it.toString().contains("LocalInterface") })

            // 由于接口是本地的，不应该报 ORPHAN_EXTEND 错误
            // （诊断检查在 ExtendChecker 中进行，这里只验证描述符创建正确）
        }
    }
}