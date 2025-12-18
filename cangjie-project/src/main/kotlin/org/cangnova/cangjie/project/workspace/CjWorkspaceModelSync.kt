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

package org.cangnova.cangjie.project.workspace

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.cangnova.cangjie.project.model.*
import org.cangnova.cangjie.project.service.CjDependencyService
import java.io.File

/**
 * 仓颉项目的 EntitySource 实现
 *
 * 用于标识 Workspace Model 中由仓颉项目管理的实体。
 * 通过此 EntitySource 可以区分哪些 ModuleEntity 是由 CangJie 插件管理的，
 * 便于在项目刷新时进行清理和更新。
 *
 * @property moduleName 模块名称
 * @property projectPath 项目路径
 */
data class CangJieEntitySource(
    val moduleName: String,
    val projectPath: String
) : EntitySource {
    override val virtualFileUrl: VirtualFileUrl? get() = null
}

/**
 * 仓颉项目到 Workspace Model 的同步器
 *
 * 负责将仓颉自定义的项目模型（CjProject/CjModule）同步到 IntelliJ 的 Workspace Model，
 * 使得 IDE 的各个子系统能够正确识别和处理仓颉项目的模块结构。
 *
 * 核心职责：
 * - 将 CjModule 转换为 ModuleEntity
 * - 同步源码根目录（SourceRootEntity）
 * - 同步内容根目录（ContentRootEntity）
 * - 管理排除目录（ExcludeUrlEntity）
 * - 清理已删除的模块
 *
 * @property intellijProject IntelliJ 项目实例
 */
@Service(Service.Level.PROJECT)
class CjWorkspaceModelSync(private val intellijProject: Project) {

    companion object {
        private val LOG: Logger = logger<CjWorkspaceModelSync>()

        /**
         * 模块名称前缀，用于避免与其他插件的模块名称冲突
         */
        private const val MODULE_NAME_PREFIX: String = ""
    }

    /**
     * 同步所有仓颉项目到 Workspace Model（智能增量更新版本）
     *
     * ## 核心优化策略
     * 1. **项目类型变化检测**：检测单模块 ↔ 工作空间的转换，执行完全重建
     * 2. **模块集合差异计算**：比较现有模块和新模块列表，识别新增/删除/可能变化的模块
     * 3. **内容级别变化检测**：对每个模块深度比较内容（源码根、依赖、排除目录）
     * 4. **按需更新**：只在内容真正变化时才执行删除-重建操作
     *
     * ## 执行流程
     * 1. 检测项目类型是否发生变化（单模块 ↔ 工作空间）
     * 2. 如果项目类型变化，清理所有旧的模块实体（完全重建）
     * 3. 如果项目类型未变化，执行智能增量更新：
     *    a. 比较现有模块和新模块列表，计算差异
     *    b. 删除已经不存在的模块（集合级别删除）
     *    c. 对每个模块进行内容比较：
     *       - 比较内容根 URL
     *       - 比较源码根列表（路径和类型）
     *       - 比较排除目录列表
     *       - 比较模块依赖和库依赖
     *    d. 只在内容变化时才更新模块（内容级别更新）
     *    e. 添加新的模块
     * 4. 解析项目的完整依赖图
     * 5. 配置模块的源码根、内容根、排除目录和依赖关系
     *
     * ## 性能优化效果
     * - **完全重建** → **智能增量更新**
     * - 单个模块的小改动：O(n) → O(1)（n = 模块总数）
     * - 多模块项目的部分改动：避免不必要的 IDE 刷新和索引重建
     * - 无改动时：直接跳过更新，零开销
     *
     * @param project 需要同步的仓颉项目
     */
    suspend fun syncProject(project: CjProject) {

        val workspaceModel = intellijProject.workspaceModel
        val storageSnapshot = workspaceModel.currentSnapshot
        val storage = MutableEntityStorage.from(storageSnapshot)

        // 检测项目类型是否发生变化（单模块 ↔ 工作空间）
        val needsFullRebuild = detectProjectTypeChange(storage, project)

        if (needsFullRebuild) {
            LOG.info("Detected project type change, performing full rebuild")
            // 清理所有相关的旧模块（包括单模块和工作空间模块）
            cleanupAllProjectModules(storage, project)
        }

        // 增量更新：只删除/更新/添加变化的模块
        if (project.isWorkspace) {
            // 工作空间项目：增量同步主模块和子模块
            val workspace = project.workspace ?: return

            // 1. 获取或创建/更新主模块
            val mainModule = syncOrCreateMainModule(storage, project)

            // 2. 收集现有的子模块
            val existingModules = storage.entities(ModuleEntity::class.java)
                .filter { it.entitySource is CangJieEntitySource }
                .filter { it.name.startsWith("${project.name}.") }
                .associateBy { it.name.substringAfterLast(".") }

            // 3. 收集新的模块列表
            val newModules = workspace.modules.associateBy { it.name }

            // 4. 计算需要删除的模块（存在于旧列表但不在新列表中）
            val modulesToRemove = existingModules.keys - newModules.keys
            for (moduleName in modulesToRemove) {
                existingModules[moduleName]?.let { moduleEntity ->
                    LOG.info("Removing deleted module: $moduleName")
                    storage.removeEntity(moduleEntity)
                }
            }

            // 5. 同步所有新模块（包括新增和更新）
            for (module in workspace.modules) {
                try {
                    val dependencyGraph = resolveModuleDependencyGraph(module)
                    val existingEntity = existingModules[module.name]

                    // 检查模块内容是否真的变化了
                    val needsUpdate = existingEntity == null ||
                                     shouldUpdateModule(storage, existingEntity, module, dependencyGraph, "${mainModule.name}.")

                    if (needsUpdate) {
                        // 只在内容变化时才更新
                        existingEntity?.let { oldEntity ->
                            LOG.info("Module content changed, updating: ${module.name}")
                            storage.removeEntity(oldEntity)
                        } ?: LOG.info("New module detected, creating: ${module.name}")

                        // 创建新的模块实体
                        syncModule(storage, module, mainModule, dependencyGraph)
                        LOG.info("Synced module: ${module.name} from project ${project.name}")
                    } else {
                        LOG.debug("Module content unchanged, skipping: ${module.name}")
                    }
                } catch (e: Exception) {
                    LOG.error("Failed to sync module ${module.name}", e)
                }
            }
        } else {
            // 单模块项目：增量同步单个模块
            project.module?.let { module ->
                try {
                    val dependencyGraph = resolveModuleDependencyGraph(module)

                    // 查找现有的模块实体
                    val moduleName = "${MODULE_NAME_PREFIX}${module.name}"
                    val existingModule = storage.entities(ModuleEntity::class.java)
                        .firstOrNull {
                            it.name == moduleName &&
                            it.entitySource is CangJieEntitySource
                        }

                    // 检查模块内容是否真的变化了
                    val needsUpdate = existingModule == null ||
                                     shouldUpdateModule(storage, existingModule, module, dependencyGraph, MODULE_NAME_PREFIX)

                    if (needsUpdate) {
                        // 只在内容变化时才更新
                        existingModule?.let { oldEntity ->
                            LOG.info("Single module content changed, updating: ${module.name}")
                            storage.removeEntity(oldEntity)
                        } ?: LOG.info("New single module detected, creating: ${module.name}")

                        // 创建新的模块实体
                        syncModuleWithoutParent(storage, module, dependencyGraph)
                        LOG.info("Synced single module: ${module.name} from project ${project.name}")
                    } else {
                        LOG.debug("Single module content unchanged, skipping: ${module.name}")
                    }
                } catch (e: Exception) {
                    LOG.error("Failed to sync single module ${module.name}", e)
                }
            } ?: LOG.warn("No module found in single-module project ${project.name}")
        }

        workspaceModel.update("Sync CangJie Projects") {
            it.applyChangesFrom(storage)
        }

        // 同步完成后，将 IntelliJ Module 关联到 CjModule
        associateIntelliJModules(project)

        LOG.info("Workspace Model sync completed (incremental)")
    }

