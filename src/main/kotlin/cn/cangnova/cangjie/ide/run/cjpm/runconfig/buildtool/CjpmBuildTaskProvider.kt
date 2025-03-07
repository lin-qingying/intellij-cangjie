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

package cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool

import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.execution.ParametersListUtil
import java.nio.file.Path

class CjpmBuildTaskProvider : CjBuildTaskProvider<CjpmBuildTaskProvider.BuildTask>() {

    class BuildTask : CjBuildTaskProvider.BuildTask<BuildTask>(ID)
    companion object {
        @JvmField
        val ID: Key<BuildTask> = Key.create("CJPM.BUILD_TASK_PROVIDER")
    }

    override fun getId(): Key<BuildTask> = ID


    override fun createTask(runConfiguration: RunConfiguration): BuildTask? =
        if (runConfiguration is CjpmCommandConfiguration) BuildTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: BuildTask
    ): Boolean {
        if (configuration !is CjpmCommandConfiguration) return false
        val buildConfiguration = getBuildConfiguration(configuration) ?: return true

//        val projectDirectory = buildConfiguration.workingDirectory ?: return false


//        val configArgs = ParametersListUtil.parse(buildConfiguration.command.executeCommand)
//        val targetFlagIdx = configArgs.indexOfFirst { it.startsWith("--target") }
//
//        val targetFlag = if (targetFlagIdx != -1) configArgs[targetFlagIdx] else null
//        val targetTriple = when {
//            targetFlag == "--target" -> configArgs.getOrNull(targetFlagIdx + 1)
//            targetFlag?.startsWith("--target=") == true -> StringUtil.unquoteString(targetFlag.substringAfter("="))
//            else -> null
//        }
//        if (targetTriple != null && checkNeedInstallTarget(configuration.project, projectDirectory, targetTriple)) {
//            return false
//        }

        return doExecuteTask(buildConfiguration, environment)
    }


}

