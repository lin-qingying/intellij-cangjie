//package com.huawei.cangjie.idea.project
//
//import com.huawei.cangjie.AsyncValue
//import com.huawei.cangjie.CangJieBundle
//import com.huawei.cangjie.idea.run.cjpm.CjpmConstants
//import com.huawei.cangjie.lang.CangJieFileType
//import com.intellij.execution.RunManager
//import com.intellij.ide.impl.isTrusted
//import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
//import com.intellij.notification.NotificationType
//import com.intellij.openapi.Disposable
//import com.intellij.openapi.application.ModalityState
//import com.intellij.openapi.application.invokeAndWaitIfNeeded
//import com.intellij.openapi.application.invokeLater
//import com.intellij.openapi.application.runWriteAction
//import com.intellij.openapi.components.*
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.diagnostic.logger
//import com.intellij.openapi.fileTypes.FileTypeManager
//import com.intellij.openapi.module.Module
//import com.intellij.openapi.module.ModuleUtilCore
//import com.intellij.openapi.progress.*
//import com.intellij.openapi.progress.impl.ProgressManagerImpl
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.project.ex.ProjectEx
//import com.intellij.openapi.roots.ContentEntry
//import com.intellij.openapi.roots.ModuleRootModificationUtil
//import com.intellij.openapi.roots.ProjectFileIndex
//import com.intellij.openapi.roots.ex.ProjectRootManagerEx
//import com.intellij.openapi.util.EmptyRunnable
//import com.intellij.openapi.util.UserDataHolderBase
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.util.concurrency.QueueProcessor
//import com.intellij.util.indexing.LightDirectoryIndex
//import com.intellij.util.io.systemIndependentPath
//import org.jdom.Element
//import java.nio.file.Path
//import java.nio.file.Paths
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.CompletionException
//import java.util.function.BiConsumer
//
//@State(
//    name = "CjpmProjects", storages = [
//        Storage(StoragePathMacros.WORKSPACE_FILE),
//        Storage("misc.xml", deprecated = true)
//    ]
//)
//open class CjpmProjectsServiceImpl(
//    final override val project: Project
//) : CjpmProjectsService, PersistentStateComponent<Element>, Disposable {
//    override fun getMyState(): Element? {
//        val myState = Element("myState")
//        for (cjpmProject in allProjects) {
//            val cjpmProjectElement = Element("cjpmProject")
//            cjpmProjectElement.setAttribute("FILE", cjpmProject.manifest.systemIndependentPath)
//            myState.addContent(cjpmProjectElement)
//        }
//
//        // Note that if [myState] is empty (there are no cjpm projects), [noStateLoaded] will be called on the next load
//
//        return myState
//    }
//
//    @Suppress("LeakingThis")
//    private val noProjectMarker = CjpmProjectImpl(Paths.get(""), this)
//
//    override var initialized: Boolean = false
//    protected fun modifyProjects(
//        updater: (List<CjpmProjectImpl>) -> CompletableFuture<List<CjpmProjectImpl>>
//    ): CompletableFuture<List<CjpmProjectImpl>> {
//        val refreshStatusPublisher = project.messageBus.syncPublisher(CjpmProjectsService.CJPM_PROJECTS_REFRESH_TOPIC)
//
//        val wrappedUpdater = { projects: List<CjpmProjectImpl> ->
//            refreshStatusPublisher.onRefreshStarted()
//            updater(projects)
//        }
//
//        return projects.updateAsync(wrappedUpdater)
//            .thenApply { projects ->
//                invokeAndWaitIfNeeded {
//                    val fileTypeManager = FileTypeManager.getInstance()
//                    runWriteAction {
//                        if (projects.isNotEmpty()) {
//
//
//                            // Android RenderScript (from Android plugin) files has the same extension (.rs) as Rust files.
//                            // In some cases, IDEA determines `*.rs` files have RenderScript file type instead of Rust one
//                            // that leads any code insight features don't work in Rust projects.
//                            // See https://youtrack.jetbrains.com/issue/IDEA-237376
//                            //
//                            // It's a hack to provide proper mapping when we are sure that it's Rust project
//                            fileTypeManager.associateExtension(CangJieFileType, CangJieFileType.defaultExtension)
//                        }
//
//
//                        // In unit tests roots change is done by the test framework in most cases
//                        runWithNonLightProject(project) {
//                            ProjectRootManagerEx.getInstanceEx(project)
//                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
//                        }
//                        project.messageBus.syncPublisher(CjpmProjectsService.CJPM_PROJECTS_TOPIC)
//                            .cjpmProjectsUpdated(this, projects)
//                        initialized = true
//                    }
//                }
//                projects
//            }.handle { projects, err ->
//                val status = err?.toRefreshStatus() ?: CjpmProjectsService.CjpmRefreshStatus.SUCCESS
//                refreshStatusPublisher.onRefreshFinished(status)
//                projects
//            }
//    }
//    private fun Throwable.toRefreshStatus(): CjpmProjectsService.CjpmRefreshStatus {
//        return when {
//            this is ProcessCanceledException -> CjpmProjectsService.CjpmRefreshStatus.CANCEL
//            this is CompletionException && cause is ProcessCanceledException -> CjpmProjectsService.CjpmRefreshStatus.CANCEL
//            else -> CjpmProjectsService.CjpmRefreshStatus.FAILURE
//        }
//    }
//    override fun refreshAllProjects(): CompletableFuture<out List<CjpmProject>> =
//        modifyProjects { doRefresh(project, it) }
//
//    override fun loadState(myState: Element) {
//        val cjpmProjects = myState.getChildren("cjpmProject")
//        val loaded = mutableListOf<CjpmProjectImpl>()
//
//        projects.updateSync { loaded }
//            .whenComplete { _, _ ->
//                val disableRefresh =
//                    System.getProperty(CJPM_DISABLE_PROJECT_REFRESH_ON_CREATION, "false").toBooleanStrictOrNull()
//                if (disableRefresh != true) {
//                    invokeLater {
//                        if (project.isDisposed) return@invokeLater
//                        refreshAllProjects()
//                    }
//                }
//            }
//
//
//    }
//
//    override fun dispose() {
//    }
//    private val directoryIndex: LightDirectoryIndex<CjpmProjectImpl> =
//        LightDirectoryIndex(project, noProjectMarker) { index ->
////            val visited = mutableSetOf<VirtualFile>()
////
////            fun VirtualFile.put(cargoProject: CjpmProjectImpl) {
////                if (this in visited) return
////                visited += this
////                index.putInfo(this, cargoProject)
////            }
////
////            fun CargoWorkspace.Package.put(cargoProject: CjpmProjectImpl) {
////                contentRoot?.put(cargoProject)
////                outDir?.put(cargoProject)
////                for (additionalRoot in additionalRoots()) {
////                    additionalRoot.put(cargoProject)
////                }
////                for (target in targets) {
////                    target.crateRoot?.parent?.put(cargoProject)
////                }
////            }
////
////            val lowPriority = mutableListOf<Pair<CargoWorkspace.Package, CjpmProjectImpl>>()
////
////            for (cargoProject in projects.currentState) {
////                cargoProject.rootDir?.put(cargoProject)
////                for (pkg in cargoProject.workspace?.packages.orEmpty()) {
////                    if (pkg.origin == PackageOrigin.WORKSPACE) {
////                        pkg.put(cargoProject)
////                    } else {
////                        lowPriority += pkg to cargoProject
////                    }
////                }
////            }
////
////            for ((pkg, cargoProject) in lowPriority) {
////                pkg.put(cargoProject)
////            }
//        }
//    private val projects = AsyncValue<List<CjpmProjectImpl>>(emptyList())
//
//    override val allProjects: Collection<CjpmProject>
//        get() = projects.currentState
//
//    override fun findProjectForFile(file: VirtualFile): CjpmProject? =
//        file.applyWithSymlink { directoryIndex.getInfoForFile(it).takeIf { info -> info !== noProjectMarker } }
//
//    companion object {
//        const val CJPM_DISABLE_PROJECT_REFRESH_ON_CREATION: String = "cjpm.disable.project.refresh.on.creation"
//    }
//
//}
//
//
//data class CjpmProjectImpl(
//    override val manifest: Path,
//    private val projectService: CjpmProjectsServiceImpl,
//) : UserDataHolderBase(), CjpmProject {
//    override val presentableName: String
//        get() = TODO("Not yet implemented")
//    override val project: Project
//        get() = TODO("Not yet implemented")
//
//    override val workspaceRootDir: VirtualFile?
//        get() = TODO("Not yet implemented")
//
//
//}
//
//class CjBackgroundTaskQueue {
//    private interface ContinuableRunnable {
//        fun run(continuation: Runnable)
//    }
//
//    private fun runTaskInCurrentThread(task: Task.Backgroundable) {
//        check(isUnitTestMode)
//        val pm = ProgressManager.getInstance() as ProgressManagerImpl
//        pm.runProcessWithProgressInCurrentThread(task, EmptyProgressIndicator(), ModalityState.NON_MODAL)
//    }
//
//    private class BackgroundableTaskData(
//        val task: Task.Backgroundable,
//        val onFinish: (BackgroundableTaskData) -> Unit
//    ) : ContinuableRunnable {
//        override fun run(continuation: Runnable) {
//            TODO("Not yet implemented")
//        }
//
//        private var myState: State = State.Pending
//
//        private sealed class State {
//            object Pending : State()
//            data class WaitForSmartMode(val continuation: Runnable) : State()
//            object Canceled : State()
//            object CanceledContinued : State()
//            data class Running(val indicator: ProgressIndicator) : State()
//        }
//
//        @Synchronized
//        fun cancel() {
//            when (val myState = myState) {
//                State.Pending -> this.myState = State.Canceled
//                is State.Running -> myState.indicator.cancel()
//                is State.WaitForSmartMode -> {
//                    this.myState = State.CanceledContinued
//                    myState.continuation.run()
//                }
//
//                State.Canceled -> Unit
//                State.CanceledContinued -> Unit
//            }
//        }
//    }
//
//    // Guarded by self object monitor (@Synchronized)
//    private val cancelableTasks: MutableList<BackgroundableTaskData> = mutableListOf()
//
//
//    @Synchronized
//    fun cancelTasks(taskType: CjTask.TaskType) {
//        cancelableTasks.removeIf { data ->
//            if (data.task is CjTask && taskType.canCancelOther(data.task.taskType)) {
//                data.cancel()
//                true
//            } else {
//                false
//            }
//        }
//    }
//
//    @Synchronized
//    private fun onFinish(data: BackgroundableTaskData) {
//        cancelableTasks.remove(data)
//    }
//
//    @Synchronized
//    private fun cancelAll() {
//        for (task in cancelableTasks) {
//            task.cancel()
//        }
//        cancelableTasks.clear()
//    }
//
//    fun dispose() {
//        isDisposed = true
//        processor.clear()
//        cancelAll()
//    }
//
//    @Synchronized
//    fun run(task: Task.Backgroundable) {
//        if (isUnitTestMode && task is CjTask && task.runSyncInUnitTests) {
//            runTaskInCurrentThread(task)
//        } else {
//            LOG.debug("Scheduling task $task")
//            if (task is CjTask) {
//                cancelTasks(task.taskType)
//            }
//            val data = BackgroundableTaskData(task, ::onFinish)
//
//            // Add to cancelable tasks even if the task is not [RsTaskExt] b/c it still can be canceled by [cancelAll]
//            cancelableTasks += data
//
//            processor.add(data)
//        }
//    }
//
//    private class QueueConsumer : BiConsumer<ContinuableRunnable, Runnable> {
//        override fun accept(t: ContinuableRunnable, u: Runnable) = t.run(u)
//    }
//
//    val isEmpty: Boolean get() = processor.isEmpty
//
//    @Volatile
//    private var isDisposed: Boolean = false
//    private val processor = QueueProcessor(
//        QueueConsumer(),
//        true,
//        QueueProcessor.ThreadToUse.AWT,
//    ) { isDisposed }
//
//    companion object {
//        private val LOG: Logger = logger<CjBackgroundTaskQueue>()
//    }
//
//}
//
//@Service
//class CjProjectTaskQueueService : Disposable {
//    private val queue: CjBackgroundTaskQueue = CjBackgroundTaskQueue()
//
//    /** Submits a task. A task can implement [RsTask] */
//    fun run(task: Task.Backgroundable) = queue.run(task)
//
//    /** Equivalent to running an empty task with [RsTask.taskType] = [taskType] */
//    fun cancelTasks(taskType: CjTask.TaskType) = queue.cancelTasks(taskType)
//
//    /** @return true if no running or pending tasks */
//    val isEmpty: Boolean get() = queue.isEmpty
//
//    override fun dispose() {
//        queue.dispose()
//    }
//}
//
//val Project.taskQueue: CjProjectTaskQueueService get() = service()
//
//private fun doRefresh(project: Project, projects: List<CjpmProjectImpl>): CompletableFuture<List<CjpmProjectImpl>> {
//    @Suppress("UnstableApiUsage")
//    if (!project.isTrusted()) return CompletableFuture.completedFuture(projects)
//    // TODO: get rid of `result` here
//    val result = if (projects.isEmpty()) {
//        CompletableFuture.completedFuture(emptyList())
//    } else {
//        val result = CompletableFuture<List<CjpmProjectImpl>>()
//        val syncTask = CjpmSyncTask(project, projects, result)
//        project.taskQueue.run(syncTask)
//        result
//    }
//
//    return result.thenApply { updatedProjects ->
//        runWithNonLightProject(project) {
//            setupProjectRoots(project, updatedProjects)
//        }
//        updatedProjects
//    }
//}
//
//class CjpmSyncTask(
//    project: Project,
//    private val cjpmProjects: List<CjpmProjectImpl>,
//    private val result: CompletableFuture<List<CjpmProjectImpl>>
//) : Task.Backgroundable(
//    project, CangJieBundle.message("progress.title.reloading.cjpm.projects"),
//    true
//), CjTask {
//    override fun run(indicator: ProgressIndicator) {
//        TODO("Not yet implemented")
//    }
//
//}
//
//interface CjTask {
//    val taskType: TaskType
//        get() = TaskType.INDEPENDENT
//
//    val runSyncInUnitTests: Boolean
//        get() = false
//
//    enum class TaskType(val canBeCanceledByOther: Boolean = true) {
//        CJPM_SYNC(canBeCanceledByOther = false),
//        MACROS_CLEAR(canBeCanceledByOther = false),
//        MACROS_UNPROCESSED,
//        MACROS_FULL,
//
//        /** Can't be canceled, cancels nothing. Should be the last variant of the enum. */
//        INDEPENDENT(canBeCanceledByOther = false);
//
//        fun canCancelOther(other: TaskType): Boolean =
//            other.canBeCanceledByOther && this.ordinal <= other.ordinal
//    }
//
//}
//private inline fun runWithNonLightProject(project: Project, action: () -> Unit) {
//    if ((project as? ProjectEx)?.isLight != true) {
//        action()
//    } else {
//        check(isUnitTestMode)
//    }
//}
//private fun setupProjectRoots(project: Project, cjpmProjects: List<CjpmProject>) {
//    invokeAndWaitIfNeeded {
//        // initialize services that we use (probably indirectly) in write action below.
//        // Otherwise, they can be initialized in write action that may lead to deadlock
//        RunManager.getInstance(project)
//        ProjectFileIndex.getInstance(project)
//
//        runWriteAction {
//            if (project.isDisposed) return@runWriteAction
//            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {
//                for (cjpmProject in cjpmProjects) {
//                    cjpmProject.workspaceRootDir?.setupContentRoots(project) { contentRoot ->
//                        addExcludeFolder("${contentRoot.url}/${CjpmConstants.ProjectLayout.target}")
//                    }
//                }
//            }
//        }
//    }
//}
//private fun VirtualFile.setupContentRoots(project: Project, setup: ContentEntryWrapper.(VirtualFile) -> Unit) {
//    val packageModule = ModuleUtilCore.findModuleForFile(this, project) ?: return
//    setupContentRoots(packageModule, setup)
//}
//private fun VirtualFile.setupContentRoots(packageModule: Module, setup: ContentEntryWrapper.(VirtualFile) -> Unit) {
//    ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
//        val contentEntry = rootModel.contentEntries.singleOrNull() ?: return@updateModel
//        ContentEntryWrapper(contentEntry).setup(this)
//    }
//}
//
//class ContentEntryWrapper(private val contentEntry: ContentEntry) {
//    private val knownFolders: Set<String> = contentEntry.knownFolders()
//
//    fun addExcludeFolder(url: String) {
//        if (url in knownFolders) return
//        contentEntry.addExcludeFolder(url)
//    }
//
//    fun addSourceFolder(url: String, isTestSource: Boolean) {
//        if (url in knownFolders) return
//        contentEntry.addSourceFolder(url, isTestSource)
//    }
//
//    private fun ContentEntry.knownFolders(): Set<String> {
//        val knownRoots = sourceFolders.mapTo(hashSetOf()) { it.url }
//        knownRoots += excludeFolderUrls
//        return knownRoots
//    }
//}
//inline fun <T> VirtualFile.applyWithSymlink(f: (VirtualFile) -> T?): T? {
//    return f(this) ?: f(canonicalFile ?: return null)
//}
