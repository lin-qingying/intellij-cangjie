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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorVisitor
import org.cangnova.cangjie.descriptors.annotations.AnnotatedImpl
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.renderer.DescriptorRenderer

/**
 * 声明描述符抽象基类 - 为所有声明描述符提供通用的基础功能
 *
 * DeclarationDescriptorImpl 是所有声明描述符的根基类，为类、函数、变量、包等各种语言元素的描述符
 * 提供统一的基础设施。它实现了名称存储、注解管理、调试信息渲染和访问者模式支持。
 *
 * ## 核心功能
 *
 * ### 名称和注解管理
 *
 * 存储声明的基本信息：
 * - [name]: 声明的名称（类名、函数名、变量名等）
 * - [annotations]: 声明的注解列表（通过 AnnotatedImpl 继承）
 *
 * ### Original 引用
 *
 * [original] 属性默认返回自身：
 * - 用于跟踪类型替换前的原始描述符
 * - 例如：`List<Int>` 的 original 指向 `List<T>`
 * - 子类可以覆盖以提供不同的行为
 *
 * ### 调试字符串渲染
 *
 * 提供健壮的 [toString] 实现：
 * - 优先使用 [DescriptorRenderer.DEBUG_TEXT] 渲染完整信息
 * - 包含内存地址便于跟踪对象身份
 * - 渲染失败时回退到简单格式（类名 + 名称）
 * - 避免在描述符未完全初始化时抛出异常
 *
 * ### 访问者模式支持
 *
 * 实现 [DeclarationDescriptor.accept] 方法：
 * - 支持带返回值的访问者
 * - 提供 [acceptVoid] 方便无返回值场景
 *
 * ## 使用场景
 *
 * ### 1. 定义类描述符
 *
 * ```kotlin
 * class LazyClassDescriptor(
 *     annotations: Annotations,
 *     name: Name,
 *     private val module: ModuleDescriptor
 * ) : DeclarationDescriptorImpl(annotations, name), ClassDescriptor {
 *     // 类的 original 可能指向泛型参数替换前的版本
 *     private val _original: ClassDescriptor? = null
 *
 *     override val original: ClassDescriptor
 *         get() = _original ?: this
 *
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitClassDescriptor(this, data)
 *     }
 *
 *     // 实现其他 ClassDescriptor 方法...
 * }
 * ```
 *
 * ### 2. 定义函数描述符
 *
 * ```kotlin
 * class SimpleFunctionDescriptorImpl(
 *     annotations: Annotations,
 *     name: Name,
 *     private val containingDeclaration: DeclarationDescriptor
 * ) : DeclarationDescriptorImpl(annotations, name), FunctionDescriptor {
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitFunctionDescriptor(this, data)
 *     }
 *
 *     // 函数签名、返回类型等...
 * }
 * ```
 *
 * ### 3. 调试和日志记录
 *
 * toString() 方法在调试时非常有用：
 *
 * ```kotlin
 * val classDescriptor: ClassDescriptor = ...
 * println(classDescriptor)
 * // 输出: class MyClass defined in package com.example [LazyClassDescriptor@1a2b3c4d]
 *
 * // 即使描述符未完全初始化，也能安全打印
 * val partiallyInitialized = LazyClassDescriptor(...)
 * println(partiallyInitialized)
 * // 输出: LazyClassDescriptor MyClass（回退格式）
 * ```
 *
 * ### 4. 访问者模式遍历
 *
 * 使用访问者遍历描述符树：
 *
 * ```kotlin
 * val visitor = object : DeclarationDescriptorVisitor<Unit, Int> {
 *     override fun visitClassDescriptor(descriptor: ClassDescriptor, depth: Int): Unit {
 *         println("${"  ".repeat(depth)}Class: ${descriptor.name}")
 *         descriptor.unsubstitutedMemberScope.getContributedDescriptors().forEach {
 *             it.accept(this, depth + 1)
 *         }
 *     }
 *
 *     override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, depth: Int): Unit {
 *         println("${"  ".repeat(depth)}Function: ${descriptor.name}")
 *     }
 *
 *     // 其他 visit 方法...
 * }
 *
 * moduleDescriptor.getPackage(fqName).memberScope.getContributedDescriptors()
 *     .forEach { it.accept(visitor, 0) }
 * ```
 *
 * ## 实现细节
 *
 * ### 继承层次
 *
 * ```
 * AnnotatedImpl (提供注解支持)
 *   ↑
 * DeclarationDescriptorImpl (提供名称、original、调试支持)
 *   ↑
 * DeclarationDescriptorNonRootImpl (添加 containingDeclaration 和 source)
 *   ↑
 * 具体的描述符类 (ClassDescriptor, FunctionDescriptor, 等)
 * ```
 *
 * ### toString 实现策略
 *
 * 使用两级回退策略确保健壮性：
 *
 * ```kotlin
 * override fun toString(): String {
 *     return try {
 *         // 第一级：尝试使用完整渲染器
 *         DescriptorRenderer.DEBUG_TEXT.render(descriptor) +
 *             "[" + descriptor.javaClass.simpleName + "@" + Integer.toHexString(
 *                 System.identityHashCode(descriptor)
 *             ) + "]"
 *     } catch (e: Throwable) {
 *         // 第二级：回退到简单格式
 *         descriptor.javaClass.simpleName + " " + descriptor.name
 *     }
 * }
 * ```
 *
 * **为什么需要 try-catch？**
 *
 * - DescriptorRenderer 可能访问未初始化的属性
 * - 在调试时，能看到部分信息比抛出异常更有用
 * - 例如：lazy 属性在循环依赖时可能触发异常
 *
 * ### original 的默认实现
 *
 * ```kotlin
 * override val original: DeclarationDescriptor
 *     get() = this
 * ```
 *
 * - 默认情况下，描述符的 original 就是自身
 * - 泛型实例化、类型替换等场景下，子类会覆盖此属性
 * - 例如：`List<Int>` 的 original 指向 `List<T>`
 *
 * ### acceptVoid 适配方法
 *
 * ```kotlin
 * override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
 *     accept(visitor, null)
 * }
 * ```
 *
 * - 便于不需要返回值和数据的访问者
 * - 自动处理 null 参数传递
 *
 * ## 与其他基类的对比
 *
 * | 基类 | DeclarationDescriptorImpl | DeclarationDescriptorNonRootImpl | AnnotatedImpl |
 * |------|---------------------------|----------------------------------|---------------|
 * | 提供功能 | 名称、original、调试 | + 包含声明、源码位置 | 仅注解 |
 * | 使用场景 | 最基础的描述符 | 有父级的描述符 | 任何需要注解的元素 |
 * | 典型子类 | 模块、包 | 类、函数、属性 | 类型参数、参数 |
 *
 * ## 性能特性
 *
 * ### 内存占用
 *
 * - **基础开销**: 16 字节（对象头）
 * - **annotations**: 引用（8 字节）
 * - **name**: 引用（8 字节）
 * - **总计**: 约 32 字节 + annotations 和 name 的实际大小
 *
 * ### toString 性能
 *
 * - **成功渲染**: 取决于 DescriptorRenderer 实现（可能较慢）
 * - **回退渲染**: O(1)，仅字符串拼接
 * - **建议**: 仅在调试时使用，避免在生产代码中频繁调用
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 简单描述符定义
 *
 * ```kotlin
 * class SimplePropertyDescriptor(
 *     annotations: Annotations,
 *     name: Name,
 *     override val containingDeclaration: DeclarationDescriptor
 * ) : DeclarationDescriptorImpl(annotations, name), PropertyDescriptor {
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitPropertyDescriptor(this, data)
 *     }
 *
 *     // 实现 PropertyDescriptor 的其他方法
 * }
 * ```
 *
 * ### 模式 2: 带 original 跟踪的描述符
 *
 * ```kotlin
 * class SubstitutedClassDescriptor(
 *     private val original: ClassDescriptor,
 *     private val substitutor: DefaultTypeSubstitutor,
 *     annotations: Annotations,
 *     name: Name
 * ) : DeclarationDescriptorImpl(annotations, name), ClassDescriptor {
 *     override val original: ClassDescriptor
 *         get() = this.original  // 返回替换前的类描述符
 *
 *     // 其他成员应用类型替换
 * }
 * ```
 *
 * ### 模式 3: 自定义调试输出
 *
 * ```kotlin
 * class MyCustomDescriptor(...) : DeclarationDescriptorImpl(...) {
 *     override fun toString(): String {
 *         // 可以覆盖以提供更详细的调试信息
 *         return "MyCustomDescriptor(name=$name, state=$state)"
 *     }
 * }
 * ```
 *
 * ## 常见陷阱
 *
 * ### 1. 在 toString 中访问未初始化属性
 *
 * ```kotlin
 * // 危险示例：可能导致 StackOverflowError
 * class BadDescriptor(...) : DeclarationDescriptorImpl(...) {
 *     val someLazyProperty by lazy {
 *         // 如果这里调用了 toString，可能触发无限递归
 *         println(this.toString())
 *         computeValue()
 *     }
 * }
 *
 * // 安全做法：避免在 lazy 初始化中调用 toString
 * ```
 *
 * ### 2. 忘记实现 accept 方法
 *
 * ```kotlin
 * // 错误示例：没有实现 accept
 * abstract class IncompleteDescriptor(...) : DeclarationDescriptorImpl(...) {
 *     // 缺少 accept 方法实现
 * }
 *
 * // 正确做法：始终实现 accept
 * class CompleteDescriptor(...) : DeclarationDescriptorImpl(...), SomeDescriptor {
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitSomeDescriptor(this, data)
 *     }
 * }
 * ```
 *
 * ### 3. 错误地覆盖 original
 *
 * ```kotlin
 * // 危险示例：可能导致无限递归
 * class BadOriginalDescriptor(...) : DeclarationDescriptorImpl(...) {
 *     override val original: DeclarationDescriptor
 *         get() = original.original  // 错误！
 * }
 *
 * // 正确做法
 * class GoodOriginalDescriptor(
 *     private val actualOriginal: DeclarationDescriptor?,
 *     ...
 * ) : DeclarationDescriptorImpl(...) {
 *     override val original: DeclarationDescriptor
 *         get() = actualOriginal ?: this
 * }
 * ```
 *
 * ## 线程安全性
 *
 * - **不可变性**: 一旦创建，name 和 annotations 不可变
 * - **线程安全**: 只读操作（name、toString）是线程安全的
 * - **子类责任**: 子类添加的可变状态需要自行保证线程安全
 *
 * ## 调试技巧
 *
 * ### 查看描述符的内存地址
 *
 * ```kotlin
 * val descriptor1 = ...
 * val descriptor2 = ...
 *
 * println(descriptor1)  // MyClass [ClassDescriptor@1a2b3c4d]
 * println(descriptor2)  // MyClass [ClassDescriptor@5e6f7g8h]
 *
 * // 通过地址判断是否是同一个对象
 * ```
 *
 * ### 追踪 original 链
 *
 * ```kotlin
 * fun printOriginalChain(descriptor: DeclarationDescriptor) {
 *     var current: DeclarationDescriptor? = descriptor
 *     var depth = 0
 *
 *     while (current != null) {
 *         println("${"  ".repeat(depth)}$current")
 *         val next = current.original
 *         if (next === current) break  // 到达链的末端
 *         current = next
 *         depth++
 *     }
 * }
 * ```
 *
 * ## 示例：完整的描述符实现
 *
 * ```kotlin
 * // 简单的变量描述符实现
 * class SimpleVariableDescriptor(
 *     annotations: Annotations,
 *     name: Name,
 *     override val containingDeclaration: DeclarationDescriptor,
 *     private val type: CangJieType,
 *     private val isMutable: Boolean
 * ) : DeclarationDescriptorImpl(annotations, name), VariableDescriptor {
 *
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitVariableDescriptor(this, data)
 *     }
 *
 *     override val returnType: CangJieType
 *         get() = type
 *
 *     override fun toString(): String {
 *         // 自定义调试输出
 *         return try {
 *             "${if (isMutable) "var" else "val"} $name: ${type.render()}"
 *         } catch (e: Throwable) {
 *             super.toString()
 *         }
 *     }
 * }
 *
 * // 使用
 * val variable = SimpleVariableDescriptor(
 *     annotations = Annotations.EMPTY,
 *     name = Name.identifier("count"),
 *     containingDeclaration = classDescriptor,
 *     type = builtIns.intType,
 *     isMutable = true
 * )
 *
 * println(variable)  // var count: Int
 * variable.accept(visitor, null)  // 访问者模式
 * ```
 *
 * @param annotations 声明的注解列表
 * @param name 声明的名称
 *
 * @property name 声明的名称（类名、函数名、变量名等）
 * @property original 原始描述符（类型替换前的版本），默认返回自身
 *
 * @see AnnotatedImpl 提供注解管理
 * @see DeclarationDescriptor 声明描述符接口
 * @see DeclarationDescriptorNonRootImpl 非根描述符实现（添加包含声明和源码位置）
 * @see DescriptorRenderer 描述符渲染器
 * @see DeclarationDescriptorVisitor 访问者模式接口
 */
abstract class DeclarationDescriptorImpl(
    annotations: Annotations,
    override val name: Name
) : AnnotatedImpl(annotations), DeclarationDescriptor {
    override val original: DeclarationDescriptor
        get() = this

    override fun toString(): String {
        return toString(this)
    }

    /**
     * 渲染描述符为调试字符串，优先使用 DescriptorRenderer；在渲染失败时回退为简单类名和名称。
     */
    fun toString(descriptor: DeclarationDescriptor): String {
        return try {
            DescriptorRenderer.DEBUG_TEXT.render(descriptor) +
                    "[" + descriptor.javaClass.simpleName + "@" + Integer.toHexString(
                System.identityHashCode(
                    descriptor
                )
            ) + "]"
        } catch (e: Throwable) {
            // DescriptionRenderer may throw if this is not yet completely initialized
            // It is very inconvenient while debugging
            descriptor.javaClass.getSimpleName() + " " + descriptor.name
        }
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
        accept(visitor, Unit)
    }

}
