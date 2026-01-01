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
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.EntitySource


/**
 * 库信息实现类
 *
 * 该类是 [LibraryInfo] 的具体实现，用于表示项目中的库模块。
 * 它继承了 [LibraryInfo] 的所有功能，并添加了工作空间实体源的跟踪。
 *
 * ## 设计目的
 *
 * [LibraryInfoImpl] 作为仓颉语言插件中库模块的标准实现：
 * 1. **统一实现**: 所有库都使用这个统一的实现类
 * 2. **工作空间集成**: 关联到 IntelliJ 工作空间模型，支持增量同步
 * 3. **实体源跟踪**: 记录库的来源（项目、模块、外部等）
 * 4. **缓存支持**: 通过 [org.cangnova.cangjie.moduleinfo.cache.LibraryInfoCache] 进行缓存和去重
 *
 * ## 核心特性
 *
 * ### 1. 工作空间实体源
 * - 关联到 IntelliJ 工作空间模型的实体源
 * - 用于跟踪库的来源和变更
 * - 支持增量更新和智能缓存失效
 *
 * ### 2. 内部构造函数
 * - 构造函数被标记为 `internal`，只能通过 [org.cangnova.cangjie.moduleinfo.cache.LibraryInfoCache] 创建
 * - 确保所有库信息都经过缓存，避免重复实例
 *
 * ## 使用场景
 *
 * ### 通过缓存获取库信息（推荐）
 * ```kotlin
 * val library: Library = getLibrary()
 * val libraryInfos = LibraryInfoCache.getInstance(project)[library]
 * val libraryInfo = libraryInfos.first() as LibraryInfoImpl
 *
 * // 访问工作空间实体源
 * val source = libraryInfo.source
 * println("库来源: $source")
 * ```
 *
 * ### 检查库的来源
 * ```kotlin
 * fun printLibrarySource(libraryInfo: LibraryInfoImpl) {
 *     when (val source = libraryInfo.source) {
 *         is JpsProjectFileEntitySource -> println("来自项目文件")
 *         is JpsGlobalFileEntitySource -> println("来自全局配置")
 *         is JpsFileDependentEntitySource -> println("来自外部依赖")
 *         else -> println("来源未知或库已过时")
 *     }
 * }
 * ```
 *
 * ## 与其他类的关系
 *
 * - **LibraryInfo**: 父类，提供库的核心功能
 * - **LibraryInfoCache**: 工厂类，负责创建和缓存 LibraryInfoImpl 实例
 * - **IdeaModuleInfo**: 祖先接口，提供 IDE 特定的模块信息
 * - **BinaryModuleInfo**: 祖先接口，支持二进制模块特性
 *
 * ## 实现说明
 *
 * ### 为什么使用 internal 构造函数？
 *
 * 1. **强制使用缓存**: 确保所有库信息都通过 LibraryInfoCache 获取
 * 2. **避免重复**: 相同的库只会有一个 LibraryInfoImpl 实例
 * 3. **性能优化**: 减少对象创建和内存占用
 * 4. **一致性**: 保证库信息的全局一致性
 *
 * ### 工作空间实体源的作用
 *
 * `source` 属性记录库在工作空间模型中的实体源：
 * - **来源跟踪**: 知道库是来自项目文件还是外部配置
 * - **变更检测**: 当工作空间模型变更时，可以快速识别受影响的库
 * - **增量更新**: 只更新实际发生变化的库信息
 *
 * ## 示例
 *
 * ### 标准库的表示
 * ```
 * 仓颉标准库：
 * - name: <library cangjie-stdlib>
 * - library: LibraryEx@12345678
 * - source: JpsGlobalFileEntitySource (来自 SDK)
 * - contentScope: LibraryWithoutSourceScope (只包含 .cjo 文件)
 * - sourcesModuleInfo: LibrarySourceInfo (如果附加了源码)
 * ```
 *
 * ### 项目依赖库的表示
 * ```
 * Maven 依赖库：
 * - name: <library com.example:mylib:1.0.0>
 * - library: LibraryEx@87654321
 * - source: JpsProjectFileEntitySource (来自项目配置)
 * - contentScope: LibraryWithoutSourceScope
 * - sourcesModuleInfo: LibrarySourceInfo (如果下载了源码)
 * ```
 *
 * @param project IntelliJ 项目实例
 * @param library IntelliJ 库对象（LibraryEx）
 *
 * @see LibraryInfo
 * @see org.cangnova.cangjie.moduleinfo.cache.LibraryInfoCache
 * @see IdeaModuleInfo
 * @see BinaryModuleInfo
 */
class LibraryInfoImpl internal constructor(project: Project, library: LibraryEx) : LibraryInfo(project, library) {
    /**
     * 工作空间实体源
     *
     * 记录此库在 IntelliJ 工作空间模型中的实体源。
     * 实体源标识库的来源位置和类型，用于跟踪变更和增量更新。
     *
     * ## 可能的值
     *
     * - **JpsProjectFileEntitySource**: 库定义在项目的 .idea/libraries/ 文件中
     * - **JpsGlobalFileEntitySource**: 库定义在全局配置中（如 SDK）
     * - **JpsFileDependentEntitySource**: 库来自外部依赖管理工具（Maven、Gradle）
     * - **null**: 库的实体源无法确定（可能是已删除或临时的库）
     *
     * ## 使用场景
     *
     * ```kotlin
     * // 判断库是否来自项目配置
     * fun isProjectLibrary(libraryInfo: LibraryInfoImpl): Boolean {
     *     return libraryInfo.source is JpsProjectFileEntitySource
     * }
     *
     * // 判断库是否来自全局配置（如 SDK）
     * fun isGlobalLibrary(libraryInfo: LibraryInfoImpl): Boolean {
     *     return libraryInfo.source is JpsGlobalFileEntitySource
     * }
     * ```
     *
     * @see EntitySource
     */
    val source: EntitySource? = library.findLibraryEntitySource(project.workspaceModel.currentSnapshot)



}

