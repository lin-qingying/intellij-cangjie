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
 * 反编译的仓颉 PSI 文件
 *
 * ## 架构概述
 *
 * CjDecompiledFile 是反编译系统的核心 PSI 表示层，负责将二进制元数据文件
 * （.cjo/.cjb）转换为 IDE 可操作的 PSI 文件对象。它是连接 Stub 索引和
 * IDE 编辑器/分析服务的关键桥梁。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * 虚拟文件 (.cjo/.cjb)
 *   ↓
 * CangJieDecompiledFileViewProvider
 *   ↓
 * CjDecompiledFile (本类)
 *   ├─ 管理 Stub 树加载
 *   ├─ 生成反编译文本
 *   └─ 提供 PSI 树访问
 *   ↓
 * IDE 服务（编辑器、分析、导航等）
 * ```
 *
 * ## 核心职责
 *
 * ### 1. Stub 树管理
 *
 * 通过 [CompiledStubBuilder] 从虚拟文件读取或构建 Stub 树：
 * - 优先从 IDE 缓存读取（StubTreeLoader）
 * - 缓存未命中时构建新的 Stub 树
 * - 确保 Stub 和 PSI 的一致性
 *
 * ### 2. 反编译文本生成
 *
 * 通过 [buildDecompiledText] 从 Stub 树生成可读源代码：
 * - 延迟加载：仅在首次访问时生成
 * - 缓存结果：避免重复反编译
 * - 支持重新加载：虚拟文件变化时清除缓存
 *
 * ### 3. PSI 文件服务
 *
 * 作为标准的 [CjFile] 实现，提供 IDE 需要的所有 PSI 功能：
 * - 语法树访问
 * - 符号解析
 * - 代码分析
 * - 导航支持
 *
 * ## 设计模式
 *
 * ### 延迟加载 + 缓存
 *
 * 使用 [LockedClearableLazyValue] 实现线程安全的延迟加载：
 * ```kotlin
 * private val decompiledText = LockedClearableLazyValue(Any()) {
 *     val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
 *     buildDecompiledText(stub)
 * }
 * ```
 *
 * **好处**:
 * - **性能**: 仅在需要时才执行昂贵的反编译操作
 * - **内存**: 避免预加载所有库文件的反编译文本
 * - **可清除**: 支持内容重新加载场景
 *
 * ### 自定义 StubBuilder
 *
 * 通过覆盖 [customStubBuilder] 提供专门的 Stub 构建逻辑：
 * ```kotlin
 * override val customStubBuilder: StubBuilder?
 *     get() = CompiledStubBuilder
 * ```
 *
 * **与普通文件的区别**:
 * - **普通 .cj 文件**: 通过 Parser 解析源码生成 Stub
 * - **反编译 .cjo 文件**: 直接从二进制元数据读取 Stub
 *
 * ## 工作流程
 *
 * ### 首次访问流程
 *
 * ```
 * 用户打开 ArrayList.cjo
 *   ↓
 * IDE 调用 getText()
 *   ↓
 * decompiledText.get() (首次)
 *   ├─ CompiledStubBuilder.readOrBuildCompiledStub()
 *   │   ├─ StubTreeLoader.readOrBuild() (优先读缓存)
 *   │   ├─ CangJieMetadataStubBuilder.buildFileStub() (缓存未命中)
 *   │   └─ 返回 CangJieFileStubImpl
 *   ├─ buildDecompiledText(stub)
 *   │   ├─ 遍历 Stub 树
 *   │   ├─ 使用 CjVisitor 生成文本
 *   │   └─ 返回 DecompiledText
 *   └─ 缓存并返回文本
 *   ↓
 * 编辑器显示反编译代码
 * ```
 *
 * ### 后续访问流程
 *
 * ```
 * 用户再次访问相同文件
 *   ↓
 * IDE 调用 getText()
 *   ↓
 * decompiledText.get() (缓存命中)
 *   └─ 直接返回缓存的文本
 * ```
 *
 * ### 内容重新加载流程
 *
 * ```
 * 虚拟文件内容变化（例如库更新）
 *   ↓
 * IDE 调用 onContentReload()
 *   ├─ super.onContentReload() (清除 PSI 缓存)
 *   ├─ provider.content.drop() (清除 ViewProvider 缓存)
 *   └─ decompiledText.drop() (清除反编译文本缓存)
 *   ↓
 * 下次访问时重新反编译
 * ```
 *
 * ## Stub 构建细节
 *
 * ### CompiledStubBuilder 对象
 *
 * 专门的 [StubBuilder] 实现，负责反编译文件的 Stub 树管理：
 * - **buildStubTree()**: 为 PSI 文件构建 Stub 树（克隆缓存的 Stub）
 * - **readOrBuildCompiledStub()**: 从虚拟文件读取或构建 Stub
 *
 * ### Stub 克隆机制
 *
 * ```kotlin
 * val clonedStub = stub.deepCopy()
 * clonedStub.psi = file
 * return clonedStub
 * ```
 *
 * **为什么需要克隆？**
 * - **缓存共享**: StubTreeLoader 缓存的 Stub 是共享的
 * - **PSI 绑定**: 每个 PSI 文件需要独立的 Stub 树副本
 * - **避免冲突**: 防止多个 PSI 文件引用同一个 Stub 树
 *
 * ### StubTreeLoader 集成
 *
 * 利用 IDE 的 Stub 缓存机制：
 * ```kotlin
 * val stubTree = stubLoader.readOrBuild(project, virtualFile, psiFile = null)
 * ```
 *
 * **好处**:
 * - **跨项目共享**: 不同项目共享相同库的 Stub
 * - **持久化缓存**: Stub 缓存到磁盘，IDE 重启后仍有效
 * - **版本管理**: Stub 版本变化时自动重新构建
 *
 * ## 错误处理
 *
 * ### Stub 树为 null
 *
 * 可能原因：
 * - 虚拟文件不存在或已删除
 * - 元数据文件损坏
 * - 反编译器内部错误
 *
 * 处理方式：
 * ```kotlin
 * val cause = if (stubTree == null) {
 *     "stub tree is not found"
 * } else {
 *     "non-CangJie stub tree (${stubTree::class.simpleName})"
 * }
 * val text = "// Could not decompile the file: $cause"
 * CangJieFileStubImpl.forInvalid(text)
 * ```
 *
 * ### 非仓颉 Stub 树
 *
 * 如果 Stub 树不是 [CangJieFileStubImpl] 类型（罕见，表示严重错误）：
 * - 创建一个无效的 Stub，包含错误消息
 * - 编辑器显示错误文本而不是崩溃
 *
 * ### Default Project 处理
 *
 * Default project（IDE 启动时的临时项目）不支持 Stub 缓存：
 * ```kotlin
 * if (project.isDefault) {
 *     stubLoader.build(null, virtualFile, null)  // 不使用缓存
 * } else {
 *     stubLoader.readOrBuild(project, virtualFile, null)  // 使用缓存
 * }
 * ```
 *
 * ## 性能优化
 *
 * ### 三级缓存架构
 *
 * 1. **ViewProvider 缓存** ([CangJieDecompiledFileViewProvider.content]):
 *    - 缓存最终的文本字符串
 *    - 用于编辑器显示
 *
 * 2. **PSI 文件缓存** ([decompiledText]):
 *    - 缓存 DecompiledText 对象
 *    - 用于 getText() 方法
 *
 * 3. **Stub 缓存** ([StubTreeLoader]):
 *    - IDE 全局缓存
 *    - 持久化到磁盘
 *    - 跨项目共享
 *
 * ### 延迟 PSI 树构建
 *
 * PSI 树不是在文件创建时构建，而是在首次访问时：
 * - 避免启动时批量构建所有库文件的 PSI 树
 * - 仅构建实际使用的文件
 *
 * ### allowMultifileClassPart 机制
 *
 * ```kotlin
 * ClsClassFinder.allowMultifileClassPart {
 *     stubLoader.readOrBuild(...)
 * }
 * ```
 *
 * 允许 Stub 加载器处理多文件类的特殊情况（类似 Kotlin 的 @JvmMultifileClass）。
 *
 * ## 线程安全
 *
 * ### LockedClearableLazyValue
 *
 * [decompiledText] 使用线程安全的延迟值：
 * - 多线程并发访问时只初始化一次
 * - synchronized 保护初始化代码
 * - drop() 操作是原子的
 *
 * ### ReadAction 要求
 *
 * PSI 和 Stub 访问必须在 ReadAction 中：
 * - IDE 框架自动确保 getText() 在 ReadAction 中调用
 * - 手动访问 Stub 时需要显式包裹 ReadAction.compute { }
 *
 * ## 使用场景
 *
 * ### 1. 编辑器浏览库代码
 *
 * ```
 * 用户导航到 std.collection.ArrayList
 *   ↓
 * IDE 打开 ArrayList.cjo
 *   ↓
 * CjDecompiledFile 生成可浏览的源码
 *   ↓
 * 用户查看类定义、方法签名
 * ```
 *
 * ### 2. Go to Definition 跨包跳转
 *
 * ```
 * 用户点击 ArrayList<Int> 中的 ArrayList
 *   ↓
 * IDE 解析引用 → 找到定义在 ArrayList.cjo 中
 *   ↓
 * 创建 CjDecompiledFile
 *   ↓
 * 在反编译文本中定位并高亮 class ArrayList
 * ```
 *
 * ### 3. 调试器显示库代码
 *
 * ```
 * 调试器在 ArrayList.add() 中暂停
 *   ↓
 * IDE 查找源文件 → 找到 ArrayList.cjo
 *   ↓
 * CjDecompiledFile 提供源码视图
 *   ↓
 * 调试器高亮当前执行行
 * ```
 *
 * ### 4. 代码补全和分析
 *
 * ```
 * 用户输入 `list.`
 *   ↓
 * 代码补全需要 ArrayList 的成员列表
 *   ↓
 * 访问 ArrayList.cjo 的 Stub（无需反编译文本）
 *   ↓
 * 从 Stub 提取方法签名用于补全
 * ```
 *
 * ## 与其他组件的关系
 *
 * ### 上游组件
 *
 * - **[CangJieDecompiledFileViewProvider]**: 创建本类实例
 * - **[CompiledStubBuilder]**: 提供 Stub 树
 * - **[StubTreeLoader]**: 提供 Stub 缓存
 *
 * ### 下游组件
 *
 * - **IDE 编辑器**: 调用 getText() 显示内容
 * - **PSI 服务**: 使用 PSI 树进行分析
 * - **导航服务**: 使用 Stub 进行符号查找
 *
 * ## 扩展性
 *
 * ### 自定义反编译逻辑
 *
 * 可以继承本类并覆盖 [decompiledText] 的初始化逻辑：
 * ```kotlin
 * class MyCustomDecompiledFile(provider: CangJieDecompiledFileViewProvider) : CjDecompiledFile(provider) {
 *     override fun getText(): String {
 *         // 自定义反编译逻辑
 *     }
 * }
 * ```
 *
 * ### 测试支持
 *
 * 测试中可以 mock CompiledStubBuilder：
 * ```kotlin
 * val testStub = CangJieFileStubImpl.forTest("test content")
 * mockkObject(CompiledStubBuilder) {
 *     every { readOrBuildCompiledStub(any()) } returns testStub
 *     // 测试代码
 * }
 * ```
 *
 * ## 已知限制
 *
 * 1. **只读**: 反编译文件不支持编辑，所有编辑操作被忽略
 * 2. **无注释**: 源码注释无法从元数据恢复
 * 3. **无方法体**: 方法实现显示为占位符 `{ /* compiled code */ }`
 * 4. **性能开销**: 首次反编译可能需要 50-200ms
 *
 * @param provider 提供此文件视图的 [CangJieDecompiledFileViewProvider]
 *
 * @see CangJieDecompiledFileViewProvider
 * @see DecompiledText
 * @see buildDecompiledText
 * @see CompiledStubBuilder
 * @see CjFile
 */
package org.cangnova.cangjie.decompiler.psi.file

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.StubTreeLoader
import org.cangnova.cangjie.decompiler.psi.CangJieDecompiledFileViewProvider
import org.cangnova.cangjie.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.decompiler.psi.text.buildDecompiledText
import org.cangnova.cangjie.decompiler.stub.file.ClsClassFinder
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.utils.LockedClearableLazyValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
open class CjDecompiledFile(
    private val provider: CangJieDecompiledFileViewProvider,
) : CjFile(provider, true) {
    /**
     * 自定义 Stub 构建器
     *
     * ## 功能说明
     *
     * 返回专门用于反编译文件的 Stub 构建器 [CompiledStubBuilder]。
     * 覆盖父类的默认 Stub 构建逻辑，使用从二进制元数据读取 Stub 的方式，
     * 而不是通过解析源码生成 Stub。
     *
     * ## 与普通文件的区别
     *
     * - **普通 .cj 文件**: `customStubBuilder = null`，使用默认的 Parser-based Stub 构建
     * - **反编译 .cjo 文件**: `customStubBuilder = CompiledStubBuilder`，直接从元数据读取 Stub
     *
     * ## IDE Stub 构建流程
     *
     * 当 IDE 需要构建 Stub 树时：
     * ```
     * IDE 调用 psiFile.getStub()
     *   ↓
     * 检查 customStubBuilder
     *   ├─ null → 使用 Parser 解析源码生成 Stub
     *   └─ 非 null → 调用 customStubBuilder.buildStubTree()
     *       ↓
     *       CompiledStubBuilder.buildStubTree(this)
     *         ├─ readOrBuildCompiledStub() (读取缓存或构建)
     *         ├─ stub.deepCopy() (克隆 Stub)
     *         └─ clonedStub.psi = file (绑定到当前 PSI 文件)
     * ```
     *
     * ## 为什么需要自定义 Stub 构建器？
     *
     * 1. **元数据已包含结构信息**: .cjo 文件本身就是编译后的元数据，包含完整的声明结构
     * 2. **避免解析反编译文本**: 解析反编译后的文本重新生成 Stub 是不必要的开销
     * 3. **保持一致性**: 直接从元数据读取的 Stub 与原始源码的 Stub 完全一致
     * 4. **性能优化**: 避免"元数据 → 文本 → PSI → Stub"的绕路，直接"元数据 → Stub"
     *
     * @see CompiledStubBuilder
     * @see StubBuilder
     */
    override val customStubBuilder: StubBuilder?
        get() = CompiledStubBuilder

    /**
     * 延迟加载的反编译文本
     *
     * ## 功能说明
     *
     * 该字段缓存从 Stub 树生成的反编译文本，使用线程安全的延迟加载机制。
     *
     * ## 初始化逻辑
     *
     * 首次访问时执行：
     * ```kotlin
     * val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
     * buildDecompiledText(stub)
     * ```
     *
     * ## 执行流程
     *
     * ```
     * decompiledText.get() (首次调用)
     *   ↓
     * 1. readOrBuildCompiledStub(this)
     *    ├─ 从虚拟文件读取
     *    ├─ StubTreeLoader.readOrBuild() (优先读缓存)
     *    ├─ 缓存未命中 → CangJieMetadataStubBuilder.buildFileStub()
     *    └─ 返回 CangJieFileStubImpl
     *   ↓
     * 2. buildDecompiledText(stub)
     *    ├─ 遍历 Stub 树
     *    ├─ 使用 CjVisitor 生成源码文本
     *    ├─ 渲染类、函数、属性等声明
     *    └─ 返回 DecompiledText 对象
     *   ↓
     * 3. 缓存 DecompiledText 并返回
     * ```
     *
     * ## 缓存清除
     *
     * 在 [onContentReload] 中调用 `decompiledText.drop()` 清除缓存。
     *
     * ## 线程安全
     *
     * [LockedClearableLazyValue] 保证：
     * - 并发访问时只初始化一次
     * - 初始化过程使用锁保护
     * - drop() 操作是线程安全的
     *
     * ## 性能特点
     *
     * - **首次访问**: 需要完整反编译（50-200ms，取决于文件大小）
     * - **后续访问**: 直接返回缓存（< 1ms）
     * - **内存开销**: DecompiledText 对象 + String（通常 1-100KB）
     *
     * @see LockedClearableLazyValue
     * @see buildDecompiledText
     * @see CompiledStubBuilder.readOrBuildCompiledStub
     * @see onContentReload
     */
    private val decompiledText = LockedClearableLazyValue(Any()) {
        val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
        buildDecompiledText(stub)
    }

    /**
     * 获取文件的文本内容
     *
     * ## 功能说明
     *
     * 返回反编译后的源代码文本。该方法是 [PsiFile] 接口的核心方法，
     * IDE 的编辑器、搜索、分析等服务都通过此方法获取文件内容。
     *
     * ## 实现方式
     *
     * 直接从 [decompiledText] 延迟值获取：
     * ```kotlin
     * return decompiledText.get().text
     * ```
     *
     * ## 调用时机
     *
     * - **编辑器打开文件**: 显示源码内容
     * - **全文搜索**: 在反编译代码中搜索关键字
     * - **差异比较**: 比较不同版本的反编译结果
     * - **代码检查**: 某些检查需要访问文本内容
     *
     * ## 与 getContents() 的区别
     *
     * - **getText()**: PSI 文件级别的方法（本方法）
     * - **getContents()**: ViewProvider 级别的方法（在 [CangJieDecompiledFileViewProvider] 中）
     *
     * 对于反编译文件，这两者应该返回相同的内容。
     *
     * ## 线程安全
     *
     * 必须在 ReadAction 中调用（由 IDE 框架保证）。
     *
     * ## 性能考虑
     *
     * 虽然该方法可能被频繁调用，但由于 [decompiledText] 的缓存机制，
     * 实际的反编译操作只执行一次。
     *
     * @return 反编译后的仓颉源代码文本
     *
     * @see decompiledText
     * @see buildDecompiledText
     * @see CangJieDecompiledFileViewProvider.getContents
     */
    override fun getText(): String {
        return decompiledText.get().text
    }

    /**
     * 内容重新加载回调
     *
     * ## 功能说明
     *
     * 当虚拟文件内容发生变化时，IDE 会调用此方法通知 PSI 文件更新缓存。
     *
     * ## 触发时机
     *
     * - **库文件更新**: 用户升级依赖，.cjo 文件内容变化
     * - **外部修改**: 文件系统监视器检测到文件变化
     * - **强制刷新**: 用户手动刷新文件
     *
     * ## 执行逻辑
     *
     * 清除所有三级缓存：
     * ```kotlin
     * super.onContentReload()         // 清除父类的 PSI 缓存
     * provider.content.drop()         // 清除 ViewProvider 的文本缓存
     * decompiledText.drop()           // 清除本类的反编译文本缓存
     * ```
     *
     * ## 三级缓存清除流程
     *
     * ```
     * onContentReload() 调用
     *   ↓
     * 1. super.onContentReload()
     *    └─ 清除 PSI 树缓存、AST 节点等
     *   ↓
     * 2. provider.content.drop()
     *    └─ 清除 ViewProvider 缓存的文本字符串
     *   ↓
     * 3. decompiledText.drop()
     *    └─ 清除本类缓存的 DecompiledText 对象
     *   ↓
     * 下次访问时重新反编译
     * ```
     *
     * ## 为什么需要清除三级缓存？
     *
     * - **PSI 树缓存**: 保证 PSI 树结构与新内容一致
     * - **ViewProvider 缓存**: 保证编辑器显示的文本是最新的
     * - **DecompiledText 缓存**: 保证反编译结果基于新的 Stub 树
     *
     * ## 与 Stub 缓存的关系
     *
     * Stub 缓存由 [StubTreeLoader] 管理，IDE 框架会自动处理：
     * - Stub 版本变化时自动重新构建
     * - 不需要在此方法中显式清除
     *
     * @see LockedClearableLazyValue.drop
     * @see CangJieDecompiledFileViewProvider.content
     */
    override fun onContentReload() {
        super.onContentReload()
        provider.content.drop()
        decompiledText.drop()
    }
}

/**
 * 反编译文件的专用 Stub 构建器
 *
 * ## 功能说明
 *
 * CompiledStubBuilder 是一个单例对象，实现了 [StubBuilder] 接口，
 * 专门用于反编译文件的 Stub 树构建和管理。
 *
 * ## 核心职责
 *
 * 1. **构建 Stub 树**: 为 PSI 文件提供 Stub 树（[buildStubTree]）
 * 2. **读取缓存 Stub**: 从 IDE 缓存或虚拟文件读取 Stub（[readOrBuildCompiledStub]）
 * 3. **Stub 克隆**: 为每个 PSI 文件创建独立的 Stub 副本
 *
 * ## 设计原理
 *
 * ### 为什么使用 object 单例？
 *
 * - **无状态**: Stub 构建逻辑不需要维护状态
 * - **共享**: 所有反编译文件共享相同的构建逻辑
 * - **性能**: 避免重复创建构建器实例
 *
 * ### Stub 克隆vs 共享
 *
 * **问题**: StubTreeLoader 缓存的 Stub 是全局共享的，但每个 PSI 文件需要自己的 Stub 树。
 *
 * **解决方案**: 克隆缓存的 Stub 并绑定到当前 PSI 文件：
 * ```kotlin
 * val clonedStub = stub.deepCopy()    // 深拷贝 Stub 树
 * clonedStub.psi = file               // 绑定到当前 PSI 文件
 * ```
 *
 * **好处**:
 * - 缓存的 Stub 保持不可变，避免并发问题
 * - 每个 PSI 文件有独立的 Stub 树，互不干扰
 * - PSI 文件销毁时，其 Stub 副本也会被垃圾回收
 *
 * ## 与其他组件的关系
 *
 * ```
 * CjDecompiledFile.customStubBuilder
 *   ↓
 * CompiledStubBuilder (本对象)
 *   ↓
 * ├─ StubTreeLoader (IDE 缓存)
 * └─ CangJieMetadataStubBuilder (构建新 Stub)
 * ```
 *
 * @see StubBuilder
 * @see CjDecompiledFile.customStubBuilder
 * @see readOrBuildCompiledStub
 * @see buildStubTree
 */
private object CompiledStubBuilder : StubBuilder {
    /**
     * 为 PSI 文件构建 Stub 树
     *
     * ## 功能说明
     *
     * 该方法是 [StubBuilder] 接口的核心方法，IDE 调用此方法为 PSI 文件
     * 获取 Stub 树。实现通过读取缓存或构建新 Stub，然后克隆并绑定到当前 PSI 文件。
     *
     * ## 执行流程
     *
     * ```
     * buildStubTree(file)
     *   ↓
     * 1. requireIsInstance<CjDecompiledFile>(file)
     *    └─ 类型检查，确保是反编译文件
     *   ↓
     * 2. readOrBuildCompiledStub(file)
     *    └─ 读取或构建共享的 Stub 树
     *   ↓
     * 3. stub.deepCopy()
     *    └─ 深拷贝 Stub 树
     *   ↓
     * 4. clonedStub.psi = file
     *    └─ 绑定到当前 PSI 文件
     *   ↓
     * 5. return clonedStub
     * ```
     *
     * ## 为什么需要类型检查？
     *
     * [requireIsInstance] 确保传入的 PSI 文件是 [CjDecompiledFile]：
     * - 编译时类型为 [PsiFile]（接口要求）
     * - 运行时必须是 [CjDecompiledFile]（实际使用）
     * - 如果类型错误，抛出 IllegalArgumentException
     *
     * ## Stub 克隆的必要性
     *
     * **不克隆会发生什么？**
     * - 多个 PSI 文件共享同一个 Stub 树
     * - Stub.psi 引用会指向错误的 PSI 文件
     * - PSI 导航、符号解析会失败
     *
     * **克隆的成本**:
     * - 内存: 每个 PSI 文件额外占用 Stub 树的内存（通常几 KB）
     * - 时间: 深拷贝 Stub 树（通常 < 5ms）
     * - 相比解析源码生成 Stub（可能 > 50ms），成本可接受
     *
     * ## 线程安全
     *
     * - 必须在 ReadAction 中调用（由 IDE 框架保证）
     * - Stub 克隆操作是线程安全的
     * - 缓存的 Stub 是不可变的，可安全共享
     *
     * @param file PSI 文件，必须是 [CjDecompiledFile] 实例
     * @return 克隆并绑定到当前 PSI 文件的 Stub 树
     *
     * @throws IllegalArgumentException 如果 file 不是 [CjDecompiledFile] 类型
     *
     * @see readOrBuildCompiledStub
     * @see CangJieFileStubImpl.deepCopy
     * @see requireIsInstance
     */
    override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
        requireIsInstance<CjDecompiledFile>(file)
        val stub = CompiledStubBuilder.readOrBuildCompiledStub(file)

        val clonedStub = stub.deepCopy()
        clonedStub.psi = file
        return clonedStub
    }

    /**
     * 读取或构建编译文件的 Stub 树
     *
     * ## 功能说明
     *
     * 从虚拟文件读取或构建 Stub 树，优先使用 IDE 的 Stub 缓存。
     * 这是 Stub 获取的核心逻辑，负责集成 [StubTreeLoader] 和处理各种边缘情况。
     *
     * ## 执行流程
     *
     * ```
     * readOrBuildCompiledStub(file)
     *   ↓
     * 1. 获取 virtualFile 和 project
     *   ↓
     * 2. ClsClassFinder.allowMultifileClassPart {
     *      (允许多文件类的特殊处理)
     *   ↓
     * 3. 判断是否为 default project
     *   ├─ Yes → stubLoader.build(null, virtualFile, null)
     *   │        (Default project 不使用缓存)
     *   └─ No  → stubLoader.readOrBuild(project, virtualFile, null)
     *            (普通项目优先读缓存)
     *   ↓
     * 4. 获取 Stub 树根节点
     *   ├─ 类型检查: stubTree.root is CangJieFileStubImpl?
     *   ├─ 成功 → 返回 fileStub
     *   └─ 失败 → 创建 Invalid Stub (错误消息)
     *   }
     * ```
     *
     * ## StubTreeLoader 集成
     *
     * ### readOrBuild() 方法
     *
     * ```kotlin
     * stubLoader.readOrBuild(project, virtualFile, psiFile = null)
     * ```
     *
     * **参数说明**:
     * - **project**: 项目上下文，用于定位缓存
     * - **virtualFile**: 虚拟文件，缓存的 key
     * - **psiFile = null**: 不传入 PSI 文件，避免循环依赖
     *
     * **执行逻辑**:
     * 1. 检查内存缓存（Map<VirtualFile, StubTree>）
     * 2. 检查持久化缓存（磁盘文件）
     * 3. 缓存未命中 → 调用 Stub 构建器构建新 Stub
     * 4. 将新 Stub 写入缓存
     * 5. 返回 Stub 树
     *
     * ### build() 方法 (Default Project)
     *
     * ```kotlin
     * stubLoader.build(project = null, virtualFile, psiFile = null)
     * ```
     *
     * 不使用缓存，直接构建 Stub：
     * - Default project 是临时项目，不需要缓存
     * - 避免污染全局缓存
     *
     * ## allowMultifileClassPart 机制
     *
     * 该机制处理多文件类的特殊情况（类似 Kotlin 的 @JvmMultifileClass）：
     * ```kotlin
     * ClsClassFinder.allowMultifileClassPart {
     *     // Stub 加载逻辑
     * }
     * ```
     *
     * **作用**:
     * - 临时修改 [ClsClassFinder] 的行为
     * - 允许 Stub 加载器访问多文件类的部分声明
     * - 加载完成后恢复原始行为
     *
     * ## 错误处理
     *
     * ### Stub 树为 null
     *
     * 可能原因：
     * - 虚拟文件不存在或已删除
     * - 元数据文件损坏，无法解析
     * - Stub 构建器内部错误
     *
     * 处理方式：
     * ```kotlin
     * val cause = "stub tree is not found"
     * val text = "// Could not decompile the file: $cause"
     * CangJieFileStubImpl.forInvalid(text)
     * ```
     *
     * ### 非仓颉 Stub 树
     *
     * 如果 Stub 根节点不是 [CangJieFileStubImpl]（罕见，表示严重错误）：
     * ```kotlin
     * val cause = "non-CangJie stub tree (${stubTree::class.simpleName})"
     * val text = "// Could not decompile the file: $cause"
     * CangJieFileStubImpl.forInvalid(text)
     * ```
     *
     * ### Invalid Stub 创建
     *
     * [CangJieFileStubImpl.forInvalid] 创建一个特殊的 Stub：
     * - 包含错误消息作为文件内容
     * - 标记为 Invalid 类型
     * - 反编译时显示错误文本而不是崩溃
     *
     * ## 性能特点
     *
     * ### 缓存命中
     *
     * - 从内存缓存读取: < 1ms
     * - 从磁盘缓存读取: 1-10ms
     *
     * ### 缓存未命中
     *
     * - 解析 Flatbuffers 元数据: 10-50ms
     * - 构建 Stub 树: 5-20ms
     * - 写入缓存: 1-5ms
     * - 总计: 15-75ms
     *
     * ## Default Project 处理
     *
     * Default project 是 IDE 启动时的临时项目，用于：
     * - 显示"欢迎屏幕"
     * - 处理命令行工具
     * - 运行不依赖项目的操作
     *
     * 对于 default project：
     * - 不使用缓存（`project = null`）
     * - 每次都重新构建 Stub
     * - 避免污染全局缓存
     *
     * @param file 反编译 PSI 文件
     * @return 文件的 Stub 树根节点，如果失败则返回包含错误消息的 Invalid Stub
     *
     * @see StubTreeLoader
     * @see ClsClassFinder.allowMultifileClassPart
     * @see CangJieFileStubImpl.forInvalid
     * @see CangJieMetadataStubBuilder
     */
    fun readOrBuildCompiledStub(file: CjDecompiledFile): CangJieFileStubImpl {
        val virtualFile = file.viewProvider.virtualFile
        val project = file.project

        val stubTree = ClsClassFinder.allowMultifileClassPart {
            val stubLoader = StubTreeLoader.getInstance()

            // The default project is not supported in the stub loader
            if (project.isDefault) {
                stubLoader.build(/* project = */ null,/* vFile = */ virtualFile,/* psiFile = */ null)
            } else {
                // Read stub from cache if it is present
                stubLoader.readOrBuild(/* project = */ project,/* vFile = */ virtualFile,/* psiFile = */ null)
            }
        }

        val fileStub = stubTree?.root as? CangJieFileStubImpl
        return if (fileStub != null) {
            fileStub
        } else {
            val cause = if (stubTree == null) {
                "stub tree is not found"
            } else {
                "non-CangJie stub tree (${stubTree::class.simpleName})"
            }

            val text = """
                // Could not decompile the file: $cause
            """.trimIndent()

            CangJieFileStubImpl.forInvalid(text)
        }
    }

    /**
     * 是否跳过子节点处理
     *
     * ## 功能说明
     *
     * [StubBuilder] 接口方法，控制在构建 Stub 树时是否跳过某些子节点的处理。
     *
     * ## 实现
     *
     * 对于反编译文件，始终返回 `false`：
     * - 不跳过任何子节点
     * - 处理所有 AST 节点
     *
     * ## 为什么始终返回 false？
     *
     * 反编译文件的 Stub 树不是从 AST 构建的（通过 [readOrBuildCompiledStub] 直接从元数据读取），
     * 所以这个方法实际上不会被调用。保留默认实现以满足接口要求。
     *
     * @param parent 父 AST 节点
     * @param node 当前 AST 节点
     * @return 始终返回 false（不跳过任何节点）
     */
    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}

/**
 * 类型检查并要求对象为指定类型
 *
 * ## 功能说明
 *
 * 该内联函数使用 Kotlin Contracts 进行类型检查，确保对象是指定类型 [T]，
 * 否则抛出 [IllegalArgumentException]。
 *
 * ## 使用 Kotlin Contracts
 *
 * ```kotlin
 * @OptIn(ExperimentalContracts::class)
 * public inline fun <reified T> requireIsInstance(obj: Any) {
 *     contract {
 *         returns() implies (obj is T)
 *     }
 *     require(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
 * }
 * ```
 *
 * **Contract 说明**:
 * - `returns() implies (obj is T)`: 如果函数正常返回（不抛异常），则 obj 一定是 T 类型
 * - 编译器可以使用这个契约进行智能类型转换
 *
 * ## 使用场景
 *
 * ### 在 CompiledStubBuilder 中
 *
 * ```kotlin
 * override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
 *     requireIsInstance<CjDecompiledFile>(file)  // 类型检查
 *     // 此处 file 已被智能转换为 CjDecompiledFile
 *     val stub = readOrBuildCompiledStub(file)
 *     ...
 * }
 * ```
 *
 * ### 智能类型转换
 *
 * 调用后，编译器知道 obj 是 T 类型，无需手动类型转换：
 * ```kotlin
 * requireIsInstance<CjDecompiledFile>(file)
 * file.customStubBuilder  // 直接访问 CjDecompiledFile 的成员
 * ```
 *
 * ## 与 checkIsInstance 的区别
 *
 * - **requireIsInstance**: 用于前置条件检查（参数验证），抛出 [IllegalArgumentException]
 * - **checkIsInstance**: 用于状态检查（不变量验证），抛出 [IllegalStateException]
 *
 * ## 错误消息
 *
 * ```
 * Expected class org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
 * instead of class org.cangnova.cangjie.psi.CjFileImpl
 * for CjFileImpl(file.cj)
 * ```
 *
 * @param T 期望的类型（reified，可在运行时访问）
 * @param obj 要检查的对象
 *
 * @throws IllegalArgumentException 如果 obj 不是 T 类型
 *
 * @see checkIsInstance
 * @see require
 */
@OptIn(ExperimentalContracts::class)
public inline fun <reified T> requireIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    require(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}

/**
 * 类型检查并检查对象为指定类型
 *
 * ## 功能说明
 *
 * 与 [requireIsInstance] 类似，但抛出 [IllegalStateException] 而不是 [IllegalArgumentException]。
 * 用于状态不变量检查而不是参数验证。
 *
 * ## 使用 Kotlin Contracts
 *
 * ```kotlin
 * @OptIn(ExperimentalContracts::class)
 * public inline fun <reified T> checkIsInstance(obj: Any) {
 *     contract {
 *         returns() implies (obj is T)
 *     }
 *     check(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
 * }
 * ```
 *
 * ## 使用场景
 *
 * ### 内部状态验证
 *
 * ```kotlin
 * fun processStub(stub: Any) {
 *     checkIsInstance<CangJieFileStubImpl>(stub)  // 状态检查
 *     // 确保内部逻辑正确，stub 应该已经是正确类型
 *     stub.getPackageFqName()
 * }
 * ```
 *
 * ### 不变量验证
 *
 * ```kotlin
 * fun getFileStub(): CangJieFileStubImpl {
 *     val stub = cachedStub
 *     checkIsInstance<CangJieFileStubImpl>(stub)  // 不变量检查
 *     return stub
 * }
 * ```
 *
 * ## 与 requireIsInstance 的区别
 *
 * | 函数 | 抛出异常 | 使用场景 | 语义 |
 * |------|---------|---------|------|
 * | [requireIsInstance] | IllegalArgumentException | 参数验证 | "调用者传入了错误的参数" |
 * | [checkIsInstance] | IllegalStateException | 状态验证 | "内部状态不正确" |
 *
 * ## 错误消息
 *
 * ```
 * Expected class org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
 * instead of class org.cangnova.cangjie.psi.stubs.impl.InvalidFileStub
 * for InvalidFileStub(...)
 * ```
 *
 * @param T 期望的类型（reified，可在运行时访问）
 * @param obj 要检查的对象
 *
 * @throws IllegalStateException 如果 obj 不是 T 类型
 *
 * @see requireIsInstance
 * @see check
 */
@OptIn(ExperimentalContracts::class)
public inline fun <reified T> checkIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    check(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}