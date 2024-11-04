package com.linqingying.cangjie.debugger.runconfig.legacy

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm.Companion.getCjpmCommonPatch

import com.linqingying.cangjie.ide.run.CjpmArgsParser.Companion.parseArgs
import com.linqingying.cangjie.ide.run.cjpm.*
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjProcessHandler
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.localBuildArgsForRemoteRun
import com.linqingying.cangjie.ide.run.hasRemoteTarget

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.RunContentExecutor
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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.nio.file.Path
import java.nio.file.Paths
fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()
abstract class CjAsyncRunner(
    private val executorId: String,
    @Suppress("UnstableApiUsage") @NlsContexts.DialogTitle private val errorMessageTitle: String
) : AsyncProgramRunner<RunnerSettings>() {
    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
        saveAllDocuments()
        state as CjpmRunStateBase
//        val commandLine = state.commandLine

//
//        val sdk = CangJieSdkManager.getProjectSdk(environment.project)
//
//        val (commandArguments, executableArguments) = parseArgs(
//            commandLine.command.executeCommand,
//            commandLine.additionalArguments
//        )
//
//
//        val isTestRun = commandLine.command in listOf(CjpmCommand.TEST)
//        val buildCommand =
//            commandLine.copy(command = CjpmCommand.BUILD, additionalArguments = commandArguments).copy(withSudo = false)
//        val getRunCommand = { executablePath: Path ->
//            GeneralCommandLine().apply {
//
//                exePath = executablePath.systemIndependentPath
//                setWorkDirectory(environment.project.basePath)
//                withEnvironment(sdk.getEnvironment())
//
//                addParameters(executableArguments)
//            }
//        }
//
//
//        return buildProjectAndGetBinaryArtifactPath(
//            environment.project,
//            buildCommand,
//            state,
//            isTestRun
//        ).then { binary ->
//            if (isTestRun) return@then null
//            val path = binary?.path ?: return@then null
//            val runCommand = getRunCommand(path)
//            getRunContentDescriptor(state, environment, runCommand)
//        }

        val commandLine = state.prepareCommandLine(getCjpmCommonPatch(environment.project))
        val (commandArguments, executableArguments) = parseArgs(commandLine.command, commandLine.additionalArguments)
        val additionalBuildArgs = state.configuration.localBuildArgsForRemoteRun

        val isTestRun = commandLine.command in listOf("test", "bench")
        val cmdHasNoRun = "--no-run" in commandLine.additionalArguments
        val buildCommand = if (isTestRun) {
            if (cmdHasNoRun) commandLine else commandLine.prependArgument("--no-run")
        } else {
            commandLine.copy(command = "build", additionalArguments = commandArguments + additionalBuildArgs)
        }.copy(withSudo = false) // building does not require root privileges

        val getRunCommand = { executablePath: Path ->
            with(commandLine) {
                state.toolchain.createGeneralCommandLine(
                    executablePath,
                    workingDirectory,
                    redirectInputFrom,
//                    backtraceMode,
                    environmentVariables,
                    executableArguments,
                    false, // emulateTerminal
                    withSudo,
                    patchToRemote = false // patching is performed for debugger/profiler/valgrind on CLion side if needed
                )
            }
        }


        return buildProjectAndGetBinaryArtifactPath(environment.project, buildCommand, state, isTestRun)
            .then { binary ->
                if (isTestRun && cmdHasNoRun) return@then null
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



        val processForUserOutput = ProcessOutput()

        val isDebug = "-g" in command.additionalArguments
//        if (isDebug) {
//
//        }

        val commandLine = if (isDebug) {
            command.toGeneralCommandLine()
        } else {
            command.copy(additionalArguments = command.additionalArguments + "-g").toGeneralCommandLine()
        }
        LOG.debug("Executing Cargo command: `${commandLine.commandLineString}`")
        val processForUser = CjProcessHandler(commandLine)
        processForUser.addProcessListener(CapturingProcessAdapter(processForUserOutput))


        invokeLater {
            if (!checkToolchainConfigured(project)) {
                promise.setResult(null)
                return@invokeLater
            }


            RunContentExecutor(project, processForUser).apply {


            }.withAfterCompletion {
                if (processForUserOutput.exitCode != 0) {
                    promise.setResult(null)
                    return@withAfterCompletion
                }


                object : Task.Backgroundable(project, CangJieBundle.message("progress.title.building.cjpm.project")) {
                    var result: BuildResult? = null


                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true


                        val projectPath = project.basePath

                        result =
                            BuildResult.Binaries(listOf("$projectPath/target/debug/bin/main${if (SystemInfo.isWindows) ".exe" else ""}"))
                    }

                    override fun onSuccess() {
                        when (val result = result!!) {
                            is BuildResult.ToolchainError -> {
                                processUnsupportedToolchain(project, result, promise)
                            }

                            is BuildResult.Binaries -> {
                                val binaries = result.paths
                                when {
                                    binaries.isEmpty() -> {
                                        project.showErrorDialog(CangJieBundle.message("dialog.message.can.t.find.binary"))
                                        promise.setResult(null)
                                    }

                                    binaries.size > 1 -> {
                                        project.showErrorDialog(
                                            CangJieBundle.message("dialog.message.more.than.one.binary.was.produced.please.specify.bin.lib.test.or.example.flag.explicitly")
                                        )
                                        promise.setResult(null)
                                    }

                                    else -> promise.setResult(Binary(Paths.get(binaries.single())))
                                }
                            }
                        }
                    }

                    override fun onThrowable(error: Throwable) = promise.setResult(null)

                }.queue()
            }.run()
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
