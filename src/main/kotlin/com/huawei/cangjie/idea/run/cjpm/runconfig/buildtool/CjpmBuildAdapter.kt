package com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool


import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.idea.project.CangJieProjectManager


import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.*
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.actions.StopProcessAction
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.serviceContainer.NonInjectable
import org.intellij.lang.annotations.Language
import java.nio.file.Paths
import javax.swing.JComponent
import kotlin.math.max


@Suppress("UnstableApiUsage")
class  CjpmBuildAdapter(
    private val context: CjpmBuildContext,
    buildProgressListener: BuildProgressListener
) : CjpmBuildAdapterBase(context, buildProgressListener) {

    init {
        val processHandler = checkNotNull(context.processHandler) { "Process handler can't be null" }
        context.environment.notifyProcessStarted(processHandler)

        val buildContentDescriptor =
            BuildContentDescriptor(null, null, object : JComponent() {}, CangJieBundle.message("build"))
        val activateToolWindow = context.environment.isActivateToolWindowBeforeRun

        buildContentDescriptor.isActivateToolWindowWhenAdded = activateToolWindow
        buildContentDescriptor.isActivateToolWindowWhenFailed = activateToolWindow

        val descriptor = DefaultBuildDescriptor(
            context.buildId,
            CangJieBundle.message("build.event.title.run.cjpm.command"),
            context.workingDirectory.toString(),
            context.started
        )
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(createRerunAction(processHandler, context.environment))
            .withRestartAction(createStopAction(processHandler))
            .apply { createFilters().forEach { withExecutionFilter(it) } }


        val buildStarted =
            StartBuildEventImpl(descriptor, CangJieBundle.message("build.event.message.running", context.taskName))
        buildProgressListener.onEvent(context.buildId, buildStarted)
    }


    override fun onBuildOutputReaderFinish(
        event: ProcessEvent,
        isSuccess: Boolean,
        isCanceled: Boolean,
        error: Throwable?
    ) {
        val (status, result) = when {
            isCanceled -> "canceled" to SkippedResultImpl()
            isSuccess -> "successful" to SuccessResultImpl()
            else -> "failed" to FailureResultImpl(error)
        }
        val buildFinished = FinishBuildEventImpl(
            context.buildId,
            null,
            System.currentTimeMillis(),
            CangJieBundle.message("build.event.message.", context.taskName, status),
            result
        )
        buildProgressListener.onEvent(context.buildId, buildFinished)
        context.finished(isSuccess)
        context.environment.notifyProcessTerminated(event.processHandler, event.exitCode)
        val targetPath = context.workingDirectory.resolve(CjpmConstants.ProjectLayout.target)
        val targetDir = VfsUtil.findFile(targetPath, true) ?: return
        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir)

    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        context.environment.notifyProcessTerminating(event.processHandler)
    }

    companion object {

        private fun createStopAction(processHandler: ProcessHandler): StopProcessAction =
            StopProcessAction("Stop", "Stop", processHandler)


        private fun createRerunAction(
            processHandler: ProcessHandler,
            environment: ExecutionEnvironment
        ): RestartProcessAction =
            RestartProcessAction(processHandler, environment)

        private class RestartProcessAction(
            private val processHandler: ProcessHandler,
            private val environment: ExecutionEnvironment
        ) : DumbAwareAction() {
            override fun actionPerformed(e: AnActionEvent) {
                ExecutionManagerImpl.stopProcess(processHandler)
                ExecutionUtil.restart(environment)
            }

            private val isEnabled: Boolean
                get() {
                    val project = environment.project
                    val settings = environment.runnerAndConfigurationSettings
                    return (!DumbService.isDumb(project) || settings == null || settings.type.isDumbAware) &&
                            !ExecutorRegistry.getInstance().isStarting(environment) &&
                            !processHandler.isProcessTerminating
                }

            override fun update(event: AnActionEvent) {
                val presentation = event.presentation
                presentation.text =
                    CangJieBundle.message("action.rerun.text", StringUtil.escapeMnemonics(environment.runProfile.name))
                presentation.icon =
                    if (processHandler.isProcessTerminated) AllIcons.Actions.Compile else AllIcons.Actions.Restart
                presentation.isEnabled = isEnabled
            }
        }

    }

}

@Suppress("UnstableApiUsage")
abstract class CjpmBuildAdapterBase(
    private val context: CjpmBuildContextBase,
    protected val buildProgressListener: BuildProgressListener
) : ProcessAdapter() {
    private val instantReader = BuildOutputInstantReaderImpl(
        context.buildId,
        context.parentId,
        buildProgressListener,
        listOf(CjBuildEventsConverter(context))
    )

    override fun processTerminated(event: ProcessEvent) {
        instantReader.closeAndGetFuture().whenComplete { _, error ->
            val isSuccess = event.exitCode == 0 && context.errors.get() == 0
            val isCanceled = context.indicator?.isCanceled ?: false
            onBuildOutputReaderFinish(event, isSuccess = isSuccess, isCanceled = isCanceled, error)
        }
    }

    open fun onBuildOutputReaderFinish(
        event: ProcessEvent,
        isSuccess: Boolean,
        isCanceled: Boolean,
        error: Throwable?
    ) {
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        // Progress messages end with '\r' instead of '\n'. We want to replace '\r' with '\n'
        // so that `instantReader` sends progress messages to parsers separately from other messages.
        val text = StringUtil.convertLineSeparators(event.text)
        instantReader.append(text)
    }
}

fun createFilters(): Collection<Filter> = buildList {

//    val dir = CangJieProjectManager.workspaceRootDir
//    if (dir != null) {
//        add(CjConsoleFilter(CangJieProjectManager.getCurrentProject(), dir))
//        add(CjDbgFilter(CangJieProjectManager.getCurrentProject(), dir))
//        add(CjPanicFilter(CangJieProjectManager.getCurrentProject(), dir))
//        add(CjBacktraceFilter( CangJieProjectManager.currentProject, dir, CangJieProjectManager.workspace))
//    }
}