    /**
     * 将 IntelliJ Module 关联到 CjModule
     *
     * 在 WorkspaceModel 更新完成后，查找所有对应的 IntelliJ Module，
     * 并通过 UserData 机制将它们关联到 CjModule。
     *
     * @param project 仓颉项目
     */
    private fun associateIntelliJModules(project: CjProject) {
        val moduleManager = ModuleManager.getInstance(intellijProject)

        // 处理单模块项目
        project.module?.let { cjModule ->
            val moduleName = "${MODULE_NAME_PREFIX}${cjModule.name}"
            val intellijModule = moduleManager.findModuleByName(moduleName)
            if (intellijModule != null) {
                cjModule.intellijModule = intellijModule
                LOG.debug("Associated IntelliJ module '$moduleName' with CjModule '${cjModule.name}'")
            } else {
                LOG.warn("IntelliJ module '$moduleName' not found for CjModule '${cjModule.name}'")
            }
        }

        // 处理工作空间项目
        project.workspace?.let { workspace ->
            // 关联主模块（如果需要）
            val mainModuleName = project.name
            val mainIntellijModule = moduleManager.findModuleByName(mainModuleName)
            if (mainIntellijModule != null) {
                LOG.debug("Found main module: $mainModuleName")
            }

            // 关联所有子模块
            workspace.modules.forEach { cjModule ->
                val moduleName = "${project.name}.${cjModule.name}"
                val intellijModule = moduleManager.findModuleByName(moduleName)
                if (intellijModule != null) {
                    cjModule.intellijModule = intellijModule
                    LOG.debug("Associated IntelliJ module '$moduleName' with CjModule '${cjModule.name}'")
                } else {
                    LOG.warn("IntelliJ module '$moduleName' not found for CjModule '${cjModule.name}'")
                }
            }
        }
    }

    /**
     * 检测项目类型是否发生变化
     *
     * 检查逻辑：
     * 1. 如果当前是工作空间项目，检查是否存在单模块实体
     * 2. 如果当前是单模块项目，检查是否存在工作空间主模块/子模块
     *
     * @param builder 可变的实体存储构建器
     * @param project 仓颉项目
     * @return true 如果需要完全重建，false 如果可以增量更新
     */
    private fun detectProjectTypeChange(builder: MutableEntityStorage, project: CjProject): Boolean {
        val allCangjieModules = builder.entities(ModuleEntity::class.java)
            .filter { it.entitySource is CangJieEntitySource }
            .filter { (it.entitySource as CangJieEntitySource).projectPath == project.rootDir.path }
            .toList()

        if (allCangjieModules.isEmpty()) {
            // 没有旧模块，不需要重建
            return false
        }

        if (project.isWorkspace) {
            // 当前是工作空间，检查是否存在单模块格式的旧实体
            val hasSingleModuleEntities = allCangjieModules.any { module ->
                // 单模块的命名：直接是模块名（没有项目名前缀）
                // 工作空间的命名：项目名 或 项目名.子模块名
                !module.name.contains(".") && module.name != project.name
            }

            if (hasSingleModuleEntities) {
                LOG.info("Detected conversion from single-module to workspace for project: ${project.name}")
                return true
            }
        } else {
            // 当前是单模块，检查是否存在工作空间格式的旧实体
            val hasWorkspaceEntities = allCangjieModules.any { module ->
                // 工作空间特征：主模块（等于项目名）或子模块（项目名.子模块名）
                module.name == project.name || module.name.startsWith("${project.name}.")
            }

            if (hasWorkspaceEntities) {
                LOG.info("Detected conversion from workspace to single-module for project: ${project.name}")
                return true
            }
        }

        return false
    }

