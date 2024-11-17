/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.run.cjpm


import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.model.CjpmProject
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.cjpm.project.model.impl.workingDirectory
import com.linqingying.cangjie.ide.notifications.CjNotifications
import com.linqingying.cangjie.ide.run.CjCommandConfiguration.Companion.emulateTerminalDefault
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

abstract class CjCommandLineBase {
    abstract val command: String
    abstract val workingDirectory: Path
    abstract val redirectInputFrom: File?
    abstract val additionalArguments: List<String>
    abstract val emulateTerminal: Boolean
//    private fun <T> runInner(
//        cjpmProject: CjpmProject ,
//
//        presentableName: String = command,
//        saveConfiguration: Boolean = true,
//        executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
//        doRun: (RunnerAndConfigurationSettings, Executor) -> Future<T>
//    ): Future<T> {
//        val project = cjpmProject.project
//
//        val runManager = RunManagerEx.getInstanceEx(project)
//        val configuration = createRunConfiguration(runManager, presentableName).apply {
//            if (saveConfiguration) {
//                runManager.setTemporaryConfiguration(this)
//            }
//        }
//
//        val runner = ProgramRunner.getRunner(executor.id, configuration.configuration)
//        val finalExecutor = if (runner == null) {
//            CjNotifications.pluginNotifications()
//                .createNotification(
//                    CangJieBundle.message(
//                        "notification.0.action.is.not.available.for.1.command",
//                        executor.actionName,
//                        "$executableName $command"
//                    ), NotificationType.WARNING
//                )
//                .notify(project)
//            DefaultRunExecutor.getRunExecutorInstance()
//        } else {
//            executor
//        }
//
//        return doRun(configuration, finalExecutor)
//    }
//


    private fun <T> runInner(
        cjpmProject: CjpmProject,
        presentableName: String = command,
        saveConfiguration: Boolean = true,
        executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
        doRun: (RunnerAndConfigurationSettings, Executor) -> Future<T>
    ): Future<T> {
        val project = cjpmProject.project
        val configurationName = when {
            project.cjpmProjects.allProjects.size > 1 -> "$presentableName "
            else -> presentableName
        }
        val runManager = RunManagerEx.getInstanceEx(project)
        val configuration = createRunConfiguration(runManager, configurationName).apply {
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

    fun run(
        cjpmProject: CjpmProject,
        presentableName: String = command,
        saveConfiguration: Boolean = true,
        executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
    ) {
        runInner(cjpmProject, presentableName, saveConfiguration, executor) { configuration, finalExecutor ->
            ProgramRunnerUtil.executeConfiguration(configuration, finalExecutor)
            CompletableFuture.completedFuture(true)
        }
    }

    protected abstract val executableName: String

    protected abstract fun createRunConfiguration(
        runManager: RunManagerEx,
        name: String? = null
    ): RunnerAndConfigurationSettings

    fun runAsync(
        cjpmProject: CjpmProject,

        presentableName: String = command,
        saveConfiguration: Boolean = true,
        executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
    ): Future<Boolean> =
        runInner(cjpmProject, presentableName, saveConfiguration, executor) { configuration, finalExecutor ->
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

    fun toGeneralCommandLine(): GeneralCommandLine {

//TODO 可能会有问题
        return GeneralCommandLine().apply {
            exePath = command
            workDirectory = workingDirectory.toFile()

            addParameters(additionalArguments)
        }
    }


}

data class CjpmCommandLine(

    override val command: String, // Can't be `enum` because of custom subcommands

    override val workingDirectory: Path,
    override val additionalArguments: List<String> = emptyList(),
    override val redirectInputFrom: File? = null,

    val toolchain: String? = null,
    override val emulateTerminal: Boolean = emulateTerminalDefault,

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


    companion object {

        fun forProject(
            cjpmProject: CjpmProject,


            command: String,
            additionalArguments: List<String> = emptyList(),
            emulateTerminal: Boolean = emulateTerminalDefault,

            environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
        ): CjpmCommandLine = CjpmCommandLine(
            command,

            workingDirectory = cjpmProject.workingDirectory,
            additionalArguments = additionalArguments,
            emulateTerminal = emulateTerminal,

            environmentVariables = environmentVariables
        )

//        fun forProject(
//
//
//            command: String,
//
//            additionalArguments: List<String> = emptyList(),
//            toolchain: String? = null,
//
//
//            environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
//        ): CjpmCommandLine = CjpmCommandLine(
//            command,
//
//            workingDirectory = Paths.get(CangJieProjectManager.getCurrentProject().basePath),
//            additionalArguments = additionalArguments,
//            toolchain = toolchain,
//
//
//            environmentVariables = environmentVariables
//        )
    }

}

fun RunManager.createCjpmCommandRunConfiguration(
    cjpmCommandLine: CjpmCommandLine,
    name: String? = null
): RunnerAndConfigurationSettings {
    val runnerAndConfigurationSettings = createConfiguration(
        name ?: cjpmCommandLine.command,
        CjpmCommandConfigurationType.instance.factory
    )
    val configuration = runnerAndConfigurationSettings.configuration as CjpmCommandConfiguration
    configuration.setFromCmd(cjpmCommandLine)
    return runnerAndConfigurationSettings
}
