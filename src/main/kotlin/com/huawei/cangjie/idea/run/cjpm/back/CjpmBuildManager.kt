//package com.huawei.cangjie.idea.run.cjpm.back
//
//import com.huawei.cangjie.CangJieBundle
//import com.huawei.cangjie.idea.project.CjpmProject
//import com.huawei.cangjie.idea.run.CjpmArgsParser.Companion.parseArgs
//import com.huawei.cangjie.idea.run.cjpm.*
//import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.*
//import com.huawei.cangjie.idea.run.cjpm.runconfig.isUnitTestMode
//import com.huawei.cangjie.idea.run.hasRemoteTarget
//import com.intellij.build.BuildContentManager
//import com.intellij.build.BuildViewManager
//import com.intellij.execution.ExecutionListener
//import com.intellij.execution.ExecutionManager
//import com.intellij.execution.ExecutorRegistry
//import com.intellij.execution.RunManager
//import com.intellij.execution.configuration.EnvironmentVariablesData
//import com.intellij.execution.executors.DefaultRunExecutor
//import com.intellij.execution.impl.RunManagerImpl
//import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
//import com.intellij.execution.runners.ExecutionEnvironment
//import com.intellij.execution.runners.ProgramRunner
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.application.TransactionGuard
//import com.intellij.openapi.components.service
//import com.intellij.openapi.fileEditor.FileDocumentManager
//import com.intellij.openapi.progress.EmptyProgressIndicator
//import com.intellij.openapi.progress.ProcessCanceledException
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.progress.Task
//import com.intellij.util.execution.ParametersListUtil
//import com.intellij.util.ui.UIUtil
//import org.jetbrains.annotations.TestOnly
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.ExecutionException
//import java.util.concurrent.Future
//fun ExecutionEnvironment.notifyProcessStartScheduled() =
//    executionListener.processStartScheduled(executor.id, this)
//private val ExecutionEnvironment.executionListener: ExecutionListener
//    get() = project.messageBus.syncPublisher(ExecutionManager.EXECUTION_TOPIC)
//fun ExecutionEnvironment.notifyProcessNotStarted() =
//    executionListener.processNotStarted(executor.id, this)
//fun ExecutionEnvironment.notifyProcessStarting() =
//    executionListener.processStarting(executor.id, this)
//
//object CjpmBuildManager {
//
//
//
//
//
//    fun createBuildEnvironment(
//        buildConfiguration: CjpmCommandConfiguration,
//        environment: ExecutionEnvironment? = null
//    ): ExecutionEnvironment? {
//        require(isBuildConfiguration(buildConfiguration))
//        val project = buildConfiguration.project
//        val runManager = RunManager.getInstance(project) as? RunManagerImpl ?: return null
//        val executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID) ?: return null
//        val runner = ProgramRunner.findRunnerById(CjpmCommandRunner.RUNNER_ID) ?: return null
//        val settings = RunnerAndConfigurationSettingsImpl(runManager, buildConfiguration)
//        settings.isActivateToolWindowBeforeRun = environment.isActivateToolWindowBeforeRun
//        val buildEnvironment = ExecutionEnvironment(executor, runner, settings, project)
//        environment?.copyUserDataTo(buildEnvironment)
//        return buildEnvironment
//    }
//
//
//    private val BUILDABLE_COMMANDS: List<String> = listOf("run", "test", "bench")
//    fun getBuildConfiguration(configuration: CjpmCommandConfiguration): CjpmCommandConfiguration? {
//        if (isBuildConfiguration(configuration)) return configuration
//
//        val parsed = ParsedCommand.parse(configuration.command) ?: return null
//        if (parsed.command !in BUILDABLE_COMMANDS) return null
//        val commandArguments = parseArgs(parsed.command, parsed.additionalArguments).commandArguments.toMutableList()
//        commandArguments.addAll(configuration.localBuildArgsForRemoteRun)
//
//        // https://github.com/intellij-rust/intellij-rust/issues/3707
//        if (parsed.command == "test" && commandArguments.contains("--doc")) return null
//
//        val buildConfiguration = configuration.clone() as CjpmCommandConfiguration
//        buildConfiguration.name = "Build `${buildConfiguration.name}`"
//        buildConfiguration.command?.command  = ParametersListUtil.join(when (parsed.command) {
//            "run" -> listOfNotNull(parsed.toolchain, "build", *commandArguments.toTypedArray())
//            "test" -> listOfNotNull(parsed.toolchain, "test", "--no-run", *commandArguments.toTypedArray())
//            "bench" -> listOfNotNull(parsed.toolchain, "bench", "--no-run", *commandArguments.toTypedArray())
//            else -> return null
//        })
//
//
//        // building does not require root privileges and redirect input anyway
//        buildConfiguration.withSudo = false
//        buildConfiguration.isRedirectInput = false
//
//        buildConfiguration.defaultTargetName = buildConfiguration.defaultTargetName
//            .takeIf { buildConfiguration.buildTarget.isRemote }
//
//        return buildConfiguration
//    }
//    private val cjpmBuildPatch: CjpmPatch = { commandLine ->
//        val additionalArguments = mutableListOf<String>().apply {
//            addAll(commandLine.additionalArguments)
//            remove("-q")
//            remove("--quiet")
//
//            addFormatJsonOption(this, "--message-format", "json-diagnostic-rendered-ansi")
//        }
//
//        val oldVariables = commandLine.environmentVariables
//        val environmentVariables = EnvironmentVariablesData.create(
//            // https://doc.rust-lang.org/cjpm/reference/environment-variables.html#configuration-environment-variables
//            // These environment variables are needed to force progress bar to non-TTY output
//            oldVariables.envs + mapOf(
//                "CJPM_TERM_PROGRESS_WHEN" to "always",
//                "CJPM_TERM_PROGRESS_WIDTH" to "1000"
//            ),
//            oldVariables.isPassParentEnvs
//        )
//
//        commandLine.copy(additionalArguments = additionalArguments, environmentVariables = environmentVariables)
//    }
//    private val CANCELED_BUILD_RESULT: Future<CjpmBuildResult> =
//        CompletableFuture.completedFuture(CjpmBuildResult(succeeded = false, canceled = true, started = 0))
//
//    @TestOnly
//    @Volatile
//    var lastBuildCommandLine: CjpmCommandLine? = null
//    @TestOnly
//    @Volatile
//    var mockProgressIndicator: MockProgressIndicator? = null
//    private fun execute(
//        context: CjpmBuildContext,
//        doExecute: CjpmBuildContext.() -> Unit
//    ): Future<CjpmBuildResult>{
//        context.environment.notifyProcessStartScheduled()
//        val processCreationLock = Any()
//
//        when {
//            isUnitTestMode ->
//                context.indicator = mockProgressIndicator ?: EmptyProgressIndicator()
//            isHeadlessEnvironment ->
//                context.indicator = EmptyProgressIndicator()
//            else -> {
//                val indicatorResult = CompletableFuture<ProgressIndicator>()
//                UIUtil.invokeLaterIfNeeded {
//                    object : Task.Backgroundable(context.project, context.taskName, true) {
//                        override fun run(indicator: ProgressIndicator) {
//                            indicatorResult.complete(indicator)
//
//                            var wasCanceled = false
//                            while (!context.result.isDone) {
//                                if (!wasCanceled && indicator.isCanceled) {
//                                    wasCanceled = true
//                                    synchronized(processCreationLock) {
//                                        context.shellProcessHandler?.destroyProcess()
//                                    }
//                                }
//
//                                try {
//                                    Thread.sleep(100)
//                                } catch (e: InterruptedException) {
//                                    throw ProcessCanceledException(e)
//                                }
//                            }
//                        }
//                    }.queue()
//                }
//
//                try {
//                    context.indicator = indicatorResult.get()
//                } catch (e: ExecutionException) {
//                    context.result.completeExceptionally(e)
//                    return context.result
//                }
//            }
//        }
//
//        context.indicator?.text = context.progressTitle
//        context.indicator?.text2 = ""
//
//
//
//        ApplicationManager.getApplication().executeOnPooledThread {
//            if (!context.waitAndStart()) return@executeOnPooledThread
//            context.environment.notifyProcessStarting()
//
//            if (isUnitTestMode) {
//                context.doExecute()
//                return@executeOnPooledThread
//            }
//
//            // BACKCOMPAT: 2019.3
//            @Suppress("DEPRECATION")
//            TransactionGuard.submitTransaction(context.project, Runnable {
//                synchronized(processCreationLock) {
//                    val isCanceled = context.indicator?.isCanceled ?: false
//                    if (isCanceled) {
//                        context.canceled()
//                        return@Runnable
//                    }
//
//                    saveAllDocuments()
//                    context.doExecute()
//                }
//            })
//        }
//        return context.result
//    }
//
//    fun build(buildConfiguration: CjpmBuildConfiguration): Future<CjpmBuildResult> {
//        val configuration = buildConfiguration.configuration
//        val environment = buildConfiguration.environment
//        val project = environment.project
//        environment.cjpmPatches += cjpmBuildPatch
//
//        val myState = CjpmRunState(
//            environment,
//            configuration,
//            configuration.clean().ok ?: return CANCELED_BUILD_RESULT
//        )
//
//        val cjpmProject = myState.cjpmProject ?: return CANCELED_BUILD_RESULT
//
//        @Suppress("UsePropertyAccessSyntax")
//        BuildContentManager.getInstance(project).getOrCreateToolWindow()
//        if (isUnitTestMode) {
//            lastBuildCommandLine = myState.prepareCommandLine()
//        }
//
//        val buildId = Any()
//        return execute(
//            CjpmBuildContext(
//            cjpmProject = cjpmProject,
//            environment = environment,
//            taskName = CangJieBundle.message("progress.title.build"),
//            progressTitle = CangJieBundle.message("progress.text.building1"),
//            isTestBuild = myState.commandLine.command in listOf("test", "bench"),
//            buildId = buildId,
//            parentId = buildId
//        )
//        ){
//            val buildProgressListener = project.service<BuildViewManager>()
//            if (!isHeadlessEnvironment) {
//                @Suppress("UsePropertyAccessSyntax")
//                val buildToolWindow = BuildContentManager.getInstance(project).getOrCreateToolWindow()
//                buildToolWindow.setAvailable(true, null)
//                if (environment.isActivateToolWindowBeforeRun) {
//                    buildToolWindow.activate(null)
//                }
//            }
//
//            shellProcessHandler = myState.startProcess(processColors = false)
//            shellProcessHandler?.addProcessListener(CjpmBuildAdapter(this, buildProgressListener))
//            shellProcessHandler?.startNotify()
//        }
//    }
//    val CjpmCommandConfiguration.isBuildToolWindowAvailable: Boolean
//        get() {
//
//            val hasBuildBeforeRunTask = beforeRunTasks.any { task -> task is CjpmBuildTaskProvider.BuildTask }
//            return hasBuildBeforeRunTask && (!hasRemoteTarget || buildTarget.isLocal)
//        }
//    fun clean(project: CjpmProject): Future<Boolean> =
//        CjpmCommandLine.forProject(project, "clean" )
//            .runAsync(project, saveConfiguration = false)
//
//    fun isBuildConfiguration(configuration: CjpmCommandConfiguration): Boolean {
//        val parsed = ParsedCommand.parse(configuration.command) ?: return false
//        return when (val command = parsed.command) {
//            "build", "check", "clippy" -> true
//            "test", "bench" -> {
//                val (commandArguments, _) = parseArgs(command, parsed.additionalArguments)
//                "--no-run" in commandArguments
//            }
//            else -> false
//        }
//    }
//}
//
//fun addFormatJsonOption(additionalArguments: MutableList<String>, formatOption: String, format: String) {
//    val formatJsonOption = "$formatOption=$format"
//    val idx = additionalArguments.indexOf(formatOption)
//    val indexArgWithValue = additionalArguments.indexOfFirst { it.startsWith(formatOption) }
//    if (idx != -1) {
//        if (idx < additionalArguments.size - 1) {
//            if (!additionalArguments[idx + 1].startsWith("-")) {
//                additionalArguments[idx + 1] = format
//            } else {
//                additionalArguments.add(idx + 1, format)
//            }
//        } else {
//            additionalArguments.add(format)
//        }
//    } else if (indexArgWithValue != -1) {
//        additionalArguments[indexArgWithValue] = formatJsonOption
//    } else {
//        additionalArguments.add(0, formatJsonOption)
//    }
//}
//val ExecutionEnvironment?.isActivateToolWindowBeforeRun: Boolean
//    get() = this?.runnerAndConfigurationSettings?.isActivateToolWindowBeforeRun != false
//val CjpmCommandConfiguration.localBuildArgsForRemoteRun: List<String>
//    get() = if (hasRemoteTarget && buildTarget.isLocal) {
//        ParametersListUtil.parse(targetEnvironment?.languageRuntime?.localBuildArgs.orEmpty())
//    } else {
//        emptyList()
//    }
//fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()
//class MockProgressIndicator : EmptyProgressIndicator() {
//    private val _textHistory: MutableList<String?> = mutableListOf()
//    val textHistory: List<String?> get() = _textHistory
//
//    override fun setText(text: String?) {
//        super.setText(text)
//        _textHistory += text
//    }
//
//    override fun setText2(text: String?) {
//        super.setText2(text)
//        _textHistory += text
//    }
//}
