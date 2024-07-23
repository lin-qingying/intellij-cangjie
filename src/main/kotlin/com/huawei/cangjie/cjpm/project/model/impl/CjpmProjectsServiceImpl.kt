package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.AsyncValue
import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.findChild
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.huawei.cangjie.cjpm.project.model.ContentEntryWrapper
import com.huawei.cangjie.cjpm.project.model.setup
import com.huawei.cangjie.cjpm.project.pathAsPath
import com.huawei.cangjie.cjpm.project.settings.CjProjectSettingsServiceBase
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.cjpm.project.toolwindow.CjpmToolWindow
import com.huawei.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.huawei.cangjie.cjpm.project.workspace.PackageOrigin
import com.huawei.cangjie.cjpm.project.workspace.additionalRoots
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.idea.notifications.CjNotifications
import com.huawei.cangjie.idea.run.cjpm.isUnitTestMode
import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.lang.lsp.CangJieLspServerManager
import com.huawei.cangjie.taskQueue
import com.intellij.execution.RunManager
import com.intellij.ide.impl.isTrusted
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.externalSystem.autoimport.AutoImportProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.indexing.LightDirectoryIndex
import com.intellij.util.io.systemIndependentPath
import com.linqingying.utils.Config
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.io.path.exists

@State(
    name = "CjpmProjects", storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
class CjpmProjectsServiceImpl(
    override val project: Project
) : CjpmProjectsService, PersistentStateComponent<Element>, Disposable {

    private val noProjectMarker = CjpmProjectImpl(Paths.get(""), this)

    /**
     * 插件项目模型的核心。必须小心确保这是线程安全的，并且在项目集更改后调度刷新
     */
    private val projects = AsyncValue<List<CjpmProjectImpl>>(emptyList())

    /**
     *[directoryIndex]允许从[VirtualFile]快速映射到
     *[CjpmProject]
     */
    private val directoryIndex: LightDirectoryIndex<CjpmProjectImpl> =
        LightDirectoryIndex(project, noProjectMarker) { index ->
            val visited = mutableSetOf<VirtualFile>()

            fun VirtualFile.put(cjpmProject: CjpmProjectImpl) {
                if (this in visited) return
                visited += this
                index.putInfo(this, cjpmProject)
            }

            fun CjpmWorkspace.Package.put(cargoProject: CjpmProjectImpl) {
                contentRoot?.put(cargoProject)
                outDir?.put(cargoProject)
                for (additionalRoot in additionalRoots()) {
                    additionalRoot.put(cargoProject)
                }

            }


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
            for ((pkg, cargoProject) in lowPriority) {
                pkg.put(cargoProject)
            }
        }


    override val hasAtLeastOneValidProject: Boolean
        get() = hasAtLeastOneValidProject(allProjects)


    override var initialized: Boolean = false
    override val allProjects: Collection<CjpmProject>
        get() = projects.currentState

    private var isLegacyCangJieNotificationShowed: Boolean = false


    init {
        val newProjectModelImportEnabled = isNewProjectModelImportEnabled
        if (newProjectModelImportEnabled) {
            @Suppress("LeakingThis")
            registerProjectAware(project, this)
        }

        with(project.messageBus.connect()) {
            if (!newProjectModelImportEnabled) {
                if (!isUnitTestMode) {
                    subscribe(VirtualFileManager.VFS_CHANGES, CjpmJsonWatcher(this@CjpmProjectsServiceImpl, fun() {
                        if (!project.cangjieSettings.autoUpdateEnabled) return
                        refreshAllProjects()
                    }))
                }

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

            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, _ ->
                StartupManager.getInstance(project).runAfterOpened {

                    ToolWindowManager.getInstance(project).invokeLater {
                        CjpmToolWindow.initializeToolWindow(project)
                    }
                }
            })
        }
    }


    private fun registerProjectAware(project: Project, disposable: Disposable) {
        // There is no sense to register `CjpmExternalSystemProjectAware` for default project.
        // Moreover, it may break searchable options building.
        // Also, we don't need to register `CjpmExternalSystemProjectAware` in light tests because:
        // - we check it only in heavy tests
        // - it heavily depends on service disposing which doesn't work in light tests
        if (project.isDefault || isUnitTestMode && (project as? ProjectEx)?.isLight == true) return

        val cjpmProjectAware = CjpmExternalSystemProjectAware(project)
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.register(cjpmProjectAware, disposable)
        projectTracker.activate(cjpmProjectAware.projectId)

        project.messageBus.connect(disposable)
            .subscribe(
                CjProjectSettingsServiceBase.CANGJIE_SETTINGS_TOPIC,
                object : CjProjectSettingsServiceBase.CjSettingsListener {
                    override fun <T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>> settingsChanged(e: CjProjectSettingsServiceBase.SettingsChangedEventBase<T>) {
                        if (e.affectsCjpmMetadata) {
                            val tracker = AutoImportProjectTracker.getInstance(project)
                            tracker.markDirty(cjpmProjectAware.projectId)
                            tracker.scheduleProjectRefresh()
                        }
                    }
                })
    }

    override fun findProjectForFile(file: VirtualFile): CjpmProject? =
        file.applyWithSymlink { directoryIndex.getInfoForFile(it).takeIf { info -> info !== noProjectMarker } }

    override fun findProjectForModuleFile(file: VirtualFile): CjpmProject? {
     return   file.applyWithSymlink { directoryIndex.getInfoForFile(it)  }

    }

    override fun suggestManifests(): Sequence<VirtualFile> =
        project.modules
            .asSequence()
            .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }
            .mapNotNull { it.findChild(CjpmConstants.MANIFEST_FILE) }

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

    @Suppress("LeakingThis")
    private val packageIndex: CjpmPackageIndex = CjpmPackageIndex(project, this)

    override fun findPackageForFile(file: VirtualFile): CjpmWorkspace.Package? =
        file.applyWithSymlink(packageIndex::findPackageForFile)

    override fun discoverAndRefresh(): CompletableFuture<out List<CjpmProject>> {
        val guessManifest = suggestManifests().firstOrNull()
            ?: return CompletableFuture.completedFuture(projects.currentState)

        return modifyProjects { projects ->
            if (hasAtLeastOneValidProject(projects)) return@modifyProjects CompletableFuture.completedFuture(projects)
            doRefresh(project, listOf(CjpmProjectImpl(guessManifest.pathAsPath, this)))
        }
    }


    override fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>> =
        modifyProjects { doRefresh(project, it) }


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
     * All modifications to project model except for low-level `loadState` should
     * go through this method: it makes sure that when we update various IDEA listeners,
     * [allProjects] contains fresh projects.
     */
    protected fun modifyProjects(
        updater: (List<CjpmProjectImpl>) -> CompletableFuture<List<CjpmProjectImpl>>
    ): CompletableFuture<List<CjpmProjectImpl>> {
        val refreshStatusPublisher = project.messageBus.syncPublisher(CjpmProjectsService.CJPM_PROJECTS_REFRESH_TOPIC)

        val wrappedUpdater = { projects: List<CjpmProjectImpl> ->
            refreshStatusPublisher.onRefreshStarted()
            updater(projects)
        }

        return projects.updateAsync(wrappedUpdater)
            .thenApply { projects ->
                invokeAndWaitIfNeeded {
                    val fileTypeManager = FileTypeManager.getInstance()
                    runWriteAction {
                        if (projects.isNotEmpty()) {
                            checkCangjieVersion(projects)
                            fileTypeManager.associateExtension(CangJieFileType, CangJieFileType.defaultExtension)
                        }

                        directoryIndex.resetIndex()
                        // In unit tests roots change is done by the test framework in most cases
                        runWithNonLightProject(project) {
                            ProjectRootManagerEx.getInstanceEx(project)
                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
                        }
                        project.messageBus.syncPublisher(CjpmProjectsService.CJPM_PROJECTS_TOPIC)
                            .cjpmProjectsUpdated(this, projects)
                        initialized = true
                    }
                }
                projects
            }.handle { projects, err ->
                val status = err?.toRefreshStatus() ?: CjpmProjectsService.CjpmRefreshStatus.SUCCESS
                refreshStatusPublisher.onRefreshFinished(status)
                projects
            }
    }

    private fun Throwable.toRefreshStatus(): CjpmProjectsService.CjpmRefreshStatus {
        return when {
            this is ProcessCanceledException -> CjpmProjectsService.CjpmRefreshStatus.CANCEL
            this is CompletionException && cause is ProcessCanceledException -> CjpmProjectsService.CjpmRefreshStatus.CANCEL
            else -> CjpmProjectsService.CjpmRefreshStatus.FAILURE
        }
    }

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
     * Note that [noStateLoaded] is called not only during the first service creation, but on any
     * service load if [getState] returned empty state during previous save (i.e. there are no cjpm project)
     */
    override fun noStateLoaded() {


        // 显示在 [com.huawei.cangjie.idea.notifications.MissingToolchainNotificationProvider]

        initialized = true // 不需要锁定B/C的服务初始时间

//应该使用该服务进行初始化，因为它存储了cjpm项目数据的一部分
        project.service<UserDisabledFeaturesHolder>()
    }

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

    override fun dispose() {

    }

    companion object {
        const val CJPM_DISABLE_PROJECT_REFRESH_ON_CREATION: String = "cjpm.disable.project.refresh.on.creation"

    }
}

