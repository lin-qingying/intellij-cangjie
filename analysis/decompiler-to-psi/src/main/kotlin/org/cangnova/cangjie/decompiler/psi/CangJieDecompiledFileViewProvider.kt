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

/**
 * 反编译文件的 PSI 视图提供者
 *
 * ## 架构概述
 *
 * CangJieDecompiledFileViewProvider 是 IntelliJ Platform 虚拟文件系统和 PSI 系统之间的桥梁。
 * 它负责为二进制元数据文件（.cjo/.cjb）提供可编辑的文本视图和 PSI 树访问。
 *
 * ## 在反编译系统中的位置
 *
 * ```
 * 虚拟文件 (.cjo/.cjb)
 *   ↓
 * CangJieMetadataDecompiler.createFileViewProvider()
 *   ↓
 * CangJieDecompiledFileViewProvider (本类)
 *   ├─ 创建 CjDecompiledFile (PSI 表示)
 *   └─ 提供文本内容给 IDE 编辑器
 *   ↓
 * IDE 显示为可浏览的 .cj 源文件
 * ```
 *
 * ## FileViewProvider 职责
 *
 * 在 IntelliJ Platform 中，FileViewProvider 负责：
 * 1. **语言映射**: 将虚拟文件映射到特定语言（仓颉语言）
 * 2. **PSI 文件创建**: 创建和管理 PSI 文件实例
 * 3. **文档同步**: 保持文档内容和 PSI 内容的一致性
 * 4. **多根支持**: 支持单个文件的多语言视图（本类使用 [SingleRootFileViewProvider]）
 *
 * ## 核心设计
 *
 * ### 职责分离
 *
 * - **本类**: 作为桥梁和适配器，不包含业务逻辑
 * - **[CjDecompiledFile]**: 负责实际的反编译文本生成和 PSI 树管理
 *
 * ### 工厂模式
 *
 * 使用工厂函数 [factory] 创建 PSI 文件：
 * ```kotlin
 * val provider = CangJieDecompiledFileViewProvider(manager, file, physical) { viewProvider ->
 *     CjDecompiledFile(viewProvider)
 * }
 * ```
 *
 * 这允许调用者自定义 PSI 文件的创建逻辑（例如用于测试）。
 *
 * ### 延迟加载机制
 *
 * 文本内容通过 [LockedClearableLazyValue] 延迟加载：
 * - **首次访问**: 创建临时 PSI 文件，获取其文本，然后销毁
 * - **缓存**: 文本内容缓存，后续访问直接返回
 * - **重新加载**: 当虚拟文件变化时，清除缓存强制重新生成
 *
 * ## 关键方法
 *
 * ### createFile()
 *
 * 创建反编译后的 PSI 文件。调用 [factory] 函数，通常返回 [CjDecompiledFile] 实例。
 *
 * **重要**: 该方法可能被多次调用：
 * - IDE 可能创建临时 PSI 文件用于分析
 * - [content] 初始化时会创建一次临时文件
 * - 实际显示时会创建最终的 PSI 文件
 *
 * ### getContents()
 *
 * 返回文档内容（反编译文本）。IDE 的编辑器组件调用此方法获取显示内容。
 *
 * **工作流程**:
 * ```
 * getContents() 被调用
 *   ↓
 * content.get() (首次调用)
 *   ├─ 创建临时 CjDecompiledFile
 *   ├─ 调用 psiFile.text (触发反编译)
 *   ├─ markInvalidated() (销毁临时文件)
 *   └─ 返回文本并缓存
 *   ↓
 * 后续调用直接返回缓存的文本
 * ```
 *
 * ### createCopy()
 *
 * 创建视图提供者的副本。用于文件复制、重命名等场景。
 * 副本标记为非物理文件 (`physical = false`)，因为它可能是内存中的临时副本。
 *
 * ## 线程安全
 *
 * ### LockedClearableLazyValue
 *
 * [content] 字段使用线程安全的延迟值容器：
 * - **锁机制**: 使用 synchronized 保护初始化
 * - **单次初始化**: 确保只执行一次昂贵的反编译操作
 * - **可清除**: 支持 `drop()` 清除缓存
 *
 * ### PSI 访问限制
 *
 * PSI 文件的创建和访问必须在正确的线程：
 * - 读操作：必须在 ReadAction 中
 * - 写操作：必须在 WriteAction 中
 *
 * ## 使用场景
 *
 * ### 1. 用户导航到 .cjo 文件
 *
 * ```
 * 用户在项目视图中双击 ArrayList.cjo
 *   ↓
 * IDE 调用 CangJieMetadataDecompiler.decompile()
 *   ↓
 * 创建 CangJieDecompiledFileViewProvider
 *   ↓
 * IDE 调用 getContents() 显示反编译文本
 *   ↓
 * 编辑器显示可浏览的 ArrayList.cj 源码
 * ```
 *
 * ### 2. 调试器显示库代码
 *
 * ```
 * 调试器在库函数中断点暂停
 *   ↓
 * IDE 查找源文件 → 找到 .cjo 文件
 *   ↓
 * 通过本类创建视图提供者
 *   ↓
 * 显示反编译代码并高亮当前行
 * ```
 *
 * ### 3. Go to Definition 跨包跳转
 *
 * ```
 * 用户点击跨包类型引用
 *   ↓
 * IDE 解析引用 → 找到目标声明在 .cjo 文件中
 *   ↓
 * 通过本类创建视图提供者
 *   ↓
 * 在反编译文本中定位并高亮目标声明
 * ```
 *
 * ## 与其他组件的关系
 *
 * ### 上游组件
 *
 * - **[CangJieMetadataDecompiler]**: 创建本类的实例
 * - **虚拟文件系统**: 提供 [VirtualFile] 实例
 * - **PSI 管理器**: 提供 [PsiManager] 实例
 *
 * ### 下游组件
 *
 * - **[CjDecompiledFile]**: 本类创建和管理的 PSI 文件
 * - **编辑器**: 调用 [getContents] 获取显示内容
 * - **PSI 服务**: 使用 PSI 文件进行代码分析
 *
 * ## 性能优化
 *
 * ### 延迟创建
 *
 * PSI 文件和反编译文本都是延迟创建的：
 * - 仅在实际需要时才反编译
 * - 避免启动时批量反编译所有库文件
 *
 * ### 临时文件销毁
 *
 * [content] 初始化时创建的临时 PSI 文件会被立即销毁：
 * ```kotlin
 * DebugUtil.performPsiModification("Invalidating throw-away copy") {
 *     (psiFile as? PsiFileImpl)?.markInvalidated()
 * }
 * ```
 *
 * 这防止内存中存在多个相同文件的 PSI 副本。
 *
 * ### 缓存策略
 *
 * - **[content]**: 缓存反编译文本（String）
 * - **IDE 缓存**: PSI 文件本身由 IDE 的 PSI 缓存管理
 * - **Stub 缓存**: Stub 树由 [StubTreeLoader] 缓存（参见 [CjDecompiledFile]）
 *
 * ## 错误处理
 *
 * ### PSI 创建失败
 *
 * 如果 [factory] 返回 null：
 * - [createFile] 返回 null
 * - [content] 会得到空字符串
 * - IDE 显示空文件或错误消息
 *
 * ### 反编译失败
 *
 * 如果 `psiFile.text` 抛出异常：
 * - 异常会传播到 IDE 的错误处理器
 * - 可能显示"无法反编译"的错误消息
 *
 * ## 已知限制
 *
 * 1. **单一语言**: 使用 [SingleRootFileViewProvider]，不支持混合语言文件
 * 2. **只读**: 反编译文件是只读的，用户无法编辑
 * 3. **性能开销**: 首次访问需要完整反编译，可能较慢
 *
 * ## 扩展点
 *
 * ### 自定义 PSI 文件类型
 *
 * 通过 [factory] 参数可以创建自定义的 PSI 文件：
 * ```kotlin
 * CangJieDecompiledFileViewProvider(manager, file, physical) { viewProvider ->
 *     MyCustomDecompiledFile(viewProvider)
 * }
 * ```
 *
 * ### 测试支持
 *
 * 测试中可以使用 mock factory：
 * ```kotlin
 * CangJieDecompiledFileViewProvider(manager, testFile, false) { viewProvider ->
 *     createTestPsiFile(viewProvider, "test content")
 * }
 * ```
 *
 * @param manager PSI 管理器，用于访问 PSI 相关服务和项目上下文
 * @param file 要提供视图的虚拟文件（通常是 .cjo 或 .cjb 文件）
 * @param physical 是否为物理文件（磁盘上的真实文件 vs 内存中的临时文件）
 * @param factory 创建 PSI 文件的工厂函数，允许自定义 PSI 文件创建逻辑
 *
 * @see SingleRootFileViewProvider
 * @see CjDecompiledFile
 * @see CangJieMetadataDecompiler.createFileViewProvider
 * @see LockedClearableLazyValue
 */
