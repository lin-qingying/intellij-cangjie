package com.linqingying.cangjie.ide.run.cjpm


import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.text.nullize
import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.linqingying.cangjie.cjpm.toolchain.cjpm
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm
import com.linqingying.cangjie.ide.run.CangJieRunConfigurationExtensionManager
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjLanguageRuntimeConfiguration

import com.linqingying.cangjie.ide.run.createProcessHandler
import com.linqingying.cangjie.ide.run.startProcess


private val CJPM_PATCHES: Key<List<CjpmPatch>> = Key.create("CJPMPATCHES")

typealias CjpmPatch = (CjpmCommandLine) -> CjpmCommandLine

var ExecutionEnvironment.cjpmPatches: List<CjpmPatch>
    get() = putUserDataIfAbsent(CJPM_PATCHES, emptyList())
    set(value) = putUserData(CJPM_PATCHES, value)

abstract class CjpmRunStateBase(
    environment: ExecutionEnvironment,
    val configuration: CjpmCommandConfiguration,
    config: CjpmCommandConfiguration.CleanConfiguration.Ok
) : CommandLineState(environment)/*, TargetEnvironmentAwareRunProfileState */ {
    val project: Project = environment.project
    val commandLine: CjpmCommandLine = config.cmd
    val executorId: String = environment.executor.id
    protected val commandLinePatches: MutableList<CjpmPatch> = mutableListOf()
    val toolchain: CjToolchainBase = config.toolchain
    val cjpmProject: CjpmProject? = CjpmCommandConfiguration.findCjpmProject(
        project,
        commandLine.additionalArguments,
        commandLine.workingDirectory
    )

    init {
        commandLinePatches.addAll(environment.cjpmPatches)
    }

    fun cjpm(): Cjpm = toolchain.cjpm()


    companion object {
        private val LOG: Logger = logger<CjpmRunStateBase>()

        private const val SSH_TARGET_TYPE_ID: String = "ssh/sftp"
    }


    fun prepareCommandLine(vararg additionalPatches: CjpmPatch): CjpmCommandLine {
        var commandLine = commandLine
        for (patch in commandLinePatches) {
            commandLine = patch(commandLine)
        }
        for (patch in additionalPatches) {
            commandLine = patch(commandLine)
        }
        return commandLine
    }




    fun startProcess(processColors: Boolean): ProcessHandler {


        val targetEnvironment = configuration.targetEnvironment
        // 在本地目标的情况下回退到非目标实施


        if (targetEnvironment == null) {


            val commandLine = cjpm().toGeneralCommandLine(environment.project, prepareCommandLine())


            LOG.debug("Executing Cjpm command: `${commandLine.commandLineString}`")
//            val handler = CjProcessHandler(commandLine, processColors)
            val handler = commandLine.createProcessHandler()
            ProcessTerminatedListener.attach(handler) // shows exit code upon termination

            CangJieRunConfigurationExtensionManager.attachExtensionsToProcess(
                configuration, handler,
                runnerSettings
            )
            return handler

        }

        val remoteRunPatch: CjpmPatch = { commandLine ->
            if (configuration.buildTarget.isRemote && targetEnvironment.typeId == SSH_TARGET_TYPE_ID) {
                commandLine.prependArgument("--build-dir=${targetEnvironment.projectRootOnTarget}/target")
            } else {
                commandLine
            }.copy(emulateTerminal = false)
        }

        val commandLine = cjpm().toGeneralCommandLine(project, prepareCommandLine(remoteRunPatch))
        commandLine.exePath = targetEnvironment.languageRuntime?.cjpmPath.nullize(true) ?: "cjpm"
        return commandLine.startProcess(project, targetEnvironment, processColors, uploadExecutable = false)


    }


    override fun startProcess(): ProcessHandler = startProcess(processColors = true)

    override fun createConsole(executor: Executor): ConsoleView? {
        val console = super.createConsole(executor) ?: return null
        return CangJieRunConfigurationExtensionManager.decorateExecutionConsole(
            configuration,
            runnerSettings, console, executor
        )

    }
}

val TargetEnvironmentConfiguration.languageRuntime: CjLanguageRuntimeConfiguration?
    get() = runtimes.findByType()


