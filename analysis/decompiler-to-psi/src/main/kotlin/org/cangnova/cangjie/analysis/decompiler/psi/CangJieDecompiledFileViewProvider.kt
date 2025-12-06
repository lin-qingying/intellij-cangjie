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

package org.cangnova.cangjie.analysis.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.PsiFileImpl
import org.cangnova.cangjie.analysis.decompiler.psi.file.CjDecompiledFile
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
 * 1. **延迟内容加载**: 通过 [content] 属性实现文件内容的延迟加载，避免不必要的反编译开销
 * 2. **PSI 文件创建**: 通过工厂函数 [factory] 创建 [CjDecompiledFile] 实例
 * 3. **副本支持**: 支持创建视图提供者的副本，用于文件复制等场景
 *
 * ## 内存优化
 *
 * 在获取文件文本内容后，会将临时创建的 PSI 文件标记为失效，
 * 以便及时释放内存资源。这对于处理大量反编译文件时尤为重要。
 *
 * ## 使用场景
 *
 * 该类通常由 [CangJieMetadataDecompiler] 在 `createFileViewProvider` 方法中创建，
 * 用于为反编译的元数据文件提供 IDE 可识别的 PSI 结构。
 *
 * @param manager PSI 管理器，用于访问 PSI 相关服务
 * @param file 要提供视图的虚拟文件
 * @param physical 是否为物理文件（非内存中的临时文件）
 * @param factory 用于创建反编译 PSI 文件的工厂函数，接收项目和当前视图提供者作为参数
 *
 * @see SingleRootFileViewProvider
 * @see CjDecompiledFile
 * @see CangJieMetadataDecompiler.createFileViewProvider
 */
class CangJieDecompiledFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (Project, CangJieDecompiledFileViewProvider) -> CjDecompiledFile?
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {

    /**
     * 延迟加载的文件内容。
     *
     * 使用 [LockedClearableLazyValue] 实现线程安全的延迟初始化和可清除特性。
     *
     * ## 初始化过程
     *
     * 1. 调用 [createFile] 创建临时 PSI 文件
     * 2. 获取 PSI 文件的文本内容
     * 3. 将临时 PSI 文件标记为失效，释放内存
     * 4. 返回文本内容供后续使用
     *
     * 这种设计确保了反编译文本只计算一次，同时避免持有不必要的 PSI 文件引用。
     */
    val content: LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, CangJieFileType.INSTANCE)
        val text = psiFile?.text ?: ""

        // 使临时创建的 PSI 文件副本失效，释放内存资源
        DebugUtil.performPsiModification<PsiInvalidElementAccessException>("Invalidating throw-away copy of file that was used for getting text") {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }

        text
    }

    /**
     * 创建反编译后的 PSI 文件。
     *
     * 该方法覆盖父类实现，使用构造时传入的 [factory] 函数来创建 [CjDecompiledFile]。
     * 工厂函数负责实际的反编译逻辑，包括元数据解析和文本生成。
     *
     * @param project 当前项目
     * @param file 源虚拟文件
     * @param fileType 文件类型（此参数被忽略，始终使用仓颉文件类型）
     * @return 反编译后的 PSI 文件，如果反编译失败则返回 `null`
     */
    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(project, this)
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
     *
     * @return 反编译后的源代码文本
     */
    override fun getContents() = content.get()
}