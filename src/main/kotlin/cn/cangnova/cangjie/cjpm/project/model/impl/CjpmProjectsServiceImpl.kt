/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.cjpm.project.model.impl

import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.cjpm.CjpmConstants
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import cn.cangnova.cangjie.cjpm.project.model.ContentEntryWrapper
import cn.cangnova.cangjie.cjpm.project.model.setup
import cn.cangnova.cangjie.cjpm.project.workspace.CjpmWorkspace
import cn.cangnova.cangjie.cjpm.project.workspace.PackageOrigin
import cn.cangnova.cangjie.cjpm.project.workspace.additionalRoots
import cn.cangnova.cangjie.toolchain.CjToolchainBase
import cn.cangnova.cangjie.configurable.services.CangJieLanguageServerServices

import cn.cangnova.cangjie.lang.CangJieFileType
import com.intellij.openapi.module.Module
import com.intellij.execution.RunManager
import com.intellij.ide.impl.isTrusted
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.indexing.LightDirectoryIndex
import com.intellij.util.io.systemIndependentPath
import cn.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import cn.cangnova.cangjie.ide.project.settings.CjProjectSettingsServiceBase
import cn.cangnova.cangjie.ide.project.settings.cangjieSettings
import cn.cangnova.cangjie.notifications.CjNotifications
import cn.cangnova.cangjie.task.taskQueue
import cn.cangnova.cangjie.utils.AsyncValue
import cn.cangnova.cangjie.utils.invokeAndWaitIfNeeded
import cn.cangnova.cangjie.utils.isUnitTestMode
import cn.cangnova.cangjie.utils.pathAsPath
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.io.path.exists

/**
 * CangJie项目服务的实现类
 * 
 * 该服务负责管理CangJie项目的生命周期，包括:
 * - 项目的创建和初始化
 * - 项目配置的持久化
 * - 项目依赖的管理
 * - 项目文件索引
 * - 项目刷新和同步
 * 
 * @property project 当前的IntelliJ项目实例
 */
