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

package org.cangnova.cangjie.project.service.impl


import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.Function
import com.intellij.util.indexing.LightDirectoryIndex
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.project.*
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectEventType
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.roots
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.service.CjProjectsService.Companion.CANGJIE_PROJECTS_REFRESH_TOPIC
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.project.task.CangJieSyncTask
import org.cangnova.cangjie.project.workspace.CjWorkspaceModelSync
import org.cangnova.cangjie.result.CjProcessResult
import org.cangnova.cangjie.task.taskQueue
import org.cangnova.cangjie.utils.*
import org.jdom.Element
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException


/**
 * 是否启用新项目模型导入
 *
 * 从 IntelliJ Registry 读取配置项 `org.cangnova.cangjie.cjpm.new.auto.import`，
 * 决定是否启用基于 [ExternalSystemProjectTracker] 的新项目自动导入机制。
 *
 * - `true`: 使用新的自动导入机制，通过 [CangJieExternalSystemProjectAware] 自动监听配置文件变更
 * - `false`: 使用旧的手动刷新机制
 *
 * @return 是否启用新项目模型导入，默认为 false
 */
val isNewProjectModelImportEnabled: Boolean
    get() = Registry.`is`("org.cangnova.cangjie.project.new.auto.import", false)

/**
 * 仓颉项目管理服务实现
 *
 * ## 架构设计：Project Model 优先，Workspace Model 补充
 *
 * 该服务实现了"Project Model 优先，Workspace Model 补充"的架构：
 * - **Project Model（主）**：基于 CjProject/CjModule 的自定义模型，作为唯一数据源
 * - **Workspace Model（辅）**：仅在必要时同步，为 IDE 子系统提供补充元数据（当前已禁用）
 *
 * ## 核心职责
 * - 项目的发现、创建和生命周期管理
 * - 模块索引和快速查找（O(1) 文件到模块映射）
 * - 项目事件发布（CREATED、OPENED、UPDATED、REMOVED、CONFIG_CHANGED）
 * - 与外部系统框架集成（通过 [CangJieExternalSystemProjectAware]）
 * - 项目配置变更的监听和自动刷新
 *
 * ## 单项目模型
 * 每个 IntelliJ 项目对应一个仓颉项目（cjProject），项目内部可以包含多个模块。
 * 这简化了项目管理，与 IntelliJ 平台的项目模型保持一致。
 *
 * 服务作用域为 PROJECT 级别，每个 IntelliJ 项目对应一个服务实例。
 * 根据配置开关 [isNewProjectModelImportEnabled] 决定是否启用新的项目自动导入机制。
 *
 * @property intellijProject IntelliJ 项目实例
 * @see CjProjectsService
 * @see CangJieExternalSystemProjectAware
 */
