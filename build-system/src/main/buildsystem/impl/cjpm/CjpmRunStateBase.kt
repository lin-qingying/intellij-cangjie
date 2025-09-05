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

package org.cangnova.cangjie.buildsystem.impl.cjpm


import org.cangnova.cangjie.cjpm.project.model.CjpmProject
import org.cangnova.cangjie.toolchain.CjToolchainBase
import org.cangnova.cangjie.toolchain.cjpm
import org.cangnova.cangjie.toolchain.tools.Cjpm

import org.cangnova.cangjie.ide.run.cjpm.runconfig.CjLanguageRuntimeConfiguration
import org.cangnova.cangjie.ide.run.cjpm.runconfig.CjProcessHandler
import org.cangnova.cangjie.ide.run.cjpm.runconfig.startProcess
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

    fun cjpm(): Cjpm = toolchain.    cjpm()


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


