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

package cn.cangnova.cangjie.buildsystem.task

import cn.cangnova.cangjie.buildsystem.CangJieBuildSystemBundle
import cn.cangnova.cangjie.buildsystem.api.BuildMode
import cn.cangnova.cangjie.buildsystem.api.CangJieBuildSystem
import cn.cangnova.cangjie.buildsystem.api.CangJieCompileContext
import cn.cangnova.cangjie.buildsystem.api.MessageLevel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.*
import org.jetbrains.concurrency.*
import java.util.concurrent.*

val isHeadlessEnvironment = ApplicationManager.getApplication().isHeadlessEnvironment

/**
 * 仓颉构建任务运行器
 * 集成到 IntelliJ 的构建流程中，通过扩展点系统调用相应的构建系统
 */
class CangJieBuildTaskRunner : ProjectTaskRunner() {

    override fun run(project: Project, context: ProjectTaskContext, vararg tasks: ProjectTask?): Promise<Result> {
        if (project.isDisposed) {
            return rejectedPromise("Project is already disposed")
        }

        val resultPromise = AsyncPromise<Result>()
        val waitingIndicator = CompletableFuture<ProgressIndicator>()
        val queuedTask = BackgroundableProjectTaskRunner(
            project,
            tasks.filterNotNull().toTypedArray(),
            this,
            resultPromise,
            waitingIndicator
        )

        if (!isHeadlessEnvironment) {
            WaitingTask(project, waitingIndicator, queuedTask.executionStarted).queue()
        }

        // 在后台执行任务
        queuedTask.queue()

        return resultPromise
    }

    override fun canRun(project: Project, projectTask: ProjectTask, context: ProjectTaskContext?): Boolean {
//        if (context != null && context.getRunConfiguration() is CangJieRunConfiguration) {
//            return false
//        }
        return canRun(project, projectTask)
    }

    override fun canRun(project: Project, projectTask: ProjectTask): Boolean {
        return when (projectTask) {
            is ModuleFilesBuildTask -> false
            is ModuleBuildTask -> {
                CangJieBuildSystem.EP_NAME.extensions.firstOrNull { it.isApplicable(project, projectTask) } != null
            }

            is ProjectModelBuildTask<*> -> {
//                CangJieBuildSystem.EP_NAME.extensions.firstOrNull { it.isApplicable(project, projectTask) } != null
                false
            }

            else -> false
        }
    }

    override fun canRun(projectTask: ProjectTask): Boolean {
        error(
            "CangJieBuildTaskRunner canRun(ProjectTask) should not be called, use canRun(Project, ProjectTask) instead"
        )


    }

    /**
     * 执行单个构建任务
     */
    fun executeTask(project: Project, task: ProjectTask): Promise<Result> {
        if (task !is ModuleBuildTask) {
            return resolvedPromise(TaskRunnerResults.ABORTED)
        }


        // 查找适用的构建系统
        val buildSystem = CangJieBuildSystem.EP_NAME.extensionList.firstOrNull { it.isApplicable(project) }
        if (buildSystem == null) {
            return resolvedPromise(TaskRunnerResults.FAILURE)
        }
//
//        // 创建构建器
        val builder = buildSystem.createBuilder(project)
//
        // 创建编译上下文
        val compileContext = object : CangJieCompileContext {
            override val project: Project = project

            override fun reportMessage(
                level: MessageLevel,
                message: String,
                file: VirtualFile?,
                line: Int?,
                column: Int?
            ) {
                // 这里可以集成到 IntelliJ 的日志系统或进度指示器
                // 暂时使用简单的日志输出
                val location = if (file != null) " at ${file.path}" else ""
                val lineInfo = if (line != null) ":$line" else ""
                val columnInfo = if (column != null) ":$column" else ""
                when (level) {
                    MessageLevel.INFO -> println("INFO: $message$location$lineInfo$columnInfo")
                    MessageLevel.WARNING -> println("WARNING: $message$location$lineInfo$columnInfo")
                    MessageLevel.ERROR -> println("ERROR: $message$location$lineInfo$columnInfo")
                }
            }

            override fun reportProgress(message: String, percent: Int) {
                // 这里可以更新进度指示器
                println("Progress: $message ($percent%)")
            }
        }

        // 执行构建任务
        return try {
            val success = builder.build(BuildMode.COMPILE, compileContext)
            if (success) {
                resolvedPromise(TaskRunnerResults.SUCCESS)
            } else {
                resolvedPromise(TaskRunnerResults.FAILURE)
            }
        } catch (e: Exception) {
            LOG.error(e)
            resolvedPromise(TaskRunnerResults.FAILURE)
        }

    }

    /**
     * 扩展任务列表
     */
    fun expandTask(project: Project, task: ProjectTask): List<ProjectTask> {
        return listOf(task)
    }
}

/**
 * 等待任务
 * 用于等待当前构建完成
 */
private class WaitingTask(
    project: Project,
    val waitingIndicator: CompletableFuture<ProgressIndicator>,
    val executionStarted: Future<Boolean>
) : Task.Backgroundable(project, "Waiting for the current build to finish...", true) {
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

/**
 * 后台项目任务运行器
 * 用于在后台执行构建任务
 */
private class BackgroundableProjectTaskRunner(
    project: Project,
    private val tasks: Array<out ProjectTask>,
    private val parentRunner: CangJieBuildTaskRunner,
    private val totalPromise: AsyncPromise<ProjectTaskRunner.Result>,
    private val waitingIndicator: Future<ProgressIndicator>
) : Task.Backgroundable(project, CangJieBuildSystemBundle.message("progress.title.building"), true) {

    private fun runTask(task: ProjectTask): Promise<ProjectTaskRunner.Result> = parentRunner.executeTask(project, task)
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
        val expandedTasks =
            tasks.filter { parentRunner.canRun(project, it) }.map { parentRunner.expandTask(project, it) }
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

private val LOG: Logger = logger<CangJieBuildTaskRunner>()