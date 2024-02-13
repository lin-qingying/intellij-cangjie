package com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.notifications.CjNotifications
import com.huawei.cangjie.idea.run.CjpmArgsParser
import com.huawei.cangjie.idea.run.cjpm.*
import com.huawei.cangjie.idea.run.cjpm.runconfig.isUnitTestMode


import com.huawei.cangjie.idea.run.hasRemoteTarget
import com.huawei.cangjie.lang.lsp.toSystemPath
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.huawei.cangjie.lang.sdk.validateSdk
import com.intellij.build.BuildContentManager
import com.intellij.build.BuildViewManager
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.ide.nls.NlsMessages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.SystemNotifications


import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future


val CjpmCommandConfiguration.localBuildArgsForRemoteRun: List<String>
    get() = if (hasRemoteTarget && buildTarget.isLocal) {
        ParametersListUtil.parse(targetEnvironment?.languageRuntime?.localBuildArgs.orEmpty())
    } else {
        emptyList()
    }


val ExecutionEnvironment?.isActivateToolWindowBeforeRun: Boolean
    get() = this?.runnerAndConfigurationSettings?.isActivateToolWindowBeforeRun != false

object CjpmBuildManager {


    val CjpmCommandConfiguration.isBuildToolWindowAvailable: Boolean
        get() {

            val hasBuildBeforeRunTask = beforeRunTasks.any { task -> task is CjpmBuildTaskProvider.BuildTask }
            return hasBuildBeforeRunTask && (!hasRemoteTarget || buildTarget.isLocal)
        }

