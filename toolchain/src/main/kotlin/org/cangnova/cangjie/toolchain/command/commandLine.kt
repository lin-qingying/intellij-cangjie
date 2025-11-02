/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.toolchain.command

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.systemIndependentPath
import org.cangnova.cangjie.result.*
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import java.io.File
import java.nio.file.Path

data class ToolchainCommandLine(
    val sdkId: String,
    val executable: String,
    val command: String,

    val workingDirectory: Path,
    val additionalArguments: List<String> = emptyList(),


    val emulateTerminal: Boolean = false,

    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
    val requiredFeatures: Boolean = true,
    val allFeatures: Boolean = false,
    val withSudo: Boolean = false
) {

    val sdk = CjSdkRegistry.getInstance().getSdk(sdkId) ?: error("CangJie SDK not found: $sdkId")
    fun splitOnDoubleDash(arguments: List<String>): Pair<List<String>, List<String>> {
        val idx = arguments.indexOf("--")

        if (idx == -1) return arguments to emptyList()
        return arguments.take(idx) to arguments.drop(idx + 1)
    }

    fun splitOnDoubleDash(): Pair<List<String>, List<String>> =
        splitOnDoubleDash(additionalArguments)

    fun patchArgs(project: Project): ToolchainCommandLine {

        val (pre, post) = splitOnDoubleDash()
            .let { (pre, post) -> pre.toMutableList() to post.toMutableList() }

//        TODO 对参数进行处理(添加或者删除)

        return copy(additionalArguments = if (post.isEmpty()) pre else pre + "--" + post)

    }

    fun createGeneralCommandLine(
        executable: Path,
        workingDirectory: Path,
        redirectInputFrom: File?,

        environmentVariables: EnvironmentVariablesData,
        parameters: List<String>,
        emulateTerminal: Boolean,
        withSudo: Boolean,
        patchToRemote: Boolean = true,

        ): GeneralCommandLine {

        val env =

            if (environmentVariables.envs.isEmpty()) {

                EnvironmentVariablesData.create(sdk.getEnvironment(), true)

            } else {
                environmentVariables
            }

        var commandLine = GeneralCommandLine(executable, withSudo)
            .withWorkDirectory(workingDirectory)
            .withInput(redirectInputFrom)

            .withParameters(parameters)
            .withCharset(Charsets.UTF_8)
            .withRedirectErrorStream(true)

        env.configureCommandLine(commandLine, true)
        if (emulateTerminal) {
            commandLine = PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false)
        }

        return commandLine

    }

    @Suppress("FunctionName", "UnstableApiUsage")
    fun GeneralCommandLine(path: Path, withSudo: Boolean = false, vararg args: String) =
        object : GeneralCommandLine(path.systemIndependentPath, *args) {
//        override fun createProcess(): Process = if (withSudo) {
//            ElevationService.getInstance().createProcess(this)
//        } else {
//            super.createProcess()
//        }
        }

    fun toGeneralCommandLine(project: Project): GeneralCommandLine =
        with(patchArgs(project)) {
            val parameters = buildList {

                add(command)
                addAll(additionalArguments)
            }


            createGeneralCommandLine(
                sdk.homePath.resolve(executable),
                workingDirectory,
                null,

                environmentVariables,
                parameters,
                emulateTerminal,

                false,

                )


        }

    fun CapturingProcessHandler.runProcess(
        indicator: ProgressIndicator?, timeoutInMilliseconds: Int? = null
    ): ProcessOutput {
        return when {
            indicator != null && timeoutInMilliseconds != null -> runProcessWithProgressIndicator(
                indicator,
                timeoutInMilliseconds
            )

            indicator != null -> runProcessWithProgressIndicator(indicator)
            timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
            else -> runProcess()
        }

    }

    fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
        return runProcess(ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds)

    }

    fun GeneralCommandLine.execute(
        owner: Disposable,
        stdIn: ByteArray? = null,
        runner: CapturingProcessHandler.() -> ProcessOutput = { runProcessWithGlobalProgress(timeoutInMilliseconds = null) },
        listener: ProcessListener? = null
    ): CjProcessResult<ProcessOutput> {


        val handler = CjCapturingProcessHandler.startProcess(this) // The OS process is started here
            .unwrapOrElse {

                return CjResult.Err(CjProcessExecutionException.Start(commandLineString, it))
            }

        val cjpmKiller = Disposable {

            if (!handler.isProcessTerminated) {
                handler.process.destroyForcibly() // Send SIGKILL
                handler.destroyProcess()
            }
        }

        val alreadyDisposed = runReadAction {
            if (Disposer.isDisposed(owner)) {
                true
            } else {
                Disposer.register(owner, cjpmKiller)
                false
            }
        }

        if (alreadyDisposed) {
            Disposer.dispose(cjpmKiller) // Kill the process

            val output = ProcessOutput().apply { setCancelled() }
            return CjResult.Err(
                CjProcessExecutionException.Canceled(
                    commandLineString,
                    output,
                    "Command failed to start"
                )
            )
        }

        listener?.let { handler.addProcessListener(it) }

        val output = try {
            if (stdIn != null) {
                handler.processInput.use { it.write(stdIn) }
            }

            handler.runner()
        } finally {
            Disposer.dispose(cjpmKiller)
        }

        return when {
            output.isCancelled -> CjResult.Err(CjProcessExecutionException.Canceled(commandLineString, output))
            output.isTimeout -> CjResult.Err(CjProcessExecutionException.Timeout(commandLineString, output))
            output.exitCode != 0 -> CjResult.Err(CjProcessExecutionException.ProcessAborted(commandLineString, output))
            else -> CjResult.Ok(output)
        }
    }

    fun execute(
        project: Project,
        owner: Disposable = project,
        stdIn: ByteArray? = null,
        listener: ProcessListener? = null
    ): CjProcessResult<ProcessOutput> {
        return copy(emulateTerminal = false).toGeneralCommandLine(project).execute(owner, stdIn, listener = listener)
    }
}

fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)