    /**
     * 清理项目的所有旧模块（用于项目类型转换）
     *
     * 删除所有属于该项目的模块实体，包括：
     * - 单模块格式的实体
     * - 工作空间格式的主模块
     * - 工作空间格式的所有子模块
     *
     * @param builder 可变的实体存储构建器
     * @param project 仓颉项目
     */
    private fun cleanupAllProjectModules(builder: MutableEntityStorage, project: CjProject) {
        val projectModules = builder.entities(ModuleEntity::class.java)
            .filter {
                it.entitySource is CangJieEntitySource &&
                (it.entitySource as CangJieEntitySource).projectPath == project.rootDir.path
            }
            .toList()

        LOG.info("Cleaning up ${projectModules.size} old modules for project type change")
        projectModules.forEach { module ->
            LOG.debug("Removing module entity due to project type change: ${module.name}")
            builder.removeEntity(module)
        }
    }

    /**
     * 解析模块的依赖图
     *
     * 从指定模块开始解析完整的依赖图，如果解析失败则返回 null。
     * 由于有缓存机制，重复解析相同的依赖不会导致性能问题。
     *
     * @param module 仓颉模块
     * @return 解析后的依赖图，失败时返回 null
     */
    private suspend fun resolveModuleDependencyGraph(module: CjModule): ResolvedGraph? {
        return try {
            val rootPackage = module.toPackage()
            val dependencyService = CjDependencyService.getInstance(intellijProject)

            val result = dependencyService.resolveGraph(rootPackage)
            result.getOrNull()?.also {
                LOG.info("Successfully resolved dependency graph for module ${module.name} with ${it.packages.size} packages")
            }
        } catch (e: Exception) {
            LOG.error("Failed to resolve dependency graph for module: ${module.name}", e)
            null
        }
    }

    /**
     * 检查模块内容是否需要更新
     *
     * ## 比较策略
     * 通过比较以下内容判断模块是否需要更新：
     * 1. 内容根目录 URL
     * 2. 源码根目录列表（路径和类型）
     * 3. 排除目录列表
     * 4. 模块依赖和库依赖
     *
     * ## 性能优化
     * - 只在内容真正变化时才执行删除-重建操作
     * - 避免不必要的 Workspace Model 更新，减少 IDE 刷新开销
     *
     * @param builder 可变的实体存储构建器
     * @param existingEntity 现有的模块实体
     * @param cjModule 新的仓颉模块定义
     * @param dependencyGraph 依赖图（可选）
     * @param moduleNamePrefix 模块名称前缀
     * @return true 如果需要更新，false 如果内容未变化
     */
    private suspend fun shouldUpdateModule(
        builder: MutableEntityStorage,
        existingEntity: ModuleEntity,
        cjModule: CjModule,
        dependencyGraph: ResolvedGraph?,
        moduleNamePrefix: String
    ): Boolean {
        try {
            val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()

            // 1. 比较内容根 URL
            val expectedContentRootUrl = urlManager.getOrCreateFromUrl(cjModule.rootDir.url)
            val existingContentRoot = existingEntity.contentRoots.firstOrNull()
            if (existingContentRoot?.url != expectedContentRootUrl) {
                LOG.debug("Content root changed for ${cjModule.name}")
                return true
            }

            // 2. 比较源码根
            val expectedSourceRoots = cjModule.sourceSets.flatMap { sourceSet ->
                sourceSet.roots.map { root ->
                    val url = urlManager.getOrCreateFromUrl(root.url)
                    val typeId = if (sourceSet.isTest) "java-test-resource" else "java-source"
                    url.url to typeId
                }
            }.toSet()

            val existingSourceRoots = existingContentRoot?.sourceRoots?.map { sourceRoot ->
                sourceRoot.url.url to sourceRoot.rootTypeId.name
            }?.toSet() ?: emptySet()

            if (expectedSourceRoots != existingSourceRoots) {
                LOG.debug("Source roots changed for ${cjModule.name}: expected=$expectedSourceRoots, existing=$existingSourceRoots")
                return true
            }

            // 3. 比较排除目录
            val expectedExcludedUrls = cjModule.sourceSets.flatMap { sourceSet ->
                sourceSet.outputDirectory.map { outputDir ->
                    urlManager.getOrCreateFromUrl(outputDir.url).url
                }
            }.toSet()

            val existingExcludedUrls = existingContentRoot?.excludedUrls?.map { it.url.url }?.toSet() ?: emptySet()

            if (expectedExcludedUrls != existingExcludedUrls) {
                LOG.debug("Excluded URLs changed for ${cjModule.name}")
                return true
            }

            // 4. 比较依赖
            val (expectedModuleDeps, expectedLibraryDeps) = buildAllDependencies(
                builder,
                cjModule,
                dependencyGraph,
                moduleNamePrefix
            )

            // 提取现有依赖（排除 ModuleSourceDependency 和 InheritedSdkDependency）
            val existingModuleDeps = existingEntity.dependencies
                .filterIsInstance<ModuleDependency>()
                .toSet()

            val existingLibraryDeps = existingEntity.dependencies
                .filterIsInstance<LibraryDependency>()
                .toSet()

            if (expectedModuleDeps.toSet() != existingModuleDeps) {
                LOG.debug("Module dependencies changed for ${cjModule.name}")
                return true
            }

            if (expectedLibraryDeps.toSet() != existingLibraryDeps) {
                LOG.debug("Library dependencies changed for ${cjModule.name}")
                return true
            }

            // 所有内容都相同，无需更新
            return false

        } catch (e: Exception) {
            // 比较失败时，安全起见选择更新
            LOG.warn("Failed to compare module content for ${cjModule.name}, will update", e)
            return true
        }
    }

