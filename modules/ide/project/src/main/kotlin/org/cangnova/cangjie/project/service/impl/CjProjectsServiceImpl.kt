/*
 * Copyright 2026 LinQingYing. and contributors.
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
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.*
import org.cangnova.cangjie.ide.base.analysis.builtins.BuiltinsDecompiledDocumentRefresher
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.project.*
import org.cangnova.cangjie.project.event.CjProjectEvent
import org.cangnova.cangjie.project.event.CjProjectEventType
import org.cangnova.cangjie.project.event.CjProjectListener
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.service.CjProjectsService.Companion.CANGJIE_PROJECTS_REFRESH_TOPIC
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.project.service.ModifyProjectsOptions
import org.cangnova.cangjie.project.task.CangJieProjectSyncTask
import org.cangnova.cangjie.project.workspace.CjWorkspaceModelSync
import org.cangnova.cangjie.projectStructure.CangJieProjectStructureProviderService
import org.cangnova.cangjie.result.CjProcessResult
import org.cangnova.cangjie.task.taskQueue
import org.cangnova.cangjie.toolchain.api.CANGJIE_PROJECT_SDK_CONFIG_TOPIC
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfigChangedEvent
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfigListener
import org.cangnova.cangjie.utils.*
import org.jdom.Element
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
@State(
    name = "CangJieProjects", storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
@Service(Service.Level.PROJECT)
internal class CjProjectsServiceImpl(
    override val intellijProject: Project,
    @Suppress("UNUSED_PARAMETER") private val cs: CoroutineScope
) : CjProjectsService, PersistentStateComponent<Element>, Disposable {

    /**
     * 项目提供者缓存
     *
     * 从扩展点获取匹配当前构建系统的 [CjProjectProvider] 实例。
     * 负责处理项目的创建和识别逻辑。
     */
    private val providerCache: CjProjectProvider by lazy {
        val buildSystemService = CjProjectBuildSystemService.getInstance()
        val buildSystemId = buildSystemService.getBuildSystem()?.id

        CjProjectProvider.EP_NAME.extensionList.find { provider ->
            buildSystemId == null || provider.getBuildSystemId().id == buildSystemId
        } ?: CjProjectProvider.EP_NAME.extensionList.firstOrNull()
        ?: error("CJProjectProvider not found")
    }


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
     * 刷新防抖动 Job
     *
     * 用于防止频繁刷新项目，当多次连续调用刷新时，只执行最后一次。
     */
    private var refreshJob: Job? = null

    /**
     * 刷新防抖动延迟（毫秒）
     */
    private val refreshDebounceMs get() = Registry.intValue("cangjie.project.refresh.debounce.ms").toLong()

    /**
     * 当前仓颉项目的异步值持有者
     */
    private val project = AsyncValue(noProjectMarker)


    /**
     * 模块索引
     *
     * 负责维护从 VirtualFile 到 CjModule 的快速映射
     */
    private val moduleIndex = CangJieModuleIndex(intellijProject, this, cs)

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

                })
            subscribe(
                CANGJIE_PROJECT_SDK_CONFIG_TOPIC,
                object : CjProjectSdkConfigListener {
                    override fun projectSdkChanged(event: CjProjectSdkConfigChangedEvent) {
                        CangJieProjectStructureProviderService.getInstance(intellijProject)
                            .incOutOfBlockModificationCount()
                        BuiltinsDecompiledDocumentRefresher.refresh(intellijProject)
                        refreshProject()
                    }
                },
            )

        }

    }


    /**
     * 发现并初始化项目
     *
     * 异步执行项目发现和初始化，避免阻塞 UI 线程。
     * 该方法会：
     * 1. 在后台线程检查项目提供者是否可以处理指定目录
     * 2. 在后台线程创建项目实例
     * 3. 异步更新项目状态
     * 4. 触发项目刷新和事件发布
     *
     * @param rootDir 项目根目录
     */
    override fun discoverProject(rootDir: VirtualFile) {
        cs.launch {
            val provider = providerCache

            // 在后台线程检查和创建项目，避免阻塞 UI
            val newProject = withContext(Dispatchers.IO) {
                try {
                    if (provider.canHandle(rootDir)) {
                        log.info("Found project provider: ${provider.providerName} for $rootDir")
                        provider.createProject(rootDir, intellijProject)
                    } else {
                        log.warn("No suitable project provider found for $rootDir")
                        null
                    }
                } catch (e: Exception) {
                    log.error("Failed to create project from $rootDir", e)
                    null
                }
            }

            if (newProject != null) {
                // 使用异步版本更新项目，避免阻塞
                modifyProjectAsync(ModifyProjectsOptions.DEFAULT) {
                    CompletableFuture.completedFuture(newProject)
                }.thenRun {
                    refreshProject()
                    log.info("Project initialized: ${newProject.name} at ${newProject.rootDir}")

                    // 发布项目创建事件
                    publishEvent(CjProjectEvent(newProject, CjProjectEventType.CREATED))
                }.exceptionally { throwable ->
                    log.error("Failed to initialize project", throwable)
                    null
                }
            }
        }
    }

    /**
     * 调度项目刷新（带防抖动）
     *
     * 取消之前的刷新任务，延迟执行新的刷新任务。
     * 这样可以避免在短时间内多次刷新项目（例如连续添加/删除多个模块时）。
     */
    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = cs.launch {
            delay(refreshDebounceMs)
            refreshProject()
        }
    }

    /**
     * 添加模块
     *
     * 使用防抖动机制调度项目刷新，避免频繁刷新。
     *
     * @param module 要添加的模块
     */
    override fun addModule(module: CjModule) {
        log.info("Module added: ${module.name} at ${module.rootDir}, scheduling project refresh")
        scheduleRefresh()
    }

    /**
     * 移除模块
     *
     * 使用防抖动机制调度项目刷新，避免频繁刷新。
     *
     * @param module 要移除的模块
     */
    override fun removeModule(module: CjModule) {
        log.info("Module removed: ${module.name} at ${module.rootDir}, scheduling project refresh")
        scheduleRefresh()
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
    private fun modifyProjectAsync(
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
                            // 允许在 EDT 上执行慢操作，因为项目初始化需要同步完成
                            // 文件类型关联
                            if (proj.isValid && !options.lightweight) {
                                val fileTypeManager = FileTypeManager.getInstance()
                                fileTypeManager.associateExtension(
                                    CangJieFileType.INSTANCE,
                                    CangJieFileType.INSTANCE.defaultExtension
                                )
                            }


                            // 发布项目更新通知
                            intellijProject.messageBus.syncPublisher(CANGJIE_PROJECTS_TOPIC)
                                .cangjieProjectsUpdated(this, listOf(proj))


                            initialized = true

                            // 触发文件索引重建，以便为延迟构建的 stub 重新索引
                            requestStubIndexRebuild()
                        }
                    }

                } else {
                    // 在 LOAD_STATE 模式下，仅设置初始化标志
                    initialized = true
                    invokeAndWaitIfNeeded {
                        // 同样需要触发索引重建
                        requestStubIndexRebuild()
                    }

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
    private fun modifyProjectSync(
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
        return when (this) {
            is ProcessCanceledException -> CjProjectsService.RefreshStatus.CANCEL
            is CompletionException if cause is ProcessCanceledException -> CjProjectsService.RefreshStatus.CANCEL
            else -> CjProjectsService.RefreshStatus.FAILURE
        }
    }

    /**
     * 刷新项目
     *
     * 使用 CangJieProjectSyncTask 进行项目刷新，提供更好的进度显示和错误处理。
     * 该方法会触发完整的刷新流程，包括：
     * 1. 提交 CangJieProjectSyncTask 到后台队列
     * 2. CangJieProjectSyncTask 执行项目刷新和 Workspace Model 同步
     * 3. 刷新完成后，通过 modifyProjectSync 更新服务中的 CjProject 对象
     * 4. 执行项目根目录更新（TOTAL_RESCAN）
     * 5. 发布项目更新事件
     *
     * 注意：此方法异步执行，不会阻塞调用者
     */
    override fun refreshProject() {
        val startTime = System.currentTimeMillis()
        log.info("Refreshing project: ${cjProject.name}")

        // 创建 CangJieProjectSyncTask，在后台执行刷新和 Workspace Model 同步
        val syncTask = CangJieProjectSyncTask(intellijProject) { syncedProject ->
            assertIsNonDispatchThread()
            // CangJieProjectSyncTask 已完成：
            // 1. 项目数据刷新 (cjProject.refresh())
            // 2. Workspace Model 同步 (workspaceSync.syncProject())

            // 现在通过 modifyProjectSync 更新服务中的 CjProject 对象
            // 并执行后续的同步操作
            modifyProjectSync(ModifyProjectsOptions.DEFAULT) { _ ->
                // 在写操作中执行项目根目录更新
                invokeAndWaitIfNeeded {
                    runWriteAction {
                        // 文件类型关联
                        if (syncedProject.isValid) {
                            val fileTypeManager = FileTypeManager.getInstance()
                            fileTypeManager.associateExtension(
                                CangJieFileType.INSTANCE,
                                CangJieFileType.INSTANCE.defaultExtension
                            )
                        }

                        // 标准库 toolchain 恢复后，必须同步刷新 `.cjo` decompiled PSI/document。
                        // 否则 editor 可能继续持有第一次失败时写入的占位文本 document。
                        BuiltinsDecompiledDocumentRefresher.refresh(intellijProject)


                        // 发布项目更新通知
                        intellijProject.messageBus.syncPublisher(CANGJIE_PROJECTS_TOPIC)
                            .cangjieProjectsUpdated(this, listOf(syncedProject))

                        initialized = true


                        // 更新项目根目录 - TOTAL_RESCAN
//                        runWithNonLightProject(intellijProject) {
//                            ProjectRootManagerEx.getInstanceEx(intellijProject)
//                                .makeRootsChange(
//                                    EmptyRunnable.getInstance(),
//                                    RootsChangeRescanningInfo.TOTAL_RESCAN
//
//                                )
//                        }
                    }
                }

                // 返回刷新后的项目
                syncedProject
            }

            // 发布配置变更事件
            publishEvent(CjProjectEvent(syncedProject, CjProjectEventType.CONFIG_CHANGED))

            // 发布同步完成事件，供 LSP 等服务监听并重启
            publishEvent(CjProjectEvent(syncedProject, CjProjectEventType.SYNCED))

            val duration = System.currentTimeMillis() - startTime
            log.info("Project refresh completed in ${duration}ms for project: ${cjProject.name}")
        }

        // 提交刷新任务到后台队列
        intellijProject.taskQueue.run(syncTask)
    }

    /**
     * 触发 stub 索引重建
     *
     * 在工作空间模型同步完成后调用，重新索引之前延迟构建的 stub。
     * 使用协程异步执行，避免阻塞主线程。
     */
    private fun requestStubIndexRebuild() {
        // 在协程中异步执行索引重建请求

        try {


            // invokeAndWaitIfNeeded 会自动处理 EDT 线程切换
            // 不需要 withContext(Dispatchers.EDT)，避免嵌套调度
            invokeAndWaitIfNeeded {
                runWriteAction {
                    runWithNonLightProject(intellijProject) {
                        val rootManager = ProjectRootManagerEx.getInstanceEx(intellijProject)
                        rootManager.makeRootsChange(
                            EmptyRunnable.getInstance(),
                            RootsChangeRescanningInfo.TOTAL_RESCAN
                        )
                    }
                }
            }

            log.info("Stub index rebuild requested after workspace initialization")
        } catch (e: Exception) {
            log.error("Failed to request stub index rebuild", e)
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
            CjProjectEventType.SYNCED -> publisher.projectSynced(event)
        }
    }

    /**
     * 释放服务资源
     *
     * 在项目关闭时自动调用，负责：
     * 1. 取消所有正在运行的协程
     * 2. 清理防抖动刷新任务
     */
    override fun dispose() {
        // 取消防抖动刷新任务
        refreshJob?.cancel()
        refreshJob = null

        // 取消所有正在运行的协程（包括项目发现、加载等）
        cs.cancel()
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
     *
     * 该方法在首次创建服务或之前保存的状态为空时调用。
     * 使用协程延迟执行项目发现，避免阻塞 IDE 启动流程。
     */
    override fun noStateLoaded() {
        initialized = true // 立即标记为已初始化，避免阻塞服务初始化

        // 应该使用该服务进行初始化，因为它存储了项目数据的一部分
        intellijProject.service<UserDisabledFeaturesHolder>()

        // 延迟到后台执行项目发现，避免阻塞 IDE 启动
        cs.launch {
            // 延迟让 IDE 先完成启动流程
            delay(Registry.intValue("cangjie.project.discovery.initial.delay.ms").toLong())

            if (!intellijProject.isDisposed) {
                log.info("Starting delayed project discovery")
                discoverProject(intellijProject.baseDir)
            }
        }
    }

    /**
     * 加载持久化状态
     *
     * 从保存的状态中恢复项目配置。为了避免阻塞 IDE 启动：
     * 1. 立即标记服务为已初始化
     * 2. 在后台协程中异步加载项目文件和创建项目
     * 3. 延迟执行项目刷新，等 IDE 完全启动后再进行
     *
     * @param state 保存的状态元素
     */
    override fun loadState(state: Element) {
        // 立即标记为已初始化，避免阻塞其他服务的创建
        initialized = true

        val projects = state.getChildren("project")
        val userDisabledFeaturesMap = intellijProject.service<UserDisabledFeaturesHolder>()
            .takeLoadedUserDisabledFeatures()

        // 在后台协程中异步加载项目，避免阻塞 IDE 启动
        cs.launch {
            var loadedProject: CjProject? = null

            // 在 IO 线程执行文件查找和项目创建，避免阻塞主线程
            for (projectElement in projects) {
                val path = projectElement.getAttributeValue("PATH").toPath()

                val file = withContext(Dispatchers.IO) {
                    VirtualFileManager.getInstance().findFileByNioPath(path)
                } ?: continue

                val newProject = withContext(Dispatchers.IO) {
                    providerCache.createProject(file, intellijProject)
                }

                if (newProject != null) {
                    loadedProject = newProject
                    break // 单项目模型，只加载第一个
                }
            }

            // 直接设置项目，跳过任何同步操作以避免阻塞服务初始化
            if (loadedProject != null) {
                this@CjProjectsServiceImpl.project.updateSync { loadedProject }

                // 发布项目打开事件
                publishEvent(CjProjectEvent(loadedProject, CjProjectEventType.OPENED))
            }

            // 延迟刷新到更合适的时机，避免在启动阶段进行重量级操作
            val disableRefresh =
                System.getProperty(CANGJIE_DISABLE_PROJECT_REFRESH_ON_CREATION, "false").toBooleanStrictOrNull()
            if (disableRefresh != true && loadedProject != null) {
                // 延迟让 IDE 完全启动后再刷新
                delay(Registry.intValue("cangjie.project.refresh.startup.delay.ms").toLong())

                // 在后台线程执行项目刷新，避免阻塞 EDT
                if (!intellijProject.isDisposed && loadedProject.isValid) {
                    refreshProject()
                }
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







