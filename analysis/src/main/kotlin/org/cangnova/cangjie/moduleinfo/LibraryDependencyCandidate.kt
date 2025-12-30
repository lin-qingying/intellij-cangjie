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

/**
 * 库依赖候选项
 *
 * 该密封类表示模块的库依赖候选项。它封装了一组相关的库信息，
 * 用于依赖解析和管理。
 *
 * ## 设计目的
 *
 * [LibraryDependencyCandidate] 是依赖管理系统的核心抽象：
 * 1. **依赖封装**: 将一组相关的库封装为一个依赖单元
 * 2. **多实现支持**: 作为密封类，支持不同类型的库依赖
 * 3. **类型安全**: 提供类型安全的库依赖表示
 * 4. **可扩展性**: 可以添加新的依赖类型而不破坏现有代码
 *
 * ## 核心概念
 *
 * ### 为什么是"候选项"？
 *
 * 在依赖解析过程中，一个库可能有多种版本或多种形式：
 * - 项目级库 vs 模块级库
 * - 不同版本的同一个库
 * - 去重前的库实例
 *
 * "候选项"表示这些可能的选择，最终会被解析为实际使用的库。
 *
 * ### 为什么 libraries 是列表？
 *
 * 一个依赖候选项可能包含多个 [LibraryInfo]：
 * - **库去重**: 相同内容的库可能有多个实例，都包含在同一个候选项中
 * - **多平台库**: 一个逻辑库可能对应多个物理库文件
 * - **分片库**: 大型库可能被分成多个部分
 *
 * ## 使用场景
 *
 * ### 依赖解析
 * ```kotlin
 * fun resolveModuleDependencies(module: Module): List<LibraryInfo> {
 *     val candidates = collectDependencyCandidates(module)
 *
 *     // 从候选项中提取所有库
 *     return candidates.flatMap { it.libraries }
 * }
 * ```
 *
 * ### 依赖去重
 * ```kotlin
 * fun deduplicateDependencies(
 *     candidates: List<LibraryDependencyCandidate>
 * ): List<LibraryInfo> {
 *     val seen = mutableSetOf<LibraryInfo>()
 *     return candidates.flatMap { it.libraries }
 *         .filter { seen.add(it) } // 去重
 * }
 * ```
 *
 * ### 依赖分析
 * ```kotlin
 * fun analyzeDependencies(module: Module) {
 *     val candidates = getDependencyCandidates(module)
 *
 *     for (candidate in candidates) {
 *         println("依赖候选项包含 ${candidate.libraries.size} 个库:")
 *         for (library in candidate.libraries) {
 *             println("  - ${library.name}")
 *         }
 *     }
 * }
 * ```
 *
 * ## 实现类型
 *
 * ### DefaultLibraryDependencyCandidate
 * 默认的库依赖候选项实现，用于表示标准的库依赖。
 *
 * ## 伴生对象方法
 *
 * ### fromLibraryOrNull
 * 工厂方法，从库信息列表创建依赖候选项。如果列表为空，返回 null。
 *
 * ## 与其他类的关系
 *
 * - **LibraryInfo**: 候选项包含的库信息
 * - **LibraryDependenciesCache**: 使用候选项来缓存模块的库依赖
 * - **ModuleDependencyCollector**: 收集模块的依赖候选项
 *
 * @see LibraryInfo
 * @see DefaultLibraryDependencyCandidate
 * @see org.cangnova.cangjie.moduleinfo.cache.LibraryDependenciesCache
 */
sealed class LibraryDependencyCandidate {
    /**
     * 候选项包含的库信息列表
     *
     * 包含此依赖候选项中的所有库。通常情况下，列表中的库具有相同的内容，
     * 但可能是不同的实例（用于去重）。
     *
     * ## 列表特性
     *
     * - **非空**: 候选项应该至少包含一个库
     * - **顺序**: 列表的顺序可能影响依赖解析的优先级
     * - **去重**: 列表中可能包含内容相同但实例不同的库
     *
     * @return 库信息的列表
     */
    abstract val libraries: List<LibraryInfo>

