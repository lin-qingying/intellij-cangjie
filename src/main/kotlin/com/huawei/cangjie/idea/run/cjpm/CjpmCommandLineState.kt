package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.util.SystemInfo


open class CjpmCommandLineState(environment: ExecutionEnvironment, val configuration: CjpmRunConfiguration) :
    CommandLineState(environment) {




    override fun startProcess(): OSProcessHandler {


        val sdk = CangJieSdkManager.getProjectSdk()
            ?: throw RuntimeConfigurationException(CangJieBundle.message("cangjie.sdk.please.configure"))

        //            获取工作目录为当前项目的根目录
        val project = environment.project


        val commandLine = GeneralCommandLine()

        val commandStrbf = StringBuilder()

        commandLine.setWorkDirectory(project.basePath)
        commandLine.charset = Charsets.UTF_8

        if (configuration.command == CjpmCommand.RUN) {
            if (SystemInfo.isWindows) {

                commandLine.exePath = "cmd"
                commandLine.addParameter("/c")
                commandStrbf.append(
                    (sdk.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath?.toWindowsPath() ?: ""
                )
            } else {
                commandLine.exePath = "/bin/bash"
                commandLine.addParameter("-c")
                commandStrbf.append((sdk.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath ?: "")
            }

            commandStrbf.append(" ")
            commandStrbf.append("build")
            commandStrbf.append(" ")
            commandStrbf.append("&&")
            commandStrbf.append(" ")
            commandStrbf.append("${project.basePath}${if (SystemInfo.isWindows) "\\build\\bin\\main.exe" else "/build/bin/main"}")

            commandLine.addParameter(commandStrbf.toString())
        } else {
            commandLine.exePath = (sdk.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath.toString()
            configuration.command?.command?.let { commandLine.addParameter(it) }
            //            TODO cjpm参数
            configuration.args?.let {
                if (it.isNotEmpty())
                    commandLine.addParameters(it)
            }
        }


//
//            commandLine.addParameter(if (configuration.runConfigEditor.command.equals("run")) "build" else configuration.runConfigEditor.command)
//
//            if (configuration.runConfigEditor.command.equals("run")) {
//                commandLine.addParameter("&&")
//                commandLine.addParameter("${project.basePath}${if (isWindows) "\\build\\bin\\main.exe" else "/build/bin/main"}")
//            }


        val handler = OSProcessHandler(commandLine)


        handler.startNotify()


        return handler

    }

    /**
     * 路径转为windows格式
     */
    private fun String.toWindowsPath(): String {
        return this.replace("/", "\\")
    }

//        override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
//            val processHandler = startProcess()
//            val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console
//            consoleView.attachToProcess(processHandler)
//
//            // 创建一个StringBuilder用于存储命令的输出
//            val output = StringBuilder()
//
//            // 在有新的输出可用时，将其添加到StringBuilder中
//            processHandler.addProcessListener(object : ProcessAdapter() {
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
//            return DefaultExecutionResult(consoleView, processHandler, *createActions(consoleView, processHandler))
//        }


}
