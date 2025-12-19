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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.workspaceModel.ide.legacyBridge.findLibraryEntity
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.projectStructure.CangJieSourceFilterScope
import org.cangnova.cangjie.projectStructure.scope.PoweredLibraryScopeBase
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices

/**
 * 库源码信息数据类
 *
 * 该类表示库的源码模块信息，实现了 [SourceForBinaryModuleInfo] 接口。
 * 它关联到一个二进制库模块，提供该库的原始源代码用于 IDE 导航和显示。
 *
 * ## 设计目的
 *
 * [LibrarySourceInfo] 为二进制库提供源码支持：
 * 1. **源码导航**: 用户跳转到库声明时，显示源码而非反编译代码
 * 2. **代码阅读**: 提供有注释、有原始格式的源代码
 * 3. **调试支持**: 在调试时关联到原始源码位置
 * 4. **依赖传递**: 继承二进制模块的依赖关系
 *
 * ## 核心特性
 *
 * ### 1. 源码作用域
 * - 使用 [LibrarySourceScope] 提供优化的源码文件查找
 * - 通过 [CangJieSourceFilterScope] 过滤源码文件
 * - 支持多源码集（commonMain、jvmMain 等）
 *
 * ### 2. 依赖继承
 * - 依赖关系直接继承自关联的二进制模块
 * - 内部可见性继承自二进制模块
 *
 * ### 3. 空内容作用域
 * - [contentScope] 返回空范围（源码不独立分析）
 * - 实际源码通过 [sourceScope] 访问
 *
 * ### 4. 性能优化
 * - 预计算的包名和文件系统信息
 * - 懒加载作用域
 * - 缓存源码查找结果
 *
 * ## 使用场景
 *
 * ### 创建库源码信息
 * ```kotlin
 * val binaryModule: LibraryInfo = getBinaryLibraryInfo(library)
 *
 * val sourceInfo = LibrarySourceInfo(
 *     project = project,
 *     library = library,
 *     binariesModuleInfo = binaryModule,
 *     topPackageNames = setOf("std", "cangjie", ""),
 *     entriesVirtualFileSystems = setOf(LocalFileSystem.getInstance())
 * )
 * ```
 *
 * ### 源码导航
 * ```kotlin
 * fun navigateToSource(
 *     binaryDeclaration: CjDeclaration,
 *     sourceInfo: LibrarySourceInfo
 * ) {
 *     // 获取源码作用域
 *     val sourceScope = sourceInfo.sourceScope()
 *
 *     // 在源码中查找声明
 *     val sourceDeclaration = findDeclaration(binaryDeclaration, sourceScope)
 *
 *     // 导航到源码
 *     if (sourceDeclaration != null) {
 *         navigateToElement(sourceDeclaration)
 *     }
 * }
 * ```
 *
 * ### 检查源码可用性
 * ```kotlin
 * val sourceInfo: LibrarySourceInfo = getSourceInfo(library)
 *
 * // 检查源码范围是否为空
 * val sourceScope = sourceInfo.sourceScope()
 * val hasFiles = !GlobalSearchScope.isEmptyScope(sourceScope)
 *
 * if (hasFiles) {
 *     println("源码可用")
 * } else {
 *     println("源码不可用")
 * }
 * ```
 *
 * ## 与其他类的关系
 *
 * - **LibraryInfo**: 二进制库模块（编译产物）
 * - **LibrarySourceInfo**: 二进制库的源码（原始代码）
 * - **SourceForBinaryModuleInfo**: 源码模块的通用接口
 *
 * ## 数据类特性
 *
 * 作为 `data class`，自动实现：
 * - `equals()`: 基于所有属性的相等性判断
 * - `hashCode()`: 基于所有属性的哈希码
 * - `toString()`: 覆盖为自定义格式
 * - `copy()`: 创建副本（带可选参数修改）
 *
 * ## 示例
 *
 * ### Maven 库的源码
 * ```
 * 依赖结构：
 * - mylib-1.0.jar (二进制，LibraryInfo)
 * - mylib-1.0-sources.jar (源码，LibrarySourceInfo)
 *
 * 关系：
 * sourceInfo.binariesModuleInfo = binaryInfo
 * binaryInfo.sourcesModuleInfo = sourceInfo
 * ```
 *
 * ### 标准库的源码
 * ```
 * 仓颉 SDK：
 * - sdk/lib/std.cjo (二进制，LibraryInfo)
 * - sdk/src/std/ (源码，LibrarySourceInfo)
 *
 * 导航流程：
 * 1. 用户点击标准库函数
 * 2. IDE 检查 binaryInfo.sourcesModuleInfo
 * 3. 找到 sourceInfo
 * 4. 在 sourceInfo.sourceScope() 中查找源码
 * 5. 显示源码文件
 * ```
 *
 * @param project IntelliJ 项目实例
 * @param library IntelliJ 库对象
 * @param binariesModuleInfo 关联的二进制模块信息
 * @param topPackageNames 预计算的顶层包名集合（用于性能优化）
 * @param entriesVirtualFileSystems 预计算的虚拟文件系统集合（用于性能优化）
 *
 * @see SourceForBinaryModuleInfo
 * @see LibraryInfo
 * @see BinaryModuleInfo
 */
