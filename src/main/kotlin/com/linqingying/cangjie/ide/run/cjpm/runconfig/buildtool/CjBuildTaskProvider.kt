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

package com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.createBuildEnvironment

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import com.intellij.task.ProjectTaskManager
import java.util.concurrent.CompletableFuture


abstract class CjBuildTaskProvider<T : CjBuildTaskProvider.BuildTask<T>> : BeforeRunTaskProvider<T>() {
    override fun getName(): String = CangJieBundle.message("build")
    abstract class BuildTask<T : BuildTask<T>>(providerId: Key<T>) : BeforeRunTask<T>(providerId) {
        init {
            isEnabled = true
        }
    }

    override fun isSingleton(): Boolean = true
    protected fun doExecuteTask(
        buildConfiguration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment
    ): Boolean {


        val buildEnvironment = createBuildEnvironment(buildConfiguration, environment) ?: return false
        val buildableElement = CjpmBuildConfiguration(buildConfiguration, buildEnvironment)

        val result = CompletableFuture<Boolean>()
        ProjectTaskManager.getInstance(environment.project).build(buildableElement).onProcessed {
            result.complete( it?.hasErrors() == false && !it.isAborted)
        }
        return result.get()
    }
}