    /**
     * 同步或创建主模块（增量版本）
     *
     * 检查主模块是否存在，如果存在且需要更新则先删除再创建，否则直接创建。
     *
     * @param builder 可变的实体存储构建器
     * @param project 仓颉项目
     * @return 主模块实体
     */
    private fun syncOrCreateMainModule(builder: MutableEntityStorage, project: CjProject): ModuleEntity {
        // 查找现有的主模块
        val existingMainModule = builder.entities(ModuleEntity::class.java)
            .firstOrNull {
                it.name == project.name &&
                it.entitySource is CangJieEntitySource &&
                (it.entitySource as CangJieEntitySource).projectPath == project.rootDir.path
            }

        // 如果主模块已存在，先删除（简化更新逻辑）
        existingMainModule?.let { oldEntity ->
            LOG.debug("Updating existing main module: ${project.name}")
            builder.removeEntity(oldEntity)
        }

        // 创建新的主模块
        return createMainModule(builder, project)
    }

    /**
     * 创建工作空间的主模块
     *
     * 为工作空间项目创建一个顶层主模块，所有子模块将作为其子模块存在。
     *
     * @param builder 可变的实体存储构建器
     * @param project 仓颉项目
     * @return 创建的主模块实体
     */
    private fun createMainModule(builder: MutableEntityStorage, project: CjProject): ModuleEntity {
        val entitySource = CangJieEntitySource(
            moduleName = project.name,
            projectPath = project.rootDir.path
        )

        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()
        val contentRootUrl = urlManager.getOrCreateFromUrl(project.rootDir.url)

        val mainModule = builder.addEntity(
            ModuleEntity(
                name = project.name,
                dependencies = listOf(
                    ModuleSourceDependency,
                    InheritedSdkDependency
                ),
                entitySource = entitySource
            ) {
                // 为主模块创建内容根（指向工作空间根目录）
                this.contentRoots = mutableListOf(
                    ContentRootEntity(
                        url = contentRootUrl,
                        excludedPatterns = emptyList(),
                        entitySource = entitySource
                    ) {
                        //                     创建所有 ExcludeUrlEntity
                        val excludeUrlBuilders = mutableListOf<ExcludeUrlEntityBuilder>()
                        // 从工作空间的源码集中获取输出目录
                        for (sourceSet in project.workspace?.sourceSets ?: emptyList()) {
                            for (outputDir in sourceSet.outputDirectory) {
                                val excludeUrl = urlManager.getOrCreateFromUrl(outputDir.url)
                                excludeUrlBuilders.add(
                                    ExcludeUrlEntity(
                                        url = excludeUrl,
                                        entitySource = entitySource
                                    )
                                )
                                LOG.debug("Excluded directory in workspace main module: ${outputDir.path}")
                            }
                        }

                        this.excludedUrls = excludeUrlBuilders
                    }
                )
            }
        )

        LOG.debug("Created main module: ${project.name}")
        return mainModule
    }

    /**
     * 同步单模块项目（不带父模块）
     *
     * 为单模块项目直接创建模块实体，不需要父模块。
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 需要同步的仓颉模块
     * @param dependencyGraph 依赖图（可选，如果为 null 则降级到逐个解析）
     */
    private suspend fun syncModuleWithoutParent(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        dependencyGraph: ResolvedGraph?
    ) {
        val entitySource = CangJieEntitySource(
            moduleName = cjModule.name,
            projectPath = cjModule.project.rootDir.path
        )

        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()
        val moduleName = "${MODULE_NAME_PREFIX}${cjModule.name}"
        val contentRootUrl = urlManager.getOrCreateFromUrl(cjModule.rootDir.url)

        // 构建依赖（分别获取模块依赖和库依赖）
        val (moduleDeps, libraryDeps) = buildAllDependencies(builder, cjModule, dependencyGraph)

        builder.addEntity(
            ModuleEntity(
                name = moduleName,
                dependencies = listOf(
                    ModuleSourceDependency,
                    InheritedSdkDependency
                ) + moduleDeps + libraryDeps,  // ModuleDependency 和 LibraryDependency 都可以放在这里
                entitySource = entitySource
            ) {
                val contentRootEntity = ContentRootEntity(
                    url = contentRootUrl,
                    excludedPatterns = emptyList(),
                    entitySource = entitySource
                ) {
//                     创建所有 SourceRootEntity
                    val sourceRootBuilders = mutableListOf<SourceRootEntityBuilder>()
                    for (sourceSet in cjModule.sourceSets) {
                        for (sourceRoot in sourceSet.roots) {
                            val sourceRootUrl = urlManager.getOrCreateFromUrl(sourceRoot.url)
                            val rootType = if (sourceSet.isTest) {
                                SourceRootTypeId("java-test-resource")
                            } else {
                                SourceRootTypeId("java-source")
                            }

                            sourceRootBuilders.add(
                                SourceRootEntity(
                                    url = sourceRootUrl,
                                    rootTypeId = rootType,
                                    entitySource = entitySource
                                )
                            )

                            LOG.debug("Added source root: ${sourceRoot.path} (test=${sourceSet.isTest})")
                        }
                    }
                    this.sourceRoots = sourceRootBuilders

//                     创建所有 ExcludeUrlEntity
                    val excludeUrlBuilders = mutableListOf<ExcludeUrlEntityBuilder>()
                    // 从源码集的 outputDirectory 获取输出目录
                    for (sourceSet in cjModule.sourceSets) {
                        for (outputDir in sourceSet.outputDirectory) {
                            val excludeUrl = urlManager.getOrCreateFromUrl(outputDir.url)
                            excludeUrlBuilders.add(
                                ExcludeUrlEntity(
                                    url = excludeUrl,
                                    entitySource = entitySource
                                )
                            )
                            LOG.debug("Excluded directory: ${outputDir.path}")
                        }
                    }
                    this.excludedUrls = excludeUrlBuilders
                }

                this.contentRoots = mutableListOf(contentRootEntity)
            }
        )

        LOG.debug("Created ModuleEntity: $moduleName with content root: ${cjModule.rootDir.path}")
    }

