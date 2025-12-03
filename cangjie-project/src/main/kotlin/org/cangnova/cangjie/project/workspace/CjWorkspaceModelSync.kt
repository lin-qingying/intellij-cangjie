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
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CjDependencyScope
import org.cangnova.cangjie.model.CjResolvedDependency
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.pathDependencies
import org.cangnova.cangjie.project.model.roots
import org.cangnova.cangjie.service.CjDependencyService
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
     * 同步所有仓颉项目到 Workspace Model
     *
     * 该方法会：
     * 1. 清理所有由 CangJieEntitySource 标记的旧实体
     * 2. 为每个 CjModule 创建对应的 ModuleEntity
     * 3. 配置模块的源码根、内容根和排除目录
     *
     * @param project 需要同步的仓颉项目列表
     */
    suspend fun syncProject(project: CjProject) {

        val workspaceModel = intellijProject.workspaceModel

        val storageSnapshot = workspaceModel.currentSnapshot


        val storage = MutableEntityStorage.from(storageSnapshot)

//        清除所有模块
        // 1. 清理旧的仓颉模块实体
        cleanupOldEntities(storage)


        if (project.isWorkspace) {
            // 工作空间项目：创建主模块，然后为每个子模块创建带父模块的模块实体
            val mainModule = createMainModule(storage, project)

            for (module in (project.workspace ?: return).modules) {
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

        // 构建依赖（分别获取模块依赖和库依赖）
        val (moduleDeps, libraryDeps) = buildAllDependencies(builder, cjModule)

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

        // 构建依赖（分别获取模块依赖和库依赖）
        val (moduleDeps, libraryDeps) = buildAllDependencies(builder, cjModule, "${parentModule.name}.")

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
     * 构建所有依赖
     *
     * 将 CjModule 的依赖信息转换为 Workspace Model 的 ModuleDependency 和 LibraryDependency。
     * 包括：
     * - Path 依赖 → ModuleDependency
     * - Library/Git/System 依赖 → LibraryDependency
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 仓颉模块
     * @param moduleNamePrefix 模块名称前缀（用于工作空间项目）
     * @return Pair<ModuleDependency列表, LibraryDependency列表>
     */
    private fun buildAllDependencies(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        moduleNamePrefix: String = MODULE_NAME_PREFIX
    ): Pair<List<ModuleDependency>, List<LibraryDependency>> {
        val moduleDeps = mutableListOf<ModuleDependency>()
        val libraryDeps = mutableListOf<LibraryDependency>()

        val entitySource = CangJieEntitySource(
            moduleName = cjModule.name,
            projectPath = cjModule.project.rootDir.path
        )

        // 1. 处理 Path 依赖（模块间依赖）
        for (pathDep in cjModule.pathDependencies) {
            // 查找依赖的模块
            val dependencyModule = cjModule.project.findModule(pathDep.name)
            if (dependencyModule != null) {
                // 构建依赖模块的完整名称（带前缀）
                val dependencyModuleName = "$moduleNamePrefix${pathDep.name}"

                // 创建模块依赖
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
                LOG.warn("Dependency module not found: ${pathDep.name} for module ${cjModule.name}")
            }
        }

        // 2. 处理外部库依赖（Library, Git, System）
        val externalLibs = buildLibraryDependencies(builder, cjModule, entitySource)
        libraryDeps.addAll(externalLibs)

        return Pair(moduleDeps, libraryDeps)
    }

    /**
     * 构建外部库依赖列表
     *
     * 为 Library、Git 和 System 依赖创建 LibraryEntity 和 LibraryDependency。
     *
     * @param builder 可变的实体存储构建器
     * @param cjModule 仓颉模块
     * @param entitySource 实体源
     * @return LibraryDependency 列表
     */
    private fun buildLibraryDependencies(
        builder: MutableEntityStorage,
        cjModule: CjModule,
        entitySource: EntitySource
    ): List<LibraryDependency> {
        val dependencies = mutableListOf<LibraryDependency>()
        val dependencyService = CjDependencyService.getInstance()

        // 获取所有非 Path 类型的依赖
        val externalDependencies = cjModule.allDependencies.filter { dep ->
            dep !is CjDependency.Path
        }

        for (dependency in externalDependencies) {
            try {
                // 解析依赖以获取路径信息
                val resolved = dependencyService.resolveDependency(dependency, intellijProject)

                if (resolved != null && resolved.isResolved) {
                    // 创建 LibraryEntity 和 LibraryDependency
                    val libraryDependency = createLibraryDependency(
                        builder,
                        dependency,
                        resolved,
                        entitySource
                    )

                    if (libraryDependency != null) {
                        dependencies.add(libraryDependency)
                        LOG.debug("Added library dependency: ${cjModule.name} -> ${dependency.name}:${dependency.version}")
                    }
                } else {
                    LOG.warn("Failed to resolve dependency: ${dependency.name}:${dependency.version} - ${resolved?.errorMessage ?: "Unknown error"}")
                }
            } catch (e: Exception) {
                LOG.error("Error resolving dependency ${dependency.name}:${dependency.version}", e)
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
        resolved: CjResolvedDependency,
        entitySource: EntitySource
    ): LibraryDependency? {
        val urlManager = WorkspaceModel.getInstance(intellijProject).getVirtualFileUrlManager()

        // 生成库的唯一名称
        val libraryName = resolved.buildLibraryName(dependency)

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
        when {
            resolved.resolvedLibrary != null -> {
                val library = resolved.resolvedLibrary ?: return null

                // 添加库文件路径（classes）
                val libraryFile = library.libraryPath.toFile()
                if (libraryFile.exists()) {
                    val libraryUrl = urlManager.getOrCreateFromUrl("file://${libraryFile.absolutePath}")
                    classesRoots.add(LibraryRoot(libraryUrl, LibraryRootTypeId.COMPILED))
                    LOG.debug("Added library classes root: ${libraryFile.absolutePath}")
                }

                // 添加源码路径
                library.sourcePath?.toFile()?.let { sourceFile ->
                    if (sourceFile.exists()) {
                        val sourceUrl = urlManager.getOrCreateFromUrl("file://${sourceFile.absolutePath}")
                        sourcesRoots.add(LibraryRoot(sourceUrl, LibraryRootTypeId.SOURCES))
                        LOG.debug("Added library sources root: ${sourceFile.absolutePath}")
                    }
                }

                // 添加文档路径
                library.documentationPath?.toFile()?.let { docFile ->
                    if (docFile.exists()) {
                        val docUrl = urlManager.getOrCreateFromUrl("file://${docFile.absolutePath}")
                        javadocRoots.add(LibraryRoot(docUrl, LibraryRootTypeId("JAVADOC")))
                        LOG.debug("Added library documentation root: ${docFile.absolutePath}")
                    }
                }
            }

            resolved.resolvedPackage != null -> {
                val pkg = resolved.resolvedPackage!!

                // 包通常有本地路径，作为 classes root
                pkg.localPath?.toFile()?.let { pkgDir ->
                    if (pkgDir.exists()) {
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
