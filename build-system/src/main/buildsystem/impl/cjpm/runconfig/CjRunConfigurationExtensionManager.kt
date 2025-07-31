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

package cn.cangnova.cangjie.buildsystem.impl.cjpm.runconfig

import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.ConfigurationExtensionContext
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.service

class CjRunConfigurationExtensionManager :
    RunConfigurationExtensionsManager<CjpmCommandConfiguration, CjpmCommandConfigurationExtension>(
        CjpmCommandConfigurationExtension.EP_NAME
    ) {

    fun patchCommandLine(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        cmdLine: GeneralCommandLine,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions1(configuration, environment.runnerSettings) {
            it.patchCommandLine(configuration, environment, cmdLine, context)
        }
    }

    fun attachExtensionsToProcess(
        configuration: CjpmCommandConfiguration,
        handler: ProcessHandler,
        environment: ExecutionEnvironment,
        context: ConfigurationExtensionContext
    ) {
        processEnabledExtensions1(configuration, environment.runnerSettings) {
            it.attachToProcess(configuration, handler, environment, context)
        }
    }


    fun patchCommandLineState(
        configuration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment,
        state: CommandLineState,
        context: ConfigurationExtensionContext
    ) {

        processEnabledExtensions1(configuration, environment.runnerSettings) {
            it.patchCommandLineState(configuration, environment, state, context)
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): CjRunConfigurationExtensionManager = service()

    }
}

private inline fun processEnabledExtensions1(
    configuration: CjpmCommandConfiguration,
    runnerSettings: RunnerSettings?,
    handler: (CjpmCommandConfigurationExtension) -> Unit
) {
    for (extension in CjpmCommandConfigurationExtension.EP_NAME.extensionList.asSequence()) {
        if (extension.isApplicableFor(configuration) && extension.isEnabledFor(configuration, runnerSettings)) {
            handler(extension)
        }
    }
}