private fun hasAtLeastOneValidProject(projects: Collection<CjpmProject>) =
    projects.any { it.manifest.exists() }

private fun doRefresh(project: Project, projects: List<CjpmProjectImpl>): CompletableFuture<List<CjpmProjectImpl>> {
    @Suppress("UnstableApiUsage")
    if (!project.isTrusted()) return CompletableFuture.completedFuture(projects)
    // TODO: get rid of `result` here
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

            if(Config.isLsp){

//            TODO 重启lsp服务器
                CangJieLspServerManager.restartLspServer(project)
            }

        }
        updatedProjects
    }
}

private inline fun runWithNonLightProject(project: Project, action: () -> Unit) {
    if ((project as? ProjectEx)?.isLight != true) {
        action()
    } else {
        check(isUnitTestMode)
    }
}

private fun setupProjectRoots(project: Project, cjpmProjects: List<CjpmProject>) {
    invokeAndWaitIfNeeded {
        // Initialize services that we use (probably indirectly) in write action below.
        // Otherwise, they can be initialized in write action that may lead to deadlock
        RunManager.getInstance(project)
        ProjectFileIndex.getInstance(project)

        runWriteAction {
            if (project.isDisposed) return@runWriteAction
            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {
                for (cjpmProject in cjpmProjects) {


                    cjpmProject.workspaceRootDir?.setupContentRoots(project) { contentRoot ->
                        addExcludeFolder("${contentRoot.url}/${CjpmConstants.ProjectLayout.target}")
                    }

//                    cjpmProject.workspaceRootDir?.setupContentRoots(project, ContentEntryWrapper::setup)
                    val workspacePackages = cjpmProject.workspace?.packages
                        .orEmpty()
                        .filter { it.origin == PackageOrigin.WORKSPACE }

                    for (pkg in workspacePackages) {
                        pkg.contentRoot?.setupContentRoots(project, ContentEntryWrapper::setup)
                    }
                }
            }
        }
    }
}

