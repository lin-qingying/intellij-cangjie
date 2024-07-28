package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.CjTask
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.project.model.CjcInfo
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.ProcessProgressListener
import com.huawei.cangjie.cjpm.project.workspace.CjpmWorkspace
import com.huawei.cangjie.cjpm.project.workspace.StandardLibrary
import com.huawei.cangjie.cjpm.resolve
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.cjc
import com.huawei.cangjie.cjpm.toolchain.cjpm
import com.huawei.cangjie.cjpm.toolchain.impl.CjcVersion
import com.huawei.cangjie.cjpm.toolchain.tools.CjpmCallType

import com.huawei.cangjie.cjpm.toolchain.tools.unwrapOrElse
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildAdapterBase
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildContextBase
import com.huawei.cangjie.ide.run.cjpm.toolchain
import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import org.jetbrains.annotations.Nls
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import kotlin.io.path.exists

class CjpmSyncTask(
    project: Project,
    private val cjpmProjects: List<CjpmProjectImpl>,
    private val result: CompletableFuture<List<CjpmProjectImpl>>
) :
    Task.Backgroundable(project, CangJieBundle.message("progress.title.reloading.cjpm.projects"), true),
    CjTask {


    private fun doRun(
        indicator: ProgressIndicator,
        syncProgress: BuildProgress<BuildProgressDescriptor>
    ): List<CjpmProjectImpl> {
        val toolchain = project.toolchain

        val refreshedProjects = if (toolchain == null) {
            syncProgress.fail(
                System.currentTimeMillis(),
                CangJieBundle.message("build.event.message.cjpm.project.update.failed.no.cangjie.toolchain")
            )
            cjpmProjects
        } else {
            cjpmProjects.map { cjpmProject ->
                syncProgress.runWithChildProgress(
                    CangJieBundle.message("build.event.title.sync.project", cjpmProject.presentableName),
                    createContext = { it },
                    action = { childProgress ->
                        if (!cjpmProject.workingDirectory.exists()) {
                            childProgress.message(
                                CangJieBundle.message("tooltip.project.directory.does.not.exist"),
                                CangJieBundle.message(
                                    "build.event.message.project.directory.does.not.exist.consider.detaching.project.from.cjpm.tool.window",
                                    cjpmProject.workingDirectory,
                                    cjpmProject.presentableName
                                ),
                                MessageEvent.Kind.ERROR,
                                null
                            )
                            val stdlibStatus =
                                CjpmProject.UpdateStatus.UpdateFailed(CangJieBundle.message("tooltip.project.directory.does.not.exist"))
                            CjpmProjectWithStdlib(cjpmProject.copy(stdlibStatus = stdlibStatus), null)
                        } else {
                            val context =
                                SyncContext(project, cjpmProject, toolchain, indicator, syncProgress.id, childProgress)
                            val cjcInfoResult = fetchCjcInfo(context)
                            val cjcInfo = (cjcInfoResult as? TaskResult.Ok)?.value
                            val cjpmProjectWithCjcInfoAndWorkspace = cjpmProject.withCjcInfo(cjcInfoResult)
                                .withWorkspace(fetchCjpmWorkspace(context, cjcInfo))
                            CjpmProjectWithStdlib(
                                cjpmProjectWithCjcInfoAndWorkspace,
//                                fetchStdlib(context, cjpmProjectWithCjcInfoAndWorkspace, cjcInfo)

                                null
                            )

                        }
                    }
                )
            }.chooseAndAttachStdlib()
        }
        return refreshedProjects
    }

    private fun fetchCjcInfo(context: SyncContext): TaskResult<CjcInfo> {
        return context.runWithChildProgress(CangJieBundle.message("progress.text.getting.toolchain.version")) { childContext ->
            if (!childContext.toolchain.looksLikeValidToolchain()) {
                val location = childContext.toolchain.presentableLocation
                return@runWithChildProgress TaskResult.Err(
                    CangJieBundle.message(
                        "invalid.cangjie.toolchain.02",
                        location
                    )
                )
            }

            val workingDirectory = childContext.oldCjpmProject.workingDirectory

            val listener = CjcVersionProcessAdapter(childContext)
            val cjVersion = childContext.toolchain.cjc()
                .queryVersion(workingDirectory, context.project, listener)
                .unwrapOrElse {
                    LOG.warn("Failed to fetch cjc version", it)
                    context.error(
                        CangJieBundle.message("build.event.title.failed.to.fetch.cjc.version"),
                        it.message.orEmpty()
                    )
                    null
                }


            TaskResult.Ok(CjcInfo(cjVersion))
        }
    }

    data class SyncContext(
        val project: Project,
        val oldCjpmProject: CjpmProjectImpl,
        val toolchain: CjToolchainBase,
        val progress: ProgressIndicator,
        val buildId: Any,
        val syncProgress: BuildProgress<BuildProgressDescriptor>
    ) {

        val id: Any get() = syncProgress.id

        fun <T> runWithChildProgress(
            @NlsContexts.ProgressText title: String,
            action: (SyncContext) -> TaskResult<T>
        ): TaskResult<T> {
            progress.checkCanceled()
            progress.text = title

            return syncProgress.runWithChildProgress(
                title,
                { copy(syncProgress = it) },
                action
            ) { childProgress, result ->
                when (result) {
                    is TaskResult.Ok -> childProgress.finish()
                    is TaskResult.Err -> {
                        childProgress.message(result.reason, result.message.orEmpty(), MessageEvent.Kind.ERROR, null)
                        childProgress.fail()
                    }


                }
            }
        }

        fun withProgressText(@NlsContexts.ProgressText @NlsContexts.ProgressTitle text: String) {
            progress.text = text
            syncProgress.progress(text)
        }
    }

    override fun run(indicator: ProgressIndicator) {
        LOG.info("CjpmSyncTask started")
        indicator.isIndeterminate = true
        val start = System.currentTimeMillis()

        val syncProgress = SyncViewManager.createBuildProgress(project)
        val refreshedProjects = try {
            syncProgress.start(createSyncProgressDescriptor(indicator))
            val refreshedProjects = doRun(indicator, syncProgress)

//            val refreshedProjects = cjpmProjects
            val isUpdateFailed = refreshedProjects.any { it.mergedStatus is CjpmProject.UpdateStatus.UpdateFailed }
            if (isUpdateFailed) {
                syncProgress.fail()
            } else {
                syncProgress.finish()
            }
            refreshedProjects
        } catch (e: Throwable) {
            if (e is ProcessCanceledException) {
                syncProgress.cancel()
            } else {
                syncProgress.fail()
            }
            result.completeExceptionally(e)
            throw e
        }
        result.complete(refreshedProjects)
        val elapsed = System.currentTimeMillis() - start
        LOG.debug("Finished Cjpm sync task in $elapsed ms")
    }

    override val taskType: CjTask.TaskType
        get() = CjTask.TaskType.CJPM_SYNC

    override val runSyncInUnitTests: Boolean
        get() = true

    private fun createSyncProgressDescriptor(progress: ProgressIndicator): BuildProgressDescriptor {
        val buildContentDescriptor = BuildContentDescriptor(
            null,
            null,
            object : JComponent() {},
            CangJieBundle.message("build.event.title.cjpm")
        )
        buildContentDescriptor.isActivateToolWindowWhenFailed = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = false
//        buildContentDescriptor.isNavigateToError =
        val refreshAction = ActionManager.getInstance().getAction("Cjpm.RefreshCjpmProject")
        val descriptor = DefaultBuildDescriptor(
            Any(),
            CangJieBundle.message("build.event.title.cjpm"),
            project.basePath!!,
            System.currentTimeMillis()
        )
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(refreshAction)
            .withRestartAction(StopAction(progress))
        return object : BuildProgressDescriptor {
            override fun getTitle(): String = descriptor.title
            override fun getBuildDescriptor(): BuildDescriptor = descriptor
        }
    }

    private class StopAction(private val progress: ProgressIndicator) :
        DumbAwareAction({ "Stop" }, AllIcons.Actions.Suspend) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = progress.isRunning
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun actionPerformed(e: AnActionEvent) {
            progress.cancel()
        }
    }


    companion object {
        private val LOG = logger<CjpmSyncTask>()
    }
}

