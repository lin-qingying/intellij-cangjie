package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.notifications.CjNotifications
import com.huawei.cangjie.idea.project.CangJieProjectManager

import com.intellij.execution.*
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationType
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

abstract class CjCommandLineBase {
    abstract val command: CjpmCommand
    abstract val workingDirectory: Path
    abstract val redirectInputFrom: File?
    abstract val additionalArguments: List<String>


    protected abstract val executableName: String

    protected abstract fun createRunConfiguration(
        runManager: RunManagerEx,
        name: String? = null
    ): RunnerAndConfigurationSettings

    fun runAsync(
        presentableName: String = command.command,
        saveConfiguration: Boolean = true,
        executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
    ): Future<Boolean> =
        runInner(presentableName, saveConfiguration, executor) { configuration, finalExecutor ->
            val environment = ExecutionEnvironmentBuilder.create(finalExecutor, configuration).build()
            val promise = CompletableFuture<Boolean>()
            ProgramRunnerUtil.executeConfigurationAsync(environment, true, true) { descriptor ->
                descriptor.processHandler?.addProcessListener(object : ProcessListener {
                    override fun processTerminated(event: ProcessEvent) {
                        promise.complete(event.exitCode == 0)
                    }
                })
            }
            promise
        }

    private fun <T> runInner(

        presentableName: String = command.command,
        saveConfiguration: Boolean = true,
        executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
        doRun: (RunnerAndConfigurationSettings, Executor) -> Future<T>
    ): Future<T> {
        val project = CangJieProjectManager.getCurrentProject()

        val runManager = RunManagerEx.getInstanceEx(project)
        val configuration = createRunConfiguration(runManager, presentableName).apply {
            if (saveConfiguration) {
                runManager.setTemporaryConfiguration(this)
            }
        }

        val runner = ProgramRunner.getRunner(executor.id, configuration.configuration)
        val finalExecutor = if (runner == null) {
            CjNotifications.pluginNotifications()
                .createNotification(
                    CangJieBundle.message(
                        "notification.0.action.is.not.available.for.1.command",
                              executor.actionName,
                        "$executableName $command"
                    ), NotificationType.WARNING
                )
                .notify(project)
            DefaultRunExecutor.getRunExecutorInstance()
        } else {
            executor
        }

        return doRun(configuration, finalExecutor)
    }


}

data class CjpmCommandLine(
    override val command: CjpmCommand,
    override val workingDirectory: Path,
    override val additionalArguments: List<String> = emptyList(),
    override val redirectInputFrom: File? = null,


    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
    val requiredFeatures: Boolean = true,
    val allFeatures: Boolean = false,
    val withSudo: Boolean = false
) : CjCommandLineBase() {
    override val executableName: String
        get() = "cjpm"

    override fun createRunConfiguration(runManager: RunManagerEx, name: String?): RunnerAndConfigurationSettings =
        runManager.createCjpmCommandRunConfiguration(this, name)

    fun splitOnDoubleDash(arguments: List<String>): Pair<List<String>, List<String>> {
        val idx = arguments.indexOf("--")

        if (idx == -1) return arguments to emptyList()
        return arguments.take(idx) to arguments.drop(idx + 1)
    }

    fun splitOnDoubleDash(): Pair<List<String>, List<String>> =
        splitOnDoubleDash(additionalArguments)

    fun prependArgument(arg: String): CjpmCommandLine =
        copy(additionalArguments = listOf(arg) + additionalArguments)


    fun toGeneralCommandLine(): GeneralCommandLine {

    }
    companion object {
        fun forProject(

            command: CjpmCommand,
            additionalArguments: List<String> = emptyList(),



            environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
        ): CjpmCommandLine = CjpmCommandLine(
            command,
            workingDirectory = Paths.get(CangJieProjectManager.getCurrentProject().basePath),
            additionalArguments = additionalArguments,


            environmentVariables = environmentVariables
        )
    }

}

fun RunManager.createCjpmCommandRunConfiguration(
    cjpmCommandLine: CjpmCommandLine,
    name: String? = null
): RunnerAndConfigurationSettings {
    val runnerAndConfigurationSettings = createConfiguration(
        name ?: cjpmCommandLine.command.command,
        CjpmCommandConfigurationType.instance.factory
    )
    val configuration = runnerAndConfigurationSettings.configuration as CjpmCommandConfiguration
    configuration.setFromCmd(cjpmCommandLine)
    return runnerAndConfigurationSettings
}
