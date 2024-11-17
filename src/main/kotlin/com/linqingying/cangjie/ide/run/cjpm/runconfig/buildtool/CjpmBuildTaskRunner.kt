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

package com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool


import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.findChild
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration.Companion.findCjpmProject
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandLine
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandRunner
import com.linqingying.cangjie.ide.run.cjpm.createCjpmCommandRunConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.*
import com.intellij.task.ProjectTaskRunner.Result
import com.intellij.task.impl.ProjectModelBuildTaskImpl
import org.jetbrains.concurrency.*
import java.util.concurrent.*

private val LOG: Logger = logger<CjpmBuildTaskRunner>()
val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment

class CjpmBuildTaskRunner : ProjectTaskRunner() {


    override fun run(project: Project, context: ProjectTaskContext, vararg tasks: ProjectTask): Promise<Result> {
        if (project.isDisposed) {
            return rejectedPromise("Project is already disposed")
        }


        val resultPromise = AsyncPromise<Result>()
        val waitingIndicator = CompletableFuture<ProgressIndicator>()
        val queuedTask = BackgroundableProjectTaskRunner(
            project,
            tasks,
            this,
            resultPromise,
            waitingIndicator
        )
        if (!isHeadlessEnvironment) {
            WaitingTask(project, waitingIndicator, queuedTask.executionStarted).queue()
        }

        CjpmBuildSessionsQueueManager.getInstance(project)
            .buildSessionsQueue
            .run(queuedTask, ModalityState.defaultModalityState(), EmptyProgressIndicator())

        return resultPromise
    }


    override fun canRun(projectTask: ProjectTask): Boolean {

        return when (projectTask) {
            is ModuleFilesBuildTask -> false
            is ModuleBuildTask -> {
                if (projectTask.module.cjpmProjectRoot != null) return true
                val runManager = RunManager.getInstance(projectTask.module.project)
                val buildableElement = runManager.selectedConfiguration?.configuration
                buildableElement is CjpmCommandConfiguration && buildableElement.isBuildToolWindowAvailable
            }

            is ProjectModelBuildTask<*> -> {
                val buildableElement = projectTask.buildableElement
                buildableElement is CjpmBuildConfiguration && buildableElement.enabled
            }

            else -> false
        }

    }


    fun expandTask(task: ProjectTask): List<ProjectTask> {
        if (task !is ModuleBuildTask) return listOf(task)
        val project = task.module.project
        val runManager = RunManager.getInstance(project)

        val selectedConfiguration = runManager.selectedConfiguration?.configuration as? CjpmCommandConfiguration

        if (selectedConfiguration != null) {
            val buildConfiguration = CjpmBuildManager.getBuildConfiguration(selectedConfiguration) ?: return emptyList()
            val environment = CjpmBuildManager.createBuildEnvironment(buildConfiguration) ?: return emptyList()
            val buildableElement = CjpmBuildConfiguration(buildConfiguration, environment)
            return listOf(ProjectModelBuildTaskImpl(buildableElement, task.isIncrementalBuild))
        }

        val cjpmProjects = project.cjpmProjects.allProjects
        if (cjpmProjects.isEmpty()) return emptyList()


        val executor =
            ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID) ?: return emptyList()
        val runner = ProgramRunner.findRunnerById(CjpmCommandRunner.RUNNER_ID) ?: return emptyList()


