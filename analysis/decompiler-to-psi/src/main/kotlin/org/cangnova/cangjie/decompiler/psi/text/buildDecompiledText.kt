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

/**
 * Descriptor 到反编译文本转换器（传统实现）
 *
 * ## 架构概述
 *
 * 本文件实现了基于 Descriptor 的反编译文本生成，是反编译系统的传统实现路径。
 * 与基于 Stub 的实现 ([buildDecompiledTextFromStub.kt]) 相比，这个实现：
 * - 使用更高级的 Descriptor API
 * - 包含完整的类型信息和语义分析结果
 * - 性能开销更大，但信息更丰富
 *
 * ## 两种实现对比
 *
 * | 特性 | Descriptor 实现 (本文件) | Stub 实现 (buildDecompiledTextFromStub.kt) |
 * |------|------------------------|------------------------------------------|
 * | 性能 | 较慢（需创建 Descriptor） | 快速（直接访问 Stub） |
 * | 信息量 | 完整语义信息 | 仅结构信息 |
 * | 内存占用 | 较大 | 较小 |
 * | 使用场景 | 需要类型解析的场景 | 快速预览、索引 |
 * | 类型检查 | ✅ 支持 | ❌ 不支持 |
 * | 推荐 | 特定场景 | 默认选择 |
 *
 * ## 核心工作流程
 *
 * ```
 * buildDecompiledText(packageFqName, descriptors, renderer)
 *   ↓
 * 1. 初始化 StringBuilder
 *   ↓
 * 2. appendDecompiledTextAndPackageName()
 *   ├─ 文件头注释
 *   └─ package 声明
 *   ↓
 * 3. 遍历顶层 Descriptors
 *   └─ appendDescriptor() 递归渲染
 *       ├─ EnumConstructorDescriptor → 枚举项
 *       ├─ ClassDescriptor → 类声明
 *       │   └─ 递归渲染成员
 *       ├─ EnumDescriptor → 枚举声明
 *       │   ├─ 枚举项列表
 *       │   └─ 成员方法
 *       ├─ ExtendDescriptor → 扩展声明
 *       │   └─ 扩展成员
 *       ├─ FunctionDescriptor → 函数声明
 *       │   └─ 方法体占位符
 *       ├─ PropertyDescriptor → 属性声明
 *       │   └─ getter/setter
 *       └─ 其他 → 使用 DescriptorRenderer
 *   ↓
 * 4. 返回 DecompiledText
 * ```
 *
 * ## Descriptor 渲染器配置
 *
 * [defaultDecompilerRendererOptions] 函数配置渲染行为：
 *
 * ```kotlin
 * fun DescriptorRendererOptions.defaultDecompilerRendererOptions() {
 *     withDefinedIn = false                    // 不显示 "defined in" 注释
 *     classWithPrimaryConstructor = true       // 显示主构造函数
 *     secondaryConstructorsAsPrimary = false   // 次构造函数单独显示
 *     modifiers = DescriptorRendererModifier.ALL // 显示所有修饰符
 *     alwaysRenderModifiers = true             // 总是渲染修饰符
 *     defaultParameterValueRenderer = { _ -> COMPILED_DEFAULT_PARAMETER_VALUE }
 *     propertyConstantRenderer = { _ -> COMPILED_DEFAULT_INITIALIZER }
 * }
 * ```
 *
 * ## 关键常量
 *
 * - [FLEXIBLE_TYPE_COMMENT]: 平台类型注释 `/* platform type */`
 * - [DECOMPILED_CODE_COMMENT]: 方法体占位符 `/* compiled code */`
 * - [DECOMPILED_COMMENT_FOR_PARAMETER]: 参数默认值占位符 `/* = compiled code */`
 * - [COMPILED_DEFAULT_PARAMETER_VALUE]: 来自 stub 模块的参数默认值常量
 * - [COMPILED_DEFAULT_INITIALIZER]: 来自 stub 模块的初始化器常量
 *
 * ## 特殊声明类型处理
 *
 * ### 1. EnumConstructorDescriptor (枚举项)
 *
 * ```kotlin
 * enum Color {
 *     RED
 *     | GREEN(0xFF00)  // 带参数的枚举项
 *     | BLUE;          // 最后一项用分号结尾
 *
 *     func toHex(): String { /* compiled code */ }
 * }
 * ```
 *
 * 特殊处理：
 * - 枚举项之间用 `|` 分隔
 * - 最后一项后用 `;` 分隔枚举项和成员
 * - 非穷尽枚举添加 `...` 占位符
 *
 * ### 2. ClassDescriptor (类/接口/结构体)
 *
 * ```kotlin
 * public class ArrayList<T> where T: Comparable<T> {
 *     init(capacity: Int32) { /* compiled code */ }
 *
 *     private var size: Int32
 *
 *     public func add(element: T): Unit { /* compiled code */ }
 * }
 * ```
 *
 * 成员渲染顺序：
 * 1. 次级构造函数
 * 2. 主构造函数（如果存在）
 * 3. 成员作用域中的其他声明
 *
 * ### 3. PropertyDescriptor (属性)
 *
 * ```kotlin
 * public var name: String {
 *     get() { /* compiled code */ }
 *     set(value) { /* compiled code */ }
 * }
 * ```
 *
 * Getter/Setter 渲染：
 * - 自动生成的访问器跳过（通过 `isDefault` 判断）
 * - Setter 参数使用 [computeParameterName] 计算名称
 *
 * ### 4. ExtendDescriptor (扩展声明)
 *
 * ```kotlin
 * extend ArrayList<T> : Iterable<T> where T: Comparable<T> {
 *     func forEach(action: (T) -> Unit) { /* compiled code */ }
 * }
 * ```
 *
 * ## 成员过滤逻辑
 *
 * ### mustNotBeWrittenToDecompiledText()
 *
 * 过滤不应该出现在反编译文本中的成员：
 *
 * ```kotlin
 * CallableMemberDescriptor.Kind.DECLARATION -> 保留（用户声明）
 * CallableMemberDescriptor.Kind.DELEGATION -> 保留（委托成员）
 * CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> 过滤（继承的覆盖）
 * CallableMemberDescriptor.Kind.SYNTHESIZED -> 按规则过滤（合成成员）
 * ```
 *
 * ### 为什么过滤 FAKE_OVERRIDE？
 *
 * FAKE_OVERRIDE 是编译器为继承成员创建的虚拟 Descriptor：
 * ```kotlin
 * // 父类
 * class Base {
 *     func foo() { ... }
 * }
 *
 * // 子类
 * class Derived : Base {
 *     // foo() 是 FAKE_OVERRIDE，不显示在反编译文本中
 * }
 * ```
 *
 * ## 灵活类型（Flexible Type）处理
 *
 * 灵活类型主要出现在 Java 互操作中：
 * ```kotlin
 * // Java: String getValue()
 * // 仓颉看到: String! /* platform type */
 * func getValue(): String /* platform type */ { ... }
 * ```
 *
 * 通过 `returnType.isFlexible()` 检测并添加注释。
 *
 * ## 缩进管理
 *
 * 使用字符串拼接管理缩进：
 * ```kotlin
 * indent = ""           // 顶层
 * subindent = "$indent    "  // 4 空格缩进
 * ```
 *
 * 每层嵌套增加 4 空格。
 *
 * ## 性能考虑
 *
 * ### 为什么性能较慢？
 *
 * 1. **Descriptor 创建开销**: 需要解析类型、解决引用
 * 2. **作用域遍历**: `memberScope.getContributedDescriptors()` 触发延迟计算
 * 3. **类型检查**: `returnType.isFlexible()` 需要类型系统支持
 *
 * ### 优化建议
 *
 * - 优先使用 Stub-based 实现（[buildDecompiledTextFromStub.kt]）
 * - 仅在需要完整类型信息时使用本实现
 * - 缓存生成的文本（由 IDE 框架处理）
 *
 * ## 使用场景
 *
 * ### 何时使用 Descriptor 实现？
 *
 * 1. **需要类型解析**: 如显示继承关系、类型参数约束
 * 2. **调试器集成**: 需要完整的语义信息
 * 3. **代码分析工具**: 需要准确的类型检查
 *
 * ### 何时使用 Stub 实现？
 *
 * 1. **快速预览**: 用户导航到文件时的默认显示
 * 2. **索引构建**: 符号索引不需要完整语义
 * 3. **大型文件**: 减少内存占用和加载时间
 *
 * ## 与其他组件的关系
 *
 * ### 输入
 *
 * - **packageFqName**: 包的完全限定名
 * - **descriptors**: 顶层声明的 Descriptor 列表
 * - **descriptorRenderer**: 配置好的渲染器
 *
 * ### 输出
 *
 * - **DecompiledText**: 包含反编译源码的数据类
 *
 * ### 依赖
 *
 * - **DescriptorRenderer**: 负责单个 Descriptor 的文本渲染
 * - **DescriptorRendererOptions**: 渲染配置选项
 * - **MemberScope**: 提供成员查询接口
 *
 * ## 已知限制
 *
 * 1. **性能开销**: 比 Stub 实现慢 2-5 倍
 * 2. **依赖解析**: 需要项目依赖完整加载
 * 3. **内存占用**: Descriptor 树比 Stub 树占用更多内存
 * 4. **注释丢失**: 仍然无法恢复源码注释
 * 5. **方法体丢失**: 与 Stub 实现相同的限制
 *
 * ## 扩展点
 *
 * ### 自定义渲染逻辑
 *
 * 通过修改 [DescriptorRendererOptions] 可以自定义：
 * - 修饰符显示方式
 * - 类型名称格式
 * - 默认值占位符
 * - 注解渲染
 *
 * ### 添加新的 Descriptor 类型
 *
 * 在 `appendDescriptor()` 函数中添加新的 `else if` 分支。
 *
 * ## 示例输出
 *
 * ### 输入 (Descriptor 列表)
 * ```
 * [ClassDescriptor(ArrayList)]
 *   - typeParameters: [T]
 *   - superTypes: [Collection<T>]
 *   - members: [size, add(), get()]
 * ```
 *
 * ### 输出 (反编译文本)
 * ```kotlin
 * // IntelliJ API Decompiler stub source generated from a cjo file
 * // Implementation of methods is not available
 *
 * package std.collection
 *
 * public class ArrayList<T> where T: Comparable<T> {
 *     private var size: Int32
 *
 *     public func add(element: T): Unit { /* compiled code */ }
 *
 *     public func get(index: Int32): T { /* compiled code */ }
 * }
 * ```
 *
 * @see buildDecompiledTextFromStub
 * @see DecompiledText
 * @see DescriptorRenderer
 * @see DescriptorRendererOptions
 */
@file:JvmName("DescriptorBuildDecompiledText")

package org.cangnova.cangjie.decompiler.psi.text

import org.cangnova.cangjie.decompiler.COMPILED_DEFAULT_INITIALIZER
import org.cangnova.cangjie.decompiler.COMPILED_DEFAULT_PARAMETER_VALUE
import org.cangnova.cangjie.decompiler.DECOMPILED_CODE_COMMENT
import org.cangnova.cangjie.decompiler.DECOMPILED_COMMENT_FOR_PARAMETER
import org.cangnova.cangjie.decompiler.FLEXIBLE_TYPE_COMMENT
import org.cangnova.cangjie.decompiler.stub.computeParameterName
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.renderer.DescriptorRendererModifier
import org.cangnova.cangjie.renderer.DescriptorRendererOptions
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.secondaryConstructors
import org.cangnova.cangjie.types.isFlexible

/**
 * 配置反编译器的默认渲染选项
 *
 * ## 功能说明
 *
 * 为 [DescriptorRenderer] 设置适合反编译的配置选项，确保生成的文本：
 * - 清晰易读
 * - 包含必要的类型信息
 * - 使用占位符替代无法恢复的部分
 *
 * ## 配置项说明
 *
 * ### withDefinedIn = false
 * 不显示 "defined in" 注释，避免冗余信息：
 * ```
 * ❌ func foo() /* defined in MyClass */
 * ✅ func foo()
 * ```
 *
 * ### classWithPrimaryConstructor = true
 * 显示主构造函数：
 * ```kotlin
 * class Foo(val x: Int)  // 主构造函数可见
 * ```
 *
 * ### secondaryConstructorsAsPrimary = false
 * 次级构造函数单独显示，不与主构造函数混淆。
 *
 * ### modifiers = DescriptorRendererModifier.ALL
 * 显示所有修饰符（可见性、模态、其他）。
 *
 * ### alwaysRenderModifiers = true
 * 总是渲染修饰符，即使是默认值：
 * ```kotlin
 * public func foo()  // 显式 public
 * ```
 *
 * ### parameterNamesInFunctionalTypes = false
 * 函数类型不显示参数名（因为注解信息不可用）：
 * ```
 * ✅ (Int, String) -> Bool
 * ❌ (x: Int, y: String) -> Bool
 * ```
 *
 * ### defaultParameterValueRenderer
 * 参数默认值使用占位符 [COMPILED_DEFAULT_PARAMETER_VALUE]。
 *
 * ### includePropertyConstant = true
 * 包含属性常量值（如果可用）。
 *
 * ### propertyConstantRenderer
 * 常量值使用占位符 [COMPILED_DEFAULT_INITIALIZER]。
 *
 * ## 使用场景
 *
 * 在创建 Descriptor-based 反编译文本时调用：
 * ```kotlin
 * val renderer = DescriptorRenderer.withOptions {
 *     defaultDecompilerRendererOptions()
 * }
 * buildDecompiledText(packageFqName, descriptors, renderer)
 * ```
 *
 * @receiver DescriptorRendererOptions 渲染器选项配置对象
 *
 * @see DescriptorRenderer
 * @see DescriptorRendererOptions
 * @see buildDecompiledText
 */
fun DescriptorRendererOptions.defaultDecompilerRendererOptions() {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
//    excludedTypeAnnotationClasses = emptySet()
    alwaysRenderModifiers = true
    parameterNamesInFunctionalTypes =
        false // to support parameters names in decompiled text we need to load annotation arguments
    defaultParameterValueRenderer = { _ -> COMPILED_DEFAULT_PARAMETER_VALUE }
    includePropertyConstant = true
    propertyConstantRenderer = { _ -> COMPILED_DEFAULT_INITIALIZER }
}

/**
 * 基于 Descriptor 构建反编译文本
 *
 * ## 功能说明
 *
 * 这是 Descriptor-based 反编译的主入口函数，从 Descriptor 列表生成格式化的仓颉源代码。
 * 与 [buildDecompiledTextFromStub] 相比，本函数使用完整的语义分析结果，提供更丰富的类型信息。
 *
 * ## 参数说明
 *
 * ### packageFqName
 * 包的完全限定名，用于生成 package 声明：
 * ```kotlin
 * FqName("std.collection") → "package std.collection"
 * FqName.ROOT → 无 package 声明
 * ```
 *
 * ### descriptors
 * 顶层声明的 Descriptor 列表，包括：
 * - ClassDescriptor (class/interface/struct)
 * - EnumDescriptor (enum)
 * - FunctionDescriptor (func)
 * - PropertyDescriptor (let/var with accessors)
 * - ExtendDescriptor (extend)
 * - TypeAliasDescriptor (type)
 *
 * ### descriptorRenderer
 * 配置好的 [DescriptorRenderer]，控制渲染细节：
 * ```kotlin
 * val renderer = DescriptorRenderer.withOptions {
 *     defaultDecompilerRendererOptions()
 * }
 * ```
 *
 * ## 工作流程
 *
 * ```
 * 1. 创建 StringBuilder
 *   ↓
 * 2. appendDecompiledTextAndPackageName()
 *   ├─ 输出文件头注释
 *   └─ 输出 package 声明
 *   ↓
 * 3. 遍历 descriptors
 *   └─ appendDescriptor(descriptor, indent="")
 *       ├─ 根据 descriptor 类型选择渲染策略
 *       ├─ 递归渲染嵌套成员
 *       └─ 添加方法体占位符
 *   ↓
 * 4. 返回 DecompiledText(text)
 * ```
 *
 * ## 渲染策略
 *
 * ### 递归渲染
 *
 * 嵌套声明（如类成员）通过递归调用 `appendDescriptor` 处理：
 * ```kotlin
 * class Outer {
 *     class Inner {  // 递归渲染 Inner
 *         func foo() { ... }
 *     }
 * }
 * ```
 *
 * ### 缩进管理
 *
 * 每层嵌套增加 4 空格：
 * ```kotlin
 * indent = ""           // 顶层
 * subindent = "    "    // 第一层嵌套
 * subsubindent = "        "  // 第二层嵌套
 * ```
 *
 * ### 成员过滤
 *
 * 通过 [mustNotBeWrittenToDecompiledText] 过滤不应显示的成员：
 * - FAKE_OVERRIDE 成员（继承的虚拟成员）
 * - 某些 SYNTHESIZED 成员（编译器生成）
 *
 * ## 输出格式
 *
 * ### 文件头
 * ```kotlin
 * // IntelliJ API Decompiler stub source generated from a cjo file
 * // Implementation of methods is not available
 *
 * package com.example.app
 * ```
 *
 * ### 类声明
 * ```kotlin
 * public class ArrayList<T> where T: Comparable<T> {
 *     private var size: Int32
 *
 *     public func add(element: T): Unit { /* compiled code */ }
 * }
 * ```
 *
 * ### 枚举声明
 * ```kotlin
 * enum Color {
 *     RED
 *     | GREEN
 *     | BLUE;
 *
 *     func toHex(): String { /* compiled code */ }
 * }
 * ```
 *
 * ### 属性声明
 * ```kotlin
 * public var name: String {
 *     get() { /* compiled code */ }
 *     set(value) { /* compiled code */ }
 * }
 * ```
 *
 * ## 与 Stub 实现的对比
 *
 * | 特性 | Descriptor 实现 | Stub 实现 |
 * |------|---------------|-----------|
 * | 函数 | buildDecompiledText() | buildDecompiledTextFromStub() |
 * | 输入 | List<DeclarationDescriptor> | CangJieFileStubImpl |
 * | 性能 | 慢 (需创建 Descriptor) | 快 (直接读 Stub) |
 * | 信息 | 完整类型信息 | 结构信息 |
 * | 推荐 | 特殊场景 | 默认选择 |
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 获取 Descriptor 列表
 * val descriptors = packageFragmentProvider.getPackageFragments(packageFqName)
 *     .flatMap { it.getMemberScope().getContributedDescriptors() }
 *
 * // 配置渲染器
 * val renderer = DescriptorRenderer.withOptions {
 *     defaultDecompilerRendererOptions()
 * }
 *
 * // 生成反编译文本
 * val decompiledText = buildDecompiledText(packageFqName, descriptors, renderer)
 *
 * // 使用文本创建 PSI 文件
 * val psiFile = PsiFileFactory.getInstance(project)
 *     .createFileFromText("Decompiled.cj", CjLanguage, decompiledText.text)
 * ```
 *
 * ## 性能考虑
 *
 * - **Descriptor 创建**: 需要解析类型和解决引用，开销较大
 * - **作用域遍历**: `memberScope.getContributedDescriptors()` 触发延迟计算
 * - **类型检查**: `isFlexible()` 等需要类型系统支持
 * - **建议**: 优先使用 Stub-based 实现，仅在需要完整类型信息时使用本函数
 *
 * ## 线程安全
 *
 * - **只读操作**: 只读取 Descriptor 数据，不修改
 * - **局部状态**: StringBuilder 是局部变量，线程安全
 * - **并发友好**: 可以在多个线程中并发调用
 *
 * @param packageFqName 包的完全限定名
 * @param descriptors 顶层声明的 Descriptor 列表
 * @param descriptorRenderer 配置好的渲染器
 * @return 反编译后的文本，包含格式化的仓颉源代码
 *
 * @see buildDecompiledTextFromStub
 * @see DecompiledText
 * @see DescriptorRenderer
 * @see defaultDecompilerRendererOptions
 */
