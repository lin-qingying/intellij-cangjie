package com.huawei.cangjie.debugger.runconfig.legacy

import com.huawei.cangjie.idea.project.CangJieProjectManager
import com.huawei.cangjie.idea.project.tools.projectWizard.wizard.getEnvironment
import com.huawei.cangjie.idea.run.CjpmArgsParser.Companion.parseArgs
import com.huawei.cangjie.idea.run.cjpm.*
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessHandler
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
import com.huawei.cangjie.idea.run.hasRemoteTarget
import com.huawei.cangjie.lang.lsp.toSystemPath
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.io.systemIndependentPath
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.nio.file.Path

abstract class CjAsyncRunner(
    private val executorId: String,
    @Suppress("UnstableApiUsage") @NlsContexts.DialogTitle private val errorMessageTitle: String
) : AsyncProgramRunner<RunnerSettings>() {
    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {

        state as CjpmRunStateBase
        val commandLine = state.commandLine


        val sdk = CangJieSdkManager.getProjectSdk(environment.project)

        val (commandArguments, executableArguments) = parseArgs(
            commandLine.command.executeCommand,
            commandLine.additionalArguments
        )
        val isTestRun = commandLine.command in listOf(CjpmCommand.TEST)
        val buildCommand =
            commandLine.copy(command = CjpmCommand.BUILD, additionalArguments = commandArguments).copy(withSudo = false)
        val getRunCommand = { executablePath: Path ->
            GeneralCommandLine().apply {

                exePath = executablePath.systemIndependentPath
                setWorkDirectory(environment.project.basePath)
                withEnvironment(sdk.getEnvironment())

                addParameters(executableArguments)
            }
        }


        return buildProjectAndGetBinaryArtifactPath(
            environment.project,
            buildCommand,
            state,
            isTestRun
        ).then { binary ->
            if (isTestRun) return@then null
            val path = binary?.path ?: return@then null
            val runCommand = getRunCommand(path)
            getRunContentDescriptor(state, environment, runCommand)
        }
    }

    open fun processUnsupportedToolchain(
        project: Project,
        toolchainError: BuildResult.ToolchainError,
        promise: AsyncPromise<Binary?>
    ) {
        project.showErrorDialog(toolchainError.message)
        promise.setResult(null)
    }

    open fun getRunContentDescriptor(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runCommand: GeneralCommandLine
    ): RunContentDescriptor? = showRunContent(executeCommandLine(state, runCommand, environment), environment)

    protected fun Project.showErrorDialog(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String) {
        Messages.showErrorDialog(this, message, errorMessageTitle)
    }

    private fun executeCommandLine(
        state: CjpmRunStateBase,
        commandLine: GeneralCommandLine,
        environment: ExecutionEnvironment
    ): DefaultExecutionResult = state.executeCommandLine(commandLine, environment)

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != this.executorId || profile !is CjpmCommandConfiguration ||
            profile.clean() !is CjpmCommandConfiguration.CleanConfiguration.Ok
        ) return false
        return !profile.hasRemoteTarget &&
                !profile.isBuildToolWindowAvailable &&
                !isBuildConfiguration(profile) &&
                getBuildConfiguration(profile) != null
    }

    open fun checkToolchainConfigured(project: Project): Boolean = true

    open fun checkToolchainSupported(project: Project, host: String): BuildResult.ToolchainError? = null

    private fun buildProjectAndGetBinaryArtifactPath(
        project: Project,
        command: CjpmCommandLine,
        state: CjpmRunStateBase,
        isTestBuild: Boolean
    ): Promise<Binary?> {

        val promise = AsyncPromise<Binary?>()

        val sdk = CangJieSdkManager.getProjectSdk(project)


        val processForUserOutput = ProcessOutput()
        val sdkVersion = CangJieSdkManager.sdkVersion.split(" ")[0]

        val commandLine = command.toGeneralCommandLine()
//        processForUser.addProcessListener(CapturingProcessAdapter(processForUserOutput))

        invokeLater {
            if (!checkToolchainConfigured(project)) {
                promise.setResult(null)
                return@invokeLater
            }
        }

        return promise
    }

    companion object {
        class Binary(val path: Path)

        private val LOG: Logger = logger<CjAsyncRunner>()
    }
}

//fun CjpmRunStateBase.executeCommandLine(
//    commandLine: GeneralCommandLine,
//    environment: ExecutionEnvironment
//): DefaultExecutionResult {
//    val runConfiguration = runConfiguration
//    val targetEnvironment = runConfiguration.targetEnvironment
//    val context = ConfigurationExtensionContext()
//
//    val extensionManager = RsRunConfigurationExtensionManager.getInstance()
//    extensionManager.patchCommandLine(runConfiguration, environment, commandLine, context)
//    extensionManager.patchCommandLineState(runConfiguration, environment, this, context)
//    val handler = commandLine.startProcess(environment.project, targetEnvironment, processColors = true, uploadExecutable = true)
//    extensionManager.attachExtensionsToProcess(runConfiguration, handler, environment, context)
//
//    val console = consoleBuilder.console
//    console.attachToProcess(handler)
//    return DefaultExecutionResult(console, handler)
//}