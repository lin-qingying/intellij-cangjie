//package com.huawei.cangjie.idea.run.cjpm.back
//
//import com.huawei.cangjie.CangJieBundle
//import com.huawei.cangjie.idea.run.cjpm.*
//import com.huawei.cangjie.idea.run.cjpm.back.CjpmBuildConfiguration
//import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager
//
//import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.createBuildEnvironment
//import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
//import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
//import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildSessionsQueueManager
//
//
//import com.intellij.execution.ExecutorRegistry
//import com.intellij.execution.RunManager
//import com.intellij.execution.executors.DefaultRunExecutor
//import com.intellij.execution.runners.ExecutionEnvironment
//import com.intellij.execution.runners.ProgramRunner
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.application.ModalityState
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.diagnostic.logger
//import com.intellij.openapi.module.Module
//import com.intellij.openapi.progress.EmptyProgressIndicator
//import com.intellij.openapi.progress.ProcessCanceledException
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.progress.Task
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.roots.ModuleRootManager
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.task.*
//import com.intellij.task.impl.ProjectModelBuildTaskImpl
//import org.jetbrains.concurrency.*
//import java.util.concurrent.*
//
//private val LOG: Logger = logger<CjpmBuildTaskRunner>()
//val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment
//
//class CjpmBuildTaskRunner : ProjectTaskRunner() {
//
//
//    override fun run(project: Project, context: ProjectTaskContext, vararg tasks: ProjectTask): Promise<Result> {
//        if (project.isDisposed) {
//            return rejectedPromise("Project is already disposed")
//        }
//
//
//        val resultPromise = AsyncPromise<Result>()
//        val waitingIndicator = CompletableFuture<ProgressIndicator>()
//
//        val queuedTask = BackgroundableProjectTaskRunner(
//            project,
//            tasks,
//            this,
//            resultPromise,
//            waitingIndicator
//        )
//        if (!isHeadlessEnvironment) {
//            WaitingTask(project, waitingIndicator, queuedTask.executionStarted).queue()
//        }
//
//        CjpmBuildSessionsQueueManager.getInstance(project)
//            .buildSessionsQueue
//            .run(queuedTask, ModalityState.defaultModalityState(), EmptyProgressIndicator())
//
//        return resultPromise
//    }
//
//    fun executeTask(task: ProjectTask): Promise<Result> {
//        if (task !is ProjectModelBuildTask<*>) {
//            return resolvedPromise(TaskRunnerResults.ABORTED)
//        }
//
//        val buildConfiguration = task.buildableElement as CjpmBuildConfiguration
//
//        if (!task.isIncrementalBuild) {
//
//
//                val result = try {
//                    val cleanFuture = CjpmBuildManager.clean()
//                    if (cleanFuture.get()) {
//                        TaskRunnerResults.SUCCESS
//                    } else {
//                        TaskRunnerResults.FAILURE
//                    }
//                } catch (e: ExecutionException) {
//                    TaskRunnerResults.FAILURE
//                }
//                if (result.hasErrors()) {
//                    resolvedPromise(result)
//                }
//            }
//
//        val result = try {
//            val buildFuture = CjpmBuildManager.build(buildConfiguration)
//            val buildResult = buildFuture.get()
//            when {
//                buildResult.canceled -> TaskRunnerResults.ABORTED
//                buildResult.succeeded -> TaskRunnerResults.SUCCESS
//                else -> TaskRunnerResults.FAILURE
//            }
//        } catch (e: ExecutionException) {
//            TaskRunnerResults.FAILURE
//        }
//
//        val promise = AsyncPromise<Result>()
//        promise.setResult(result)
//        return promise
//    }
//
//
//    fun expandTask(task: ProjectTask): List<ProjectTask> {
//        if (task !is ModuleBuildTask) return listOf(task)
//
//        val project = task.module.project
//        val runManager = RunManager.getInstance(project)
//
//        val selectedConfiguration = runManager.selectedConfiguration?.configuration as? CjpmCommandConfiguration
//        if (selectedConfiguration != null) {
//            val buildConfiguration = getBuildConfiguration(selectedConfiguration) ?: return emptyList()
//            val environment = createBuildEnvironment(buildConfiguration) ?: return emptyList()
//            val buildableElement = CjpmBuildConfiguration(buildConfiguration, environment)
//            return listOf(ProjectModelBuildTaskImpl(buildableElement, task.isIncrementalBuild))
//        }
//
//        val cjpmProjects = project.cjpmProjects.allProjects
//        if (cjpmProjects.isEmpty()) return emptyList()
//
//        val executor =
//            ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID) ?: return emptyList()
//        val runner = ProgramRunner.findRunnerById(CjpmCommandRunner.RUNNER_ID) ?: return emptyList()
//
//        val additionalArguments = buildList {
//            val settings = project.cangjieSettings
//            add("--all")
//            if (settings.compileAllTargets) {
//                val allTargets = settings.toolchain
//                    ?.cjpm()
//                    ?.checkSupportForBuildCheckAllTargets()
//                    ?: false
//                if (allTargets) add("--all-targets")
//            }
//        }
//
//        return cjpmProjects.mapNotNull { cjpmProject ->
//            val commandLine = CjpmCommandLine.forProject(cjpmProject, "build", additionalArguments)
//            val settings = runManager.createCjpmCommandRunConfiguration(commandLine)
//            val environment = ExecutionEnvironment(executor, runner, settings, project)
//            val configuration = settings.configuration as? CjpmCommandConfiguration ?: return@mapNotNull null
//
//            val buildableElement = CjpmBuildConfiguration(configuration, environment)
//            ProjectModelBuildTaskImpl(buildableElement, task.isIncrementalBuild)
//        }
//    }
//
//    override fun canRun(projectTask: ProjectTask): Boolean {
//        return when (projectTask) {
//            is ModuleFilesBuildTask -> false
//            is ModuleBuildTask -> {
//                if (projectTask.module.CjpmProjectRoot != null) return true
//                val runManager = RunManager.getInstance(projectTask.module.project)
//                val buildableElement = runManager.selectedConfiguration?.configuration
//                buildableElement is CjpmCommandConfiguration && buildableElement.isBuildToolWindowAvailable
//            }
//
//            is ProjectModelBuildTask<*> -> {
//                val buildableElement = projectTask.buildableElement
//                buildableElement is CjpmBuildConfiguration && buildableElement.enabled
//            }
//
//            else -> false
//        }
//    }
//}
//
//const val MANIFEST_FILE = "module.json"
//val Module.CjpmProjectRoot: VirtualFile?
//    get() = ModuleRootManager.getInstance(this).contentRoots.firstOrNull {
//        it.findChild(MANIFEST_FILE) != null
//    }
//
//private class BackgroundableProjectTaskRunner(
//    project: Project,
//    private val tasks: Array<out ProjectTask>,
//    private val parentRunner: CjpmBuildTaskRunner,
//    private val totalPromise: AsyncPromise<ProjectTaskRunner.Result>,
//    private val waitingIndicator: Future<ProgressIndicator>
//) : Task.Backgroundable(project, CangJieBundle.message("progress.title.building"), true) {
//    val executionStarted: CompletableFuture<Boolean> = CompletableFuture()
//
//    private fun collectTasks(tasks: Array<out ProjectTask>): Collection<ProjectTask> {
//        val expandedTasks = tasks.filter { parentRunner.canRun(it) }.map { parentRunner.expandTask(it) }
//        return if (expandedTasks.any { it.isEmpty() }) emptyList() else expandedTasks.flatten()
//    }
//
//    private fun waitForStart(): Boolean {
//        if (isHeadlessEnvironment) return true
//
//        try {
//            // Check if this build wasn't cancelled while it was in queue through waiting indicator
//            val cancelled = waitingIndicator.get().isCanceled
//            // Notify waiting background task that this build started and there is no more need for this indicator
//            executionStarted.complete(true)
//            return !cancelled
//        } catch (e: InterruptedException) {
//            totalPromise.setResult(TaskRunnerResults.ABORTED)
//            throw ProcessCanceledException(e)
//        } catch (e: CancellationException) {
//            totalPromise.setResult(TaskRunnerResults.ABORTED)
//            throw ProcessCanceledException(e)
//        } catch (e: Throwable) {
//            LOG.error(e)
//            totalPromise.setResult(TaskRunnerResults.FAILURE)
//            throw ProcessCanceledException(e)
//        }
//    }
//
//    override fun run(indicator: ProgressIndicator) {
//        if (!waitForStart()) {
//            if (totalPromise.myState == Promise.State.PENDING) {
//                totalPromise.cancel()
//            }
//            return
//        }
//
//        val allTasks = collectTasks(tasks)
//        if (allTasks.isEmpty()) {
//            totalPromise.setResult(TaskRunnerResults.FAILURE)
//            return
//        }
//
//        try {
//            for (task in allTasks) {
//                val promise = runTask(task)
//                if (promise.blockingGet(Integer.MAX_VALUE) != TaskRunnerResults.SUCCESS) {
//                    // Do not continue session if one of builds failed
//                    totalPromise.setResult(TaskRunnerResults.FAILURE)
//                    break
//                }
//            }
//
//            // everything succeeded - set final result to success
//            if (totalPromise.isPending) {
//                totalPromise.setResult(TaskRunnerResults.SUCCESS)
//            }
//        } catch (e: InterruptedException) {
//            totalPromise.setResult(TaskRunnerResults.ABORTED)
//            throw ProcessCanceledException(e)
//        } catch (e: CancellationException) {
//            totalPromise.setResult(TaskRunnerResults.ABORTED)
//            throw ProcessCanceledException(e)
//        } catch (e: Throwable) {
//            LOG.error(e)
//            totalPromise.setResult(TaskRunnerResults.FAILURE)
//        }
//    }
//
//    private fun runTask(task: ProjectTask): Promise<ProjectTaskRunner.Result> = parentRunner.executeTask(task)
//}
//
//private class WaitingTask(
//    project: Project,
//    val waitingIndicator: CompletableFuture<ProgressIndicator>,
//    val executionStarted: Future<Boolean>
//) : Task.Backgroundable(project, CangJieBundle.message("progress.text.waiting.for.current.build.to.finish"), true) {
//    override fun run(indicator: ProgressIndicator) {
//        // Wait until queued task will start executing.
//        // Needed so that user can cancel build tasks from queue.
//        waitingIndicator.complete(indicator)
//        try {
//            while (true) {
//                indicator.checkCanceled()
//                try {
//                    executionStarted.get(100, TimeUnit.MILLISECONDS)
//                    break
//                } catch (ignore: TimeoutException) {
//                }
//            }
//        } catch (e: CancellationException) {
//            throw ProcessCanceledException(e)
//        } catch (e: InterruptedException) {
//            throw ProcessCanceledException(e)
//        } catch (e: ExecutionException) {
//            LOG.error(e)
//            throw ProcessCanceledException(e)
//        }
//
//    }
//}
//
//fun CjToolchainBase.cjpm(): Cjpm = Cjpm(this)
