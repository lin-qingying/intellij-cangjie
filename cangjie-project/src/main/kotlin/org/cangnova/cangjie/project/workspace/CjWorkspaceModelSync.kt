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

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.roots

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
        private val LOG = logger<CjWorkspaceModelSync>()

        /**
         * 模块名称前缀，用于避免与其他插件的模块名称冲突
         */
        private const val MODULE_NAME_PREFIX = ""
    }

    /**
     * 同步所有仓颉项目到 Workspace Model
     *
     * 该方法会：
     * 1. 清理所有由 CangJieEntitySource 标记的旧实体
     * 2. 为每个 CjModule 创建对应的 ModuleEntity
     * 3. 配置模块的源码根、内容根和排除目录
     *
     * @param projects 需要同步的仓颉项目列表
     */
    suspend fun syncProjects(project: CjProject) {

        val workspaceModel = intellijProject.workspaceModel

        val storageSnapshot = workspaceModel.currentSnapshot


        val storage = MutableEntityStorage.from(storageSnapshot)

//        清除所有模块
        // 1. 清理旧的仓颉模块实体
        cleanupOldEntities(storage)


        if (project.isWorkspace) {
            // 工作空间项目：创建主模块，然后为每个子模块创建带父模块的模块实体
            val mainModule = createMainModule(storage, project)

            for (module in project.workspace!!.modules) {
                try {
                    syncModule(storage, module, mainModule)
                    LOG.info("Synced module: ${module.name} from project ${project.name}")
                } catch (e: Exception) {
                    LOG.error("Failed to sync module ${module.name}", e)
                }
            }
        } else {
            // 单模块项目：直接创建模块，不需要父模块
            project.module?.let { module ->
                try {
                    syncModuleWithoutParent(storage, module)
                    LOG.info("Synced single module: ${module.name} from project ${project.name}")
                } catch (e: Exception) {
                    LOG.error("Failed to sync single module ${module.name}", e)
                }
            } ?: LOG.warn("No module found in single-module project ${project.name}")
        }



        workspaceModel.update("Sync CangJie Projects") {
            it.applyChangesFrom(storage)
        }


        LOG.info("Workspace Model sync completed")
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
                        val excludeUrlBuilders = mutableListOf<ExcludeUrlEntity.Builder>()
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
     */
    private fun syncModuleWithoutParent(builder: MutableEntityStorage, cjModule: CjModule) {
        val entitySource = CangJieEntitySource(
            moduleName = cjModule.name,
            projectPath = cjModule.project.rootDir.path
        )

        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()
        val moduleName = "${MODULE_NAME_PREFIX}${cjModule.name}"
        val contentRootUrl = urlManager.getOrCreateFromUrl(cjModule.rootDir.url)
        val moduleDependencies = buildModuleDependencies(builder, cjModule)

        builder.addEntity(
            ModuleEntity(
                name = moduleName,
                dependencies = listOf(
                    ModuleSourceDependency,
                    InheritedSdkDependency
                ) + moduleDependencies,
                entitySource = entitySource
            ) {
                val contentRootEntity = ContentRootEntity(
                    url = contentRootUrl,
                    excludedPatterns = emptyList(),
                    entitySource = entitySource
                ) {
//                     创建所有 SourceRootEntity
                    val sourceRootBuilders = mutableListOf<SourceRootEntity.Builder>()
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
                    val excludeUrlBuilders = mutableListOf<ExcludeUrlEntity.Builder>()
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
     * 清理所有由 CangJieEntitySource 管理的旧实体
     *
     * 在同步开始前，移除所有标记为 CangJieEntitySource 的实体，
     * 避免旧数据残留。新的实体会在 syncModule 中重新创建。
     *
     * @param builder 可变的实体存储构建器
     */
    private fun cleanupOldEntities(builder: MutableEntityStorage) {
        val cangjieModules = builder.entities(ModuleEntity::class.java)
//            .filter { it.entitySource is CangJieEntitySource }
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
     * - 排除目录（如 target 构建目录）
     * - 模块依赖关系（从 cjModule.dependencies 提取）
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 需要同步的仓颉模块
     */
    private fun syncModule(builder: MutableEntityStorage, cjModule: CjModule, parentModule: ModuleEntity) {
        val entitySource = CangJieEntitySource(
            moduleName = cjModule.name,
            projectPath = cjModule.project.rootDir.path
        )

        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()

        // 创建模块名称（添加前缀避免冲突）
        val moduleName = "${parentModule.name}.${cjModule.name}"

        // 准备 ContentRoot URL
        val contentRootUrl = urlManager.getOrCreateFromUrl(cjModule.rootDir.url)

        // 构建模块依赖列表（使用工作空间前缀）
        val moduleDependencies = buildModuleDependencies(builder, cjModule, "${parentModule.name}.")

        // 创建 ModuleEntity（包含所有子实体）
        builder.addEntity(
            ModuleEntity(
                name = moduleName,
                dependencies = listOf(
                    ModuleSourceDependency,
                    InheritedSdkDependency
                ) + moduleDependencies,  // 添加模块依赖
                entitySource = entitySource
            ) {
                // 创建 ContentRootEntity
                val contentRootEntity = ContentRootEntity(
                    url = contentRootUrl,
                    excludedPatterns = emptyList(),
                    entitySource = entitySource
                ) {
                    // 创建所有 SourceRootEntity
                    val sourceRootBuilders = mutableListOf<SourceRootEntity.Builder>()
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
                    val excludeUrlBuilders = mutableListOf<ExcludeUrlEntity.Builder>()
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
     * 构建模块依赖列表
     *
     * 将 CjModule 的依赖信息转换为 Workspace Model 的 ModuleDependency。
     * 这是 Workspace Model 的补充功能，为 IDE 提供更详细的依赖元数据。
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 仓颉模块
     * @param moduleNamePrefix 模块名称前缀（用于工作空间项目）
     * @return ModuleDependency 列表
     */
    private fun buildModuleDependencies(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        moduleNamePrefix: String = MODULE_NAME_PREFIX
    ): List<ModuleDependency> {
        val dependencies = mutableListOf<ModuleDependency>()

        for (dependency in cjModule.dependencies) {
            // 查找依赖的模块
            val dependencyModule = cjModule.project.findModule(dependency.moduleName)
            if (dependencyModule != null) {
                // 构建依赖模块的完整名称（带前缀）
                val dependencyModuleName = "$moduleNamePrefix${dependency.moduleName}"

                // 创建模块依赖
                dependencies.add(
                    ModuleDependency(
                        module = ModuleId(dependencyModuleName),
                        exported = dependency.exported,
                        scope = when (dependency.scope) {
                            org.cangnova.cangjie.project.model.CjDependencyScope.COMPILE -> DependencyScope.COMPILE
                            org.cangnova.cangjie.project.model.CjDependencyScope.TEST -> DependencyScope.TEST
                            org.cangnova.cangjie.project.model.CjDependencyScope.RUNTIME -> DependencyScope.RUNTIME
                            org.cangnova.cangjie.project.model.CjDependencyScope.PROVIDED -> DependencyScope.PROVIDED
                        },
                        productionOnTest = false
                    )
                )

                LOG.debug("Added module dependency: ${cjModule.name} -> ${dependency.moduleName} (scope=${dependency.scope})")
            } else {
                LOG.warn("Dependency module not found: ${dependency.moduleName} for module ${cjModule.name}")
            }
        }

        return dependencies
    }


}