@Service(Service.Level.PROJECT)
@State(
    name = "CangJieProjects", storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
class CjProjectsServiceImpl(
    override val intellijProject: Project
) : CjProjectsService, PersistentStateComponent<Element>, Disposable {
    /**
     * 项目提供者缓存
     *
     * 从扩展点获取的第一个可用的 [CjProjectProvider] 实例。
     * 负责处理项目的创建和识别逻辑。
     */
    private val providerCache =
        CjProjectProvider.EP_NAME.extensionList.firstOrNull() ?: error("CJProjectProvider not found")


    /**
     * 注册项目感知器到外部系统框架
     *
     * 将 [CangJieExternalSystemProjectAware] 注册到 IntelliJ 的 [ExternalSystemProjectTracker]，
     * 启用项目配置文件的自动监听和项目自动刷新功能。同时订阅项目设置变更事件，
     * 当设置影响 CJPM 元数据时，标记项目为脏并调度刷新。
     *
     * 注意：不会为以下情况注册：
     * - 默认项目（Default Project）
     * - 轻量级测试项目（Light Test Project）
     *
     * @param project IntelliJ 项目实例
     * @param disposable 用于管理注册生命周期的 Disposable
     */
    private fun registerProjectAware(project: Project, disposable: Disposable) {
        // 为默认项目注册 `CangJieExternalSystemProjectAware` 没有意义。
        // 而且，这可能会破坏可搜索选项的构建。
        // 此外，我们不需要在轻量级测试中注册 `CangJieExternalSystemProjectAware`，因为：
        // - 我们只在重量级测试中检查它
        // - 它严重依赖于服务释放机制，而这在轻量级测试中不起作用
        if (project.isDefault || isUnitTestMode && (project as? ProjectEx)?.isLight == true) return

        // 创建仓颉外部系统项目感知器实例
        val cangjieProjectAware = CangJieExternalSystemProjectAware(project)

        // 获取外部系统项目追踪器，负责监听配置文件变更
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)

        // 将项目感知器注册到追踪器，并关联到 disposable 生命周期
        projectTracker.register(cangjieProjectAware, disposable)

        // 激活项目追踪，开始监听配置文件变更
        projectTracker.activate(cangjieProjectAware.projectId)

        // 连接到项目消息总线并订阅仓颉项目设置变更事件
        // 当 disposable 被释放时，订阅会自动取消
        project.messageBus.connect(disposable)
            .subscribe(

                CANGJIE_SETTINGS_TOPIC,  // 订阅仓颉设置变更主题
                object : CjSettingsListener {
                    // 当项目设置发生变更时被调用
                    override fun <T : CjProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>) {
                        // 检查设置变更是否影响元数据（如依赖、工具链配置等）
                        if (e.affectsMetadata) {
                            val tracker = ExternalSystemProjectTracker.getInstance(project)

                            // 标记项目为"脏"状态，表示需要重新加载
                            tracker.markDirty(cangjieProjectAware.projectId)

                            // 调度项目刷新，IDE 会在合适的时机重新加载项目结构
                            tracker.scheduleProjectRefresh()
                        }
                    }
                })
    }

    /** 日志记录器 */
    private val log = logger<CjProjectsServiceImpl>()

    override var initialized: Boolean = false


    /**
     * 当前仓颉项目的异步值持有者
     */
    private val project = AsyncValue<CjProject>(noProjectMarker)

    /**
     * 目录索引，用于快速从VirtualFile映射到CjProject
     * 这是一个轻量级的索引实现，支持快速查找文件所属的项目
     */
    private val directoryIndex: LightDirectoryIndex<CjProject> =
        LightDirectoryIndex(intellijProject, noProjectMarker) { index ->
            // 遍历所有项目，为每个项目的可索引目录建立映射

            // 获取该项目对应的提供者
            val provider = providerCache

            // 使用提供者获取需要索引的目录列表
            val directories = provider.getIndexableDirectories(cjProject)

            // 为每个目录建立到项目的映射
            directories.forEach { dir ->
                index.putInfo(dir, cjProject)
            }

        }


    /**
     * 模块索引
     *
     * 负责维护从 VirtualFile 到 CjModule 的快速映射
     */
    private val moduleIndex = CangJieModuleIndex(intellijProject, this)

    /**
     * Workspace Model 同步器
     *
     * 负责将仓颉项目模型同步到 IntelliJ 的 Workspace Model
     */
    private val workspaceModelSync = intellijProject.service<CjWorkspaceModelSync>()


    override val cjProject: CjProject
        get() = project.currentState


    init {
        val newProjectModelImportEnabled = isNewProjectModelImportEnabled
        if (newProjectModelImportEnabled) {
            registerProjectAware(intellijProject, this)
        }

        with(intellijProject.messageBus.connect()) {
            if (!newProjectModelImportEnabled) {
                if (!isUnitTestMode) {
                    subscribe(
                        VirtualFileManager.VFS_CHANGES,
                        CangJieConfigFileWatcher(this@CjProjectsServiceImpl, fun() {
                            if (!intellijProject.cangjieSettingsData.autoUpdateEnabled) return
                            refreshProject()
                        })
                    )
                }

                subscribe(
                    CANGJIE_SETTINGS_TOPIC,
                    object : CjSettingsListener {
                        override fun <T : CjProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>) {
                            if (e.affectsMetadata) {
                                refreshProject()
                            }
                        }
                    })
            }
            subscribe(
                ModuleListener.TOPIC,
                object : ModuleListener {
                    override fun moduleRemoved(project: Project, module: Module) {
                        super.moduleRemoved(project, module)
                    }

                    override fun modulesRenamed(
                        project: Project,
                        modules: List<Module>,
                        oldNameProvider: Function<in Module, String?>
                    ) {
                        super.modulesRenamed(project, modules, oldNameProvider)
                    }
                })

        }

    }


    override fun discoverProject(rootDir: VirtualFile) {
        // 获取项目提供者
        val provider = providerCache

        // 尝试使用提供者创建项目
        if (provider.canHandle(rootDir)) {
            log.info("Found project provider: ${provider.providerName} for $rootDir")
            val newProject = provider.createProject(rootDir, intellijProject)
            if (newProject != null) {
                // 使用 modifyProjectSync 设置项目并触发相关事件
                modifyProjectSync(ModifyProjectsOptions.DEFAULT) { newProject }
                log.info("Project initialized: ${newProject.name} at ${newProject.rootDir}")

                // 发布项目创建事件
                publishEvent(CjProjectEvent(newProject, CjProjectEventType.CREATED))
                return
            }
        }

        log.warn("No suitable project provider found for $rootDir")
    }

    override fun addModule(module: CjModule) {
        log.info("Module added: ${module.name} at ${module.rootDir}, triggering project refresh")
        // 在单项目模型中，添加模块意味着项目结构发生变化，需要刷新项目
        refreshProject()
    }

    override fun removeModule(module: CjModule) {
        log.info("Module removed: ${module.name} at ${module.rootDir}, triggering project refresh")
        // 在单项目模型中，移除模块意味着项目结构发生变化，需要刷新项目
        refreshProject()
    }


    /**
     * 项目更新选项
     *
     * @property lightweight 是否为轻量级更新（不触发完整的刷新事件和根目录变更）
     * @property publishRefreshEvents 是否发布刷新状态事件（onRefreshStarted/onRefreshFinished）
     * @property resetIndices 是否重置索引
     * @property updateRoots 是否更新项目根目录
     */
    data class ModifyProjectsOptions(
        val lightweight: Boolean = false,
        val publishRefreshEvents: Boolean = true,
        val resetIndices: Boolean = true,
        val updateRoots: Boolean = true
    ) {
        companion object {
            /**
             * 默认选项：完整更新流程
             */
            val DEFAULT = ModifyProjectsOptions()

            /**
             * 轻量级选项：适用于单个项目的增删操作
             * - 不发布刷新事件
             * - 不更新项目根目录（避免触发大规模索引重建）
             * - 仍然重置索引以保持一致性
             */
            val LIGHTWEIGHT = ModifyProjectsOptions(
                lightweight = true,
                publishRefreshEvents = false,
                updateRoots = false
            )

            /**
             * 初始化加载选项：适用于 loadState
             * - 不发布刷新事件（避免在启动时触发不必要的通知）
             * - 不更新根目录（启动后会单独刷新）
             * - 不重置索引（延迟到刷新时）
             * - 跳过文件类型关联和消息发布（避免阻塞服务初始化）
             */
            val LOAD_STATE = ModifyProjectsOptions(
                lightweight = true,
                publishRefreshEvents = false,
                resetIndices = false,
                updateRoots = false
            )
        }
    }


    /**
     * 项目模型修改的统一入口（异步版本）
     *
     * 负责刷新单个项目模型，确保：
     * 1. 状态更新的原子性和一致性
     * 2. 相关 IDE 子系统（索引、监听器、文件类型、项目根）的同步
     * 3. 事件发布的完整性
     *
     * @param options 更新选项，控制更新流程的行为
     * @param updater 更新函数，接收当前项目，返回包含更新后项目的 CompletableFuture
     * @return CompletableFuture，包含更新后的项目
     */
    protected fun modifyProjectAsync(
        options: ModifyProjectsOptions = ModifyProjectsOptions.DEFAULT,
        updater: (CjProject) -> CompletableFuture<CjProject>
    ): CompletableFuture<CjProject> {

        val refreshStatusPublisher = if (options.publishRefreshEvents) {
            intellijProject.messageBus.syncPublisher(CANGJIE_PROJECTS_REFRESH_TOPIC)
        } else null

        // 包装 updater 函数，在调用 updater 前发布刷新开始通知
        val wrappedUpdater = { proj: CjProject ->
            refreshStatusPublisher?.onRefreshStarted()
            updater(proj)
        }

        return project.updateAsync(wrappedUpdater)
            .thenApply { proj ->
                // 在 LOAD_STATE 模式下，跳过所有同步操作以避免阻塞服务初始化
                if (options != ModifyProjectsOptions.LOAD_STATE) {
                    invokeAndWaitIfNeeded {
                        runWriteAction {
                            // 文件类型关联
                            if (proj.isValid && !options.lightweight) {
                                val fileTypeManager = FileTypeManager.getInstance()
                                fileTypeManager.associateExtension(
                                    CangJieFileType.INSTANCE,
                                    CangJieFileType.INSTANCE.defaultExtension
                                )
                            }

                            // 重置索引
                            if (options.resetIndices) {
                                directoryIndex.resetIndex()
                            }

                            // 更新项目根目录
                            if (options.updateRoots) {
                                runWithNonLightProject(intellijProject) {
                                    ProjectRootManagerEx.getInstanceEx(intellijProject)
                                        .makeRootsChange(
                                            EmptyRunnable.getInstance(),
                                            RootsChangeRescanningInfo.TOTAL_RESCAN
                                        )
                                }
                            }

                            // 发布项目更新通知
                            intellijProject.messageBus.syncPublisher(CANGJIE_PROJECTS_TOPIC)
                                .cangjieProjectsUpdated(this, listOf(proj))

                            // 发布项目更新事件（用于 UI 组件如 CjProjectToolWindow）
//                            if (!options.lightweight) {
//                                publishEvent(CjProjectEvent(proj, CjProjectEventType.UPDATED))
//                            }

                            initialized = true
                        }
                    }
                } else {
                    // 在 LOAD_STATE 模式下，仅设置初始化标志
                    initialized = true
                }
                proj
            }.whenComplete { proj, err ->
                // 发布刷新结束通知
                if (refreshStatusPublisher != null) {
                    val status = err?.toRefreshStatus() ?: CjProjectsService.RefreshStatus.SUCCESS
                    refreshStatusPublisher.onRefreshFinished(status)
                }
            }
    }

    /**
     * 项目模型修改的统一入口（同步版本）
     *
     * 适用于简单的同步更新操作。
     * 对于耗时操作，应使用 [modifyProjectAsync] 避免阻塞。
     *
     * @param options 更新选项，控制更新流程的行为
     * @param updater 更新函数，接收当前项目，返回更新后的项目
     * @return 更新后的项目
     */
    protected fun modifyProjectSync(
        options: ModifyProjectsOptions = ModifyProjectsOptions.DEFAULT,
        updater: (CjProject) -> CjProject
    ): CjProject {
        return modifyProjectAsync(options) { proj ->
            CompletableFuture.completedFuture(updater(proj))
        }.join()
    }


    /**
     * 将异常转换为刷新状态
     */
    private fun Throwable.toRefreshStatus(): CjProjectsService.RefreshStatus {
        return when {
            this is ProcessCanceledException -> CjProjectsService.RefreshStatus.CANCEL
            this is CompletionException && cause is ProcessCanceledException -> CjProjectsService.RefreshStatus.CANCEL
            else ->
                CjProjectsService.RefreshStatus.FAILURE
        }
    }

    /**
     * 刷新项目
     *
     * 使用 CangJieSyncTask 进行项目刷新，提供更好的进度显示和错误处理。
     * 该方法会触发完整的刷新流程，包括：
     * 1. 执行项目的 refresh() 方法重新加载项目结构
     * 2. 使用 CangJieSyncTask 提供进度显示
     * 3. 通过 modifyProjectSync 确保正确的事件发布和索引更新
     */
    override fun refreshProject() {
        log.info("Refreshing project: ${cjProject.name}")

        // 使用 modifyProjectSync 确保正确的事件发布和索引更新
        modifyProjectSync(ModifyProjectsOptions.DEFAULT) { currentProject ->
            // 使用 CangJieSyncTask 进行项目刷新，提供更好的进度显示和错误处理
            val syncTask = CangJieSyncTask(intellijProject, {
                publishEvent(CjProjectEvent(it, CjProjectEventType.CONFIG_CHANGED))
            }
            )
            intellijProject.taskQueue.run(syncTask)


            currentProject
        }
    }


    override fun findModuleForFile(file: VirtualFile): CjModule? {
        // 使用模块索引进行 O(1) 查找
        return moduleIndex.findModuleForFile(file)
    }

    override fun createProject(
        sdkId: String,
        owner: Disposable,
        directory: VirtualFile,
        projectType: String,
        name: String?
    ): CjProcessResult<GeneratedFilesHolder> {
        return providerCache.createProjectFromPhysicalFile(sdkId, intellijProject, owner, directory, projectType, name)
    }

    /**
     * 发布项目事件
     */
    private fun publishEvent(event: CjProjectEvent) {
        val publisher = intellijProject.messageBus.syncPublisher(CjProjectListener.TOPIC)
        when (event.eventType) {
            CjProjectEventType.CREATED -> publisher.projectCreated(event)
            CjProjectEventType.OPENED -> publisher.projectOpened(event)
            CjProjectEventType.UPDATED -> publisher.projectUpdated(event)
            CjProjectEventType.REMOVED -> publisher.projectRemoved(event)
            CjProjectEventType.CONFIG_CHANGED -> publisher.projectConfigChanged(event)
        }
    }

    override fun dispose() {

    }

    override fun getState(): Element {
        val state = Element("state")
        if (cjProject.isValid) {
            val projectElement = Element("project")
            projectElement.setAttribute("PATH", cjProject.rootDir.path)
            state.addContent(projectElement)
        }
        return state
    }

    /**
     * 当没有状态加载时调用
     * 这不仅在首次创建服务时调用，
     * 也在之前保存的状态为空时调用
     */
    override fun noStateLoaded() {


        // 显示在 [org.cangnova.cangjie.notifications.MissingToolchainNotificationProvider]

        initialized = true // 不需要锁定B/C的服务初始时间

//应该使用该服务进行初始化，因为它存储了项目数据的一部分
        intellijProject.service<UserDisabledFeaturesHolder>()

        // 初始化项目模型（仅在没有保存状态时执行）
        discoverProject(intellijProject.baseDir)
    }

    override fun loadState(state: Element) {
        val projects = state.getChildren("project")
        val userDisabledFeaturesMap = intellijProject.service<UserDisabledFeaturesHolder>()
            .takeLoadedUserDisabledFeatures()

        var loadedProject: CjProject? = null
        for (projectElement in projects) {
            val path = projectElement.getAttributeValue("PATH").toPath()

            val file = VirtualFileManager.getInstance().findFileByNioPath(path) ?: continue
//            val userDisabledFeatures = userDisabledFeaturesMap[manifest] ?: UserDisabledFeatures.EMPTY
            val newProject =
                providerCache.createProject(file, intellijProject)
            if (newProject != null) {
                loadedProject = newProject
                break // 单项目模型，只加载第一个
            }
        }

        // 直接设置项目，跳过任何同步操作以避免阻塞服务初始化
        if (loadedProject != null) {
            this.project.updateSync { loadedProject }

            // 发布项目打开事件
            publishEvent(CjProjectEvent(loadedProject, CjProjectEventType.OPENED))
        }

        // 延迟刷新到更合适的时机，避免在启动阶段进行重量级操作
        val disableRefresh =
            System.getProperty(CANGJIE_DISABLE_PROJECT_REFRESH_ON_CREATION, "false").toBooleanStrictOrNull()
        if (disableRefresh != true && loadedProject != null) {
            // 在后台线程执行项目刷新，避免阻塞 EDT

            if (intellijProject.isDisposed) return
            // 再次检查，避免重复刷新
            if (loadedProject.isValid) {
                refreshProject()

            }
        }
    }

    companion object {
        /**
         * 系统属性：是否在创建时禁用项目刷新
         */
        const val CANGJIE_DISABLE_PROJECT_REFRESH_ON_CREATION: String = "cangjie.disable.project.refresh.on.creation"
    }
}