    /**
     * 清理所有由 CangJieEntitySource 管理的旧实体（已废弃，保留用于完全重建）
     *
     * 注意：现在默认使用增量更新，不再使用此方法。
     * 如果需要完全重建所有模块，可以手动调用此方法。
     *
     * @param builder 可变的实体存储构建器
     */
    @Deprecated("Use incremental sync instead", ReplaceWith("syncProject with incremental logic"))
    private fun cleanupOldEntities(builder: MutableEntityStorage) {
        val cangjieModules = builder.entities(ModuleEntity::class.java)
            .filter { it.entitySource is CangJieEntitySource }
            .toList()

        cangjieModules.forEach { module ->
            LOG.debug("Removing old CangJie module entity: ${module.name}")
            builder.removeEntity(module)
        }
    }

    /**
     * 同步单个 CjModule 到 Workspace Model
     *
     * 创建或更新模块对应的 ModuleEntity，并配置：
     * - 模块名称（带前缀以避免冲突）
     * - 内容根目录（模块根目录）
     * - 源码根目录（从 sourceSets 中提取）
     * - 排除目录（如 targetPlatform 构建目录）
     * - 模块依赖关系（从 cjModule.dependencies 提取）
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 需要同步的仓颉模块
     * @param parentModule 父模块实体
     * @param dependencyGraph 依赖图（可选，如果为 null 则降级到逐个解析）
     */
    private suspend fun syncModule(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        parentModule: ModuleEntity,
        dependencyGraph: ResolvedGraph?
    ) {
        val entitySource = CangJieEntitySource(
            moduleName = cjModule.name,
            projectPath = cjModule.project.rootDir.path
        )

        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()

        // 创建模块名称（添加前缀避免冲突）
        val moduleName = "${parentModule.name}.${cjModule.name}"

        // 准备 ContentRoot URL
        val contentRootUrl = urlManager.getOrCreateFromUrl(cjModule.rootDir.url)

        // 构建依赖（分别获取模块依赖和库依赖）
        val (moduleDeps, libraryDeps) = buildAllDependencies(
            builder,
            cjModule,
            dependencyGraph,
            "${parentModule.name}."
        )

        // 创建 ModuleEntity（包含所有子实体）
        builder.addEntity(
            ModuleEntity(
                name = moduleName,
                dependencies = listOf(
                    ModuleSourceDependency,
                    InheritedSdkDependency
                ) + moduleDeps + libraryDeps,  // 添加模块依赖和库依赖
                entitySource = entitySource
            ) {
                // 创建 ContentRootEntity
                val contentRootEntity = ContentRootEntity(
                    url = contentRootUrl,
                    excludedPatterns = emptyList(),
                    entitySource = entitySource
                ) {
                    // 创建所有 SourceRootEntity
                    val sourceRootBuilders = mutableListOf<SourceRootEntityBuilder>()
                    for (sourceSet in cjModule.sourceSets) {
                        for (sourceRoot in sourceSet.roots) {
                            val sourceRootUrl = urlManager.getOrCreateFromUrl(sourceRoot.url)
                            val rootType = if (sourceSet.isTest) {
                                SourceRootTypeId("java-test-resource")
                            } else {
                                SourceRootTypeId("java-source")
                            }

                            sourceRootBuilders.add(
                                SourceRootEntity(
                                    url = sourceRootUrl,
                                    rootTypeId = rootType,
                                    entitySource = entitySource
                                )
                            )

                            LOG.debug("Added source root: ${sourceRoot.path} (test=${sourceSet.isTest})")
                        }
                    }
                    this.sourceRoots = sourceRootBuilders

                    // 创建所有 ExcludeUrlEntity
                    val excludeUrlBuilders = mutableListOf<ExcludeUrlEntityBuilder>()
                    // 从源码集的 outputDirectory 获取输出目录
                    for (sourceSet in cjModule.sourceSets) {
                        for (outputDir in sourceSet.outputDirectory) {
                            val excludeUrl = urlManager.getOrCreateFromUrl(outputDir.url)
                            excludeUrlBuilders.add(
                                ExcludeUrlEntity(
                                    url = excludeUrl,
                                    entitySource = entitySource
                                )
                            )
                            LOG.debug("Excluded directory: ${outputDir.path}")
                        }
                    }

                    this.excludedUrls = excludeUrlBuilders
                }

                this.contentRoots = mutableListOf(contentRootEntity)
            })



        LOG.debug("Created ModuleEntity: $moduleName with content root: ${cjModule.rootDir.path}")
    }

