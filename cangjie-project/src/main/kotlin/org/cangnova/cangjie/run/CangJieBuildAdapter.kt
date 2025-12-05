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

package org.cangnova.cangjie.run

import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.*
import com.intellij.build.output.BuildOutputInstantReaderImpl
import com.intellij.execution.ExecutionManager
import com.intellij.execution.actions.StopProcessAction
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
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.run.CjExecutableRunner.Companion.artifacts
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JComponent

/**
 * Build adapter that handles build process events and output.
 * Integrates with IntelliJ's Build Tool Window.
 */
@Suppress("UnstableApiUsage")
class CangJieBuildAdapter(
    private val context: CangJieBuildContext,
    private val buildProgressListener: BuildProgressListener
) : ProcessAdapter() {

    private val instantReader = BuildOutputInstantReaderImpl(
        context.buildId,
        context.parentId ?: context.buildId,
        buildProgressListener,
        listOf(CangJieBuildEventsConverter(context))
    )

    init {
        val processHandler = checkNotNull(context.processHandler) { "Process handler can't be null" }
        context.environment.notifyProcessStarted(processHandler)

        val buildContentDescriptor = BuildContentDescriptor(
            null,
            null,
            object : JComponent() {},
            CjProjectBundle.message("run.configuration.build.building")
        )

        // Activate tool window by default
        val activateToolWindow = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = activateToolWindow
        buildContentDescriptor.isActivateToolWindowWhenFailed = activateToolWindow

        val descriptor = DefaultBuildDescriptor(
            context.buildId,
            context.taskName,
            context.workingDirectory.toString(),
            context.started
        )
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(createRerunAction(processHandler, context.environment))
            .withRestartAction(createStopAction(processHandler))

        val buildStarted = StartBuildEventImpl(
            descriptor,
            "Building ${context.taskName}..."
        )
        buildProgressListener.onEvent(context.buildId, buildStarted)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        // Convert line separators for proper parsing
        val text = StringUtil.convertLineSeparators(event.text)
        instantReader.append(text)
    }

    override fun processTerminated(event: ProcessEvent) {
        instantReader.closeAndGetFuture().whenComplete { _, error ->
            val isSuccess = event.exitCode == 0 && context.errors.get() == 0
            val isCanceled = context.indicator?.isCanceled ?: false
            onBuildFinished(event, isSuccess, isCanceled, error)
        }
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {
        context.environment.notifyProcessTerminating(event.processHandler)
    }

    private fun onBuildFinished(
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
            "Build $status",
            result
        )
        buildProgressListener.onEvent(context.buildId, buildFinished)

        context.finished(isSuccess)
        context.environment.notifyProcessTerminated(event.processHandler, event.exitCode)

        // Refresh targetPlatform directory
        refreshTargetDirectory()
    }

    private fun refreshTargetDirectory() {
        val targetPath = context.workingDirectory.resolve("target")
        val targetDir = VfsUtil.findFile(targetPath, true) ?: return
        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir)
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
                presentation.text = "Rerun ${StringUtil.escapeMnemonics(environment.runProfile.name)}"
                presentation.icon = if (processHandler.isProcessTerminated) {
                    AllIcons.Actions.Compile
                } else {
                    AllIcons.Actions.Restart
                }
                presentation.isEnabled = isEnabled
            }
        }
    }
}

/**
 * Build context that holds information about the current build
 */