package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.PsiFileImpl
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.decompiler.psi.text.buildDecompiledText
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.utils.LockedClearableLazyValue
class CangJieDecompiledFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    /**
     * PSI 文件创建工厂函数
     *
     * 该函数负责创建反编译的 PSI 文件实例。通过工厂模式允许调用者自定义 PSI 文件的创建逻辑。
     *
     * ## 典型实现
     *
     * ```kotlin
     * { viewProvider -> CjDecompiledFile(viewProvider) }
     * ```
     *
     * ## 使用场景
     *
     * - **正常使用**: 创建标准的 [CjDecompiledFile]
     * - **测试**: 创建 mock PSI 文件用于单元测试
     * - **扩展**: 创建自定义的反编译文件子类
     *
     * @see CjDecompiledFile
     */
    private val factory: (CangJieDecompiledFileViewProvider) -> CjDecompiledFile?

) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {

    /**
     * 创建反编译后的 PSI 文件
     *
     * ## 功能说明
     *
     * 该方法是 IntelliJ Platform PSI 系统的核心回调，负责创建反编译文件的 PSI 表示。
     * 通过调用 [factory] 函数委托实际的 PSI 文件创建逻辑。
     *
     * ## 调用时机
     *
     * IDE 在以下场景会调用此方法：
     * 1. **编辑器打开文件**: 用户打开 .cjo 文件时
     * 2. **代码分析**: 后台索引或代码检查需要访问 PSI 时
     * 3. **导航**: Go to Definition 跳转到反编译文件时
     * 4. **调试**: 调试器显示库代码时
     *
     * **重要**: 该方法可能被多次调用创建临时 PSI 文件：
     * - [content] 初始化时会创建一次临时文件获取文本
     * - IDE 实际显示文件时会创建最终的 PSI 文件
     *
     * ## 参数说明
     *
     * - **project**: 当前项目上下文，提供项目级服务访问
     * - **file**: 虚拟文件，通常是 .cjo 或 .cjb 文件
     * - **fileType**: 文件类型（被忽略，始终使用 [CangJieFileType]）
     *
     * ## 返回值
     *
     * - **非 null**: [CjDecompiledFile] 实例或其子类
     * - **null**: 如果 factory 创建失败（罕见，通常表示严重错误）
     *
     * ## 线程安全
     *
     * 必须在 ReadAction 中调用（由 IDE 框架保证）。
     *
     * @param project 当前项目
     * @param file 源虚拟文件
     * @param fileType 文件类型（此参数被忽略，始终使用仓颉文件类型）
     * @return 反编译后的 PSI 文件，如果创建失败则返回 null
     *
     * @see CjDecompiledFile
     * @see factory
     */
    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(this)
    }

    /**
     * 延迟加载的文档内容缓存
     *
     * ## 功能说明
     *
     * 该字段缓存反编译后的文本内容，避免重复执行昂贵的反编译操作。
     * 使用 [LockedClearableLazyValue] 提供线程安全的延迟初始化和缓存清除功能。
     *
     * ## 初始化流程
     *
     * 首次访问 `content.get()` 时执行以下步骤：
     * ```
     * 1. 调用 createFile() 创建临时 PSI 文件
     *    ↓
     * 2. 调用 psiFile.text 触发反编译
     *    ├─ PSI 文件内部调用 buildDecompiledText()
     *    ├─ 从 Stub 树生成源代码文本
     *    └─ 返回完整的反编译文本
     *    ↓
     * 3. 销毁临时 PSI 文件（markInvalidated）
     *    ↓
     * 4. 缓存文本内容并返回
     * ```
     *
     * ## 临时文件销毁
     *
     * 为什么需要销毁临时 PSI 文件？
     * - **内存优化**: 避免同一文件存在多个 PSI 副本
     * - **缓存一致性**: 确保 IDE 只缓存最终的 PSI 文件
     * - **避免混淆**: 防止临时文件参与代码分析
     *
     * 通过 `DebugUtil.performPsiModification` 在特殊上下文中执行销毁操作：
     * ```kotlin
     * DebugUtil.performPsiModification<PsiInvalidElementAccessException>(
     *     "Invalidating throw-away copy of file that was used for getting text"
     * ) {
     *     (psiFile as? PsiFileImpl)?.markInvalidated()
     * }
     * ```
     *
     * ## 缓存清除
     *
     * 当虚拟文件内容变化时（通过 [CjDecompiledFile.onContentReload]），
     * 调用 `content.drop()` 清除缓存，强制下次访问时重新反编译。
     *
     * ## 线程安全
     *
     * - **锁机制**: LockedClearableLazyValue 内部使用 synchronized 保护
     * - **单次初始化**: 并发访问时只执行一次初始化
     * - **原子操作**: drop() 操作是线程安全的
     *
     * ## 性能特点
     *
     * - **首次访问**: 慢（需要完整反编译，可能 50-200ms）
     * - **后续访问**: 快（直接返回缓存，< 1ms）
     * - **内存开销**: String 对象（通常 1-100KB）
     *
     * @see LockedClearableLazyValue
     * @see CjDecompiledFile.getText
     * @see CjDecompiledFile.onContentReload
     */
    val content: LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, CangJieFileType.INSTANCE)
        val text = psiFile?.text ?: ""

        DebugUtil.performPsiModification<PsiInvalidElementAccessException>("Invalidating throw-away copy of file that was used for getting text") {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }

        text
    }

    /**
     * 获取文档内容
     *
     * ## 功能说明
     *
     * 该方法是 [SingleRootFileViewProvider] 的核心回调，IDE 编辑器调用此方法获取显示内容。
     *
     * ## 工作流程
     *
     * 直接委托给 [content] 的延迟加载机制：
     * ```
     * getContents() 调用
     *   ↓
     * content.get()
     *   ├─ (首次) 执行初始化逻辑 → 返回反编译文本
     *   └─ (后续) 直接返回缓存文本
     * ```
     *
     * ## 使用场景
     *
     * - **编辑器显示**: 编辑器组件获取文本内容用于渲染
     * - **文本搜索**: 全文搜索功能访问文件内容
     * - **差异比较**: 文件比较工具获取文本
     *
     * ## 与 getText() 的区别
     *
     * - **getContents()**: 返回文档级别的文本（由 ViewProvider 提供）
     * - **psiFile.getText()**: 返回 PSI 级别的文本（由 PSI 文件提供）
     *
     * 对于反编译文件，这两者应该完全一致。
     *
     * ## 性能考虑
     *
     * 该方法可能被频繁调用，但由于 [content] 的缓存机制，性能开销很小。
     *
     * @return 反编译后的源代码文本
     *
     * @see content
     * @see CjDecompiledFile.getText
     */
    override fun getContents() = content.get()

    /**
     * 创建视图提供者的副本
     *
     * ## 功能说明
     *
     * 该方法在 IDE 需要复制文件或创建临时视图时调用，返回一个新的视图提供者实例。
     *
     * ## 使用场景
     *
     * - **文件复制**: 用户复制 .cjo 文件到另一位置
     * - **重命名**: 重命名文件时可能需要创建临时副本
     * - **本地历史**: IDE 创建本地历史记录时
     * - **差异查看**: 比较不同版本的文件时
     *
     * ## 实现细节
     *
     * 创建新实例时：
     * - **相同的 manager**: 复用同一个 PSI 管理器
     * - **新的 virtualFile**: 使用副本对应的虚拟文件
     * - **physical = false**: 标记为非物理文件（内存中的副本）
     * - **相同的 factory**: 复用相同的 PSI 文件创建逻辑
     *
     * ## 为什么 physical = false？
     *
     * 副本通常是临时的、内存中的视图，不对应磁盘上的真实文件：
     * - 文件复制操作可能被取消
     * - 重命名操作可能失败
     * - 差异查看的临时副本不应持久化
     *
     * ## 缓存独立性
     *
     * 新创建的副本拥有独立的 [content] 缓存：
     * - 副本和原始文件各自维护自己的反编译文本缓存
     * - 修改副本不影响原始文件
     *
     * @param copy 副本对应的虚拟文件
     * @return 新的视图提供者实例，与当前实例共享 factory 但拥有独立缓存
     *
     * @see factory
     * @see content
     */
    override fun createCopy(copy: VirtualFile) = CangJieDecompiledFileViewProvider(manager, copy, false, factory)


}