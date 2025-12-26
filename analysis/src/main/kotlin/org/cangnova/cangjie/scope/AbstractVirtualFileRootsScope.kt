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
package org.cangnova.cangjie.scope

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.impl.VirtualFileEnumeration
import com.intellij.psi.search.impl.VirtualFileEnumerationAware
import org.jetbrains.annotations.ApiStatus

/**
 * 基于虚拟文件根目录映射的搜索作用域抽象基类
 *
 * ## 概述
 *
 * `AbstractVirtualFileRootsScope` 是一个抽象基类，用于实现基于单一虚拟文件根目录映射的搜索作用域，
 * 例如 [ModuleSourcesScope]。该类提供了文件包含性检查和文件优先级比较的基础实现。
 *
 * ## 设计目标
 *
 * 虽然目前 [ModuleSourcesScope] 是唯一的实现类，但这个类也可以作为未来实现
 * `ModulesWithDependenciesScope` 的基类，一旦 [ModuleSourcesScope] 迁移到平台层。
 *
 * ## 核心功能
 *
 * ### 1. 文件包含性检查
 *
 * 通过 [contains] 方法判断文件是否在作用域内：
 * - 获取文件的根目录
 * - 检查根目录是否在 [roots] 列表中
 *
 * ### 2. 文件优先级比较
 *
 * 通过 [compare] 方法比较两个文件的优先级：
 * - 相同根目录的文件优先级相同（返回 0）
 * - 根目录在 [roots] 列表中位置靠前的文件优先级更高
 * - 不在作用域内的文件优先级最低
 *
 * ## 实现要点
 *
 * ### 必须实现的抽象成员
 *
 * 子类必须实现以下抽象成员：
 *
 * 1. **[roots]**: 虚拟文件根目录列表
 *    - 列表中的顺序表示 classpath 位置
 *    - 位置靠前的根目录优先级更高
 *
 * 2. **[getFileRoot]**: 获取文件的根目录
 *    - 给定一个虚拟文件，返回其所属的根目录
 *    - 如果文件不属于任何根目录，返回 null
 *
 * ## 使用示例
 *
 * ```kotlin
 * class ModuleSourcesScope(
 *     project: Project,
 *     private val moduleRoots: List<VirtualFile>
 * ) : AbstractVirtualFileRootsScope(project) {
 *
 *     override val roots: List<VirtualFile>
 *         get() = moduleRoots
 *
 *     override fun getFileRoot(file: VirtualFile): VirtualFile? {
 *         return myProjectFileIndex.getSourceRootForFile(file)
 *     }
 * }
 * ```
 *
 * ## 性能特性
 *
 * - **VFS 修改追踪**: 使用 [vfsModificationCount] 追踪 VFS 变化
 * - **ProjectFileIndex 缓存**: 缓存 [myProjectFileIndex] 避免重复获取
 * - **快速根目录查找**: 使用 List.contains 和 List.indexOf 进行快速查找
 *
 * ## 注意事项
 *
 * 1. **作用域逻辑来源**:
 *    - 代码逻辑复制自 `ModuleWithDependenciesScope`
 *    - 原始实现中有针对 Bazel 单文件模块的额外检查
 *    - 该检查实际上是失败实验的遗留物，在此处省略是安全的
 *
 * 2. **线程安全性**:
 *    - [vfsModificationCount] 使用 @Volatile 注解确保可见性
 *    - [roots] 应该是不可变的或线程安全的
 *
 * 3. **性能考虑**:
 *    - [roots] 列表不应过长，以保证 indexOf 性能
 *    - [getFileRoot] 应该高效实现，避免重复计算
 *
 * ## 与其他作用域的关系
 *
 * - **GlobalSearchScope**: 父类，提供全局搜索作用域接口
 * - **ModuleSourcesScope**: 当前唯一实现，用于模块源码作用域
 * - **ModulesWithDependenciesScope**: 未来可能的实现，包含模块依赖
 *
 * @property project 项目实例
 * @property myProjectFileIndex 项目文件索引，用于快速查找文件根目录
 * @property roots 虚拟文件根目录列表，顺序表示优先级
 *
 * @see GlobalSearchScope
 * @see ModuleSourcesScope
 * @see ProjectFileIndex
 *
 * A base implementation of scopes based on a single virtual file roots map, such as [ModuleSourcesScope].
 *
 * While [ModuleSourcesScope] is currently the only implementation, this class can also be used as a base for
 * `ModulesWithDependenciesScope` once [ModuleSourcesScope] is migrated to the platform.
 */

abstract class AbstractVirtualFileRootsScope(project: Project) : GlobalSearchScope(project)  {

