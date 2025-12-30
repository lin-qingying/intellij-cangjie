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
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.utils.firstIsInstanceOrNull

/**
 * 模块来源能力键
 *
 * 该能力键用于在 [ModuleInfo.capabilities] 映射中存储 [ModuleOrigin] 信息。
 *
 * ## 使用场景
 *
 * - 识别模块的来源类型（源码、库、SDK 等）
 * - 在分析过程中区分不同来源的模块
 * - 用于调试和诊断
 *
 * ## 示例
 *
 * ```kotlin
 * val origin = moduleInfo.capabilities[OriginCapability] as? ModuleOrigin
 * when (origin) {
 *     ModuleOrigin.MODULE -> println("源码模块")
 *     ModuleOrigin.LIBRARY -> println("库模块")
 *     ModuleOrigin.OTHER -> println("其他类型")
 * }
 * ```
 *
 * @see ModuleCapability
 * @see ModuleOrigin
 */
val OriginCapability = ModuleCapability< ModuleOrigin>("MODULE_ORIGIN")

/**
 * IDEA 模块信息接口
 *
 * 该接口扩展了 [ModuleInfo]，添加了 IntelliJ IDEA 特定的模块信息和功能。
 * 它是在 IDE 环境中使用的模块信息的基础接口。
 *
 * ## 设计目的
 *
 * [IdeaModuleInfo] 桥接了编译器层面的 [ModuleInfo] 和 IDE 层面的模块概念：
 * - **ModuleInfo**: 编译器视角，提供分析和编译所需的模块元数据
 * - **IdeaModuleInfo**: IDE 视角，提供 IDE 功能所需的额外信息
 *
 * ## 核心职责
 *
 * 1. **内容作用域**: 提供模块内容的搜索范围 ([contentScope])
 * 2. **模块来源**: 区分模块的来源类型 ([moduleOrigin])
 * 3. **项目引用**: 关联到 IntelliJ 项目实例 ([project])
 * 4. **依赖管理**: 管理模块间的依赖关系
 * 5. **能力扩展**: 通过 capabilities 机制扩展模块信息
 *
 * ## 继承层次
 *
 * ```
 * ModuleInfo (编译器接口)
 *   ↓
 * IdeaModuleInfo (IDE 基础接口)
 *   ↓
 * ├── LibraryModuleInfo (库模块)
 * │   └── LibraryInfo (具体库实现)
 * ├── BinaryModuleInfo (二进制模块)
 * │   └── LibraryInfo
 * ├── SourceForBinaryModuleInfo (二进制对应的源码)
 * │   └── LibrarySourceInfo
 * └── ModuleSourceInfo (项目源码模块)
 * ```
 *
 * ## 使用场景
 *
 * ### 代码分析
 * ```kotlin
 * fun analyzeFile(file: PsiFile, ideaModuleInfo: IdeaModuleInfo) {
 *     // 获取模块内容范围
 *     val scope = ideaModuleInfo.contentScope
 *
 *     // 在模块范围内查找符号
 *     val symbols = findSymbolsInScope(file, scope)
 * }
 * ```
 *
 * ### 依赖遍历
 * ```kotlin
 * fun collectAllDependencies(module: IdeaModuleInfo): Set<IdeaModuleInfo> {
 *     val result = mutableSetOf<IdeaModuleInfo>()
 *     val queue = ArrayDeque<IdeaModuleInfo>()
 *     queue.add(module)
 *
 *     while (queue.isNotEmpty()) {
 *         val current = queue.removeFirst()
 *         if (result.add(current)) {
 *             // 添加直接依赖
 *             queue.addAll(current.dependencies())
 *         }
 *     }
 *
 *     return result
 * }
 * ```
 *
 * ## 与 AnalysisContext 的关系
 *
 * 该接口已被标记为过时，正在逐步迁移到 [AnalysisContext]。
 * - **IdeaModuleInfo**: 旧的 Kotlin 风格接口，功能更丰富但更复杂
 * - **AnalysisContext**: 新的简化接口，专注于分析上下文
 *
 * @see ModuleInfo
 * @see AnalysisContext
 * @see LibraryInfo
 * @see ModuleSourceInfo
 */