/**
 * 在非轻量级项目上执行操作
 * 轻量级项目通常用于单元测试，需要特殊处理
 *
 * @param project 当前项目实例
 * @param action 要执行的操作
 */
private inline fun runWithNonLightProject(project: Project, action: () -> Unit) {
    if ((project as? ProjectEx)?.isLight != true) {
        action()
    } else {
        check(isUnitTestMode)
    }
}

/**
 * 仓颉模块索引
 *
 * 负责维护从 VirtualFile 到 CjModule 的快速映射，实现 O(1) 查找。
 * 该索引监听项目更新事件，自动重建索引以保持数据一致性。
 *
 * 设计思路：
 * - 为每个 CjProject 维护独立的 LightDirectoryIndex
 * - 索引模块的所有相关目录：模块根目录、源码目录、输出目录
 * - 使用 Optional 包装模块对象，以区分"未找到"和"无模块"两种情况
 * - 订阅 CANGJIE_PROJECTS_TOPIC，在项目更新时自动刷新索引
 *
 * @property intellijProject IntelliJ 项目实例
 * @property service 仓颉项目服务实例
 */
class CangJieModuleIndex(
    private val intellijProject: Project,
    private val service: CjProjectsService
) : CjProjectListener {
    /**
     * 模块索引
     * 使用 Optional 包装以区分"未找到"和"无模块"两种情况
     */
    private var moduleIndex: LightDirectoryIndex<Optional<CjModule>>? = null

    /**
     * 索引生命周期管理器
     */
    private var indexDisposable: Disposable? = null

    init {
        // 订阅仓颉项目更新事件
        intellijProject.messageBus.connect(intellijProject)
            .subscribe(CjProjectListener.TOPIC, this)
    }

    /**
     * 项目创建时的回调
     */
    override fun projectCreated(event: CjProjectEvent) {
        runWriteAction {
            rebuildIndices()
        }
    }

    /**
     * 项目更新时的回调
     */
    override fun projectUpdated(event: CjProjectEvent) {
        runWriteAction {
            rebuildIndices()
        }
    }

    /**
     * 项目删除时的回调
     */
    override fun projectRemoved(event: CjProjectEvent) {
        runWriteAction {
            rebuildIndices()
        }
    }

    /**
     * 项目配置变更时的回调
     */
    override fun projectConfigChanged(event: CjProjectEvent) {
        runWriteAction {
            rebuildIndices()
        }
    }

    /**
     * 为文件查找所属模块
     *
     * @param file 要查找的文件
     * @return 文件所属的模块，如果不属于任何模块返回 null
     */
    fun findModuleForFile(file: VirtualFile): CjModule? {
        checkReadAccessAllowed()

        // 使用索引查找模块
        return moduleIndex?.getInfoForFile(file)?.orElse(null)
    }

    /**
     * 重建所有索引
     *
     * 该方法必须在写操作中调用，因为它会创建和注册 Disposable。
     * 重建过程：
     * 1. 清理旧的索引
     * 2. 为项目的每个模块建立目录映射
     * 3. 索引模块根目录、源码目录和输出目录
     */
    private fun rebuildIndices() {
        checkWriteAccessAllowed()

        // 清理旧索引
        resetIndex()

        // 创建新的 Disposable，用于管理索引生命周期
        val disposable = Disposer.newDisposable("CangJieModuleIndexDisposable")
        Disposer.register(intellijProject, disposable)

        // 构建模块索引
        val cjProject = service.cjProject
        moduleIndex = LightDirectoryIndex(disposable, Optional.empty()) { index ->
            if (cjProject.isWorkspace) {
                // 遍历项目中的所有模块
                for (module in cjProject.workspace!!.modules) {
                    val moduleInfo = Optional.of(module)

                    // 索引模块根目录
                    index.putInfo(module.rootDir, moduleInfo)

                    // 索引所有源码集的根目录
                    for (sourceSet in module.sourceSets) {
                        for (root in sourceSet.roots) {
                            index.putInfo(root, moduleInfo)
                        }
                    }

                    // 索引所有目标的输出目录

                }
            } else {
                val moduleInfo = Optional.of(cjProject.module!!)

                // 索引模块根目录
                index.putInfo(cjProject.module!!.rootDir, moduleInfo)

                // 索引所有源码集的根目录
                for (sourceSet in cjProject.module!!.sourceSets) {
                    for (root in sourceSet.roots) {
                        index.putInfo(root, moduleInfo)
                    }
                }

                // 索引所有目标的输出目录

            }
        }

        indexDisposable = disposable
    }

    /**
     * 清理索引
     *
     * 释放所有索引占用的资源。
     */
    private fun resetIndex() {
        indexDisposable?.let { Disposer.dispose(it) }
        indexDisposable = null
        moduleIndex = null
    }
}