private fun <T, R> BuildProgress<BuildProgressDescriptor>.runWithChildProgress(
    @BuildEventsNls.Title title: String,
    createContext: (BuildProgress<BuildProgressDescriptor>) -> T,
    action: (T) -> R,
    onResult: (BuildProgress<BuildProgressDescriptor>, R) -> Unit = { progress, _ -> progress.finish() }
): R {
    val childProgress = startChildProgress(title)
    try {
        val context = createContext(childProgress)
        val result = action(context)
        onResult(childProgress, result)
        return result
    } catch (e: Throwable) {
        if (e is ProcessCanceledException) {
            cancel()
        } else {
            fail()
        }
        throw e
    }
}


sealed class TaskResult<out T> {
    class Ok<out T>(val value: T) : TaskResult<T>()
    class Err<out T>(@Nls val reason: String, @BuildEventsNls.Message val message: String? = null) : TaskResult<T>()
}

private class CjcVersionProcessAdapter(
    private val context: CjpmSyncTask.SyncContext
) : ProcessAdapter() {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<Any>) {
        val text = event.text.trim { it <= ' ' }
        if (text.startsWith("info:")) {
            context.withProgressText(text.removePrefix(CangJieBundle.message("progress.text.info")).trim())
        }
    }
}

private fun CjpmSyncTask.SyncContext.error(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    syncProgress.message(title, message, com.intellij.build.events.MessageEvent.Kind.ERROR, null)
}

private fun CjpmSyncTask.SyncContext.warning(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    syncProgress.message(title, message, com.intellij.build.events.MessageEvent.Kind.WARNING, null)
}

