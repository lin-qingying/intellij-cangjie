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

package org.cangnova.cangjie.projectStructure.scope

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem

/**
 * 顶层包名提供者接口
 *
 * 该接口用于提供作用域中包含的所有顶层包名。
 * 顶层包名是指位于类路径根目录下的直接子目录名称，代表包结构的第一层。
 *
 * ## 使用场景
 *
 * - **包过滤**: 快速判断某个包是否在作用域内
 * - **性能优化**: 避免遍历整个文件系统来查找包
 * - **代码补全**: 提供包名的自动补全建议
 * - **导航**: 在包浏览器中显示可用的包
 *
 * ## 示例
 *
 * 对于以下目录结构：
 * ```
 * lib/
 *   std/
 *     collection/
 *   cangjie/
 *   myapp/
 * ```
 *
 * topPackageNames 应该返回: `["std", "cangjie", "myapp", ""]`
 * 注意：空字符串 "" 代表默认包（无包名的类）
 */
interface TopPackageNamesProvider {
    /**
     * 顶层包名集合
     *
     * 包含作用域中所有的顶层包名，加上空包名（""）。
     * 如果为 null，表示包含所有可能的包（不进行过滤）。
     *
     * @return 顶层包名集合，或 null（表示不限制）
     */
    val topPackageNames: Set<String>?
}
  fun Array<VirtualFile>.calculateTopPackageNames(): Set<String>? {
    if (isEmpty()) return null
    val topPackageNames = this.flatMap { it.children.toList() }
        .filter(VirtualFile::isDirectory)
        .map(VirtualFile::getName)
        .toSet()
    return topPackageNames + "" // empty package is always present
}


  fun Array<VirtualFile>.calculateEntriesVirtualFileSystems(): Set<NewVirtualFileSystem>? {
    val fileSystems = hashSetOf<NewVirtualFileSystem>()
    // optimization to use NewVirtualFileSystem is applicable iff all the library files are archives
    for (file in this) {
        val newVirtualFile = file as? NewVirtualFile ?: return null
        fileSystems.add(newVirtualFile.fileSystem)
    }
    if (fileSystems.isEmpty()) {
        // No roots case:
        // When we get an event from the workspace model that library was changed, vfs might have no file on disc, yet
        // at this point we might call current (`calculateEntriesVirtualFileSystems`) method and remember an empty set.
        // Later, when we create the scope based on such LibraryInfo, the cached set would be used together with calls to lib#getFiles()
        // which would return non-null values because changes were arrived to vfs.
        // So the cache file systems would result in wrong `contains`
        return null
    }
    return fileSystems
}
/**
 * 增强的库作用域基类
 *
 * 该类扩展了 IntelliJ Platform 的 [LibraryScopeBase]，添加了顶层包名的优化支持。
 *
 * ## 核心功能
 *
 * 1. **包名过滤**: 支持预先提供的包名列表进行快速过滤
 * 2. **自动包发现**: 如果未提供包名，自动扫描根目录提取顶层包名
 * 3. **懒加载**: 只在首次访问时计算包名，避免不必要的性能开销
 * 4. **缓存**: 计算后的包名会被缓存，避免重复计算
 *
 * ## 包名计算逻辑
 *
 * ```
 * 如果提供了 providedTopPackageNames:
 *   ↓
 * 直接使用提供的包名（高性能）
 *   ↓
 * 否则：
 *   ↓
 * 1. 遍历所有类文件根目录和源文件根目录
 * 2. 获取每个根目录的直接子目录
 * 3. 过滤出目录（`isDirectory`）
 * 4. 提取目录名作为包名
 * 5. 去重并添加空包名
 * ```
 *
 * ## 使用场景
 *
 * - **库作用域**: 表示单个库的文件范围
 * - **依赖分析**: 分析库中包含的包结构
 * - **索引优化**: 为索引系统提供包名信息
 * - **性能优化**: 通过预计算的包名避免文件系统扫描
 *
 * ## 继承关系
 *
 * ```
 * GlobalSearchScope (IntelliJ)
 *   ↓
 * LibraryScopeBase (IntelliJ)
 *   ↓
 * PoweredLibraryScopeBase (仓颉插件)
 *   ↓
 * LibraryWithoutSourceScope (具体实现)
 * ```
 *
 * @param project 当前项目
 * @param classes 类文件根目录数组（.cjo 文件等）
 * @param sources 源文件根目录数组（.cj 文件等）
 * @param providedTopPackageNames 预先提供的顶层包名集合（可选，用于性能优化）
 *
 * @see LibraryScopeBase
 * @see TopPackageNamesProvider
 * @see LibraryWithoutSourceScope
 */
  open class PoweredLibraryScopeBase(
    project: Project,
   val classes: Array<VirtualFile>,
    sources: Array<VirtualFile>,
    override val topPackageNames: Set<String>?,
    private val entriesVirtualFileSystems: Set<NewVirtualFileSystem>?
) : LibraryScopeBase(project, classes, sources), TopPackageNamesProvider {

    @Deprecated("Use the primary constructor", level = DeprecationLevel.HIDDEN)
    constructor(project: Project, classes: Array<VirtualFile>, sources: Array<VirtualFile>) : this(
        project,
        classes,
        sources,
        (classes + sources).calculateTopPackageNames(),
        (classes + sources).calculateEntriesVirtualFileSystems()
    )

    override fun contains(file: VirtualFile): Boolean {
        ((file as? NewVirtualFile)?.fileSystem)?.let {
            if (entriesVirtualFileSystems != null && !entriesVirtualFileSystems.contains(it)) {
                return false
            }
        }
        return super.contains(file)
    }
}

/**
 * 可组合的源码和类文件根作用域接口
 *
 * 该接口定义了能够暴露其根目录的作用域，使其可以与其他作用域组合。
 * 这是一个内部接口，用于优化多个作用域的组合查找。
 *
 * ## 设计目的
 *
 * 当需要在多个库作用域中查找文件时，将所有作用域的根目录合并到一个
 * [CombinedSourceAndClassRootsScope] 中，可以显著提升查找性能。
 *
 * ## 使用场景
 *
 * - **多库查找**: 同时在多个库中查找符号
 * - **性能优化**: 避免重复遍历多个作用域
 * - **作用域组合**: 创建复合作用域
 */
interface CombinableSourceAndClassRootsScope {
    /**
     * 根目录映射
     *
     * 映射从虚拟文件到其在内部索引中的位置。
     * 这个映射用于快速定位文件所属的根目录。
     *
     * @return 根目录到索引位置的映射
     */
    val roots: List<VirtualFile>

    /**
     * 相关的模块集合
     *
     * 对于纯库作用域，通常返回空集合。
     * 对于模块相关的作用域，返回相关联的模块。
     *
     * @return 模块集合，可能为空
     */
    val modules: Set<Module>

    /**
     * 是否包含库的类文件根目录
     *
     * @return true 如果包含类文件根目录
     */
    val includesLibraryClassRoots: Boolean

    /**
     * 是否包含库的源文件根目录
     *
     * @return true 如果包含源文件根目录
     */
    val includesLibrarySourceRoots: Boolean
}