@State(
    name = "CjpmProjects", storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
class CjpmProjectsServiceImpl(
    override val project: Project
) : CjpmProjectsService, PersistentStateComponent<Element>, Disposable {

    /**
     * 表示无项目状态的标记对象
     * 用于在找不到对应项目时返回默认值
     */
    private val noProjectMarker = CjpmProjectImpl(Paths.get(""), this)

    /**
     * 插件项目模型的核心数据结构
     * 使用AsyncValue确保线程安全，并在项目集更改后调度刷新
     */
    private val projects = AsyncValue<List<CjpmProjectImpl>>(emptyList())

    /**
     * 目录索引，用于快速从VirtualFile映射到CjpmProject
     * 这是一个轻量级的索引实现，支持快速查找文件所属的项目
     */
    private val directoryIndex: LightDirectoryIndex<CjpmProjectImpl> =
        LightDirectoryIndex(project, noProjectMarker) { index ->
            val visited = mutableSetOf<VirtualFile>()

            // 为VirtualFile添加项目映射的扩展函数
            fun VirtualFile.put(cjpmProject: CjpmProjectImpl) {
                if (this in visited) return
                visited += this
                index.putInfo(this, cjpmProject)
            }

            // 为Package添加项目映射的扩展函数
            fun CjpmWorkspace.Package.put(cjpmProject: CjpmProjectImpl) {
                contentRoot?.put(cjpmProject)
                outDir?.put(cjpmProject)
                for (additionalRoot in additionalRoots()) {
                    additionalRoot.put(cjpmProject)
                }
            }

            // 处理不同优先级的包映射
            val lowPriority = mutableListOf<Pair<CjpmWorkspace.Package, CjpmProjectImpl>>()
            for (cjpmProject in projects.currentState) {
                cjpmProject.rootDir?.put(cjpmProject)
                for (pkg in cjpmProject.workspace?.packages.orEmpty()) {
                    if (pkg.origin == PackageOrigin.WORKSPACE) {
                        pkg.put(cjpmProject)
                    } else {
                        lowPriority += pkg to cjpmProject
                    }
                }
            }
            for ((pkg, cjpmProject) in lowPriority) {
                pkg.put(cjpmProject)
            }
        }

    /**
     * 检查是否至少有一个有效的项目
     */
    override val hasAtLeastOneValidProject: Boolean
        get() = hasAtLeastOneValidProject(allProjects)

    /**
     * 服务是否已初始化完成的标志
     */
    override var initialized: Boolean = false

    /**
     * 获取所有管理的项目列表
     */
    override val allProjects: Collection<CjpmProject>
        get() = projects.currentState

    /**
     * 是否已显示过旧版本CangJie工具链的通知
     */
    private var isLegacyCangJieNotificationShowed: Boolean = false

    init {
        val newProjectModelImportEnabled = isNewProjectModelImportEnabled
        if (newProjectModelImportEnabled) {
            registerProjectAware(project, this)
        }

        with(project.messageBus.connect()) {
            if (!newProjectModelImportEnabled) {
                if (!isUnitTestMode) {
                    // 监听VFS变化，在非测试模式下自动更新项目
                    subscribe(VirtualFileManager.VFS_CHANGES, CjpmTomlWatcher(this@CjpmProjectsServiceImpl, fun() {
                        if (!project.cangjieSettings.autoUpdateEnabled) return
                        refreshAllProjects()
                    }))
                }

                // 监听设置变化
                subscribe(
                    CjProjectSettingsServiceBase.CANGJIE_SETTINGS_TOPIC,
                    object : CjProjectSettingsServiceBase.CjSettingsListener {
                        override fun <T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>> settingsChanged(e: CjProjectSettingsServiceBase.SettingsChangedEventBase<T>) {
                            if (e.affectsCjpmMetadata) {
                                refreshAllProjects()
                            }
                        }
                    })
            }

//            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, _ ->
//
//                // 使用 DumbService 确保在索引就绪时执行
//                DumbService.getInstance(project).smartInvokeLater {
//                    if (!project.isDisposed) {
//                        CjpmToolWindow.initializeToolWindow(project)
//                    }
//                }
//
//            })
        }
    }

    /**
     * 注册项目感知器
     * 用于处理外部系统的项目导入和更新
     */
    private fun registerProjectAware(project: Project, disposable: Disposable) {
        // 跳过默认项目和轻量级测试项目
        if (project.isDefault || isUnitTestMode && (project as? ProjectEx)?.isLight == true) return

        val cjpmProjectAware = CjpmExternalSystemProjectAware(project)
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.register(cjpmProjectAware, disposable)
        projectTracker.activate(cjpmProjectAware.projectId)

        // 监听设置变化并触发项目刷新
        project.messageBus.connect(disposable)
            .subscribe(
                CjProjectSettingsServiceBase.CANGJIE_SETTINGS_TOPIC,
                object : CjProjectSettingsServiceBase.CjSettingsListener {
                    override fun <T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>> settingsChanged(e: CjProjectSettingsServiceBase.SettingsChangedEventBase<T>) {
                        if (e.affectsCjpmMetadata) {
                            val tracker = ExternalSystemProjectTracker.getInstance(project)
                            tracker.markDirty(cjpmProjectAware.projectId)
                            tracker.scheduleProjectRefresh()
                        }
                    }
                })
    }

    /**
     * 查找指定文件所属的项目
     */
    override fun findProjectForFile(file: VirtualFile): CjpmProject? =
        file.applyWithSymlink { directoryIndex.getInfoForFile(it).takeIf { info -> info !== noProjectMarker } }

    /**
     * 查找指定模块文件所属的项目
     */
    override fun findProjectForModuleFile(file: VirtualFile): CjpmProject? {
        return file.applyWithSymlink { directoryIndex.getInfoForFile(it) }
    }

    /**
     * 建议可能的清单文件位置
     */
    override fun suggestManifests(): Sequence<VirtualFile> =
        project.modules
            .asSequence()
            .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }
            .mapNotNull { it.findChild(CjpmConstants.MANIFEST_FILE) }

    /**
     * 附加新的CangJie项目
     * @param manifest 项目清单文件的路径
     * @return 是否成功附加项目
     */
    override fun attachCjpmProject(manifest: Path): Boolean {
        if (isExistingProject(allProjects, manifest)) return false
        modifyProjects { projects ->
            if (isExistingProject(projects, manifest))
                CompletableFuture.completedFuture(projects)
            else
                doRefresh(project, projects + CjpmProjectImpl(manifest, this))
        }
        return true
    }

    /**
     * 包索引，用于快速查找文件所属的包
     */
    @Suppress("LeakingThis")
    private val packageIndex: CjpmPackageIndex = CjpmPackageIndex(project, this)

    /**
     * 查找指定文件所属的包
     */
    override fun findPackageForFile(file: VirtualFile): CjpmWorkspace.Package? =
        file.applyWithSymlink(packageIndex::findPackageForFile)

    /**
     * 发现并刷新项目
     * 自动检测项目并进行刷新
     */
    override fun discoverAndRefresh(): CompletableFuture<out List<CjpmProject>> {
        val guessManifest = suggestManifests().firstOrNull()
            ?: return CompletableFuture.completedFuture(projects.currentState)

        return modifyProjects { projects ->
            if (hasAtLeastOneValidProject(projects)) return@modifyProjects CompletableFuture.completedFuture(projects)
            doRefresh(project, listOf(CjpmProjectImpl(guessManifest.pathAsPath, this)))
        }
    }

    /**
     * 刷新所有项目
     */
    override fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>> =
        modifyProjects { doRefresh(project, it) }

    /**
     * 检查CangJie工具链版本
     * 如果版本过低，显示警告通知
     */
    private fun checkCangjieVersion(projects: List<CjpmProjectImpl>) {
        val minToolchainVersion = projects.asSequence()
            .mapNotNull { it.cjcInfo?.version?.semver }
            .minOrNull()
        if (minToolchainVersion != null && minToolchainVersion < CjToolchainBase.MIN_SUPPORTED_TOOLCHAIN) {
            if (!isLegacyCangJieNotificationShowed) {
                val content = CangJieBundle.message(
                    "notification.content.cangjie.toolchain.no.longer.supported",
                    minToolchainVersion,
                    CjToolchainBase.MIN_SUPPORTED_TOOLCHAIN
                )
                project.showBalloon(content, NotificationType.WARNING)
            }
            isLegacyCangJieNotificationShowed = true
        } else {
            isLegacyCangJieNotificationShowed = false
        }
    }

    /**
     * 除了低级别的 `loadState` 操作外，所有对项目模型的修改都应该通过此方法进行。
     * 它确保在更新各种 IDEA 监听器时，[allProjects] 包含最新的项目。
     *
     * @param updater 一个函数，接收当前项目列表并返回一个 CompletableFuture，其中包含更新后的项目列表。
     *                该函数负责执行具体的项目更新逻辑。
     * @return 返回一个 CompletableFuture，表示异步更新操作的结果，结果为更新后的项目列表。
     */
    protected fun modifyProjects(
        updater: (List<CjpmProjectImpl>) -> CompletableFuture<List<CjpmProjectImpl>>
    ): CompletableFuture<List<CjpmProjectImpl>> {

        // 发布刷新开始的通知
        val refreshStatusPublisher = project.messageBus.syncPublisher(CjpmProjectsService.CJPM_PROJECTS_REFRESH_TOPIC)

        // 包装 updater 函数，在调用 updater 前发布刷新开始通知
        val wrappedUpdater = { projects: List<CjpmProjectImpl> ->
            refreshStatusPublisher.onRefreshStarted()
            updater(projects)
        }

        return projects.updateAsync(wrappedUpdater)
            .thenApply { projects ->
                invokeAndWaitIfNeeded {
                    // 获取文件类型管理器实例，并在写入操作中进行必要的文件类型关联和索引重置
                    val fileTypeManager = FileTypeManager.getInstance()
                    runWriteAction {
                        if (projects.isNotEmpty()) {
                            checkCangjieVersion(projects)
                            fileTypeManager.associateExtension(
                                CangJieFileType.INSTANCE,
                                CangJieFileType.INSTANCE.defaultExtension
                            )
                        }

                        directoryIndex.resetIndex()
                        // 在非轻量级项目中，通过 ProjectRootManagerEx 更新项目根目录
                        runWithNonLightProject(project) {
                            ProjectRootManagerEx.getInstanceEx(project)
                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
                        }
                        // 发布项目更新通知
                        project.messageBus.syncPublisher(CjpmProjectsService.CJPM_PROJECTS_TOPIC)
                            .cjpmProjectsUpdated(this, projects)
                        initialized = true
                    }
                }
                projects
            }.handle { projects, err ->
                // 处理异常情况，发布刷新结束通知
                val status = err?.toRefreshStatus() ?: CjpmProjectsService.CjpmRefreshStatus.SUCCESS
                refreshStatusPublisher.onRefreshFinished(status)
                projects
            }
    }

    /**
     * 将异常转换为刷新状态
     */
    private fun Throwable.toRefreshStatus(): CjpmProjectsService.CjpmRefreshStatus {
        return when {
            this is ProcessCanceledException -> CjpmProjectsService.CjpmRefreshStatus.CANCEL
            this is CompletionException && cause is ProcessCanceledException -> CjpmProjectsService.CjpmRefreshStatus.CANCEL
            else -> CjpmProjectsService.CjpmRefreshStatus.FAILURE
        }
    }

    /**
     * 获取持久化状态
     * 实现PersistentStateComponent接口
     */
    override fun getState(): Element {
        val state = Element("state")
        for (cjpmProject in allProjects) {
            val cjpmProjectElement = Element("cjpmProject")
            cjpmProjectElement.setAttribute("FILE", cjpmProject.manifest.systemIndependentPath)
            state.addContent(cjpmProjectElement)
        }
        return state
    }

    /**
     * 当没有状态加载时调用
     * 这不仅在首次创建服务时调用，
     * 也在之前保存的状态为空时调用
     */
    override fun noStateLoaded() {


        // 显示在 [cn.cangnova.cangjie.notifications.MissingToolchainNotificationProvider]

        initialized = true // 不需要锁定B/C的服务初始时间

//应该使用该服务进行初始化，因为它存储了cjpm项目数据的一部分
        project.service<UserDisabledFeaturesHolder>()
    }

    /**
     * 加载持久化状态
     * 实现PersistentStateComponent接口
     */
    override fun loadState(state: Element) {
        val cjpmProjects = state.getChildren("cjpmProject")
        val loaded = mutableListOf<CjpmProjectImpl>()
        val userDisabledFeaturesMap = project.service<UserDisabledFeaturesHolder>()
            .takeLoadedUserDisabledFeatures()

        for (cjpmProject in cjpmProjects) {
            val file = cjpmProject.getAttributeValue("FILE")
            val manifest = Paths.get(file)
//            val userDisabledFeatures = userDisabledFeaturesMap[manifest] ?: UserDisabledFeatures.EMPTY
            val newProject = CjpmProjectImpl(manifest, this)
            loaded.add(newProject)
        }

        //通过`invokeLater`刷新项目，避免修改模型
//在打开项目时。直接使用`updateSync`
//因此不是`ModifyProjects`
        projects.updateSync { loaded }
            .whenComplete { _, _ ->
                val disableRefresh =
                    System.getProperty(CJPM_DISABLE_PROJECT_REFRESH_ON_CREATION, "false").toBooleanStrictOrNull()
                if (disableRefresh != true) {
                    invokeLater {
                        if (project.isDisposed) return@invokeLater
//                        刷新所有项目
                        refreshAllProjects()
                    }
                }
            }
    }

    /**
     * 释放资源
     * 实现Disposable接口
     */
    override fun dispose() {
    }

    companion object {
        /**
         * 系统属性：是否在创建时禁用项目刷新
         */
        const val CJPM_DISABLE_PROJECT_REFRESH_ON_CREATION: String = "cjpm.disable.project.refresh.on.creation"
    }
}

