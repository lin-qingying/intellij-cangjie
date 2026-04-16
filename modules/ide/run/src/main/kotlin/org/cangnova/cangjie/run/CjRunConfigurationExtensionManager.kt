/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/**
 * Manager for CangJie run configuration extensions.
 * Allows other plugins or modules to extend run configuration behavior.
 */
@Service(Service.Level.APP)
class CjRunConfigurationExtensionManager :
    RunConfigurationExtensionsManager<CangJieRunConfigurationBase, CangJieRunConfigurationExtension>(
        CangJieRunConfigurationExtension.EP_NAME
    ) {

    /**
     * Patch the command line before execution
     */
    fun patchCommandLine(
        configuration: CangJieRunConfigurationBase,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment.runnerSettings) {
            it.patchCommandLine(configuration, environment, cmdLine, context)
        }
    }

    /**
     * Attach extensions to the process after it starts
     */
    fun attachExtensionsToProcess(
        configuration: CangJieRunConfigurationBase,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment.runnerSettings) {
            it.attachToProcess(configuration, handler, environment, context)
        }
    }

    /**
     * Patch the command line state before execution
     */
    fun patchCommandLineState(
        configuration: CangJieRunConfigurationBase,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions(configuration, environment.runnerSettings) {
            it.patchCommandLineState(configuration, environment, state, context)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): CjRunConfigurationExtensionManager = service()
    }
}

/**
 * Process all enabled extensions for a configuration
 */
private inline fun processEnabledExtensions(
    configuration: CangJieRunConfigurationBase,
    runnerSettings: RunnerSettings?,
    handler: (CangJieRunConfigurationExtension) -> Unit
) {
    for (extension in CangJieRunConfigurationExtension.EP_NAME.extensionList) {
        if (extension.isApplicableFor(configuration) && extension.isEnabledFor(configuration, runnerSettings)) {
            handler(extension)
        }
    }
}