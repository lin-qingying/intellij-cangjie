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

package org.cangnova.cangjie.decompiler.psi.text

/**
 * 反编译文本数据类
 *
 * ## 架构作用
 *
 * DecompiledText 是反编译系统的最终输出，封装从 Stub 树生成的可读源代码文本。
 * 它是 Stub 结构（轻量级索引）和 PSI 树（完整语法树）之间的桥梁。
 *
 * ## 生成流程
 *
 * ```
 * .cjo 二进制元数据
 *   ↓
 * CangJieMetadataStubBuilder
 *   ↓
 * Stub 树（索引结构）
 *   ↓
 * buildDecompiledText() / buildDecompiledTextFromStub()
 *   ↓
 * DecompiledText（可读源码）
 *   ↓
 * 编辑器显示
 * ```
 *
 * ## 使用场景
 *
 * 1. **编辑器显示**: 在 IDE 中显示二进制库的源码
 *    - 用户导航到编译后的类时，显示反编译代码
 *    - 支持语法高亮、代码折叠等编辑器功能
 *
 * 2. **调试支持**: 在调试器中查看库代码
 *    - 设置断点在反编译代码中
 *    - 单步调试跨二进制库的调用
 *
 * 3. **文档生成**: 从元数据生成 API 文档
 *    - 提取类、方法、参数的签名
 *    - 保留类型信息和可见性修饰符
 *
 * ## 数据模型
 *
 * ### 文本内容
 *
 * [text] 字段包含完整的反编译源代码，格式为合法的仓颉语法：
 * - **包声明**: `package com.example`
 * - **导入语句**: 可选（跨包引用通过完全限定名表示）
 * - **类声明**: 包括类型参数、继承、成员
 * - **函数声明**: 包括参数、返回类型、类型约束
 * - **属性和变量**: 带类型注解
 *
 * ### 示例输出
 *
 * ```kotlin
 * package std.collection
 *
 * public class ArrayList<T> where T: Comparable<T> {
 *     private var size: Int32
 *     private var capacity: Int32
 *
 *     public func add(element: T): Unit { COMPILED_CODE }
 *     public func get(index: Int32): T { COMPILED_CODE }
 *     public func size(): Int32 { COMPILED_CODE }
 * }
 * ```
 *
 * ## 文本特征
 *
 * ### 保留的信息
 *
 * - ✅ **签名**: 完整的类型签名和参数列表
 * - ✅ **可见性**: public, private, protected, internal
 * - ✅ **模态**: abstract, open, sealed, final
 * - ✅ **类型参数**: 泛型参数和约束（where 子句）
 * - ✅ **继承关系**: 父类和接口
 * - ✅ **跨包引用**: 使用完全限定名表示其他包的类型
 *
 * ### 丢失的信息
 *
 * - ❌ **方法体**: 用占位符 `COMPILED_CODE` 替代
 * - ❌ **注释**: 源码注释在元数据中不保留
 * - ❌ **初始化器**: 变量初始化表达式丢失
 * - ❌ **默认参数值**: 参数默认值显示为 `COMPILED_CODE`
 * - ❌ **局部变量**: 方法内部实现细节不可恢复
 *
 * ## 编码和格式
 *
 * - **编码**: UTF-8 字符串
 * - **换行**: 使用 `\n` (LF)
 * - **缩进**: 4 空格
 * - **行长**: 尽量不超过 120 字符
 *
 * ## 性能考虑
 *
 * ### 延迟生成
 *
 * 文本仅在需要时生成：
 * - 用户打开文件时触发
 * - 通过 [CjDecompiledFile.calcTreeElement] 调用
 * - 生成后缓存在内存中
 *
 * ### 内存优化
 *
 * - 使用 Kotlin `String` 的内部优化（字符数组共享）
 * - 对于大型类文件（> 10KB），可能被拆分为多个虚拟文件
 * - IDE 的软引用缓存机制自动管理内存
 *
 * ## 与其他组件的集成
 *
 * ### 上游组件
 *
 * - **CangJieMetadataStubBuilder**: 提供 Stub 树
 * - **buildDecompiledText()**: 从元数据生成文本
 * - **buildDecompiledTextFromStub()**: 从 Stub 生成文本
 *
 * ### 下游组件
 *
 * - **CjDecompiledFile**: 使用文本创建 PSI 树
 * - **CangJieDecompiledFileViewProvider**: 管理文件视图
 * - **编辑器**: 显示文本并提供语法高亮
 *
 * ## 错误处理
 *
 * ### 空文本
 *
 * 如果元数据解析失败，返回空字符串或错误消息：
 * ```kotlin
 * DecompiledText("// Failed to decompile: ${errorMessage}")
 * ```
 *
 * ### 不兼容版本
 *
 * 对于不兼容的 ABI 版本：
 * ```kotlin
 * DecompiledText("// Incompatible binary version: ${version}")
 * ```
 *
 * ## 线程安全
 *
 * - **不可变**: DecompiledText 是不可变数据类，天然线程安全
 * - **并发生成**: 多个文件的反编译可以并发执行
 * - **缓存竞争**: 由 IDE 的缓存系统处理
 *
 * ## 扩展性
 *
 * 虽然目前只包含 [text] 字段，设计允许未来扩展：
 * - **元数据**: 添加文件来源、版本信息等
 * - **映射表**: 添加反编译位置到原始位置的映射
 * - **诊断**: 附加反编译过程中的警告和错误
 *
 * ## 示例用法
 *
 * ```kotlin
 * // 从 Stub 生成反编译文本
 * val fileStub: CangJieFileStub = ...
 * val decompiledText = buildDecompiledTextFromStub(fileStub)
 *
 * // 使用文本创建 PSI 文件
 * val psiFile = PsiFileFactory.getInstance(project)
 *     .createFileFromText("Foo.cj", CjLanguage.INSTANCE, decompiledText.text)
 * ```
 *
 * @property text 反编译后的源代码文本（合法的仓颉语法）
 *
 * @see buildDecompiledText
 * @see buildDecompiledTextFromStub
 * @see CjDecompiledFile
 * @see CangJieDecompiledFileViewProvider
 */
data class DecompiledText(val text: String)
