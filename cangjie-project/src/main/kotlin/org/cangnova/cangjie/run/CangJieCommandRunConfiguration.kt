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

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.jdom.Element

/**
 * Run configuration for executing CangJie build commands.
 * Used for running commands like build, test, clean, etc.
 */
class CangJieCommandRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : CangJieRunConfigurationBase(project, factory, name) {

    /**
     * The command to execute (e.g., "run", "build", "test")
     */
    var command: String = "run"

    /**
     * Additional arguments for the command
     */
    var args: String? = null

    override fun createRunState(
        environment: ExecutionEnvironment,
        commandExecutor: CangJieCommandExecutor
    ) = CangJieCommandRunState(environment, this, commandExecutor)

    override fun getConfigurationEditor() = CangJieRunConfigurationEditor(project)

    override fun suggestedName(): String {
        return "$command ${project.name}"
    }

    override fun checkConfiguration() {
        super.checkConfiguration()

        if (command.isBlank()) {
            throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.command.empty"))
        }

        val systemId = org.cangnova.cangjie.project.service.CjProjectBuildSystemService.getInstance().getBuildSystem()
            ?: throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.cannot.detect.build.system"))

        val executor = CangJieCommandExecutor.findExecutor()
            ?: throw RuntimeConfigurationError(
                CjProjectBundle.message(
                    "run.configuration.error.no.command.executor",
                    systemId.id
                )
            )

        // Validate using subsystem executor
        val error = executor.validateConfiguration(this)
        if (error != null) {
            throw RuntimeConfigurationError(error)
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString("command", command)
        args?.let { element.writeString("args", it) }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("command")?.let { command = it }
        element.readString("args")?.let { args = it }
    }
}