    /**
     * 构建所有依赖
     *
     * 将 CjModule 的依赖信息转换为 Workspace Model 的 ModuleDependency 和 LibraryDependency。
     * 包括：
     * - Path 依赖 → ModuleDependency
     * - Library/Git/Stdlib 依赖 → LibraryDependency（使用依赖图获取传递依赖）
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 仓颉模块
     * @param dependencyGraph 依赖图（可选）
     * @param moduleNamePrefix 模块名称前缀（用于工作空间项目）
     * @return Pair<ModuleDependency列表, LibraryDependency列表>
     */
    private suspend fun buildAllDependencies(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        dependencyGraph: ResolvedGraph?,
        moduleNamePrefix: String = MODULE_NAME_PREFIX
    ): Pair<List<ModuleDependency>, List<LibraryDependency>> {
        val moduleDeps = mutableListOf<ModuleDependency>()
        val libraryDeps = mutableListOf<LibraryDependency>()

        val entitySource = CangJieEntitySource(
            moduleName = cjModule.name,
            projectPath = cjModule.project.rootDir.path
        )

        // 1. 处理 Path 依赖（区分模块间依赖和外部路径依赖）
        for (pathDep in cjModule.pathDependencies) {
            // 查找依赖的模块
            val dependencyModule = cjModule.project.findModule(pathDep.name)
            if (dependencyModule != null) {
                // 情况1: 工作空间内的模块间依赖 → ModuleDependency
                val dependencyModuleName = "$moduleNamePrefix${pathDep.name}"

                moduleDeps.add(
                    ModuleDependency(
                        module = ModuleId(dependencyModuleName),
                        exported = false, // Path 依赖默认不导出
                        scope = mapDependencyScope(pathDep.scope),
                        productionOnTest = false
                    )
                )

                LOG.debug("Added module dependency: ${cjModule.name} -> ${pathDep.name} (scope=${pathDep.scope})")
            } else {
                // 情况2: 工作空间外的路径依赖 → LibraryDependency
                LOG.debug("Path dependency ${pathDep.name} is not a workspace module, treating as external library")

                // 从依赖图中获取已解析的 Path 包，或直接解析
                val resolvedPackage = if (dependencyGraph != null) {
                    val modulePackageId = PackageId(
                        name = cjModule.name,
                        version = cjModule.metadata.version,
                        sourceId = SourceId.Local(cjModule.rootDir.toNioPath())
                    )
                    val directDeps = dependencyGraph.getDirectDependencies(modulePackageId)
                    val depEdge = directDeps.find { it.declaration.name == pathDep.name }
                    depEdge?.let { dependencyGraph.packages[it.resolvedTo] }
                } else {
                    // 降级：直接解析
                    val dependencyService = CjDependencyService.getInstance(intellijProject)
                    dependencyService.resolveSingle(pathDep).getOrNull()
                }

                if (resolvedPackage is CjPackage.Path) {
                    // 创建 LibraryEntity 和 LibraryDependency
                    val libraryDependency = createLibraryDependency(
                        builder,
                        pathDep,
                        resolvedPackage,
                        entitySource
                    )
                    if (libraryDependency != null) {
                        libraryDeps.add(libraryDependency)
                        LOG.debug("Added external path dependency as library: ${cjModule.name} -> ${pathDep.name}")
                    }
                } else {
                    LOG.warn("Failed to resolve external path dependency: ${pathDep.name} for module ${cjModule.name}")
                }
            }
        }

        // 2. 处理外部库依赖（Library, Git, Stdlib）
        val externalLibs = buildLibraryDependencies(builder, cjModule, dependencyGraph, entitySource)
        libraryDeps.addAll(externalLibs)

        return Pair(moduleDeps, libraryDeps)
    }

    /**
     * 构建外部库依赖列表
     *
     * 为 Library、Git 和 Stdlib 依赖创建 LibraryEntity 和 LibraryDependency。
     *
     * 如果提供了依赖图，将使用依赖图获取所有传递依赖；
     * 否则降级到旧的逐个解析方式（仅处理直接依赖）。
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 仓颉模块
     * @param dependencyGraph 依赖图（可选）
     * @param entitySource 实体源
     * @return LibraryDependency 列表
     */
    private suspend fun buildLibraryDependencies(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        dependencyGraph: ResolvedGraph?,
        entitySource: EntitySource
    ): List<LibraryDependency> {
        return if (dependencyGraph != null) {
            buildLibraryDependenciesFromGraph(builder, cjModule, dependencyGraph, entitySource)
        } else {
            buildLibraryDependenciesLegacy(builder, cjModule, entitySource)
        }
    }

    /**
     * 使用依赖图构建库依赖
     *
     * 从依赖图中获取模块的所有传递依赖，并为每个依赖创建 LibraryDependency
     */
    private fun buildLibraryDependenciesFromGraph(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        dependencyGraph: ResolvedGraph,
        entitySource: EntitySource
    ): List<LibraryDependency> {
        val dependencies = mutableListOf<LibraryDependency>()

        // 构建模块的 PackageId
        val modulePackageId = PackageId(
            name = cjModule.name,
            version = cjModule.metadata.version,
            sourceId = SourceId.Local(cjModule.rootDir.toNioPath())
        )

        // 获取所有传递依赖
        val transitiveDeps = dependencyGraph.getTransitiveDependencies(modulePackageId)
        LOG.debug("Found ${transitiveDeps.size} transitive dependencies for module: ${cjModule.name}")

        for (depPackageId in transitiveDeps) {
            val depPackage = dependencyGraph.packages[depPackageId]
            if (depPackage == null) {
                LOG.warn("Package not found in graph: $depPackageId")
                continue
            }

            // 跳过本地模块（已作为 ModuleDependency 处理）
            if (depPackage is CjPackage.LocalModule) continue

            try {
                // 获取直接依赖声明（用于确定 scope）
                val directDep = dependencyGraph.getDirectDependencies(modulePackageId)
                    .find { it.resolvedTo == depPackageId }

                val libraryDependency = createLibraryDependency(
                    builder,
                    directDep?.declaration ?: createDefaultDependency(depPackage, cjModule),
                    depPackage,
                    entitySource
                )

                if (libraryDependency != null) {
                    dependencies.add(libraryDependency)
                    LOG.debug("Added library dependency: ${cjModule.name} -> ${depPackageId.name}")
                }
            } catch (e: Exception) {
                LOG.error("Error creating library for dependency ${depPackageId.name}", e)
            }
        }

        return dependencies
    }

