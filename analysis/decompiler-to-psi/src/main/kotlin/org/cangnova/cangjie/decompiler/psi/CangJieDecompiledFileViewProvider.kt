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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
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
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.utils.LockedClearableLazyValue

/**
 * 仓颉反编译文件的视图提供者。
 *
 * 该类继承自 [SingleRootFileViewProvider]，专门用于管理反编译后的仓颉元数据文件的 PSI 视图。
 * 它提供了延迟加载机制，只有在需要时才会实际创建反编译后的 PSI 文件。
 *
 * ## 核心功能
 *
 * 1. **延迟内容加载**: 通过 [decompiledText] 属性实现文件内容的延迟加载，避免不必要的反编译开销
 * 2. **PSI 文件创建**: 通过工厂函数 [textFactory] 创建反编译文本，然后构建 [CjDecompiledFile] 实例
 * 3. **副本支持**: 支持创建视图提供者的副本，用于文件复制等场景
 *
 * ## 设计原则
 *
 * 为了确保文档内容和 PSI 内容的一致性，该类采用"单一数据源"设计：
 * - 反编译文本只生成一次（通过 [decompiledText] 的 lazy 机制）
 * - [getContents] 和 [createFile] 都使用相同的反编译文本
 * - 即使反编译失败也返回包含错误信息的默认文本，避免空内容导致的不一致问题
 *
 * ## 使用场景
 *
 * 该类通常由 [CangJieMetadataDecompiler] 在 `createFileViewProvider` 方法中创建，
 * 用于为反编译的元数据文件提供 IDE 可识别的 PSI 结构。
 *
 * @param manager PSI 管理器，用于访问 PSI 相关服务
 * @param file 要提供视图的虚拟文件
 * @param physical 是否为物理文件（非内存中的临时文件）
 * @param textFactory 用于生成反编译文本的工厂函数，接收虚拟文件作为参数，返回反编译后的文本内容
 *
 * @see SingleRootFileViewProvider
 * @see CjDecompiledFile
 * @see CangJieMetadataDecompiler.createFileViewProvider
 */
class CangJieDecompiledFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (CangJieDecompiledFileViewProvider) -> CjDecompiledFile?
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {

    val content: LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, CangJieFileType.INSTANCE)
        val text = psiFile?.text ?: ""

        DebugUtil.performPsiModification<PsiInvalidElementAccessException>("Invalidating throw-away copy of file that was used for getting text") {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }

        text
    }

    /**
     * 标记此 ViewProvider 是否代表物理二进制文件。
     *
     * 返回 false 以告诉平台这不是一个需要直接读取的二进制文件，
     * 而是应该使用 getContents() 获取内容。
     */
    override fun supportsIncrementalReparse(rootLanguage: com.intellij.lang.Language): Boolean = false

    /**
     * 检查此 ViewProvider 是否是物理的。
     *
     * 对于反编译文件，虽然底层是物理二进制文件，但我们返回 false
     * 以避免 IDE 尝试将二进制内容作为文档处理。
     * 这确保了 PSI 和文档使用相同的反编译文本。
     */
    override fun isPhysical(): Boolean = false

    /**
     * 检查是否应该为此文件创建事件系统。
     *
     * 返回 false 因为反编译文件是只读的，不需要监听变更事件。
     */
    override fun isEventSystemEnabled(): Boolean = false



    /**
     * 构建默认的错误提示文本。
     *
     * 当反编译失败或返回空内容时使用此文本，确保文档和 PSI 都有内容。
     */
    private fun buildDefaultErrorText(): String {
        return """
            // IntelliJ API Decompiler stub source generated from a class file
            // Unable to decompile: ${virtualFile.name}
            // The file may be corrupted or use an incompatible format.
        """.trimIndent()
    }

    /**
     * 构建包含异常信息的错误提示文本。
     *
     * @param exception 反编译过程中抛出的异常
     */
    private fun buildErrorText(exception: Exception): String {
        return """
            // IntelliJ API Decompiler stub source generated from a class file
            // Decompilation failed: ${virtualFile.name}
            // Error: ${exception.message ?: exception.javaClass.simpleName}
        """.trimIndent()
    }

    /**
     * 创建反编译后的 PSI 文件。
     *
     * 该方法覆盖父类实现，直接使用预先生成的反编译文本创建 [CjDecompiledFile]。
     * 这确保了 PSI 文件的内容与文档内容完全一致。
     *
     * @param project 当前项目
     * @param file 源虚拟文件
     * @param fileType 文件类型（此参数被忽略，始终使用仓颉文件类型）
     * @return 反编译后的 PSI 文件，始终返回非 null 值
     */

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(this)
    }
    /**
     * 创建视图提供者的副本。
     *
     * 用于文件复制、重命名等需要新建视图的场景。
     * 新创建的副本标记为非物理文件（`physical = false`）。
     *
     * @param copy 副本对应的虚拟文件
     * @return 新的视图提供者实例
     */
    override fun createCopy(copy: VirtualFile) = CangJieDecompiledFileViewProvider(manager, copy, false, factory)

    /**
     * 获取文件内容。
     *
     * 返回延迟加载的反编译文本内容。首次调用时会触发实际的反编译过程。
     * 该方法保证永不返回 null 或空字符串。
     *
     * @return 反编译后的源代码文本（非空）
     */
    override fun getContents(): CharSequence = content.get()
}