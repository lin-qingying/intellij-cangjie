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

package org.cangnova.cangjie.run

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import org.cangnova.cangjie.process.CjProcessHandler
import org.cangnova.cangjie.run.target.startProcess

/**
 * Run state for executing CangJie programs.
 * Handles the execution of compiled CangJie modules.
 */
class CangJieProgramRunState(
    environment: ExecutionEnvironment,
    configuration: CangJieProgramRunConfiguration
) : CangJieRunState<CangJieProgramRunConfiguration>(environment, configuration) {

    override fun startProcess(): ProcessHandler {
        return startProcess(processColors = true)
    }

    fun startProcess(processColors: Boolean): ProcessHandler {
        // Save all documents before executing program to ensure we're running the latest code
        // Must be done on EDT (Event Dispatch Thread)
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
            com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveAllDocuments()
        }

        val commandLine = createCommandLine()
            ?: throw IllegalStateException("Failed to create command line for program execution")

        LOG.debug("Executing program: `${commandLine.commandLineString}`")

        // Support for remote target environments
        val processHandler = if (configuration.targetEnvironment != null) {
            commandLine.startProcess(
                project = environment.project,
                config = configuration.targetEnvironment,
                processColors = processColors,
                uploadExecutable = true  // Upload executable for program runs
            )
        } else {
            // Local execution
            val handler = CjProcessHandler(commandLine, processColors = processColors)
            ProcessTerminatedListener.attach(handler)
            handler
        }

        // Attach build adapter for Build Tool Window integration
        CangJieBuildManager.attachBuildAdapter(
            project = environment.project,
            taskName = "Run ${configuration.name}",
            workingDirectory = configuration.workingDirectory ?: java.nio.file.Paths.get(
                environment.project.basePath ?: "."
            ),
            environment = environment,
            processHandler = processHandler
        )

        return processHandler
    }

    private fun createCommandLine(): GeneralCommandLine? {
        val module = configuration.getCjModule() ?: return null
        val workingDir = configuration.workingDirectory ?: return null

        val commandLine = GeneralCommandLine()
        commandLine.workDirectory = workingDir.toFile()

        // Set environment variables
        commandLine.environment.putAll(configuration.env.envs)
        if (configuration.env.isPassParentEnvs) {
            commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        }

        // TODO: Build the actual command to run the program
        // This will depend on the build system (cjpm, cjc, etc.)
        // For now, this is a placeholder that needs to be implemented
        // based on the specific build system's requirements

        return commandLine
    }

    companion object {
        private val LOG: Logger = logger<CangJieProgramRunState>()
    }
}