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

package org.cangnova.cangjie.cjo

import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.FqName

/**
 * CJO 文件扫描结果
 *
 * 包含解析后的包信息
 *
 * @property packageFqName 从 CJO 文件解析出的包名
 * @property file CJO/CJB 文件
 * @property packageWrapper 解析后的包装器对象
 * @property libraryName 所属库名（可选）
 */
data class CjoFileInfo(
    val packageFqName: FqName,
    val file: VirtualFile,
    val packageWrapper: PackageWrapper,
    val libraryName: String? = null
)

/**
 * CJO 库扫描器接口
 *
 * 用于扫描库的 classes 目录，查找并解析 CJO 文件。
 * 包名从 CJO 文件内部的元数据获取，而非从文件路径推断。
 *
 * 使用场景：
 * - 当库变更时（添加/移除/更新），重新扫描其中的 CJO 文件
 * - 初始化时扫描所有已加载的库
 *
 * @see com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
 */
interface CjoLibraryScanner {

    /**
     * 扫描库中的 CJO 文件
     *
     * 遍历库的 classes 目录，找到所有 CJO/CJB 文件，
     * 解析每个文件获取真正的包名。
     *
     * @param library 要扫描的库
     * @return 找到的 CJO 文件列表，包含解析后的包名
     */
    fun scanLibrary(library: Library): List<CjoFileInfo>

    /**
     * 扫描目录中的 CJO 文件
     *
     * @param root classes 根目录
     * @param libraryName 库名（可选）
     * @return 找到的 CJO 文件信息列表
     */
    fun scanDirectory(root: VirtualFile, libraryName: String? = null): List<CjoFileInfo>
}

/**
 * 默认的 CJO 库扫描器实现
 *
 * 扫描库的 classes 目录，解析 CJO 文件获取包元数据。
 */
class DefaultCjoLibraryScanner : CjoLibraryScanner {

    override fun scanLibrary(library: Library): List<CjoFileInfo> {
        val result = mutableListOf<CjoFileInfo>()

        // 获取库的 classes 根目录
        val classesRoots = library.getFiles(OrderRootType.CLASSES)

        for (root in classesRoots) {
            if (!root.isValid) continue
            val files = scanDirectory(root, library.name)
            result.addAll(files)
        }

        return result
    }

    override fun scanDirectory(root: VirtualFile, libraryName: String?): List<CjoFileInfo> {
        val result = mutableListOf<CjoFileInfo>()

        if (!root.isValid) return result

        // 处理两种情况：
        // 1. root 是一个 CJO 文件
        // 2. root 是一个目录
        if (!root.isDirectory) {
            // 如果是文件，检查是否是 CJO 文件并直接处理
            if (CjoFileLoader.isSupportedFile(root)) {
                val packageWrapper = CjoFileLoader.loadFromFile(root)
                if (packageWrapper != null) {
                    result.add(CjoFileInfo(
                        packageFqName = packageWrapper.packageName,
                        file = root,
                        packageWrapper = packageWrapper,
                        libraryName = libraryName
                    ))
                }
            }
            return result
        }

        // 递归扫描目录，收集所有 CJO 文件
        val cjoFiles = mutableListOf<VirtualFile>()
        collectCjoFiles(root, cjoFiles)

        // 解析每个 CJO 文件获取真正的包名
        for (file in cjoFiles) {
            val packageWrapper = CjoFileLoader.loadFromFile(file)
            if (packageWrapper != null) {
                result.add(CjoFileInfo(
                    packageFqName = packageWrapper.packageName,
                    file = file,
                    packageWrapper = packageWrapper,
                    libraryName = libraryName
                ))
            }
        }

        return result
    }

    /**
     * 递归收集目录中的 CJO 文件
     */
    private fun collectCjoFiles(current: VirtualFile, result: MutableList<VirtualFile>) {
        for (child in current.children) {
            if (child.isDirectory) {
                collectCjoFiles(child, result)
            } else if (CjoFileLoader.isSupportedFile(child)) {
                result.add(child)
            }
        }
    }

    companion object {
        val INSTANCE = DefaultCjoLibraryScanner()
    }
}

/**
 * 库变更回调接口
 *
 * 当工作空间中的库发生变更时调用
 */
interface LibraryChangeCallback {

    /**
     * 库被添加
     */
    fun onLibraryAdded(library: Library)

    /**
     * 库被移除
     */
    fun onLibraryRemoved(library: Library)

    /**
     * 库被更新
     */
    fun onLibraryUpdated(library: Library)
}
