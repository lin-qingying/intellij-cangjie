package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.AsyncValue
import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.project.model.CjcInfo
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.huawei.cangjie.cjpm.project.model.setup
import com.huawei.cangjie.cjpm.project.pathAsPath
import com.huawei.cangjie.cjpm.project.settings.CjProjectSettingsServiceBase
import com.huawei.cangjie.cjpm.project.settings.CjProjectSettingsServiceBase.Companion.CANGJIE_SETTINGS_TOPIC
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.cjpm.project.toolwindow.CjpmToolWindow.Companion.initializeToolWindow

import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.idea.notifications.CjNotifications
import com.huawei.cangjie.idea.run.cjpm.isUnitTestMode
import com.huawei.cangjie.lang.CangJieFileType
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
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.indexing.LightDirectoryIndex
import com.intellij.util.io.systemIndependentPath
import org.jdom.Element
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.exists
import kotlin.reflect.KProperty

val isNewProjectModelImportEnabled: Boolean
    get() = Registry.`is`("com.huawei.cangjie.cjpm.new.auto.import", false)


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


            for (cjpmProject in projects.currentState) {
                cjpmProject.rootDir?.put(cjpmProject)

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

                subscribe(CANGJIE_SETTINGS_TOPIC, object : CjProjectSettingsServiceBase.CjSettingsListener {
                    override fun <T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>> settingsChanged(e: CjProjectSettingsServiceBase.SettingsChangedEventBase<T>) {
                        if (e.affectsCjpmMetadata) {
                            refreshAllProjects()
                        }
                    }
                })
            }

            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, _ ->
                StartupManager.getInstance(project).runAfterOpened {
                    // TODO: provide a proper solution instead of using `invokeLater`
                    ToolWindowManager.getInstance(project).invokeLater {
                        initializeToolWindow(project)
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
            .subscribe(CANGJIE_SETTINGS_TOPIC, object : CjProjectSettingsServiceBase.CjSettingsListener {
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

data class CjpmProjectImpl(
    override val manifest: Path,
    private val projectService: CjpmProjectsServiceImpl,
//    override val userDisabledFeatures: UserDisabledFeatures = UserDisabledFeatures.EMPTY,

//    val rawWorkspace: CjpmWorkspace? = null,
//    private val stdlib: StandardLibrary? = null,

    override val cjcInfo: CjcInfo? = null,
    override val workspaceStatus: CjpmProject.UpdateStatus = CjpmProject.UpdateStatus.NeedsUpdate,
    override val stdlibStatus: CjpmProject.UpdateStatus = CjpmProject.UpdateStatus.NeedsUpdate,
    override val cjcInfoStatus: CjpmProject.UpdateStatus = CjpmProject.UpdateStatus.NeedsUpdate
) : UserDataHolderBase(), CjpmProject {
    override val workspaceRootDir: VirtualFile? = project.baseDir


    private val rootDirCache = AtomicReference<VirtualFile>()

    override val rootDir: VirtualFile?
        get() {
            val cached = rootDirCache.get()
            if (cached != null && cached.isValid) return cached
            val file = LocalFileSystem.getInstance().findFileByIoFile(workingDirectory.toFile())
            rootDirCache.set(file)
            return file
        }
    override val project: Project
        get() = projectService.project

    //    override val workspace: CjpmWorkspace? by lazy(LazyThreadSafetyMode.PUBLICATION) {
//        val rawWorkspace = rawWorkspace ?: return@lazy null
//        val stdlib = stdlib ?: return@lazy if (!userDisabledFeatures.isEmpty() && isUnitTestMode) {
//            rawWorkspace.withDisabledFeatures(userDisabledFeatures)
//        } else {
//            rawWorkspace
//        }
//        rawWorkspace.withStdlib(stdlib,  cjcInfo)
//            .withDisabledFeatures(userDisabledFeatures)
//    }
    override val presentableName: String
        get() {
            return workingDirectory.fileName.toString()
        }

    fun withCjcInfo(result: TaskResult<CjcInfo>): CjpmProjectImpl = when (result) {
        is TaskResult.Ok -> copy(cjcInfo = result.value, cjcInfoStatus = CjpmProject.UpdateStatus.UpToDate)
        is TaskResult.Err -> copy(cjcInfoStatus = CjpmProject.UpdateStatus.UpdateFailed(result.reason))
    }
}

val CjpmProject.workingDirectory: Path get() = manifest.parent
inline fun <T> VirtualFile.applyWithSymlink(f: (VirtualFile) -> T?): T? {
    return f(this) ?: f(canonicalFile ?: return null)
}

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

                    cjpmProject.workspaceRootDir?.setupContentRoots(project,ContentEntryWrapper::setup)



                }
            }
        }
    }
}

class ContentEntryWrapper(private val contentEntry: ContentEntry) {
    private val knownFolders: Set<String> = contentEntry.knownFolders()

    fun addExcludeFolder(url: String) {
        if (url in knownFolders) return
        contentEntry.addExcludeFolder(url)
    }

    fun addSourceFolder(url: String, isTestSource: Boolean) {
        if (url in knownFolders) return
        contentEntry.addSourceFolder(url, isTestSource)
    }

    private fun ContentEntry.knownFolders(): Set<String> {
        val knownRoots = sourceFolders.mapTo(hashSetOf()) { it.url }
        knownRoots += excludeFolderUrls
        return knownRoots
    }
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

private fun isExistingProject(projects: Collection<CjpmProject>, manifest: Path): Boolean {
    if (projects.any { it.manifest == manifest }) return true
    return projects.map { it.workingDirectory }
        .any { it.parent == manifest.parent }
}

private fun hasAtLeastOneValidProject(projects: Collection<CjpmProject>) =
    projects.any { it.manifest.exists() }

class CachedVirtualFile(private val url: String?) {
    private val cache = AtomicReference<VirtualFile>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): VirtualFile? {
        if (url == null) return null
        val cached = cache.get()
        if (cached != null && cached.isValid) return cached
        val file = VirtualFileManager.getInstance().findFileByUrl(url)
        cache.set(file)
        return file
    }
}