interface IdeaModuleInfo : ModuleInfo {
    /**
     * 模块内容的搜索作用域
     *
     * 返回包含此模块所有文件的搜索范围，用于：
     * - 文件查找和索引
     * - 符号搜索
     * - 代码导航和引用查找
     * - 代码分析和检查
     *
     * ## 与其他作用域的区别
     *
     * - **contentScope**: 仅包含模块自身的文件
     * - **moduleContentScope**: 与 contentScope 相同（别名）
     * - **dependencies scope**: 包含依赖模块的文件（通过 dependencies() 计算）
     *
     * ## 使用示例
     *
     * ```kotlin
     * // 在模块范围内查找文件
     * val files = FilenameIndex.getFilesByName(
     *     project,
     *     "Main.cj",
     *     moduleInfo.contentScope
     * )
     *
     * // 限定符号搜索范围
     * val classes = CangJieClassShortNameIndex.getInstance()
     *     .get(className, project, moduleInfo.contentScope)
     * ```
     *
     * @return 模块内容的全局搜索作用域
     * @see GlobalSearchScope
     * @see moduleContentScope
     */
    val contentScope: GlobalSearchScope

    /**
     * 模块内容作用域的别名
     *
     * 提供与 [contentScope] 相同的功能，用于兼容不同的命名约定。
     * 默认实现直接返回 [contentScope]。
     *
     * @return 模块内容的搜索作用域
     */
    val moduleContentScope: GlobalSearchScope
        get() = contentScope

    /**
     * 模块来源类型
     *
     * 标识此模块的来源，用于区分不同类型的模块：
     * - **MODULE**: 项目源码模块
     * - **LIBRARY**: 库模块（依赖的外部库）
     * - **OTHER**: 其他类型（如 SDK、临时文件等）
     *
     * ## 使用场景
     *
     * - **分析策略选择**: 源码模块需要完整分析，库模块只需加载元数据
     * - **可见性控制**: 库模块的 internal 声明对源码模块不可见
     * - **错误报告**: 区分错误来自项目代码还是依赖库
     * - **性能优化**: 库模块可以使用预构建的索引
     *
     * ## 示例
     *
     * ```kotlin
     * fun shouldAnalyze(module: IdeaModuleInfo): Boolean {
     *     return module.moduleOrigin == ModuleOrigin.MODULE
     * }
     * ```
     *
     * @return 模块的来源类型
     * @see ModuleOrigin
     */
    val moduleOrigin: ModuleOrigin

    /**
     * 所属 IntelliJ 项目
     *
     * 返回此模块所属的 IntelliJ 项目实例，用于：
     * - 访问项目级别的服务
     * - 获取项目配置和设置
     * - 查询项目结构
     * - 触发项目级别的操作
     *
     * ## 使用示例
     *
     * ```kotlin
     * // 获取项目服务
     * val service = moduleInfo.project.service<MyProjectService>()
     *
     * // 获取项目根目录
     * val projectDir = moduleInfo.project.basePath
     *
     * // 触发索引更新
     * DumbService.getInstance(moduleInfo.project).smartInvokeLater {
     *     // 在索引完成后执行
     * }
     * ```
     *
     * @return IntelliJ 项目实例
     * @see Project
     */
      val project: Project

