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

package org.cangnova.cangjie.projectStructure.scope

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile

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
internal open class PoweredLibraryScopeBase(
    project: Project,
    val classes: Array<VirtualFile>,
    sources: Array<VirtualFile>,
    private val providedTopPackageNames: Set<String>? = null
) :
    LibraryScopeBase(project, classes, sources), TopPackageNamesProvider {

    /**
     * 顶层包名集合
     *
     * ## 优化策略
     *
     * 1. 如果构造时提供了 `providedTopPackageNames`，直接使用（高性能）
     * 2. 否则，首次访问时自动计算并缓存（延迟计算）
     *
     * ## 自动计算步骤（仅当未提供 providedTopPackageNames 时）
     *
     * 1. 合并类文件和源文件根目录数组
     * 2. 遍历每个根目录的子文件/目录
     * 3. 过滤出目录（`isDirectory`）
     * 4. 提取目录名称作为包名
     * 5. 去重并添加空包名
     *
     * ## 示例
     *
     * 假设库包含以下结构：
     * ```
     * classes/
     *   std/
     *   cangjie/
     * sources/
     *   std/
     *   mylib/
     * ```
     *
     * 返回: `["std", "cangjie", "mylib", ""]`
     *
     * @return 包含所有顶层包名的不可变集合，或 null（表示不进行包过滤）
     */
    override val topPackageNames: Set<String>? by lazy {
        providedTopPackageNames ?: run {
            (classes + sources)
                .flatMap { it.children.toList() }  // 获取所有根目录的子文件/目录
                .filter(VirtualFile::isDirectory)  // 只保留目录
                .map(VirtualFile::getName)         // 提取目录名
                .toSet() + ""                      // 去重并添加空包名
        }
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
    val roots: Set<VirtualFile>

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
 * @param library 关联的库对象
 *
 * @see PoweredLibraryScopeBase
 * @see CombinableSourceAndClassRootsScope
 */
@Suppress("EqualsOrHashCode") // DelegatingGlobalSearchScope requires to provide calcHashCode()
private class LibraryWithoutSourceScope(
    project: Project,
    topPackageNames: Set<String>? = null,
    private val library: Library
) : PoweredLibraryScopeBase(
    project,
    library.getFiles(OrderRootType.CLASSES),  // 只获取类文件根目录
    VirtualFile.EMPTY_ARRAY,                   // 源文件数组为空
    topPackageNames                            // 传递包名优化参数
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
    override val roots: Set<VirtualFile> get() = classes.toSet()

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