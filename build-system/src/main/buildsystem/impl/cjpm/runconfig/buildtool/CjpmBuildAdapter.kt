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

package org.cangnova.cangjie.buildsystem.impl.cjpm.runconfig.buildtool


import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.cjpm.CjpmConstants



import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.*
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.execution.ExecutionManager
import com.intellij.execution.actions.StopProcessAction
import com.intellij.execution.filters.Filter
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import javax.swing.JComponent


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
            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }
            private val isEnabled: Boolean
                get() {
                    val project = environment.project
                    val settings = environment.runnerAndConfigurationSettings
                    return (!DumbService.isDumb(project) || settings == null || settings.type.isDumbAware) &&
                            !ExecutionManager.getInstance(project).isStarting(environment) &&
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