private fun isExistingProject(projects: Collection<CjpmProject>, manifest: Path): Boolean {
    if (projects.any { it.manifest == manifest }) return true
    return projects.map { it.workingDirectory }
        .any { it.parent == manifest.parent }
}

private fun VirtualFile.setupContentRoots(project: Project, setup: ContentEntryWrapper.(VirtualFile) -> Unit) {
    val packageModule = ModuleUtilCore.findModuleForFile(this, project) ?: return
    setupContentRoots(packageModule, setup)
}

private fun VirtualFile.setupContentRoots(packageModule: Module, setup: ContentEntryWrapper.(VirtualFile) -> Unit) {
    ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
        val contentEntry = rootModel.contentEntries.singleOrNull() ?: return@updateModel
        ContentEntryWrapper(contentEntry).setup(this)
    }
}


inline fun <T> VirtualFile.applyWithSymlink(f: (VirtualFile) -> T?): T? {
    return f(this) ?: f(canonicalFile ?: return null)
}


fun Project.showBalloon(
    @Suppress("UnstableApiUsage") @NlsContexts.NotificationTitle title: String,
    @Suppress("UnstableApiUsage") @NlsContexts.NotificationContent content: String,
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

fun Project.showBalloon(
    @Suppress("UnstableApiUsage") @NlsContexts.NotificationContent content: String,
    type: NotificationType,
    action: AnAction? = null
) {
    showBalloon("", content, type, action)
}
