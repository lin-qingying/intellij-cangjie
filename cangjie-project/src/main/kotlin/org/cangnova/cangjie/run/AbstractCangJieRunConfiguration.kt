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

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.CjProjectBundle
import org.jdom.Element

/**
 * Abstract run configuration implementation.
 * Uses extension points to delegate UI and execution to subsystems.
 */
abstract class AbstractCangJieRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : CangJieCommandConfiguration(project, name, factory) {

    abstract override var command: String

    /**
     * Build system ID (e.g., "cjpm", "cjc")
     * Auto-detected or manually set
     */
    var buildSystemId: String? = null

    /**
     * Additional arguments for the command
     */
    var args: String? = null

    /**
     * Environment variables
     */
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        // Auto-detect build system if not set
        if (buildSystemId == null) {
            buildSystemId = CangJieBuildSystemDetector.detectBuildSystem(project)
        }

        val systemId = buildSystemId ?: return null
        val commandExecutor = CangJieCommandExecutor.findExecutor(systemId) ?: return null

        return CangJieRunState(environment, this, commandExecutor)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        // For program configurations, use specialized editor
        if (this is CangJieProgramRunConfiguration) {
            return CangJieProgramRunConfigurationEditor(project)
        }

        // For command configurations, use existing logic
        val group = SettingsEditorGroup<AbstractCangJieRunConfiguration>()

        // Auto-detect build system if not set
        if (buildSystemId == null) {
            buildSystemId = CangJieBuildSystemDetector.detectBuildSystem(project)
        }

        // Try to get subsystem-specific editor
        val systemId = buildSystemId
        if (systemId != null) {
            val editorProvider = CangJieRunConfigurationEditorProvider.findProvider(systemId)
            if (editorProvider != null) {
                group.addEditor("Configuration", editorProvider.createEditor(project, this))
                return group
            }
        }

        // Fallback to default editor
        group.addEditor("Configuration", CangJieRunConfigurationDefaultEditor(project))
        return group
    }

    override fun checkConfiguration() {
        super.checkConfiguration()

        // Auto-detect build system if not set
        if (buildSystemId == null) {
            buildSystemId = CangJieBuildSystemDetector.detectBuildSystem(project)
        }

        val systemId = buildSystemId
            ?: throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.cannot.detect.build.system"))

        val executor = CangJieCommandExecutor.findExecutor(systemId)
            ?: throw RuntimeConfigurationError(CjProjectBundle.message("run.configuration.error.no.command.executor", systemId))

        // Validate using subsystem executor
        val error = executor.validateConfiguration(this)
        if (error != null) {
            throw RuntimeConfigurationError(error)
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        buildSystemId?.let { element.writeString("buildSystemId", it) }
        args?.let { element.writeString("args", it) }
        env.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.readString("buildSystemId")?.let { buildSystemId = it }
        element.readString("args")?.let { args = it }
        env = EnvironmentVariablesData.readExternal(element)
    }
}