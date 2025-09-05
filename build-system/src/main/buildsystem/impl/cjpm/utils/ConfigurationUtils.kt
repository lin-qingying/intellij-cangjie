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

package org.cangnova.cangjie.buildsystem.impl.cjpm.utils

import org.cangnova.cangjie.ide.run.CjCommandConfiguration
import org.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfigurationType
import org.cangnova.cangjie.ide.run.cjpm.CjpmRunConfigurationProducer
import org.cangnova.cangjie.psi.CjFile
import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

fun executeActionCjpmCommand(e: AnActionEvent, configName: String, command: String, vararg args: String) {
    val file = e.getData(CommonDataKeys.PSI_FILE) as? CjFile ?: return
    val project = file.project

    val settings = createRunConfig(e) ?: run {
        RunManager.getInstance(project)
            .createConfiguration(configName, CjpmCommandConfigurationType::class.java)
    }


    val runConfiguration = settings.configuration as CjCommandConfiguration

    runConfiguration.command = "$command  ${args.joinToString(" ")}"
    runConfiguration.name = configName


    val executorInstance = DefaultRunExecutor.getRunExecutorInstance()
    val executionEnvironment = ExecutionEnvironmentBuilder
        .createOrNull(executorInstance, runConfiguration)
        ?.build() ?: run {
        throw RuntimeException("cannot create executive environment")
    }

    ExecutionManager.getInstance(project).restartRunProfile(executionEnvironment)
}

private fun createRunConfig(e: AnActionEvent): RunnerAndConfigurationSettings? {
    val context = ConfigurationContext.getFromContext(e.dataContext, e.place)
    val configProducer = RunConfigurationProducer.getInstance(CjpmRunConfigurationProducer::class.java)

    val existingConfiguration = configProducer.findExistingConfiguration(context)
    return existingConfiguration
}