/**
 * 检查项目集合中是否至少有一个有效的项目
 * 有效项目的定义是：项目的manifest文件存在
 * 
 * @param projects 要检查的项目集合
 * @return 如果至少有一个有效项目返回true，否则返回false
 */
private fun hasAtLeastOneValidProject(projects: Collection<CjpmProject>) =
    projects.any { it.manifest.exists() }

/**
 * 执行项目刷新操作
 * 这是一个核心的刷新方法，负责：
 * - 检查项目信任状态
 * - 执行同步任务
 * - 设置项目根目录
 * - 处理LSP服务器重启
 * 
 * @param project 当前IntelliJ项目实例
 * @param projects 要刷新的项目列表
 * @return 包含刷新后项目列表的Future
 */
private fun doRefresh(project: Project, projects: List<CjpmProjectImpl>): CompletableFuture<List<CjpmProjectImpl>> {
    @Suppress("UnstableApiUsage")
    if (!project.isTrusted()) return CompletableFuture.completedFuture(projects)
    val result = if (projects.isEmpty()) {
        CompletableFuture.completedFuture(emptyList())
    } else {
        val result = CompletableFuture<List<CjpmProjectImpl>>()
        val syncTask = CjpmSyncTask(project, projects, result)
        project.taskQueue.run(syncTask)
        result
    }

    return result.thenApply { updatedProjects ->
        runWithNonLightProject(project) {
            setupProjectRoots(project, updatedProjects)

            if (CangJieLanguageServerServices.getInstance().lspConfig.enabled) {
                //TODO 重启lsp服务器
            }
        }
        updatedProjects
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
 * 设置项目的根目录结构
 * 包括：
 * - 设置依赖关系
 * - 合并根目录变更
 * - 设置内容根目录
 * - 更新文件索引
 * 
 * @param project 当前项目实例
 * @param cjpmProjects 要设置的项目列表
 */
private fun setupProjectRoots(project: Project, cjpmProjects: List<CjpmProject>) {
    invokeAndWaitIfNeeded {
        RunManager.getInstance(project)

        runWriteAction {
            if (project.isDisposed) return@runWriteAction

            addDependencies(project, cjpmProjects)

            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {
                for (cjpmProject in cjpmProjects) {
//                    if (cjpmProject !is CjpmProjectImpl) {
//                        continue
//                    }

//保持与cjpm模块名称一致
//                    if (cjpmProject.project.name != cjpmProject.workspace?.metadata?.name) {
//                        cjpmProject.workspace?.metadata?.name?.let {
//                            (cjpmProject.project as? ProjectEx)?.setProjectName(
//                                it
//                            )
//                        }
//                    }


// 设置生产文件夹
//                    cjpmProject.workspaceRootDir?.setupContentRoots(project) { contentRoot ->
//                        addExcludeFolder("${contentRoot.url}/${CjpmConstants.ProjectLayout.target}")
//                    }

                    val workspacePackages = cjpmProject.workspace?.packages
                        .orEmpty()
                        .filter { it.origin == PackageOrigin.WORKSPACE }

                    for (pkg in workspacePackages) {
                        pkg.contentRoot?.setupContentRoots(project, pkg.metadata, ContentEntryWrapper::setup)
                    }
                }
            }
        }
//        更新索引
//        updateIndex()
        ProjectFileIndex.getInstance(project)
    }
}

/**
 * 全局的库表注册器实例
 */
private val libraryTablesRegistrar = LibraryTablesRegistrar.getInstance()

/**
 * 添加项目依赖
 * 处理：
 * - 标准库依赖
 * - 工作空间包依赖
 * - 外部包依赖
 * 
 * @param project 当前项目实例
 * @param cjpmProjects 要处理依赖的项目列表
 */
private fun addDependencies(project: Project, cjpmProjects: List<CjpmProject>) {
    val libraryTable = libraryTablesRegistrar.getLibraryTable(project)
    val module = ModuleManager.getInstance(project).findModuleByName(project.name)
    val moduleModel: ModifiableRootModel? = module?.let { ModuleRootManager.getInstance(it).modifiableModel }

    // 移除现有库
    moduleModel?.let { model ->
        val existingEntries = model.orderEntries.filterIsInstance<LibraryOrderEntry>()
        existingEntries.forEach { model.removeOrderEntry(it) }
    }

    cjpmProjects.forEach { cjpmProject ->
        cjpmProject.workspace?.packages?.forEach {
            if (it.origin == PackageOrigin.WORKSPACE) {
                return@forEach
            }
            val library = it.getOrCreateLibrary(libraryTable)
            val modifiableModel = library.modifiableModel
            if (it.origin == PackageOrigin.STDLIB) {
                it.contentRoot?.let { it1 ->
                    modifiableModel.addRoot(it1, OrderRootType.CLASSES)
                    modifiableModel.addRoot(it1, OrderRootType.SOURCES)
                }
            } else {
                it.contentRoot?.url?.let { it1 ->
                    modifiableModel.addRoot(it1, OrderRootType.CLASSES)
                    modifiableModel.addRoot(it1, OrderRootType.SOURCES)
                }
            }
            modifiableModel.commit()
            moduleModel?.addLibraryEntry(library)
        }
    }
    moduleModel?.commit()
}

/**
 * 获取或创建包的库
 * 根据包的类型(标准库或普通包)创建相应的库
 * 
 * @param libraryTable 库表实例
 * @return 创建或获取的库实例
 */
private fun CjpmWorkspace.Package.getOrCreateLibrary(libraryTable: LibraryTable): Library {
    return if (this.origin == PackageOrigin.STDLIB) {
        libraryTable.getLibraryByName("stdlib") ?: libraryTable.createLibrary("stdlib")
    } else {
        libraryTable.getLibraryByName(this.name) ?: libraryTable.createLibrary(this.name)
    }
}

/**
 * 检查给定的manifest路径是否已存在于项目集合中
 * 
 * @param projects 要检查的项目集合
 * @param manifest 要检查的manifest路径
 * @return 如果项目已存在返回true，否则返回false
 */
private fun isExistingProject(projects: Collection<CjpmProject>, manifest: Path): Boolean {
    if (projects.any { it.manifest == manifest }) return true
    return projects.map { it.workingDirectory }
        .any { it.parent == manifest.parent }
}

/**
 * 为虚拟文件设置内容根目录
 * 
 * @param project 当前项目实例
 * @param metadata 包元数据
 * @param setup 设置内容根目录的函数
 */
private fun VirtualFile.setupContentRoots(
    project: Project,
    metadata: CjpmTomlConfig?,
    setup: ContentEntryWrapper.(VirtualFile, CjpmTomlConfig?) -> Unit
) {
    val packageModule = ModuleUtilCore.findModuleForFile(this, project) ?: return
    setupContentRoots(packageModule, metadata, setup)
}

/**
 * 为模块设置内容根目录
 * 
 * @param packageModule 目标模块
 * @param metadata 包元数据
 * @param setup 设置内容根目录的函数
 */
private fun VirtualFile.setupContentRoots(
    packageModule: Module,
    metadata: CjpmTomlConfig?,
    setup: ContentEntryWrapper.(VirtualFile, CjpmTomlConfig?) -> Unit
) {
    ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
        val contentEntry = rootModel.contentEntries.singleOrNull() ?: return@updateModel
        ContentEntryWrapper(contentEntry).setup(this, metadata)
    }
}

/**
 * 处理符号链接的虚拟文件
 * 如果文件是符号链接，尝试获取其规范文件
 * 
 * @param f 要应用于文件的函数
 * @return 函数的执行结果
 */
inline fun <T> VirtualFile.applyWithSymlink(f: (VirtualFile) -> T?): T? {
    return f(this) ?: f(canonicalFile ?: return null)
}

/**
 * 显示项目通知气球
 * 
 * @param title 通知标题
 * @param content 通知内容
 * @param type 通知类型
 * @param action 可选的通知动作
 * @param listener 可选的通知监听器
 */
fun Project.showBalloon(
    @NlsContexts.NotificationTitle title: String,
    @NlsContexts.NotificationContent content: String,
    type: NotificationType,
    action: AnAction? = null,
    listener: NotificationListener? = null
) {
    val notification = CjNotifications.pluginNotifications().createNotification(title, content, type)
    if (listener != null) {
        notification.setListener(listener)
    }
    if (action != null) {
        notification.addAction(action)
    }
    Notifications.Bus.notify(notification, this)
}

/**
 * 显示项目通知气球的简化版本
 * 
 * @param content 通知内容
 * @param type 通知类型
 * @param action 可选的通知动作
 */
fun Project.showBalloon(
    @NlsContexts.NotificationContent content: String,
    type: NotificationType,
    action: AnAction? = null
) {
    showBalloon("", content, type, action)
}
