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
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.runners.executeState
import com.intellij.execution.ui.RunContentDescriptor
import org.cangnova.cangjie.project.model.cjSdk
import java.io.File

abstract class CjDefaultProgramRunnerBase : GenericProgramRunner<RunnerSettings>() {


    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        return executeState(state, environment, this)
    }
}

/**
 * Program runner for all CangJie run configurations.
 * Handles execution of both command and program configurations.
 */
class CangJieProgramRunner : CjExecutableRunner(
    DefaultRunExecutor.EXECUTOR_ID,
    "CangJie Run Error"
) {
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state !is CangJieProgramRunState) return null


        if(state.configuration.buildConfiguration?.runState == null) return null

        val buildEnvironment = state.configuration.buildConfiguration?.runState!!.environment

        val artifacts = buildEnvironment.artifacts.orEmpty()
        if (artifacts.isEmpty()) {
            // No artifacts available, run the configuration directly
            return super.doExecute(state, environment)
        }

        val artifact = artifacts.firstOrNull() ?: return null
        val binaries = artifact.executables

        if (binaries.isEmpty()) {
            return super.doExecute(state, environment)
        }

        val runExecutable = GeneralCommandLine().apply {
            exePath = binaries.first()
            workDirectory = state.configuration.workingDirectory?.toFile() ?: File(environment.project.basePath ?: ".")

            // Add environment variables
            val sdkEnv = environment.project.cjSdk?.getEnvironment() ?: emptyMap()
            withEnvironment(state.configuration.env.envs + sdkEnv)
        }

        return showRunContent(state, environment, runExecutable)
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        return profile is CangJieProgramRunConfiguration && super.canRun(executorId, profile)
    }
    companion object {

        val RUNNER_ID: String = "CangJieProgramRunner"

    }
    override fun getRunnerId(): String = RUNNER_ID
}

class CangJieCommondRunner : CjExecutableRunner(
    DefaultRunExecutor.EXECUTOR_ID,
    "CangJie Run Error"
) {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        return profile is CangJieCommandRunConfiguration && super.canRun(executorId, profile)
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {

        if (state !is CangJieCommandRunState) return null


        return super.doExecute(state, environment)
    }

    override fun getRunnerId(): String = "CangJieCommondRunner"
}