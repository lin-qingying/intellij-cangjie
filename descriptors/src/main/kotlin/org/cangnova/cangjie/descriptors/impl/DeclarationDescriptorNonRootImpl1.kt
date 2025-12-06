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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorNonRoot
import org.cangnova.cangjie.descriptors.DeclarationDescriptorWithSource
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name

/**
 * 非根声明描述符抽象基类 - 为有父级的声明描述符提供包含声明和源码位置支持
 *
 * DeclarationDescriptorNonRootImpl 是 [DeclarationDescriptorImpl] 的扩展，专门用于表示位于某个
 * 作用域内的声明（如类中的方法、属性等），与根声明（如包、模块）相对。它添加了两个关键特性：
 * 包含声明（containingDeclaration）和源码元素（source）。
 *
 * ## 核心功能
 *
 * ### 包含声明关系
 *
 * [containingDeclaration] 属性指向父级声明：
 * - 类的成员 → 包含声明是类
 * - 局部变量 → 包含声明是函数
 * - 嵌套类 → 包含声明是外层类
 * - 形成声明的层次树结构
 *
 * ### 源码位置跟踪
 *
 * [source] 属性关联 PSI 元素或其他源码表示：
 * - 用于错误报告的精确位置定位
 * - IDE 导航（跳转到声明）
 * - 调试信息生成
 * - 可能是 [SourceElement.NO_SOURCE]（反序列化的声明）
 *
 * ### 类型安全的 Original
 *
 * 覆盖 [original] 返回类型为 [DeclarationDescriptorWithSource]：
 * - 提供更强的类型保证
 * - 确保 original 也包含源码信息
 *
 * ### 验证传播
 *
 * [validate] 方法递归验证包含声明：
 * - 确保整个声明链的一致性
 * - 从内向外传播验证
 * - 用于一致性检查和错误检测
 *
 * ## 使用场景
 *
 * ### 1. 类成员描述符
 *
 * ```kotlin
 * class SimpleFunctionDescriptorImpl(
 *     containingDeclaration: ClassDescriptor,  // 所属的类
 *     annotations: Annotations,
 *     name: Name,
 *     source: SourceElement  // PSI 函数元素
 * ) : DeclarationDescriptorNonRootImpl(
 *     containingDeclaration,
 *     annotations,
 *     name,
 *     source
 * ), FunctionDescriptor {
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitFunctionDescriptor(this, data)
 *     }
 *
 *     // 函数的参数、返回类型等...
 * }
 *
 * // 使用示例
 * val classDescriptor: ClassDescriptor = ...
 * val functionPsi: CjFunction = ...  // PSI 元素
 *
 * val functionDescriptor = SimpleFunctionDescriptorImpl(
 *     containingDeclaration = classDescriptor,
 *     annotations = extractAnnotations(functionPsi),
 *     name = functionPsi.nameIdentifier.name,
 *     source = PsiSourceElement(functionPsi)
 * )
 *
 * // 导航到包含类
 * val ownerClass = functionDescriptor.containingDeclaration as ClassDescriptor
 *
 * // 跳转到源码
 * val psi = (functionDescriptor.source as? PsiSourceElement)?.psi
 * navigateToElement(psi)
 * ```
 *
 * ### 2. 局部变量描述符
 *
 * ```kotlin
 * // 源代码:
 * // fun foo() {
 * //     val x = 10  // 局部变量
 * // }
 *
 * class LocalVariableDescriptor(
 *     containingFunction: FunctionDescriptor,  // 所属的函数
 *     annotations: Annotations,
 *     name: Name,
 *     source: SourceElement
 * ) : DeclarationDescriptorNonRootImpl(
 *     containingFunction,
 *     annotations,
 *     name,
 *     source
 * ), VariableDescriptor {
 *     // 变量类型等...
 * }
 *
 * // 使用
 * val variableDescriptor = LocalVariableDescriptor(
 *     containingFunction = functionDescriptor,
 *     annotations = Annotations.EMPTY,
 *     name = Name.identifier("x"),
 *     source = PsiSourceElement(variablePsi)
 * )
 *
 * // 查找变量所在的函数
 * val ownerFunction = variableDescriptor.containingDeclaration as FunctionDescriptor
 * ```
 *
 * ### 3. 嵌套类描述符
 *
 * ```kotlin
 * // 源代码:
 * // class Outer {
 * //     class Inner {}
 * // }
 *
 * class LazyClassDescriptor(
 *     outerClass: ClassDescriptor,  // 外层类
 *     annotations: Annotations,
 *     name: Name,
 *     source: SourceElement
 * ) : DeclarationDescriptorNonRootImpl(
 *     outerClass,
 *     annotations,
 *     name,
 *     source
 * ), ClassDescriptor {
 *     // 类的成员等...
 * }
 *
 * // 使用
 * val innerClassDescriptor = LazyClassDescriptor(
 *     outerClass = outerClassDescriptor,
 *     annotations = Annotations.EMPTY,
 *     name = Name.identifier("Inner"),
 *     source = PsiSourceElement(innerClassPsi)
 * )
 *
 * // 获取外层类
 * val outer = innerClassDescriptor.containingDeclaration as ClassDescriptor
 * ```
 *
 * ### 4. 错误报告和导航
 *
 * ```kotlin
 * // 报告错误到精确位置
 * fun reportError(descriptor: DeclarationDescriptorNonRoot, message: String) {
 *     val source = descriptor.source
 *     if (source is PsiSourceElement) {
 *         val element = source.psi
 *         diagnosticReporter.report(
 *             Errors.SOME_ERROR.on(element, message)
 *         )
 *     }
 * }
 *
 * // IDE 导航
 * fun navigateToDeclaration(descriptor: DeclarationDescriptorNonRoot) {
 *     val source = descriptor.source
 *     if (source is PsiSourceElement) {
 *         val element = source.psi
 *         val offset = element.textRange.startOffset
 *         editor.caretModel.moveToOffset(offset)
 *     }
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### 继承层次
 *
 * ```
 * DeclarationDescriptorImpl (基类)
 *   ↑
 * DeclarationDescriptorNonRootImpl (添加 containingDeclaration 和 source)
 *   ↑
 * PackageFragmentDescriptorImpl (包片段)
 *   ↑
 * MutablePackageFragmentDescriptor (可变包片段)
 *
 * 或:
 *
 * DeclarationDescriptorNonRootImpl
 *   ↑
 * CallableMemberDescriptor (函数、属性等)
 *   ↑
 * SimpleFunctionDescriptorImpl, PropertyDescriptorImpl, 等
 * ```
 *
 * ### 构造器参数
 *
 * ```kotlin
 * protected constructor(
 *     override val containingDeclaration: DeclarationDescriptor,  // 包含此声明的父级
 *     annotations: Annotations,                                   // 注解
 *     name: Name,                                                 // 名称
 *     override val source: SourceElement                          // 源码元素
 * )
 * ```
 *
 * - **protected**: 仅供子类调用
 * - **override val**: 覆盖接口属性并存储为字段
 *
 * ### Original 类型收窄
 *
 * ```kotlin
 * override val original: DeclarationDescriptorWithSource
 *     get() = super.original as DeclarationDescriptorWithSource
 * ```
 *
 * - 将返回类型从 `DeclarationDescriptor` 收窄为 `DeclarationDescriptorWithSource`
 * - 提供更强的类型保证
 * - 安全转换（因为所有 NonRoot 描述符都是 WithSource）
 *
 * ### Validate 实现
 *
 * ```kotlin
 * override fun validate() {
 *     containingDeclaration.validate()
 * }
 * ```
 *
 * - 递归调用父级的验证
 * - 从内向外传播验证
 * - 确保整个声明链的一致性
 *
 * ## 与其他基类的对比
 *
 * | 特性 | DeclarationDescriptorImpl | DeclarationDescriptorNonRootImpl | PackageFragmentDescriptorImpl |
 * |------|---------------------------|----------------------------------|-------------------------------|
 * | containingDeclaration | 否 | 是 | 是（类型为 ModuleDescriptor） |
 * | source | 否 | 是 | 是（固定为 NO_SOURCE） |
 * | 验证传播 | 否 | 是 | 继承自父类 |
 * | 使用场景 | 根声明（模块） | 有父级的声明 | 包片段 |
 * | 典型子类 | ModuleDescriptor | 类、函数、属性 | 包片段实现 |
 *
 * ## 源码元素类型
 *
 * ### PsiSourceElement
 *
 * 最常见的源码元素，关联 PSI 树：
 *
 * ```kotlin
 * class PsiSourceElement(val psi: PsiElement) : SourceElement {
 *     override fun getPsi(): PsiElement = psi
 * }
 *
 * // 使用
 * val functionPsi: CjFunction = ...
 * val source = PsiSourceElement(functionPsi)
 * ```
 *
 * ### SourceElement.NO_SOURCE
 *
 * 表示无源码的声明（反序列化、合成的声明）：
 *
 * ```kotlin
 * // 从编译后的库加载的类
 * val libraryClassDescriptor = LazyClassDescriptor(
 *     containingDeclaration = packageFragment,
 *     annotations = loadAnnotations(),
 *     name = className,
 *     source = SourceElement.NO_SOURCE  // 无源码
 * )
 * ```
 *
 * ### 自定义 SourceElement
 *
 * 可以实现自定义的源码元素：
 *
 * ```kotlin
 * class BinarySourceElement(
 *     val classFile: VirtualFile,
 *     val offset: Int
 * ) : SourceElement {
 *     override fun getPsi(): PsiElement? = null
 *     // 提供二进制文件的位置信息
 * }
 * ```
 *
 * ## 性能特性
 *
 * ### 内存占用
 *
 * 相比 `DeclarationDescriptorImpl` 增加：
 * - **containingDeclaration**: 引用（8 字节）
 * - **source**: 引用（8 字节）
 * - **总计**: 约 48 字节 + 引用对象大小
 *
 * ### Validate 性能
 *
 * - **时间复杂度**: O(depth)，depth 是声明嵌套深度
 * - **典型深度**: 1-5 层（类中的方法、局部变量等）
 * - **开销**: 非常小，仅递归调用
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 从 PSI 创建描述符
 *
 * ```kotlin
 * fun createFunctionDescriptor(
 *     functionPsi: CjFunction,
 *     containingClass: ClassDescriptor
 * ): FunctionDescriptor {
 *     return SimpleFunctionDescriptorImpl(
 *         containingDeclaration = containingClass,
 *         annotations = extractAnnotations(functionPsi),
 *         name = functionPsi.nameIdentifier?.let { Name.identifier(it.text) }
 *             ?: Name.special("<anonymous>"),
 *         source = PsiSourceElement(functionPsi)
 *     ).apply {
 *         // 初始化参数、返回类型等
 *     }
 * }
 * ```
 *
 * ### 模式 2: 查找声明的顶层容器
 *
 * ```kotlin
 * fun getTopLevelContainer(descriptor: DeclarationDescriptor): DeclarationDescriptor {
 *     var current = descriptor
 *     while (current is DeclarationDescriptorNonRoot) {
 *         current = current.containingDeclaration
 *     }
 *     return current  // 返回模块或包
 * }
 * ```
 *
 * ### 模式 3: 构建完全限定名
 *
 * ```kotlin
 * fun getFqName(descriptor: DeclarationDescriptor): FqName {
 *     val parts = mutableListOf<Name>()
 *     var current: DeclarationDescriptor? = descriptor
 *
 *     while (current != null) {
 *         if (current.name.isSpecial) break
 *         parts.add(0, current.name)
 *
 *         current = if (current is DeclarationDescriptorNonRoot) {
 *             current.containingDeclaration
 *         } else {
 *             null
 *         }
 *     }
 *
 *     return FqName.fromSegments(parts.map { it.asString() })
 * }
 * ```
 *
 * ### 模式 4: 验证声明链
 *
 * ```kotlin
 * fun validateDescriptor(descriptor: DeclarationDescriptorNonRoot) {
 *     try {
 *         descriptor.validate()
 *         println("Descriptor is valid")
 *     } catch (e: IllegalStateException) {
 *         println("Descriptor validation failed: ${e.message}")
 *     }
 * }
 * ```
 *
 * ## 常见陷阱
 *
 * ### 1. 循环包含关系
 *
 * ```kotlin
 * // 错误示例：可能导致无限递归
 * class BadDescriptor(...) : DeclarationDescriptorNonRootImpl(...) {
 *     init {
 *         // 如果 containingDeclaration 又引用了 this，可能导致循环
 *         (containingDeclaration as SomeDescriptor).addMember(this)
 *     }
 * }
 *
 * // 安全做法：延迟初始化
 * class GoodDescriptor(...) : DeclarationDescriptorNonRootImpl(...) {
 *     fun initialize() {
 *         (containingDeclaration as SomeDescriptor).addMember(this)
 *     }
 * }
 * ```
 *
 * ### 2. 忘记传递 source
 *
 * ```kotlin
 * // 问题示例：使用 NO_SOURCE 导致无法导航
 * val descriptor = MyDescriptor(
 *     containingDeclaration = ...,
 *     annotations = ...,
 *     name = ...,
 *     source = SourceElement.NO_SOURCE  // 失去了 PSI 关联
 * )
 *
 * // 正确做法：传递实际的 PSI 源码
 * val descriptor = MyDescriptor(
 *     containingDeclaration = ...,
 *     annotations = ...,
 *     name = ...,
 *     source = PsiSourceElement(psiElement)
 * )
 * ```
 *
 * ### 3. containingDeclaration 类型错误
 *
 * ```kotlin
 * // 错误示例：方法的包含声明应该是类，而不是模块
 * val methodDescriptor = MethodDescriptor(
 *     containingDeclaration = moduleDescriptor,  // 错误！
 *     ...
 * )
 *
 * // 正确做法
 * val methodDescriptor = MethodDescriptor(
 *     containingDeclaration = classDescriptor,  // 正确
 *     ...
 * )
 * ```
 *
 * ## 线程安全性
 *
 * - **containingDeclaration**: 不可变引用，线程安全
 * - **source**: 不可变引用，线程安全
 * - **validate**: 只读操作，线程安全
 * - **子类责任**: 子类添加的可变状态需要自行保证线程安全
 *
 * ## 调试技巧
 *
 * ### 打印声明链
 *
 * ```kotlin
 * fun printContainingDeclarationChain(descriptor: DeclarationDescriptor) {
 *     var current: DeclarationDescriptor? = descriptor
 *     var depth = 0
 *
 *     while (current != null) {
 *         println("${"  ".repeat(depth)}${current.name} (${current.javaClass.simpleName})")
 *
 *         current = if (current is DeclarationDescriptorNonRoot) {
 *             current.containingDeclaration
 *         } else {
 *             null
 *         }
 *         depth++
 *     }
 * }
 *
 * // 示例输出:
 * // myMethod (SimpleFunctionDescriptor)
 * //   MyClass (LazyClassDescriptor)
 * //     com.example (PackageFragmentDescriptor)
 * //       myModule (ModuleDescriptor)
 * ```
 *
 * ### 检查源码关联
 *
 * ```kotlin
 * fun checkSource(descriptor: DeclarationDescriptorNonRoot) {
 *     when (val source = descriptor.source) {
 *         is PsiSourceElement -> {
 *             println("PSI element: ${source.psi}")
 *             println("File: ${source.psi.containingFile.name}")
 *             println("Offset: ${source.psi.textRange.startOffset}")
 *         }
 *         is SourceElement.NO_SOURCE -> {
 *             println("No source (binary or synthetic)")
 *         }
 *         else -> {
 *             println("Custom source: ${source.javaClass.simpleName}")
 *         }
 *     }
 * }
 * ```
 *
 * ## 示例：完整的属性描述符实现
 *
 * ```kotlin
 * class SimplePropertyDescriptor(
 *     containingDeclaration: DeclarationDescriptor,
 *     annotations: Annotations,
 *     name: Name,
 *     source: SourceElement,
 *     private val type: CangJieType,
 *     private val isVar: Boolean
 * ) : DeclarationDescriptorNonRootImpl(
 *     containingDeclaration,
 *     annotations,
 *     name,
 *     source
 * ), PropertyDescriptor {
 *
 *     override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
 *         return visitor.visitPropertyDescriptor(this, data)
 *     }
 *
 *     override val returnType: CangJieType
 *         get() = type
 *
 *     override fun toString(): String {
 *         return "${if (isVar) "var" else "val"} $name: ${type.render()}"
 *     }
 * }
 *
 * // 使用示例
 * val propertyPsi: CjProperty = ...
 * val classDescriptor: ClassDescriptor = ...
 *
 * val propertyDescriptor = SimplePropertyDescriptor(
 *     containingDeclaration = classDescriptor,
 *     annotations = extractAnnotations(propertyPsi),
 *     name = Name.identifier(propertyPsi.nameIdentifier.text),
 *     source = PsiSourceElement(propertyPsi),
 *     type = resolveType(propertyPsi.typeReference),
 *     isVar = propertyPsi.isVar
 * )
 *
 * // 导航到包含类
 * val owner = propertyDescriptor.containingDeclaration as ClassDescriptor
 * println("Property ${propertyDescriptor.name} belongs to ${owner.name}")
 *
 * // 跳转到源码
 * val psi = (propertyDescriptor.source as PsiSourceElement).psi
 * navigateToElement(psi)
 *
 * // 验证
 * propertyDescriptor.validate()
 * ```
 *
 * @param containingDeclaration 包含此声明的父级声明描述符
 * @param annotations 声明的注解列表
 * @param name 声明的名称
 * @param source 关联的源码元素（PSI 元素、二进制位置等）
 *
 * @property containingDeclaration 包含此声明的父级（类、函数、包等）
 * @property source 源码元素，用于错误报告和导航
 * @property original 原始描述符（类型为 DeclarationDescriptorWithSource）
 *
 * @see DeclarationDescriptorImpl 父类，提供名称和注解管理
 * @see DeclarationDescriptorNonRoot 实现的接口
 * @see DeclarationDescriptorWithSource 带源码信息的描述符接口
 * @see SourceElement 源码元素接口
 * @see PackageFragmentDescriptorImpl 包片段描述符（继承自此类）
 */
abstract class DeclarationDescriptorNonRootImpl protected constructor(
    override val containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    override val source: SourceElement
) : DeclarationDescriptorImpl(annotations, name), DeclarationDescriptorNonRoot {
    override val original: DeclarationDescriptorWithSource
        get() = super.original as DeclarationDescriptorWithSource

    override fun validate() {
        containingDeclaration.validate()
    }


}
