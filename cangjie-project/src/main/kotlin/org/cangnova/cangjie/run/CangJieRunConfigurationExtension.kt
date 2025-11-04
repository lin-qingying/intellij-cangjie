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

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Extension point for CangJie run configurations.
 * Allows plugins to extend run configuration behavior.
 */
abstract class CangJieRunConfigurationExtension :
    RunConfigurationExtensionBase<CangJieRunConfigurationBase>() {

    companion object {
        val EP_NAME = ExtensionPointName<CangJieRunConfigurationExtension>(
            "org.cangnova.cangjie.run.runConfigurationExtension"
        )
    }

    /**
     * Patch the command line before execution.
     * This is called before the process is started.
     */
    open fun patchCommandLine(
        configuration: CangJieRunConfigurationBase,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    ) {
    }

    /**
     * Attach to the process after it starts.
     * This is called after the process handler is created.
     */
    open fun attachToProcess(
        configuration: CangJieRunConfigurationBase,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    ) {
    }

    /**
     * Patch the command line state before execution.
     * This is called before the command line is created.
     */
    open fun patchCommandLineState(
        configuration: CangJieRunConfigurationBase,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {
    }

    /**
     * Check if this extension is applicable for the given configuration
     */
    override fun isApplicableFor(configuration: CangJieRunConfigurationBase): Boolean = true

    /**
     * Check if this extension is enabled for the given configuration
     */
    override fun isEnabledFor(
        applicableConfiguration: CangJieRunConfigurationBase,
        runnerSettings: RunnerSettings?
    ): Boolean = true
}