data class LibrarySourceInfo(
    override val project: Project,
    val library: Library,
    override val binariesModuleInfo: BinaryModuleInfo,
    private val topPackageNames: Set<String>?,
    private val entriesVirtualFileSystems: Set<NewVirtualFileSystem>?
) :
    IdeaModuleInfo, SourceForBinaryModuleInfo {

    /**
     * 工作空间实体源
     *
     * 关联到 IntelliJ 工作空间模型的实体源，用于跟踪库的来源和变更。
     */
    val source: EntitySource? = library.findLibraryEntity(project.workspaceModel.currentSnapshot)?.entitySource

    /**
     * 模块名称
     *
     * 使用特殊名称格式 `<sources for library 库名>`，用于内部标识。
     */
    override val name: Name = Name.special("<sources for library ${library.name}>")

    /**
     * 显示名称
     *
     * 用于 UI 显示的源码模块名称，使用国际化消息格式 `<sources for library {0}>`。
     *
     * @see CangJieModuleInfoBundle
     */
    override val displayedName: String
        get() = CangJieModuleInfoBundle.message("sources.for.library.0", library.presentableName)

    /**
     * 获取源码文件的搜索作用域
     *
     * 返回包含此库所有源码文件的搜索范围，经过 [CangJieSourceFilterScope] 过滤。
     *
     * ## 特殊处理
     *
     *
     * [CangJieSourceFilterScope.librarySources] 会正确处理这种多源码集结构。
     *
     * ## 使用场景
     *
     * ```kotlin
     * val sourceScope = librarySourceInfo.sourceScope()
     *
     * // 在源码中查找文件
     * val files = FilenameIndex.getFilesByName(
     *     project,
     *     "MyClass.cj",
     *     sourceScope
     * )
     * ```
     *
     * @return 源码文件的全局搜索作用域
     * @see CangJieSourceFilterScope
     * @see LibrarySourceScope
     */
    override fun sourceScope(): GlobalSearchScope =
        // kotlin stdlib source.jar is known to pack multiple source-sets in the same jar as `.jar!/commonMain/*`, `.jar!/jvmMain/*` etc
      CangJieSourceFilterScope.librarySources(LibrarySourceScope(project, null, entriesVirtualFileSystems, library), project)

    /**
     * 可以访问此模块 internal 声明的模块列表
     *
     * 继承自二进制模块的内部可见性设置。
     * 通过 [LibraryInfoCache] 缓存查找结果以提升性能。
     *
     * ## 为什么继承？
     *
     * 源码模块和二进制模块是同一个库的不同表现形式，
     * 因此它们的内部可见性设置应该一致。
     *
     * @return 可以访问 internal 声明的模块集合
     * @see LibraryInfoCache
     */
    override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> {
        return LibraryInfoCache.getInstance(project)[library]
    }


    /**
     * 平台相关的分析服务
     *
     * 继承自关联的二进制模块的分析服务。
     *
     * ## 为什么继承？
     *
     * 源码和二进制是同一个库，应该使用相同的平台分析服务。
     *
     * @return 分析服务实例
     */
    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = binariesModuleInfo.analyzerServices

    /**
     * 返回源码信息的字符串表示
     *
     * 格式: `LibrarySourceInfo(libraryName=库名)`
     *
     * @return 调试用的字符串表示
     */
    override fun toString(): String = "LibrarySourceInfo(libraryName=${library.name})"

}

@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide 'calcHashCode()'
private class LibrarySourceScope(
    project: Project,
    topPackageNames: Set<String>?,
    entriesVirtualFileSystems: Set<NewVirtualFileSystem>?,
    private val library: Library,
) : PoweredLibraryScopeBase(
    project,
    VirtualFile.EMPTY_ARRAY,
    library.getFiles(OrderRootType.SOURCES),
    topPackageNames,
    entriesVirtualFileSystems
) {
    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getSourceRootForFile(file)
    override fun equals(other: Any?): Boolean = other is LibrarySourceScope && library == other.library
    override fun calcHashCode(): Int = library.hashCode()
    override fun toString(): String = "LibrarySourceScope($library)"
}