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

package org.cangnova.cangjie.cjpm.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.run.CangJieCommandExecutor
import org.cangnova.cangjie.run.CangJieCommandRunConfiguration
import org.cangnova.cangjie.run.CangJieRunConfigurationBase
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.nio.file.Files
import java.nio.file.Path

/**
 * CJPM command executor implementation.
 * Executes CJPM commands like run, build, test, etc.
 */
class CjpmCommandExecutor : CangJieCommandExecutor {

    override fun getBuildSystemId(): ProjectBuildSystemId {
        return CjpmBuildSystemId
    }

    override fun createCommandLine(configuration: CangJieRunConfigurationBase): GeneralCommandLine? {
        // Only handle CangJieCommandRunConfiguration
        if (configuration !is CangJieCommandRunConfiguration) {
            return null
        }

        val workingDir = configuration.workingDirectory ?: return null
        val project = configuration.project

        // Get SDK from toolchain
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk() ?: return null

        // Get CJPM executable path
        val cjpmPath = getCjpmExecutablePath(sdk.homePath) ?: return null
        if (!Files.isExecutable(cjpmPath)) {
            return null
        }

        // Build command line
        val commandLine = GeneralCommandLine()
            .withExePath(cjpmPath.toString())
            .withParameters(configuration.command)
            .withWorkDirectory(workingDir.toFile())

        // Add additional arguments if present
        configuration.args?.let { args ->
            if (args.isNotBlank()) {
                commandLine.addParameters(args.split("\\s+".toRegex()))
            }
        }

        // Add SDK environment variables
        commandLine.environment.putAll(sdk.getEnvironment())

        return commandLine
    }

    override fun validateConfiguration(configuration: CangJieRunConfigurationBase): String? {
        // Only validate CangJieCommandRunConfiguration
        if (configuration !is CangJieCommandRunConfiguration) {
            return "Invalid configuration type for CJPM executor"
        }

        val workingDir = configuration.workingDirectory
        if (workingDir == null || !workingDir.toFile().exists()) {
            return "Working directory does not exist"
        }

        // Check if SDK is available
        val project = configuration.project
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()

        if (sdk == null) {
            return "CangJie SDK is not configured"
        }

        if (!sdk.isValid) {
            return "CangJie SDK is invalid"
        }

        // Check if CJPM executable exists
        val cjpmPath = getCjpmExecutablePath(sdk.homePath)
        if (cjpmPath == null || !Files.isExecutable(cjpmPath)) {
            return "CJPM executable not found in SDK"
        }

        return null
    }

    override fun shouldAttachBuildAdapter(configuration: CangJieRunConfigurationBase): Boolean {
        // Only attach build adapter for commands that produce build artifacts
        if (configuration !is CangJieCommandRunConfiguration) {
            return false
        }

        // Commands that produce build artifacts
        val buildCommands = setOf("build", "run", "test")
        return configuration.command in buildCommands
    }

    /**
     * Get the CJPM executable path from SDK home path
     */
    private fun getCjpmExecutablePath(homePath: Path): Path? {
        val toolsPath = homePath.resolve("tools")
        val binPath = toolsPath.resolve("bin")
        val executableName = if (SystemInfo.isWindows) "cjpm.exe" else "cjpm"
        val cjpmPath = binPath.resolve(executableName)
        return if (Files.exists(cjpmPath)) cjpmPath else null
    }
}