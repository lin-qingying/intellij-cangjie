package com.huawei.cangjie.cjpm.project.model.impl

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.CjTask
import com.huawei.cangjie.cjpm.project.model.CjcInfo
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.tools.cjc
import com.huawei.cangjie.cjpm.toolchain.tools.unwrapOrElse
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
import com.intellij.openapi.actionSystem.AnActionEvent
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

class CjpmSyncTask(
    project: Project,
    private val cjpmProjects: List<CjpmProjectImpl>,
    private val result: CompletableFuture<List<CjpmProjectImpl>>
) :
    Task.Backgroundable(project, CangJieBundle.message("progress.title.reloading.cjpm.projects"), true),
    CjTask {


//    private fun doRun(
//        indicator: ProgressIndicator,
//        syncProgress: BuildProgress<BuildProgressDescriptor>
//    ): List<CjpmProjectImpl> {
//        val toolchain = project.toolchain
//
//        @Suppress("UnnecessaryVariable")
//        val refreshedProjects = if (toolchain == null) {
//            syncProgress.fail(
//                System.currentTimeMillis(),
//                CangJieBundle.message("build.event.message.cjpm.project.update.failed.no.cangjie.toolchain")
//            )
//            cjpmProjects
//        } else {
//            cjpmProjects.map { cjpmProject ->
//                syncProgress.runWithChildProgress(
//                    CangJieBundle.message("build.event.title.sync.project", cjpmProject.presentableName),
//                    createContext = { it },
//                    action = { childProgress ->
////                        if (!cjpmProject.workingDirectory.exists()) {
////                            childProgress.message(
////                                CangJieBundle.message("tooltip.project.directory.does.not.exist"),
////                                CangJieBundle.message(
////                                    "build.event.message.project.directory.does.not.exist.consider.detaching.project.from.cjpm.tool.window",
////                                    cjpmProject.workingDirectory,
////                                    cjpmProject.presentableName
////                                ),
////                                MessageEvent.Kind.ERROR,
////                                null
////                            )
////                            val stdlibStatus =
////                                CjpmProject.UpdateStatus.UpdateFailed(CangJieBundle.message("tooltip.project.directory.does.not.exist"))
////                            CjpmProjectWithStdlib(cjpmProject.copy(stdlibStatus = stdlibStatus), null)
////                        } else {
////                            val context =
////                                SyncContext(project, cjpmProject, toolchain, indicator, syncProgress.id, childProgress)
////                            val cjcInfoResult = fetchCjcInfo(context)
////                            val cjcInfo = (cjcInfoResult as? TaskResult.Ok)?.value
////                            val cjpmProjectWithCjcInfoAndWorkspace = cjpmProject.withCjcInfo(cjcInfoResult)
////                                .withWorkspace(fetchCjpmWorkspace(context, cjcInfo))
////                            CjpmProjectWithStdlib(
////                                cjpmProjectWithCjcInfoAndWorkspace,
////                                fetchStdlib(context, cjpmProjectWithCjcInfoAndWorkspace, cjcInfo)
////                            )
////                        }
//                    }
//                )
//            }.chooseAndAttachStdlib()
//                .deduplicateProjects()
//        }
//        return refreshedProjects
//    }

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
                    LOG.warn("Failed to fetch cj version", it)
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
//            val refreshedProjects = doRun(indicator, syncProgress)

            val refreshedProjects = cjpmProjects
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