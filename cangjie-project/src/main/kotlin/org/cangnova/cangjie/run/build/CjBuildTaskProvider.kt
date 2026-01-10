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

package org.cangnova.cangjie.run.build

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.intellij.openapi.util.Key
import com.intellij.task.ProjectTaskManager
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.run.CangJieBuildManager.createBuildEnvironment
import org.cangnova.cangjie.run.CangJieBuildManager.getBuildConfiguration
import org.cangnova.cangjie.run.CangJieCommandRunConfiguration
import org.cangnova.cangjie.run.CangJieProgramRunConfiguration
import org.cangnova.cangjie.run.CangJieRunConfigurationBase
import java.util.concurrent.CompletableFuture


class CangJieBuildTaskProvider : CjBuildTaskProvider<CangJieBuildTaskProvider.BuildTask>() {

    class BuildTask : CjBuildTaskProvider.BuildTask<BuildTask>(ID)
    companion object {
        @JvmField
        val ID: Key<BuildTask> = Key.create("CANGJIE.BUILD_TASK_PROVIDER")
    }

    override fun getId(): Key<BuildTask> = ID


    override fun createTask(runConfiguration: RunConfiguration): BuildTask? =
        if (runConfiguration is CangJieProgramRunConfiguration) BuildTask() else null

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: BuildTask
    ): Boolean {
        if (configuration !is CangJieProgramRunConfiguration) return false
        val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID


        val buildConfiguration = getBuildConfiguration(configuration) ?: return true
        if(isDebug) {
            buildConfiguration.args += "-g"
        }
        configuration.buildConfiguration = buildConfiguration


        return doExecuteTask(buildConfiguration, environment)
    }


}


abstract class CjBuildTaskProvider<T : CjBuildTaskProvider.BuildTask<T>> : BeforeRunTaskProvider<T>() {
    override fun getName(): String = CjProjectBundle.message("build")
    abstract class BuildTask<T : BuildTask<T>>(providerId: Key<T>) : BeforeRunTask<T>(providerId) {
        init {
            isEnabled = true
        }
    }

    override fun isSingleton(): Boolean = true
    protected fun doExecuteTask(
        buildConfiguration: CangJieRunConfigurationBase,
        environment: ExecutionEnvironment
    ): Boolean {


        val buildEnvironment = createBuildEnvironment(buildConfiguration, environment) ?: return false
        val buildableElement = CangjieBuildConfiguration(buildConfiguration, buildEnvironment)

        val result = CompletableFuture<Boolean>()
        ProjectTaskManager.getInstance(environment.project).build(buildableElement).onProcessed {
            result.complete(it?.hasErrors() == false && !it.isAborted)
        }
        return result.get()
    }
}

class CangjieBuildConfiguration(
    val configuration: CangJieRunConfigurationBase,
    val environment: ExecutionEnvironment
) : ProjectModelBuildableElement {

    override fun getExternalSource(): ProjectModelExternalSource? {
        return null
    }
}