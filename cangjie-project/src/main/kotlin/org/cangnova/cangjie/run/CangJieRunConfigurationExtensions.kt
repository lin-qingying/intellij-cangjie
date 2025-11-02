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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/**
 * Extension point for providing UI editors for run configurations.
 * Subsystems (e.g., CJPM, CJC) implement this to provide custom UI.
 */
interface CangJieRunConfigurationEditorProvider {
    /**
     * Returns the ID of the build system this provider supports (e.g., "cjpm", "cjc")
     */
    fun getBuildSystemId(): String

    /**
     * Creates a settings editor for the given configuration
     */
    fun createEditor(project: Project, configuration: AbstractCangJieRunConfiguration): SettingsEditor<AbstractCangJieRunConfiguration>

    /**
     * Returns the priority of this provider (higher = preferred)
     */
    fun getPriority(): Int = 0

    companion object {
        val EP_NAME = ExtensionPointName<CangJieRunConfigurationEditorProvider>(
            "org.cangnova.cangjie.run.editorProvider"
        )

        /**
         * Finds the appropriate editor provider for the given build system
         */
        fun findProvider(buildSystemId: String): CangJieRunConfigurationEditorProvider? {
            return EP_NAME.extensionList
                .filter { it.getBuildSystemId() == buildSystemId }
                .maxByOrNull { it.getPriority() }
        }
    }
}

/**
 * Extension point for executing commands.
 * Subsystems implement this to provide command execution logic.
 */
interface CangJieCommandExecutor {
    /**
     * Returns the ID of the build system this executor supports (e.g., "cjpm", "cjc")
     */
    fun getBuildSystemId(): String

    /**
     * Creates a command line for execution
     * @param configuration The run configuration
     * @return GeneralCommandLine ready to execute, or null if cannot execute
     */
    fun createCommandLine(configuration: AbstractCangJieRunConfiguration): GeneralCommandLine?

    /**
     * Validates the configuration before execution
     * @return Error message if invalid, null if valid
     */
    fun validateConfiguration(configuration: AbstractCangJieRunConfiguration): String? = null

    /**
     * Returns the priority of this executor (higher = preferred)
     */
    fun getPriority(): Int = 0

    companion object {
        val EP_NAME = ExtensionPointName<CangJieCommandExecutor>(
            "org.cangnova.cangjie.run.commandExecutor"
        )

        /**
         * Finds the appropriate executor for the given build system
         */
        fun findExecutor(buildSystemId: String): CangJieCommandExecutor? {
            return EP_NAME.extensionList
                .filter { it.getBuildSystemId() == buildSystemId }
                .maxByOrNull { it.getPriority() }
        }
    }
}

/**
 * Extension point for detecting build system type from project.
 * Subsystems implement this to identify their projects.
 */
interface CangJieBuildSystemDetector {
    /**
     * Returns the ID of the build system (e.g., "cjpm", "cjc")
     */
    fun getBuildSystemId(): String

    /**
     * Detects if this build system is used in the project
     * @return true if this build system is detected
     */
    fun detectBuildSystem(project: Project): Boolean

    /**
     * Returns the priority of this detector (higher = preferred)
     */
    fun getPriority(): Int = 0

    companion object {
        val EP_NAME = ExtensionPointName<CangJieBuildSystemDetector>(
            "org.cangnova.cangjie.run.buildSystemDetector"
        )

        /**
         * Detects the build system used in the project
         * @return Build system ID, or null if none detected
         */
        fun detectBuildSystem(project: Project): String? {
            return EP_NAME.extensionList
                .filter { it.detectBuildSystem(project) }
                .maxByOrNull { it.getPriority() }
                ?.getBuildSystemId()
        }
    }
}