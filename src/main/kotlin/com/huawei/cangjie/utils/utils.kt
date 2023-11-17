package com.huawei.cangjie.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import java.util.*

/**
 * 判断系统是否为windows
 */
fun isWindows(): Boolean {
    val os = System.getProperty("os.name")
    return os.lowercase(Locale.getDefault()).startsWith("win")
}

/**
 * 在intellij终端执行命令
 */

fun executeCommand(command: String) {
    val commandLine = GeneralCommandLine(command.split(" "))
    val processHandler = OSProcessHandler(commandLine)

    processHandler.addProcessListener(object : ProcessListener {
        override fun startNotified(event: ProcessEvent) {
            println("Command started: $command")
        }

        override fun processTerminated(event: ProcessEvent) {
            println("Command finished with exit code: ${event.exitCode}")
        }

        override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}

        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
            println("Output: ${event.text}")
        }
    })

    ApplicationManager.getApplication().executeOnPooledThread {
        processHandler.startNotify()
    }
}

//fun runCustomTask( ) {
//    val project = ProjectManager.getInstance().openProjects[0]
//    val runManager = RunManager.getInstance(project)
//    val configurationFactory = CjpmRunConfigurationType().configurationFactories[0]
//    val runConfiguration = configurationFactory.createTemplateConfiguration(project)
//    val settings = runManager.createConfiguration(runConfiguration, configurationFactory)
//
//    runManager.addConfiguration(settings)
//    runManager.selectedConfiguration = settings
//
//    ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
//}