    /**
     * 创建默认依赖声明（用于传递依赖）
     *
     * 当从依赖图获取传递依赖时，可能没有原始的依赖声明，
     * 此方法根据包信息创建一个默认的依赖声明
     *
     * @param pkg 包信息
     * @param sourceModule 声明此依赖的源模块
     */
    private fun createDefaultDependency(pkg: CjPackage, sourceModule: CjDependencyDeclarant): CjDependency {
        return when (pkg) {
            is CjPackage.Library -> CjDependency.Library(
                name = pkg.id.name,
                versionReq = VersionRequirement.Exact(pkg.id.version),
                scope = CjDependencyScope.COMPILE,
                sourceModule = sourceModule
            )

            is CjPackage.Git -> CjDependency.Git(
                name = pkg.id.name,
                url = pkg.url,
                ref = GitRef.Rev(pkg.rev),
                versionReq = VersionRequirement.Exact(pkg.id.version),
                sourceModule = sourceModule
            )

            is CjPackage.Path -> CjDependency.Path(
                name = pkg.id.name,
                path = pkg.path,
                sourceModule = sourceModule
            )

            is CjPackage.Stdlib -> CjDependency.Stdlib(
                name = pkg.id.name,
                versionReq = VersionRequirement.Exact(pkg.id.version),
                sourceModule = sourceModule
            )

            is CjPackage.Binary -> CjDependency.Binary(
                name = pkg.id.name,
                cjoPath = pkg.cjoPath,
                libPath = pkg.libPath,
                target = pkg.target,
                sourceModule = sourceModule
            )

            is CjPackage.LocalModule -> {
                // 本地模块作为路径依赖
                CjDependency.Path(
                    name = pkg.id.name,
                    path = pkg.module.rootDir.toNioPath(),
                    sourceModule = sourceModule
                )
            }

            is CjPackage.Failed -> {
                LOG.warn("Creating default dependency for failed package: ${pkg.id.name}")
                CjDependency.Library(
                    name = pkg.id.name,
                    versionReq = VersionRequirement.Exact(pkg.id.version),
                    sourceModule = sourceModule
                )
            }
        }
    }

    /**
     * 逐个解析库依赖（旧方法，降级使用）
     *
     * 当依赖图解析失败时使用此方法，仅处理直接依赖，不处理传递依赖
     */
    private suspend fun buildLibraryDependenciesLegacy(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        entitySource: EntitySource
    ): List<LibraryDependency> {
        LOG.warn("Using legacy dependency resolution (no dependency graph available)")
        val dependencies = mutableListOf<LibraryDependency>()
        val dependencyService = CjDependencyService.getInstance(intellijProject)

        // 获取所有非 Path 类型的依赖
        val externalDependencies = cjModule.dependencies.filter { dep ->
            dep !is CjDependency.Path
        }

        for (dependency in externalDependencies) {
            try {
                // 解析依赖以获取路径信息
                val resolveResult = dependencyService.resolveSingle(dependency, emptySet())

                val resolved = resolveResult.getOrNull()

                if (resolved != null && resolved !is CjPackage.Failed) {
                    // 创建 LibraryEntity 和 LibraryDependency
                    val libraryDependency = createLibraryDependency(
                        builder,
                        dependency,
                        resolved,
                        entitySource
                    )

                    if (libraryDependency != null) {
                        dependencies.add(libraryDependency)
                        LOG.debug("Added library dependency: ${cjModule.name} -> ${dependency.name}")
                    }
                } else {
                    val errorMsg = if (resolved is CjPackage.Failed) resolved.errorMessage else "Unknown error"
                    LOG.warn("Failed to resolve dependency: ${dependency.name} - $errorMsg")
                }
            } catch (e: Exception) {
                LOG.error("Error resolving dependency ${dependency.name}", e)
            }
        }

        return dependencies
    }

    /**
     * 创建单个库依赖
     *
     * 根据已解析的依赖信息创建 LibraryEntity 和 LibraryDependency。
     *
     * @param builder 可变的实体存储构建器
     * @param dependency 原始依赖
     * @param resolved 已解析的依赖
     * @param entitySource 实体源
     * @return LibraryDependency 或 null（如果创建失败）
     */
    private fun createLibraryDependency(
        builder: MutableEntityStorage,
        dependency: CjDependency,
        resolved: CjPackage,
        entitySource: EntitySource
    ): LibraryDependency? {
        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()

        // 生成库的唯一名称
        val libraryName = resolved.id.toString()

        // 检查库是否已存在
        val existingLibrary = builder.entities(LibraryEntity::class.java)
            .firstOrNull { it.name == libraryName }

        if (existingLibrary != null) {
            // 库已存在，直接创建依赖引用
            return LibraryDependency(
                library = LibraryId(libraryName, LibraryTableId.ProjectLibraryTableId),
                exported = false,
                scope = mapDependencyScope(dependency.scope)
            )
        }

        // 收集库的根目录（classes, sources, documentation）
        val classesRoots = mutableListOf<LibraryRoot>()
        val sourcesRoots = mutableListOf<LibraryRoot>()
        val javadocRoots = mutableListOf<LibraryRoot>()

        // 根据解析结果类型提取路径
        when (resolved) {
            is CjPackage.Library -> {
                // TODO: Registry 库支持（待中心仓库实现后添加）
                LOG.warn("Registry library not yet supported: ${resolved.id}")
                return null
            }

            is CjPackage.Git -> {
                // Git 依赖：从 checkoutPath 获取路径
                val pkgDir = resolved.checkoutPath.toFile()
                if (pkgDir.exists() && pkgDir.isDirectory) {
                    addPackageRoots(pkgDir, classesRoots, sourcesRoots, urlManager)
                } else {
                    LOG.warn("Git dependency path does not exist: ${resolved.checkoutPath}")
                    return null
                }
            }

            is CjPackage.Path -> {
                // Path 依赖：从 path 获取路径
                val pkgDir = resolved.path.toFile()
                if (pkgDir.exists() && pkgDir.isDirectory) {
                    addPackageRoots(pkgDir, classesRoots, sourcesRoots, urlManager)
                } else {
                    LOG.warn("Path dependency does not exist: ${resolved.path}")
                    return null
                }
            }

            is CjPackage.Stdlib -> {
                // Stdlib 依赖：从 SDK 路径获取
                val stdlibDir = resolved.path.toFile()
                if (stdlibDir.exists() && stdlibDir.isDirectory) {
                    addPackageRoots(stdlibDir, classesRoots, sourcesRoots, urlManager)
                } else {
                    LOG.warn("Stdlib path does not exist: ${resolved.path}")
                    return null
                }
            }

            is CjPackage.Failed -> {
                // 解析失败，记录日志并返回 null
                LOG.warn("Cannot create library for failed package: ${resolved.errorMessage}")
                return null
            }

            is CjPackage.LocalModule -> {
                // LocalModule 应该作为模块依赖处理，不应该到这里
                LOG.warn("LocalModule should be handled as module dependency: ${resolved.id}")
                return null
            }

            is CjPackage.Binary -> {
                // 二进制依赖：将 cjo 和 lib 文件添加为 classes roots
                val cjoFile = resolved.cjoPath.toFile()
                if (cjoFile.exists()) {
                    val cjoUrl = urlManager.getOrCreateFromUrl("file://${cjoFile.absolutePath}")
                    classesRoots.add(LibraryRoot(cjoUrl, LibraryRootTypeId.COMPILED))
                    LOG.debug("Added binary CJO file: ${cjoFile.absolutePath}")
                }

                // 添加库文件（.so 或 .a）
                resolved.libPath?.let { libPath ->
                    val libFile = libPath.toFile()
                    if (libFile.exists()) {
                        val libUrl = urlManager.getOrCreateFromUrl("file://${libFile.absolutePath}")
                        classesRoots.add(LibraryRoot(libUrl, LibraryRootTypeId.COMPILED))
                        LOG.debug("Added binary library file: ${libFile.absolutePath}")
                    }
                }
            }
        }

        // 如果没有找到任何根目录，无法创建库实体
        if (classesRoots.isEmpty()) {
            LOG.warn("No classes roots found for library: $libraryName")
            return null
        }

        // 创建 LibraryEntity
        builder.addEntity(
            LibraryEntity(
                name = libraryName,
                tableId = LibraryTableId.ProjectLibraryTableId,
                roots = classesRoots + sourcesRoots + javadocRoots,
                entitySource = entitySource
            )
        )

        LOG.info("Created library entity: $libraryName with ${classesRoots.size} classes, ${sourcesRoots.size} sources, ${javadocRoots.size} docs")

        // 返回库依赖
        return LibraryDependency(
            library = LibraryId(libraryName, LibraryTableId.ProjectLibraryTableId),
            exported = false,
            scope = mapDependencyScope(dependency.scope)
        )
    }