    companion object {
        /**
         * 从库信息列表创建依赖候选项
         *
         * 工厂方法，用于从一个或多个 [LibraryInfo] 创建 [LibraryDependencyCandidate]。
         * 如果列表为空，返回 null。
         *
         * ## 使用场景
         *
         * ```kotlin
         * // 从单个库创建候选项
         * val library: LibraryInfo = getLibrary()
         * val candidate = LibraryDependencyCandidate.fromLibraryOrNull(listOf(library))
         *
         * // 从库列表创建候选项（去重场景）
         * val libraries: List<LibraryInfo> = getDeduplicatedLibraries()
         * val candidate = LibraryDependencyCandidate.fromLibraryOrNull(libraries)
         *
         * // 处理空列表
         * val emptyList: List<LibraryInfo> = emptyList()
         * val nullCandidate = LibraryDependencyCandidate.fromLibraryOrNull(emptyList)
         * // nullCandidate == null
         * ```
         *
         * ## 实现说明
         *
         * 目前总是返回 [DefaultLibraryDependencyCandidate]。
         * 在未来可能根据库的类型返回不同的候选项实现。
         *
         * @param libraryInfos 库信息列表
         * @return 依赖候选项，如果列表为空则返回 null
         * @see DefaultLibraryDependencyCandidate
         */
        fun fromLibraryOrNull(libraryInfos: List<LibraryInfo>): LibraryDependencyCandidate? {
            val libraryInfo = libraryInfos.firstOrNull() ?: return null
            return    DefaultLibraryDependencyCandidate(
                libraries = libraryInfos
            )

        }
    }
}

/**
 * 默认的库依赖候选项
 *
 * 该数据类是 [LibraryDependencyCandidate] 的标准实现，用于表示普通的库依赖。
 *
 * ## 设计目的
 *
 * [DefaultLibraryDependencyCandidate] 提供了最基本的库依赖候选项实现：
 * 1. **简单封装**: 直接封装库信息列表
 * 2. **数据类特性**: 自动提供 equals、hashCode、toString 和 copy
 * 3. **通用适用**: 适用于大多数库依赖场景
 *
 * ## 数据类特性
 *
 * 作为 `data class`，自动实现：
 * - `equals()`: 基于 libraries 列表的相等性
 * - `hashCode()`: 基于 libraries 列表的哈希码
 * - `toString()`: 生成包含 libraries 的字符串表示
 * - `copy()`: 创建副本（可选择修改 libraries）
 *
 * ## 使用场景
 *
 * ### 直接创建
 * ```kotlin
 * val libraries = listOf(stdlibInfo, runtimeInfo)
 * val candidate = DefaultLibraryDependencyCandidate(libraries)
 * ```
 *
 * ### 通过工厂方法创建（推荐）
 * ```kotlin
 * val libraries = listOf(stdlibInfo, runtimeInfo)
 * val candidate = LibraryDependencyCandidate.fromLibraryOrNull(libraries)
 * // candidate is DefaultLibraryDependencyCandidate
 * ```
 *
 * ### 访问库列表
 * ```kotlin
 * val candidate: DefaultLibraryDependencyCandidate = ...
 * for (library in candidate.libraries) {
 *     println("依赖库: ${library.name}")
 * }
 * ```
 *
 * ### 相等性比较
 * ```kotlin
 * val candidate1 = DefaultLibraryDependencyCandidate(listOf(lib1, lib2))
 * val candidate2 = DefaultLibraryDependencyCandidate(listOf(lib1, lib2))
 *
 * // 因为是数据类，基于内容相等
 * println(candidate1 == candidate2) // true
 * ```
 *
 * ## 与其他类的关系
 *
 * - **LibraryDependencyCandidate**: 父类，定义了候选项的基本契约
 * - **LibraryInfo**: 候选项包含的库信息
 * - **LibraryDependenciesCache**: 缓存中存储的候选项类型
 *
 * @param libraries 依赖候选项包含的库信息列表
 *
 * @see LibraryDependencyCandidate
 * @see LibraryInfo
 */
data class DefaultLibraryDependencyCandidate(
    override val libraries: List<LibraryInfo>
) : LibraryDependencyCandidate()