        return cjpmProjects.mapNotNull {

            val commandLine = CjpmCommandLine.forProject(it, "build")
            val settings = runManager.createCjpmCommandRunConfiguration(commandLine)
            val environment = ExecutionEnvironment(executor, runner, settings, project)
            val configuration = settings.configuration as? CjpmCommandConfiguration ?: return@mapNotNull null

            val buildableElement = CjpmBuildConfiguration(configuration, environment)

            ProjectModelBuildTaskImpl(buildableElement, task.isIncrementalBuild)

        }

    }


    fun executeTask(task: ProjectTask): Promise<Result> {
        if (task !is ProjectModelBuildTask<*>) {
            return resolvedPromise(TaskRunnerResults.ABORTED)
        }

        val buildConfiguration = task.buildableElement as CjpmBuildConfiguration


        if (!task.isIncrementalBuild) {
            val cjpmProject = with(buildConfiguration.configuration) {
                findCjpmProject(project, command, workingDirectory)
            }

            if (cjpmProject != null) {
                val result = try {
                    val cleanFuture = CjpmBuildManager.clean(cjpmProject)
                    if (cleanFuture.get()) {
                        TaskRunnerResults.SUCCESS
                    } else {
                        TaskRunnerResults.FAILURE
                    }
                } catch (e: ExecutionException) {
                    TaskRunnerResults.FAILURE
                }
                if (result.hasErrors()) {
                    resolvedPromise(result)
                }
            }


        }

        val result = try {
            val buildFuture = CjpmBuildManager.build(buildConfiguration)
            val buildResult = buildFuture.get()
            when {
                buildResult.canceled -> TaskRunnerResults.ABORTED
                buildResult.succeeded -> TaskRunnerResults.SUCCESS
                else -> TaskRunnerResults.FAILURE
            }
        } catch (e: ExecutionException) {
            LOG.error(e)
            TaskRunnerResults.FAILURE
        } catch (e: RuntimeException) {
//            LOG.error(e)

            TaskRunnerResults.FAILURE
        }

        val promise = AsyncPromise<Result>()
        promise.setResult(result)
        return promise
    }
}

data class CjpmBuildResult(
    val succeeded: Boolean,
    val canceled: Boolean,
    val started: Long,
    val duration: Long = 0,
    val errors: Int = 0,
    val warnings: Int = 0,
    val message: String = ""
)


private class WaitingTask(
    project: Project,
    val waitingIndicator: CompletableFuture<ProgressIndicator>,
    val executionStarted: Future<Boolean>
) : Task.Backgroundable(project, CangJieBundle.message("progress.text.waiting.for.current.build.to.finish"), true) {
    override fun run(indicator: ProgressIndicator) {
        // Wait until queued task will start executing.
        // Needed so that user can cancel build tasks from queue.
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


private class BackgroundableProjectTaskRunner(
    project: Project,
    private val tasks: Array<out ProjectTask>,
    private val parentRunner: CjpmBuildTaskRunner,
    private val totalPromise: AsyncPromise<Result>,
    private val waitingIndicator: Future<ProgressIndicator>
) : Task.Backgroundable(project, CangJieBundle.message("progress.title.building"), true) {

    private fun runTask(task: ProjectTask): Promise<Result> = parentRunner.executeTask(task)
    val executionStarted: CompletableFuture<Boolean> = CompletableFuture()


    //    TODO 需要重构为CompileDriver类
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

        try {
            for (task in allTasks) {
// TODO 验证sdk
//                if(validateSdk(project)){
                val promise = runTask(task)
                if (promise.blockingGet(Integer.MAX_VALUE) != TaskRunnerResults.SUCCESS) {
                    // Do not continue session if one of builds failed
                    totalPromise.setResult(TaskRunnerResults.FAILURE)
                    break
                }
//                }


            }

            // everything succeeded - set final result to success
            if (totalPromise.isPending) {
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
        val expandedTasks = tasks.filter { parentRunner.canRun(it) }.map { parentRunner.expandTask(it) }
        return if (expandedTasks.any { it.isEmpty() }) emptyList() else expandedTasks.flatten()
    }

    private fun waitForStart(): Boolean {
        if (isHeadlessEnvironment) return true

        try {
            // Check if this build wasn't cancelled while it was in queue through waiting indicator
            val cancelled = waitingIndicator.get().isCanceled
            // Notify waiting background task that this build started and there is no more need for this indicator
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

val Module.cjpmProjectRoot: VirtualFile?
    get() = ModuleRootManager.getInstance(this).contentRoots.firstOrNull {
        it.findChild(CjpmConstants.MANIFEST_FILE) != null
    }