    /**
     * 映射依赖范围
     */
    private fun mapDependencyScope(scope: CjDependencyScope): DependencyScope {
        return when (scope) {
            CjDependencyScope.COMPILE -> DependencyScope.COMPILE
            CjDependencyScope.TEST -> DependencyScope.TEST
            CjDependencyScope.RUNTIME -> DependencyScope.RUNTIME
            CjDependencyScope.PROVIDED -> DependencyScope.PROVIDED
        }
    }

    /**
     * 添加包的根目录（classes 和 sources）
     *
     * 从包目录中提取编译产物和源码目录，并添加到对应的根列表中
     *
     * @param pkgDir 包目录
     * @param classesRoots classes roots 列表
     * @param sourcesRoots sources roots 列表
     * @param urlManager URL 管理器
     */
    private fun addPackageRoots(
        pkgDir: File,
        classesRoots: MutableList<LibraryRoot>,
        sourcesRoots: MutableList<LibraryRoot>,
        urlManager: com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
    ) {
        // 查找包中的编译产物
        val compiledFiles = findCompiledFiles(pkgDir)

        if (compiledFiles.isNotEmpty()) {
            // 有编译产物：添加编译产物作为 classes roots
            for (compiledFile in compiledFiles) {
                val url = urlManager.getOrCreateFromUrl("file://${compiledFile.absolutePath}")
                classesRoots.add(LibraryRoot(url, LibraryRootTypeId.COMPILED))
                LOG.debug("Added package compiled root: ${compiledFile.absolutePath}")
            }
        } else {
            // 没有编译产物（纯源码包）：将整个包目录作为 classes root
            val pkgUrl = urlManager.getOrCreateFromUrl("file://${pkgDir.absolutePath}")
            classesRoots.add(LibraryRoot(pkgUrl, LibraryRootTypeId.COMPILED))
            LOG.debug("Added package directory as classes root (pure source package): ${pkgDir.absolutePath}")
        }

        // 查找包中的源码目录
        val sourceDirs = findSourceDirectories(pkgDir)
        for (sourceDir in sourceDirs) {
            val url = urlManager.getOrCreateFromUrl("file://${sourceDir.absolutePath}")
            sourcesRoots.add(LibraryRoot(url, LibraryRootTypeId.SOURCES))
            LOG.debug("Added package sources root: ${sourceDir.absolutePath}")
        }
    }

    /**
     * 查找目录中的编译产物文件
     *
     * 查找 .cjo, .a, .so, .dll, .dylib 等文件
     */
    private fun findCompiledFiles(directory: File): List<File> {
        val compiledExtensions = setOf(".cjo", ".a", ".so", ".dll", ".dylib", ".lib")
        val result = mutableListOf<File>()

        if (!directory.isDirectory) {
            return result
        }

        directory.walk().maxDepth(3).forEach { file ->
            if (file.isFile) {
                val extension = file.extension.let { if (it.isEmpty()) "" else ".$it" }
                if (extension in compiledExtensions) {
                    result.add(file)
                }
            }
        }

        return result
    }

    /**
     * 查找包中的源码目录
     *
     * 查找常见的源码目录：src, source, sources 等
     */
    private fun findSourceDirectories(packageDir: File): List<File> {
        val sourceDirectoryNames = setOf("src", "source", "sources", "lib")
        val result = mutableListOf<File>()

        if (!packageDir.isDirectory) {
            return result
        }

        packageDir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name in sourceDirectoryNames) {
                result.add(file.parentFile)
            }
        }

        // 如果没有找到标准源码目录，将包目录本身作为源码根
        if (result.isEmpty() && packageDir.listFiles()?.any { it.extension == "cj" } == true) {
            result.add(packageDir)
        }

        return result
    }


}