fun buildDecompiledText(
    packageFqName: FqName,
    descriptors: List<DeclarationDescriptor>,
    descriptorRenderer: DescriptorRenderer,
): DecompiledText {

    val builder = StringBuilder()

    fun appendDecompiledTextAndPackageName() {
        builder.append("// IntelliJ API Decompiler stub source generated from a cjo file\n" + "// Implementation of methods is not available")
        builder.append("\n\n")
        if (!packageFqName.isRoot) {
            builder.append("package ").append(packageFqName.render()).append("\n\n")
        }
    }

    fun appendDescriptor(descriptor: DeclarationDescriptor, indent: String, lastEnumEntry: Boolean = false) {

        if (descriptor is EnumConstructorDescriptor) {
            for (annotation in descriptor.annotations) {
                builder.append(descriptorRenderer.renderAnnotation(annotation))
                builder.append(" ")
            }

            builder.append(descriptor.name.asString().quoteIfNeeded())
            if (!descriptor.valueParameters.isEmpty()) {


                builder.append(descriptorRenderer.renderValueParameters(descriptor.valueParameters, true))


            }
            if (descriptor.containingDeclaration.isNonExhaustive && lastEnumEntry) {
                val subindent = "$indent    "
                builder.append("|\n")

                builder.append(subindent)
                builder.append("...")
            }
            builder.append(if (lastEnumEntry) ";" else "|")
        } else {
            builder.append(
                descriptorRenderer.render(descriptor).replace(
                    "= ...",
                    DECOMPILED_COMMENT_FOR_PARAMETER
                )
            )
        }

        if (descriptor is CallableDescriptor) {
            //NOTE: assuming that only return types can be flexible
            if (descriptor.returnType!!.isFlexible()) {
                builder.append(" ").append(FLEXIBLE_TYPE_COMMENT)
            }
        }

        if (descriptor is FunctionDescriptor || descriptor is PropertyDescriptor) {
            if ((descriptor as MemberDescriptor).modality != Modality.ABSTRACT) {
                if (descriptor is FunctionDescriptor) {
                    with(builder) {
                        append(" { ")
//                        if (descriptor.getUserData(ContractProviderKey)?.getContractDescription() != null) {
//                            append(DECOMPILED_CONTRACT_STUB).append("; ")
//                        }
                        append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                } else {
                    // descriptor instanceof PropertyDescriptor
//                    builder.append(" ").append(DECOMPILED_CODE_COMMENT)
                }
            }
            if (descriptor is PropertyDescriptor) {
                builder.append("{")

                for (accessor in descriptor.accessors) {
//                    if (accessor.isDefault) continue
                    builder.append("\n$indent    ")


                    if (accessor is PropertyGetterDescriptor) {
                        builder.append("get()")
                        builder.append(" {").append(DECOMPILED_CODE_COMMENT).append(" }")

                    } else if (accessor is PropertySetterDescriptor) {
                        builder.append("set(")
                        val parameterDescriptor = accessor.valueParameters[0]
                        for (annotation in parameterDescriptor.annotations) {
                            builder.append(descriptorRenderer.renderAnnotation(annotation))
                            builder.append(" ")
                        }
                        val parameterName = computeParameterName(parameterDescriptor.name)
                        builder.append(parameterName.asString())
//                            .append("<: ")
//                            .append(descriptorRenderer.renderType(parameterDescriptor.type))
                        builder.append(")")
                        builder.append(" {").append(DECOMPILED_CODE_COMMENT).append(" }")
                    }
                }
                builder.append("}")
            }
        } else if (descriptor is ClassDescriptor) {
            builder.append(" {\n")

            val subindent = "$indent    "

            var firstPassed = false
            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                } else {
                    firstPassed = true
                }
            }

            val allDescriptors =
                descriptor.secondaryConstructors + descriptor.defaultType.memberScope.getContributedDescriptors() +
                        if (descriptor.unsubstitutedPrimaryConstructor != null) {
                            listOf(descriptor.unsubstitutedPrimaryConstructor!!)
                        } else {
                            listOf()
                        }





            for (member in allDescriptors) {

                if (member.containingDeclaration != descriptor) {
                    continue
                }

                if (member is CallableMemberDescriptor && member.mustNotBeWrittenToDecompiledText()) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")
        } else if (descriptor is EnumDescriptor) {

            builder.append(" {\n")

            val subindent = "$indent    "

            var firstPassed = false


            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                } else {
                    firstPassed = true
                }
            }

            val allDescriptors =
                descriptor.defaultType.memberScope.getContributedDescriptors()

            val enumEntries = descriptor.constructors.toList()
            for ((index, enumEntry) in enumEntries.withIndex()) {
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(enumEntry, subindent, index == enumEntries.lastIndex)
            }



            for (member in allDescriptors) {

                if (member.containingDeclaration != descriptor) {
                    continue
                }

                if (member is CallableMemberDescriptor && member.mustNotBeWrittenToDecompiledText()) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")

        } else if (descriptor is ExtendDescriptor) {
            builder.append(" {\n")

            val subindent = "$indent    "

            var firstPassed = false
            fun newlineExceptFirst() {
                if (firstPassed) {
                    builder.append("\n")
                } else {
                    firstPassed = true
                }
            }

            val allDescriptors =
                descriptor.memberScope.getContributedDescriptors()




            for (member in allDescriptors) {

                if (member.containingDeclaration != descriptor) {
                    continue
                }

                if (member is CallableMemberDescriptor && member.mustNotBeWrittenToDecompiledText()) {
                    continue
                }
                newlineExceptFirst()
                builder.append(subindent)
                appendDescriptor(member, subindent)
            }

            builder.append(indent).append("}")
        }
        builder.append("\n")
    }

    appendDecompiledTextAndPackageName()
    for (member in descriptors) {
        appendDescriptor(member, "")
        builder.append("\n")
    }

    return DecompiledText(builder.toString())
}

private fun CallableMemberDescriptor.syntheticMemberMustNotBeWrittenToDecompiledText(): Boolean {
    val containingClass = containingDeclaration as? ClassDescriptor ?: return false

    return when {


        else -> false
    }
}

internal fun CallableMemberDescriptor.mustNotBeWrittenToDecompiledText(): Boolean {
    return when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION, CallableMemberDescriptor.Kind.DELEGATION -> false
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> true
        CallableMemberDescriptor.Kind.SYNTHESIZED -> syntheticMemberMustNotBeWrittenToDecompiledText()
    }
}