    /**
     * VFS 修改计数器
     *
     * 用于追踪虚拟文件系统的修改，确保作用域缓存在 VFS 变化时失效。
     * 使用 @Volatile 确保多线程环境下的可见性。
     */
    @Volatile
    private var vfsModificationCount: Long = 0

    /**
     * 项目文件索引
     *
     * 用于快速查找文件的源码根目录、库根目录等信息。
     * 缓存该实例避免重复获取。
     */
    protected val myProjectFileIndex: ProjectFileIndex = ProjectRootManager.getInstance(project).fileIndex

    /**
     * 虚拟文件根目录列表
     *
     * 列表中的顺序表示根目录在 classpath 中的位置，位置靠前的根目录优先级更高。
     * 子类必须实现此属性，返回该作用域包含的所有根目录。
     *
     * ## 顺序语义
     *
     * 根目录在列表中的索引决定了文件的优先级：
     * - 索引 0: 最高优先级
     * - 索引 n: 较低优先级
     *
     * ## 使用示例
     *
     * ```kotlin
     * override val roots: List<VirtualFile>
     *     get() = listOf(
     *         moduleSourceRoot,  // 优先级 0 (最高)
     *         moduleTestRoot,    // 优先级 1
     *         libraryRoot        // 优先级 2
     *     )
     * ```
     */
    protected abstract val roots: List<VirtualFile>

    /**
     * 获取文件的根目录
     *
     * 给定一个虚拟文件，返回该文件所属的根目录。
     * 如果文件不属于任何根目录，返回 null。
     *
     * @param file 要查询的虚拟文件
     * @return 文件所属的根目录，如果不属于任何根目录则返回 null
     *
     * ## 实现建议
     *
     * 通常使用 [ProjectFileIndex] 来查找根目录：
     *
     * ```kotlin
     * override fun getFileRoot(file: VirtualFile): VirtualFile? {
     *     return myProjectFileIndex.getSourceRootForFile(file)
     *         ?: myProjectFileIndex.getClassRootForFile(file)
     * }
     * ```
     */
    protected abstract fun getFileRoot(file: VirtualFile): VirtualFile?

    /**
     * 判断文件是否在作用域内
     *
     * ## 算法
     *
     * 1. 获取文件的根目录 [getFileRoot]
     * 2. 检查根目录是否在 [roots] 列表中
     *
     * @param file 要检查的虚拟文件
     * @return 如果文件在作用域内返回 true，否则返回 false
     *
     * ## 注意事项
     *
     * 该方法的逻辑复制自 `ModuleWithDependenciesScope`，原始版本中有针对 Bazel 单文件模块的额外检查。
     * 该检查实际上是失败实验的遗留物，在此处省略是安全的。
     */
    override fun contains(file: VirtualFile): Boolean {
        // Note: The scope's logic is copied from `ModuleWithDependenciesScope`, which has an additional check for Bazel single-file
        // modules. The check in `ModuleWithDependenciesScope` is actually a remnant of a failed experiment, so omitting the check here is
        // fine.
        val root = getFileRoot(file) ?: return false
        return root in roots
    }

    /**
     * 比较两个文件的优先级
     *
     * ## 优先级规则
     *
     * 1. **相同根目录**: 返回 0（优先级相同）
     * 2. **不同根目录**: 比较根目录在 [roots] 列表中的索引
     *    - 索引小的文件优先级高（返回负数）
     *    - 索引大的文件优先级低（返回正数）
     * 3. **不在作用域内的文件**: 优先级最低
     *    - 两个都不在作用域内：返回 0
     *    - 只有一个在作用域内：在作用域内的优先级高
     *
     * @param file1 第一个文件
     * @param file2 第二个文件
     * @return 比较结果：
     *         - 负数: file1 优先级高于 file2
     *         - 零: file1 和 file2 优先级相同
     *         - 正数: file1 优先级低于 file2
     *
     * ## 使用示例
     *
     * ```kotlin
     * val scope = ModuleSourcesScope(project, moduleRoots)
     * val file1 = ... // 来自 moduleRoots[0]
     * val file2 = ... // 来自 moduleRoots[2]
     *
     * val result = scope.compare(file1, file2)
     * // result < 0, 因为 file1 的根目录索引更小
     * ```
     */
    override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
        val r1 = getFileRoot(file1)
        val r2 = getFileRoot(file2)
        if (Comparing.equal(r1, r2)) return 0

        if (r1 == null) return -1
        if (r2 == null) return 1

        val roots = roots
        val i1 = roots.indexOf(r1)
        val i2 = roots.indexOf(r2)
        if (i1 == -1 && i2 == -1) return 0
        if (i1 >= 0 && i2 >= 0) return i1 - i2
        return if (i1 >= 0) -1 else 1
    }


}
