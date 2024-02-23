package com.huawei.cangjie.idea.run.cjpm


import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.tools.Cjpm
import com.huawei.cangjie.cjpm.toolchain.tools.cjpm
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjLanguageRuntimeConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessHandler
import com.huawei.cangjie.idea.run.cjpm.runconfig.startProcess
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.text.nullize


private val CJPM_PATCHES: Key<List<CjpmPatch>> = Key.create("CJPMPATCHES")

typealias CjpmPatch = (CjpmCommandLine) -> CjpmCommandLine

var ExecutionEnvironment.cjpmPatches: List<CjpmPatch>
    get() = putUserDataIfAbsent(CJPM_PATCHES, emptyList())
    set(value) = putUserData(CJPM_PATCHES, value)

abstract class CjpmRunStateBase(
    environment: ExecutionEnvironment,
    val configuration: CjpmCommandConfiguration,
    config: CjpmCommandConfiguration.CleanConfiguration.Ok
) : CommandLineState(environment) {
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


    var handler: OSProcessHandler? = null
    fun startProcess(processColors: Boolean): ProcessHandler {


        val targetEnvironment = configuration.targetEnvironment
        // 在本地目标的情况下回退到非目标实施


        if (targetEnvironment == null) {
            val commandLine = cjpm().toGeneralCommandLine(environment.project, prepareCommandLine())
            LOG.debug("Executing Cjpm command: `${commandLine.commandLineString}`")
            val handler = CjProcessHandler(commandLine, processColors)
            ProcessTerminatedListener.attach(handler) // shows exit code upon termination
            return handler

        }

        val remoteRunPatch: CjpmPatch = { commandLine ->
            if (configuration.buildTarget.isRemote && targetEnvironment.typeId == SSH_TARGET_TYPE_ID) {
                commandLine.prependArgument("--build-dir=${targetEnvironment.projectRootOnTarget}/build")
            } else {
                commandLine
            }.copy(emulateTerminal = false)
        }

        val commandLine = cjpm().toGeneralCommandLine(project, prepareCommandLine(remoteRunPatch))
        commandLine.exePath = targetEnvironment.languageRuntime?.cjpmPath.nullize(true) ?: "cjpm"
        return commandLine.startProcess(project, targetEnvironment, processColors, uploadExecutable = false)


    }


    override fun startProcess(): ProcessHandler = startProcess(processColors = true)


}

val TargetEnvironmentConfiguration.languageRuntime: CjLanguageRuntimeConfiguration?
    get() = runtimes.findByType()


