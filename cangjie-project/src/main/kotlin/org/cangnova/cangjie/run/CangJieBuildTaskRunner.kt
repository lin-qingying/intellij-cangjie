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

package org.cangnova.cangjie.run

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskRunner
import com.intellij.task.TaskRunnerResults
import org.cangnova.cangjie.project.CjProjectBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import java.util.concurrent.*

private val LOG: Logger = logger<CangJieBuildTaskRunner>()
private val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment

/**
 * Main project task runner for CangJie projects.
 * Delegates to subsystem-specific runners based on build system.
 */
class CangJieBuildTaskRunner : ProjectTaskRunner() {

    override fun run(
        project: Project,
        context: ProjectTaskContext,
        vararg tasks: ProjectTask
    ): Promise<Result> {
        if (project.isDisposed) {
            return rejectedPromise("Project is already disposed")
        }

        val runner = CangJieProjectTaskRunner.findRunner()
        if (runner == null) {
            LOG.warn("No CangJie project task runner found for current build system")
            return rejectedPromise("No task runner available")
        }

        val resultPromise = AsyncPromise<Result>()
        val waitingIndicator = CompletableFuture<ProgressIndicator>()
        val queuedTask = BackgroundableProjectTaskRunner(
            project,
            context,
            tasks,
            runner,
            resultPromise,
            waitingIndicator
        )

        if (!isHeadlessEnvironment) {
            WaitingTask(project, waitingIndicator, queuedTask.executionStarted).queue()
        }

        CangJieBuildSessionsQueueManager.getInstance(project).queue(queuedTask)

        return resultPromise
    }

    override fun canRun(projectTask: ProjectTask): Boolean {
        val runner = CangJieProjectTaskRunner.findRunner() ?: return false
        return runner.canRun(projectTask)
    }
}

/**
 * Waiting task shown while build is in queue
 */
private class WaitingTask(
    project: Project,
    private val waitingIndicator: CompletableFuture<ProgressIndicator>,
    private val executionStarted: Future<Boolean>
) : Task.Backgroundable(
    project,
    CjProjectBundle.message("run.configuration.build.waiting"),
    true
) {
    override fun run(indicator: ProgressIndicator) {
        waitingIndicator.complete(indicator)
        try {
            while (true) {
                indicator.checkCanceled()
                try {
                    executionStarted.get(100, TimeUnit.MILLISECONDS)
                    break
                } catch (ignore: TimeoutException) {
                }
            }
        } catch (e: CancellationException) {
            throw ProcessCanceledException(e)
        } catch (e: InterruptedException) {
            throw ProcessCanceledException(e)
        } catch (e: ExecutionException) {
            LOG.error(e)
            throw ProcessCanceledException(e)
        }
    }
}

/**
 * Background task that executes the actual build
 */
private class BackgroundableProjectTaskRunner(
    project: Project,
    private val context: ProjectTaskContext,
    private val tasks: Array<out ProjectTask>,
    private val runner: CangJieProjectTaskRunner,
    private val totalPromise: AsyncPromise<ProjectTaskRunner.Result>,
    private val waitingIndicator: Future<ProgressIndicator>
) : Task.Backgroundable(
    project,
    CjProjectBundle.message("run.configuration.build.building"),
    true
), Runnable {

    val executionStarted: CompletableFuture<Boolean> = CompletableFuture()

    override fun run() {
        run(EmptyProgressIndicator())
    }

    override fun run(indicator: ProgressIndicator) {
        if (!waitForStart()) {
            if (totalPromise.state == Promise.State.PENDING) {
                totalPromise.cancel()
            }
            return
        }

        val allTasks = collectTasks(tasks)
        if (allTasks.isEmpty()) {
            totalPromise.setResult(TaskRunnerResults.FAILURE)
            return
        }

        // Start the indicator before using it
        indicator.start()

        try {
            for (task in allTasks) {
                indicator.checkCanceled()

                val buildResult = runner.executeTask(project, context, task)
                    .blockingGet(Integer.MAX_VALUE)

                if (buildResult == null || buildResult.hasErrors()) {
                    totalPromise.setResult(TaskRunnerResults.FAILURE)
                    return
                }

                if (buildResult.isAborted) {
                    totalPromise.setResult(TaskRunnerResults.ABORTED)
                    return
                }
            }

            // All tasks succeeded
            if (totalPromise.state == Promise.State.PENDING) {
                totalPromise.setResult(TaskRunnerResults.SUCCESS)
            }
        } catch (e: InterruptedException) {
            totalPromise.setResult(TaskRunnerResults.ABORTED)
            throw ProcessCanceledException(e)
        } catch (e: CancellationException) {
            totalPromise.setResult(TaskRunnerResults.ABORTED)
            throw ProcessCanceledException(e)
        } catch (e: Throwable) {
            LOG.error(e)
            totalPromise.setResult(TaskRunnerResults.FAILURE)
        } finally {
            indicator.stop()
        }
    }

    private fun collectTasks(tasks: Array<out ProjectTask>): Collection<ProjectTask> {
        val expandedTasks = tasks
            .filter { runner.canRun(it) }
            .map { runner.expandTask(it) }

        return if (expandedTasks.any { it.isEmpty() }) {
            emptyList()
        } else {
            expandedTasks.flatten()
        }
    }

    private fun waitForStart(): Boolean {
        if (isHeadlessEnvironment) return true

        try {
            val cancelled = waitingIndicator.get().isCanceled
            executionStarted.complete(true)
            return !cancelled
        } catch (e: InterruptedException) {
            totalPromise.setResult(TaskRunnerResults.ABORTED)
            throw ProcessCanceledException(e)
        } catch (e: CancellationException) {
            totalPromise.setResult(TaskRunnerResults.ABORTED)
            throw ProcessCanceledException(e)
        } catch (e: Throwable) {
            LOG.error(e)
            totalPromise.setResult(TaskRunnerResults.FAILURE)
            throw ProcessCanceledException(e)
        }
    }
}