private fun fetchCjpmWorkspace(context: CjpmSyncTask.SyncContext, cjcInfo: CjcInfo?): TaskResult<CjpmWorkspace> {
    return context.runWithChildProgress(CangJieBundle.message("progress.text.updating.workspace.info")) { childContext ->
        val toolchain = childContext.toolchain
        if (!toolchain.looksLikeValidToolchain()) {
            return@runWithChildProgress TaskResult.Err(
                CangJieBundle.message(
                    "invalid.cangjie.toolchain.02",
                    toolchain.presentableLocation
                )
            )
        }
        val projectDirectory = childContext.oldCjpmProject.workingDirectory
        val cjpm = toolchain.cjpm()
        CjpmEventService.getInstance(childContext.project).onMetadataCall(projectDirectory)
        val projectDescriptionData = cjpm.fullProjectDescription(
            childContext.project,
            projectDirectory,


            ) {
            when (it) {
                CjpmCallType.METADATA -> SyncProcessAdapter(childContext)
                CjpmCallType.BUILD_SCRIPT_CHECK -> {
                    val childProgress =
                        childContext.syncProgress.startChildProgress(CangJieBundle.message("build.event.title.build.scripts.evaluation"))
                    val syncContext = childContext.copy(syncProgress = childProgress)

                    val buildContext = SyncCjpmBuildContext(
                        childContext.oldCjpmProject,
                        buildId = syncContext.buildId,
                        parentId = syncContext.id,
                        progressIndicator = syncContext.progress
                    )

                    SyncCjpmBuildAdapter(syncContext, buildContext)
                }
            }
        }.unwrapOrElse {
            return@runWithChildProgress TaskResult.Err(
                CangJieBundle.message("failed.to.run.cjpm"),
                it.message
            )
        }
        val manifestPath = projectDirectory.resolve(CjpmConstants.MANIFEST_FILE)

        val ws = CjpmWorkspace.deserialize(manifestPath, projectDescriptionData)

        TaskResult.Ok(ws)


    }
}

private class SyncProcessAdapter(
    private val context: CjpmSyncTask.SyncContext
) : ProcessAdapter(),
    ProcessProgressListener {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<Any>) {
//        val text = event.text.trim { it <= ' ' }
//        if (text.startsWith("Updating") || text.startsWith("Downloading")) {
//            context.withProgressText(text)
//        }
//        if (text.startsWith("Vendoring")) {
//            // This code expect that vendoring message has the following format:
//            // "Vendoring %package_name% v%package_version% (%src_dir%) to %dst_dir%".
//            // So let's extract "Vendoring %package_name% v%package_version%" part and show it for users
//            val index = text.indexOf(" (")
//            val progressText = if (index != -1) text.substring(0, index) else text
//            context.withProgressText(progressText)
//        }
    }

    override fun error(title: String, message: String) = context.error(title, message)
    override fun warning(title: String, message: String) = context.warning(title, message)
}

private class SyncCjpmBuildContext(
    cjpmProject: CjpmProject,
    buildId: Any,
    parentId: Any,
    progressIndicator: ProgressIndicator
) : CjpmBuildContextBase(cjpmProject, CangJieBundle.message("progress.text.building"), false, buildId, parentId) {
    init {
        indicator = progressIndicator
    }
}

private class SyncCjpmBuildAdapter(
    private val context: CjpmSyncTask.SyncContext,
    buildContext: CjpmBuildContextBase
) : CjpmBuildAdapterBase(buildContext, context.project.service<SyncViewManager>()) {

    override fun onBuildOutputReaderFinish(
        event: ProcessEvent,
        isSuccess: Boolean,
        isCanceled: Boolean,
        error: Throwable?
    ) {
        when {
            isSuccess -> context.syncProgress.finish()
            isCanceled -> context.syncProgress.cancel()
            else -> context.syncProgress.fail()
        }
    }
}

private class CjpmProjectWithStdlib(
    val cjpmProject: CjpmProjectImpl,
    val stdlib: TaskResult<StandardLibrary>?
)

/**
 * 标准库
 */
private fun List<CjpmProjectWithStdlib>.chooseAndAttachStdlib(): List<CjpmProjectImpl> {
    val projectsWithStdlib = mapNotNull {
        val cjcVersion = it.cjpmProject.cjcInfo?.version ?: return@mapNotNull null
        val stdlib = (it.stdlib as? TaskResult.Ok)?.value ?: return@mapNotNull null
        CjpmProjectWithExistingStdlib(it.cjpmProject, cjcVersion, stdlib)
    }
    val embeddedStdlib = projectsWithStdlib.find { it.stdlib.isPartOfCjpmProject }
    val theMostRecentStdlib = embeddedStdlib
        ?: projectsWithStdlib.maxByOrNull { it.cjcVersion.semver }
    return map {
        when {
            it.stdlib == null -> it.cjpmProject
            it.stdlib is TaskResult.Err -> it.cjpmProject.withStdlib(it.stdlib)
            theMostRecentStdlib != null -> it.cjpmProject.withStdlib(TaskResult.Ok(theMostRecentStdlib.stdlib))
            else -> it.cjpmProject.withStdlib(it.stdlib)
        }
    }
}

private class CjpmProjectWithExistingStdlib(
    @Suppress("unused") val cargoProject: CjpmProjectImpl,
    val cjcVersion: CjcVersion,
    val stdlib: StandardLibrary
)
