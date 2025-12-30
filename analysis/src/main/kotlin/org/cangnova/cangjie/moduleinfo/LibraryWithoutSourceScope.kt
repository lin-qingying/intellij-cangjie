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

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.NewVirtualFileSystem
import org.cangnova.cangjie.projectStructure.scope.CombinableSourceAndClassRootsScope
import org.cangnova.cangjie.projectStructure.scope.PoweredLibraryScopeBase

/**
 * 不包含源码的库作用域
 *
 * 该类表示一个只包含编译后的类文件（.cjo）而不包含源代码的库作用域。
 * 这是最常见的库作用域类型，用于表示第三方库或已编译的标准库。
 *
 * ## 核心特性
 *
 * - **仅类文件**: 只包含库的类文件根目录，不包含源码
 * - **可组合**: 实现 [CombinableSourceAndClassRootsScope]，可与其他作用域组合
 * - **高效查找**: 通过内部索引快速定位文件所属的根目录
 * - **包名优化**: 支持预先提供的包名列表，避免文件系统扫描
 * - **相等性**: 基于库对象的相等性，而非文件内容
 *
 * ## 使用场景
 *
 * - **依赖库**: 项目依赖的外部库（只有编译后的文件）
 * - **标准库**: 仓颉语言的标准库（通常只有 .cjo 文件）
 * - **二进制库**: 只提供编译产物的闭源库
 * - **反编译导航**: 为反编译器提供精确的查找范围
 *
 * ## 与其他作用域的区别
 *
 * - **LibraryWithoutSourceScope**: 不包含源码（本类）
 * - **LibraryScope**: 可能包含源码
 * - **LibrarySourceScope**: 只包含源码
 *
 * ## 性能优化
 *
 * 通过 `topPackageNames` 参数提供预计算的包名列表，可以显著提升性能：
 * - 避免扫描文件系统
 * - 加速包查找和过滤
 * - 减少 I/O 操作
 *
 * ## 示例
 *
 * ```kotlin
 * // 创建不包含源码的库作用域（带包名优化）
 * val packageNames = setOf("std", "cangjie", "")
 * val scope = LibraryWithoutSourceScope(
 *     project = project,
 *     topPackageNames = packageNames,
 *     library = stdLibrary
 * )
 *
 * // 检查文件是否在作用域内
 * if (scope.contains(file)) {
 *     // 文件属于这个库
 * }
 *
 * // 获取文件的根目录
 * val root = scope.getFileRoot(file)
 * ```
 *
 * @param project 当前项目
 * @param topPackageNames 预先提供的顶层包名集合（可选，用于性能优化）
 * @param entriesVirtualFileSystems 虚拟文件系统集合（可选，用于性能优化）
 * @param library 关联的库对象
 *
 * @see PoweredLibraryScopeBase
 * @see CombinableSourceAndClassRootsScope
 */
@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
class LibraryWithoutSourceScope(
    project: Project,
    topPackageNames: Set<String>?,
    entriesVirtualFileSystems: Set<NewVirtualFileSystem>?,
    private val library: Library
) : PoweredLibraryScopeBase(
    project,
    library.getFiles(OrderRootType.CLASSES),
    VirtualFile.EMPTY_ARRAY,
    topPackageNames,
    entriesVirtualFileSystems
), CombinableSourceAndClassRootsScope {

    /**
     * 获取文件所属的根目录
     *
     * 通过内部索引快速查找给定文件所属的类文件根目录。
     *
     * ## 工作原理
     *
     * 使用 `myIndex`（继承自 LibraryScopeBase）来快速定位文件的根目录。
     * 索引在作用域创建时构建，包含所有类文件根目录的映射。
     *
     * ## 使用场景
     *
     * - **包推断**: 根据文件路径推断其包名
     * - **相对路径计算**: 计算文件相对于根目录的路径
     * - **文件分类**: 判断文件属于哪个库根目录
     *
     * @param file 要查找的文件
     * @return 文件所属的类文件根目录，如果文件不在此作用域内则返回 null
     */
    override fun getFileRoot(file: VirtualFile): VirtualFile? = myIndex.getClassRootForFile(file)

    /**
     * 根目录映射
     *
     * [LibraryWithoutSourceScope] 暴露其根目录，以便可以集成到
     * [CombinedSourceAndClassRootsScope] 中进行组合查找。
     *
     * ## 用途
     *
     * 当多个库作用域需要组合时，可以将所有根目录合并到一个统一的映射中，
     * 提升查找性能。
     *
     * @return 从虚拟文件到索引位置的映射
     */
    override val roots: List<VirtualFile> get() = classes.toList()

    /**
     * 相关模块集合
     *
     * 库作用域通常不直接关联到特定模块，而是被模块所依赖。
     * 因此返回空集合。
     *
     * @return 空的模块集合
     */
    override val modules: Set<Module> get() = emptySet()

    /**
     * 是否包含库的类文件根目录
     *
     * 此作用域只包含类文件，因此总是返回 true。
     *
     * @return true
     */
    override val includesLibraryClassRoots: Boolean get() = true

    /**
     * 是否包含库的源文件根目录
     *
     * 此作用域不包含源文件，因此总是返回 false。
     *
     * @return false
     */
    override val includesLibrarySourceRoots: Boolean get() = false

    /**
     * 判断与其他对象是否相等
     *
     * 两个 [LibraryWithoutSourceScope] 相等当且仅当它们关联的库对象相同。
     * 这意味着即使文件内容不同，只要是同一个库对象，就认为作用域相等。
     *
     * ## 设计考虑
     *
     * 基于库对象而非文件内容的相等性判断更加高效，避免了比较大量文件。
     * 这在缓存和查找优化中非常重要。
     *
     * @param other 要比较的对象
     * @return true 如果两个作用域关联的库相同
     */
    override fun equals(other: Any?): Boolean = other is LibraryWithoutSourceScope && library == other.library

    /**
     * 计算哈希码
     *
     * 哈希码基于关联的库对象计算，与 [equals] 方法保持一致。
     *
     * ## 为什么需要 calcHashCode？
     *
     * [DelegatingGlobalSearchScope] 要求子类提供 `calcHashCode()` 方法
     * 而非覆盖 `hashCode()`，以支持委托模式下的哈希码计算。
     *
     * @return 基于库对象的哈希码
     */
    override fun calcHashCode(): Int = library.hashCode()

    /**
     * 返回作用域的字符串表示
     *
     * 用于调试和日志输出。
     *
     * @return 作用域的描述字符串，包含库信息
     */
    override fun toString(): String = "LibraryWithoutSourceScope($library)"
}