    /**
     * 模块能力映射
     *
     * 扩展父接口的 capabilities，自动添加 [OriginCapability] 能力，
     * 使调用者可以通过 capabilities 获取 [moduleOrigin]。
     *
     * ## 扩展机制
     *
     * 通过覆盖此属性，可以在不修改接口的情况下为模块添加额外的元数据：
     *
     * ```kotlin
     * class MyModuleInfo : IdeaModuleInfo {
     *     override val capabilities: Map<ModuleCapability<*>, Any?>
     *         get() = super.capabilities + mapOf(
     *             MyCustomCapability to myCustomData
     *         )
     * }
     * ```
     *
     * ## 默认能力
     *
     * - [ModuleInfo.Capability]: ModuleInfo 自身
     * - [OriginCapability]: 模块来源信息
     *
     * @return 能力键到值的映射
     * @see ModuleCapability
     * @see OriginCapability
     */
    override val capabilities: Map<ModuleCapability<*>, Any?>
        get() = super.capabilities + mapOf(OriginCapability to moduleOrigin)

    /**
     * 模块依赖列表
     *
     * 返回此模块直接依赖的所有 [IdeaModuleInfo] 模块。
     * 这是 [ModuleInfo.dependencies] 的类型细化版本。
     *
     * ## 依赖顺序
     *
     * 依赖列表的顺序很重要：
     * 1. **符号解析**: 按顺序在依赖模块中查找符号
     * 2. **优先级**: 前面的依赖优先级更高
     * 3. **自依赖**: 通常包含自身作为第一个元素
     *
     * ## 示例
     *
     * ```kotlin
     * // 获取所有依赖的库
     * val libraries = moduleInfo.dependencies()
     *     .filterIsInstance<LibraryInfo>()
     *
     * // 检查是否依赖某个特定库
     * val usesStdlib = moduleInfo.dependencies()
     *     .any { it.name.asString() == "stdlib" }
     * ```
     *
     * @return 依赖的 IdeaModuleInfo 列表
     * @see dependenciesWithoutSelf
     */
    override val dependencies: List<IdeaModuleInfo>
    /**
     * 获取不包含自身的依赖序列
     *
     * 返回一个过滤掉自身的依赖序列，用于避免循环依赖问题。
     *
     * ## 使用场景
     *
     * - **依赖遍历**: 遍历所有外部依赖，不包括模块自身
     * - **依赖分析**: 分析模块的外部依赖关系
     * - **依赖图构建**: 构建不含自环的依赖图
     *
     * ## 为什么需要这个方法？
     *
     * [dependencies] 通常包含模块自身作为第一个元素，这在某些场景下会导致问题：
     * - 递归遍历时可能死循环
     * - 统计依赖数量时会多算一个
     * - 构建依赖图时会产生自环
     *
     * ## 示例
     *
     * ```kotlin
     * // 统计外部依赖数量
     * val externalDepsCount = moduleInfo.dependenciesWithoutSelf().count()
     *
     * // 收集所有外部库
     * val externalLibraries = moduleInfo.dependenciesWithoutSelf()
     *     .filterIsInstance<LibraryInfo>()
     *     .toList()
     * ```
     *
     * @return 不包含自身的依赖序列
     */
    fun dependenciesWithoutSelf(): Sequence<IdeaModuleInfo> = dependencies.asSequence().filter { it != this }

    /**
     * 检查模块信息的有效性
     *
     * 验证模块信息是否仍然有效。如果模块已被删除、修改或不再可用，
     * 此方法应该抛出异常。
     *
     * ## 使用场景
     *
     * - **缓存验证**: 在使用缓存的模块信息前检查其有效性
     * - **增量更新**: 检测模块是否发生变化
     * - **错误恢复**: 在分析失败时检查模块是否仍然存在
     *
     * ## 默认实现
     *
     * 默认实现为空，表示总是有效。子类可以覆盖此方法提供实际的验证逻辑。
     *
     * ## 示例实现
     *
     * ```kotlin
     * override fun checkValidity() {
     *     if (!library.isValid) {
     *         throw InvalidModuleException("Library ${library.name} is no longer valid")
     *     }
     * }
     * ```
     *
     * @throws InvalidModuleException 如果模块信息无效
     */
    fun checkValidity() {}
}