class CangJieBuildContext(
    val buildId: Any,
    val parentId: Any?,
    val taskName: String,
    val workingDirectory: Path,
    val environment: ExecutionEnvironment,
    val isTestBuild: Boolean = false,
    val buildProfile: BuildProfile = BuildProfile.RELEASE  // Default to RELEASE for now
) {
    @Volatile
    var processHandler: ProcessHandler? = null

    @Volatile
    var indicator: com.intellij.openapi.progress.ProgressIndicator? = null

    val started: Long = System.currentTimeMillis()
    var finished: Long = started
        private set

    val errors: AtomicInteger = AtomicInteger(0)
    val warnings: AtomicInteger = AtomicInteger(0)

    @Volatile
    var artifacts: List<CompilerArtifact> = emptyList()

    private val duration: Long get() = finished - started

    private var finishedCallback: ((Boolean) -> Unit)? = null

    companion object {
        private val BUILD_SEMAPHORE_KEY: Key<java.util.concurrent.Semaphore> =
            Key.create("CANGJIE_BUILD_SEMAPHORE_KEY")
    }

    private val buildSemaphore: java.util.concurrent.Semaphore =
        environment.project.getUserData(BUILD_SEMAPHORE_KEY)
            ?: (environment.project as com.intellij.openapi.util.UserDataHolderEx)
                .putUserDataIfAbsent(BUILD_SEMAPHORE_KEY, java.util.concurrent.Semaphore(1))

    fun onFinished(callback: (Boolean) -> Unit) {
        finishedCallback = callback
    }

    fun finished(success: Boolean) {
        val isCanceled = indicator?.isCanceled ?: false

        // Set artifacts if build succeeded
        environment.artifacts = artifacts.takeIf { success && !isCanceled }

        finished = System.currentTimeMillis()
        buildSemaphore.release()

        finishedCallback?.invoke(success)

        // Show notification
        showBuildNotification(success, isCanceled)
    }

    fun canceled() {
        finished = System.currentTimeMillis()
        buildSemaphore.release()
        finishedCallback?.invoke(false)
        environment.notifyProcessNotStarted()
    }

    /**
     * Wait for any ongoing build to finish and acquire the build semaphore
     */
    fun waitAndStart(): Boolean {
        indicator?.pushState()
        try {
            indicator?.text = CjProjectBundle.message("run.configuration.build.waiting")
            indicator?.text2 = ""
            while (true) {
                indicator?.checkCanceled()
                try {
                    if (buildSemaphore.tryAcquire(100, java.util.concurrent.TimeUnit.MILLISECONDS)) break
                } catch (e: InterruptedException) {
                    throw com.intellij.openapi.progress.ProcessCanceledException()
                }
            }
        } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
            canceled()
            return false
        } finally {
            indicator?.popState()
        }
        return true
    }

    private fun showBuildNotification(isSuccess: Boolean, isCanceled: Boolean) {
        val project = environment.project
        val errors = errors.get()
        val warnings = warnings.get()

        val (message, messageType) = when {
            isCanceled -> "$taskName canceled" to com.intellij.openapi.ui.MessageType.INFO
            !isSuccess -> "$taskName failed" to com.intellij.openapi.ui.MessageType.ERROR
            errors > 0 || warnings > 0 -> "$taskName completed with warnings" to com.intellij.openapi.ui.MessageType.WARNING
            else -> "$taskName successful" to com.intellij.openapi.ui.MessageType.INFO
        }

        val details = if (errors > 0 || warnings > 0) {
            val errorsString = if (errors == 1) "error" else "errors"
            val warningsString = if (warnings == 1) "warning" else "warnings"
            "$errors $errorsString, $warnings $warningsString"
        } else null

        CangJieBuildManager.showBuildNotification(project, messageType, message, details, duration)
    }
}

// Extension functions for ExecutionEnvironment
fun ExecutionEnvironment.notifyProcessNotStarted() {
    val executionListener = project.messageBus.syncPublisher(
        com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
    )
    executionListener.processNotStarted(executor.id, this)
}

fun ExecutionEnvironment.notifyProcessStartScheduled() {
    val executionListener = project.messageBus.syncPublisher(
        com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
    )
    executionListener.processStartScheduled(executor.id, this)
}

fun ExecutionEnvironment.notifyProcessStarting() {
    val executionListener = project.messageBus.syncPublisher(
        com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
    )
    executionListener.processStarting(executor.id, this)
}

fun ExecutionEnvironment.notifyProcessStarted(handler: ProcessHandler) {
    val executionListener = project.messageBus.syncPublisher(
        com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
    )
    executionListener.processStarted(executor.id, this, handler)
}

fun ExecutionEnvironment.notifyProcessTerminating(handler: ProcessHandler) {
    val executionListener = project.messageBus.syncPublisher(
        com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
    )
    executionListener.processTerminating(executor.id, this, handler)
}

fun ExecutionEnvironment.notifyProcessTerminated(handler: ProcessHandler, exitCode: Int) {
    val executionListener = project.messageBus.syncPublisher(
        com.intellij.execution.ExecutionManager.EXECUTION_TOPIC
    )
    executionListener.processTerminated(executor.id, this, handler, exitCode)
}