    fun createBuildEnvironment(
        buildConfiguration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment? = null
    ): ExecutionEnvironment? {
        require(isBuildConfiguration(buildConfiguration))
        val project = buildConfiguration.project
        val runManager = RunManager.getInstance(project) as? RunManagerImpl ?: return null
        val executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID) ?: return null
        val runner = ProgramRunner.findRunnerById(CjpmCommandRunner.RUNNER_ID) ?: return null
        val settings = RunnerAndConfigurationSettingsImpl(runManager, buildConfiguration)
        settings.isActivateToolWindowBeforeRun = environment.isActivateToolWindowBeforeRun
        val buildEnvironment = ExecutionEnvironment(executor, runner, settings, project)
        environment?.copyUserDataTo(buildEnvironment)
        return buildEnvironment
    }


    fun isBuildConfiguration(configuration: CjpmCommandConfiguration): Boolean {
        val parsed = configuration.command?.let { ParsedCommand.parse(it) } ?: return false
        return when (val command = parsed.command.command) {
            "build", "check", "clippy" -> true
            "test", "bench" -> {
                val (commandArguments, _) = CjpmArgsParser.parseArgs(command, parsed.additionalArguments)
                "--no-run" in commandArguments
            }

            else -> false
        }
    }


    private val BUILDABLE_COMMANDS: List<String> = listOf("run", "test", "bench")
    fun getBuildConfiguration(configuration: CjpmCommandConfiguration): CjpmCommandConfiguration? {
        if (isBuildConfiguration(configuration)) return configuration

        val parsed = configuration.command?.let { ParsedCommand.parse(it) } ?: return null
        if (parsed.command.command !in BUILDABLE_COMMANDS) return null



        if (configuration.executorId == "Debug") {
            parsed.additionalArguments.add("-g")
        }

        val commandArguments = CjpmArgsParser.parseArgs(
            parsed.command.command,
            parsed.additionalArguments
        ).commandArguments.toMutableList()
        commandArguments.addAll(configuration.localBuildArgsForRemoteRun)


        if (parsed.command == CjpmCommand.TEST && commandArguments.contains("--doc")) return null

        val buildConfiguration = configuration.clone() as CjpmCommandConfiguration


        buildConfiguration.name = "Build `${buildConfiguration.name}`"

        buildConfiguration.command = when (parsed.command) {

            CjpmCommand.RUN -> {
                CjpmCommand.BUILD.apply {
                    executeCommand = ParametersListUtil.join(listOfNotNull("build", *commandArguments.toTypedArray()))
                }
            }


            CjpmCommand.TEST -> {
                CjpmCommand.TEST.apply {
                    executeCommand =
                        ParametersListUtil.join(listOfNotNull("test", "--no-run", *commandArguments.toTypedArray()))
                }
            }
//            parsed.command.command == "run" -> {
//                CjpmCommand.BUILD.apply {
//                    executeCommand = ParametersListUtil.join(listOfNotNull("build", *commandArguments.toTypedArray()))
//                }
//            }
//
//
//
//            parsed.command.command == "test" -> {
//                val command = CjpmCommand.TEST
//                command.executeCommand =
//                    ParametersListUtil.join(
//                        listOfNotNull(
//
//                            "test",
//                            "--no-run",
//                            *commandArguments.toTypedArray()
//                        )
//                    )
//                command
//            }

            else -> return null
        }


        // building does not require root privileges and redirect input anyway
        buildConfiguration.withSudo = false
        buildConfiguration.isRedirectInput = false

        buildConfiguration.defaultTargetName = buildConfiguration.defaultTargetName
            .takeIf { buildConfiguration.buildTarget.isRemote }

        return buildConfiguration
    }

    private val CANCELED_BUILD_RESULT: Future<CjpmBuildResult> =
        CompletableFuture.completedFuture(CjpmBuildResult(succeeded = false, canceled = true, started = 0))

    fun clean(): Future<Boolean> =
        CjpmCommandLine.forProject(CjpmCommand.CLEAN)
            .runAsync(saveConfiguration = false)


    private fun execute(
        context: CjpmBuildContext,
        doExecute: CjpmBuildContext.() -> Unit
    ): Future<CjpmBuildResult> {
        context.environment.notifyProcessStartScheduled()
        val processCreationLock = Any()
        when {
            isUnitTestMode ->
                context.indicator = mockProgressIndicator ?: EmptyProgressIndicator()

            isHeadlessEnvironment ->
                context.indicator = EmptyProgressIndicator()

            else -> {
                val indicatorResult = CompletableFuture<ProgressIndicator>()
                UIUtil.invokeLaterIfNeeded {
                    object : Task.Backgroundable(context.project, context.taskName, true) {
                        override fun run(indicator: ProgressIndicator) {
                            indicatorResult.complete(indicator)
                            var wasCanceled = false
                            while (!context.result.isDone) {
                                if (!wasCanceled && indicator.isCanceled) {
                                    wasCanceled = true
                                    synchronized(processCreationLock) {
                                        context.processHandler?.destroyProcess()
                                    }
                                }

                                try {
                                    Thread.sleep(100)
                                } catch (e: InterruptedException) {
                                    throw ProcessCanceledException(e)
                                }
                            }
                        }

                    }.queue()
                }


                try {
                    context.indicator = indicatorResult.get()
                } catch (e: ExecutionException) {
                    context.result.completeExceptionally(e)
                    return context.result
                }


                context.indicator?.text = context.progressTitle
                context.indicator?.text2 = ""


                ApplicationManager.getApplication().executeOnPooledThread {
                    if (!context.waitAndStart()) return@executeOnPooledThread
                    context.environment.notifyProcessStarting()
                    if (isUnitTestMode) {
                        context.doExecute()
                        return@executeOnPooledThread
                    }

                    @Suppress("DEPRECATION")
                    TransactionGuard.submitTransaction(context.project, Runnable {
                        synchronized(processCreationLock) {
                            val isCanceled = context.indicator?.isCanceled ?: false
                            if (isCanceled) {
                                context.canceled()
                                return@Runnable
                            }

                            saveAllDocuments()
                            context.doExecute()
                        }
                    })
                }
            }
        }
        return context.result
    }

    fun build(buildConfiguration: CjpmBuildConfiguration): Future<CjpmBuildResult> {

        val configuration = buildConfiguration.configuration
        val environment = buildConfiguration.environment
        val project = environment.project
//        TODO 验证SDK
        if (!validateSdk(project)) {
            throw RuntimeException("SDK error")
        }
        val state = CjpmRunState(
            environment,
            configuration,
            configuration.clean().ok ?: return CANCELED_BUILD_RESULT
        )
        // 确保生成工具窗口已初始化：
        @Suppress("UsePropertyAccessSyntax")
        ApplicationManager.getApplication().invokeLater {
            BuildContentManager.getInstance(project).getOrCreateToolWindow()
        }
        val buildId = configuration.executorId
        return execute(
            CjpmBuildContext(

                environment = environment,
                taskName = CangJieBundle.message("progress.title.build"),
                progressTitle = CangJieBundle.message("progress.text.building1"),
                isTestBuild = state.commandLine.command.command in listOf("test", "bench"),
                buildId = buildId,
                parentId = buildId
            )
        ) {
            val buildProgressListener = project.service<BuildViewManager>()


            if (!isHeadlessEnvironment) {
                @Suppress("UsePropertyAccessSyntax")
                val buildToolWindow = BuildContentManager.getInstance(project).getOrCreateToolWindow()
                buildToolWindow.setAvailable(true, null)
                if (environment.isActivateToolWindowBeforeRun) {
                    buildToolWindow.activate(null)
                }
            }

            processHandler = state.startProcess(processColors = false)
            processHandler?.addProcessListener(CjpmBuildAdapter(this, buildProgressListener))
            processHandler?.startNotify()
        }

    }

    @NlsContexts.NotificationContent
    private fun buildNotificationMessage(message: String, details: String?, time: Long): String {
        var notificationContent = CangJieBundle.message(
            "notification.content.choice.with",
            message,
            details ?: "",
            if (details == null) 0 else 1
        )
        if (time > 0) notificationContent += CangJieBundle.message(
            "notification.content.in",
            NlsMessages.formatDuration(time)
        )
        return notificationContent
    }

    fun showBuildNotification(
        project: Project,
        messageType: MessageType,
        @NlsContexts.SystemNotificationTitle message: String,
        @NlsContexts.SystemNotificationText details: String? = null,
        time: Long = 0
    ) {
        val notificationContent = buildNotificationMessage(message, details, time)
        val notification = CjNotifications.buildLogGroup().createNotification(notificationContent, messageType)
        notification.notify(project)

        if (messageType === MessageType.ERROR) {
            val manager = ToolWindowManager.getInstance(project)
            invokeLater {
                manager.notifyByBalloon(BuildContentManager.TOOL_WINDOW_ID, messageType, notificationContent)
            }
        }

        SystemNotifications.getInstance().notify(
            notification.groupId,
            StringUtil.capitalizeWords(message, true),
            details ?: ""
        )
    }

    fun getExecutable(project: Project, buildId: String): String {
        val sdkVersion = CangJieSdkManager.sdkVersion.split(" ")[0]

        if (sdkVersion < "0.45.2") return "${project.basePath}/build/bin/main".toSystemPath()
        return when (buildId) {
            "Debug" -> "${project.basePath}/build/debug/bin/main".toSystemPath()
            "Run" -> "${project.basePath}/build/release/bin/main".toSystemPath()
            else -> ""
        }


    }

    @TestOnly
    @Volatile
    var mockProgressIndicator: MockProgressIndicator? = null
}

class MockProgressIndicator : EmptyProgressIndicator() {
    private val _textHistory: MutableList<String?> = mutableListOf()
    val textHistory: List<String?> get() = _textHistory

    override fun setText(text: String?) {
        super.setText(text)
        _textHistory += text
    }

    override fun setText2(text: String?) {
        super.setText2(text)
        _textHistory += text
    }
}

fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()
