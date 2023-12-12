package com.huawei.cangjie.idea.run.cjpm

import com.intellij.execution.configurations.CommandLineState

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.project.CangJieProjectManager


import com.huawei.cangjie.idea.run.cjpm.runconfig.CjLanguageRuntimeConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessHandler
import com.huawei.cangjie.idea.run.cjpm.runconfig.startProcess
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.text.nullize
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val CARGO_PATCHES: Key<List<CjpmPatch>> = Key.create("CJPMPATCHES")

typealias CjpmPatch = (CjpmCommandLine) -> CjpmCommandLine

var ExecutionEnvironment.cjpmPatches: List<CjpmPatch>
    get() = putUserDataIfAbsent(CARGO_PATCHES, emptyList())
    set(value) = putUserData(CARGO_PATCHES, value)

abstract class CjpmRunStateBase(
    environment: ExecutionEnvironment,
    val configuration: CjpmCommandConfiguration,
    config: CjpmCommandConfiguration.CleanConfiguration.Ok
) : CommandLineState(environment) {
    val project: Project = environment.project
    val commandLine: CjpmCommandLine = config.cmd
    val executorId: String = environment.executor.id
    protected val commandLinePatches: MutableList<CjpmPatch> = mutableListOf()


//    val toolchain: CjToolchainBase = config.toolchain

    init {
        commandLinePatches.addAll(environment.cjpmPatches)
    }


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
        // Fallback to non-target implementation in case of local target
        if (targetEnvironment == null) {
//            val commandLine = cjpm().toColoredCommandLine(environment.project, prepareCommandLine())
            val params = ParametersListUtil.parse(commandLine.command.executeCommand)
//            如果sdkversion小于0.45.2
//0.39.8 (476fdc21baf0 2023-09-23) 提取出来的版本号是0.39.8
            val sdkVersion = CangJieSdkManager.sdkVersion.split(" ")[0]



            val commandLine =
                if (sdkVersion < "0.45.2" && commandLine.command == CjpmCommand.RUN) {
                GeneralCommandLine().apply {
//                  执行 build/bin/main.exe
                    exePath = if (SystemInfo.isWindows) {
                        CangJieProjectManager.getCurrentProject().basePath + "\\build\\bin\\main.exe"
                    } else {
                        CangJieProjectManager.getCurrentProject().basePath + "/build/bin/main"
                    }
                    workDirectory = CangJieProjectManager.getCurrentProject().basePath?.let {
                        Paths.get(it).toFile()
                    }
                }
            } else {
                GeneralCommandLine().apply {
                    exePath =
                        (CangJieSdkManager.getProjectSdk()?.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath.toString()
                    addParameters(params)
                    workDirectory =
                        CangJieProjectManager.getCurrentProject().basePath?.let { Paths.get(it).toFile() }
                }
            }
            LOG.debug("Executing Cjpm command: `${commandLine.commandLineString}`")
            val handler = CjProcessHandler(commandLine, processColors)
            ProcessTerminatedListener.attach(handler) // shows exit code upon termination
            return handler
        }

        val remoteRunPatch: CjpmPatch = { commandLine ->
            if (configuration.buildTarget.isRemote && targetEnvironment.typeId == SSH_TARGET_TYPE_ID) {
                commandLine.prependArgument("--target-dir=${targetEnvironment.projectRootOnTarget}/target")
            } else {
                commandLine
            }.copy()
        }

//        val commandLine = cjpm().toColoredCommandLine(project, prepareCommandLine(remoteRunPatch))
        val commandLine = GeneralCommandLine().apply {
            addParameter("build")

        }
        commandLine.exePath = targetEnvironment.languageRuntime?.cjpmPath.nullize(true) ?: "cjpm"
        return commandLine.startProcess(project, targetEnvironment, processColors, uploadExecutable = false)
    }

    //        fun cjpm(): Cjpm = toolchain.cjpmOrWrapper(workingDirectory)
    override fun startProcess(): ProcessHandler = startProcess(processColors = true)

    /**
     * 路径转为windows格式
     */
    private fun String.toWindowsPath(): String {
        return this.replace("/", "\\")
    }

//        override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//            val shellProcessHandler = startProcess()
//            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console
//            consoleView.attachToProcess(shellProcessHandler)
//
//            // 创建一个StringBuilder用于存储命令的输出
//            val output = StringBuilder()
//
//            // 在有新的输出可用时，将其添加到StringBuilder中
//            shellProcessHandler.addProcessListener(object : ProcessAdapter() {
//                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
//                    output.append(event.text)
//                }
//
//                override fun processTerminated(event: ProcessEvent) {
//                    // 在命令结束后，将命令的输出打印到控制台
//                    consoleView.print("Command Output:\n$output\n", ConsoleViewContentType.NORMAL_OUTPUT)
//                }
//            })
//
//            return DefaultExecutionResult(consoleView, shellProcessHandler, *createActions(consoleView, shellProcessHandler))
//        }


}

val TargetEnvironmentConfiguration.languageRuntime: CjLanguageRuntimeConfiguration?
    get() = runtimes.findByType()


object CjpmConstants {

    const val MANIFEST_FILE = "module.json"

    object ProjectLayout {
        val sources = listOf("src", "examples")
        val tests = listOf("tests", "benches")
        const val target = "build"
    }
}
