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

package org.cangnova.cangjie.dapDebugger

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.redhat.devtools.lsp4ij.dap.DAPDebugRunner
import org.cangnova.cangjie.project.model.cjSdk
import org.cangnova.cangjie.run.CangJieProgramRunConfiguration
import org.cangnova.cangjie.run.CangJieProgramRunState
import org.cangnova.cangjie.run.CjExecutableRunner
import java.io.File


private const val RUNNER_ID = "CjDAPDebugRunner"

class CjDAPDebugRunner : DAPDebugRunner() {
    override fun getRunnerId(): String = RUNNER_ID
    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        return profile is CangJieProgramRunConfiguration
    }


//    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
//        if (state !is CangJieProgramRunState) return null
//
//
//        if (state.configuration.buildConfiguration?.runState == null) return null
//
//        val buildEnvironment = state.configuration.buildConfiguration?.runState!!.environment
//
//        val artifacts = buildEnvironment.artifacts.orEmpty()
//        if (artifacts.isEmpty()) {
//            // No artifacts available, run the configuration directly
//            return super.doExecute(state, environment)
//        }
//
//        val artifact = artifacts.firstOrNull() ?: return null
//        val binaries = artifact.executables
//
//        if (binaries.isEmpty()) {
//            return super.doExecute(state, environment)
//        }
//
//        val runExecutable = GeneralCommandLine().apply {
//            exePath = binaries.first()
//            workDirectory = state.configuration.workingDirectory?.toFile() ?: File(environment.project.basePath ?: ".")
//
//
//            // Add environment variables
//            val sdkEnv = environment.project.cjSdk?.getEnvironment() ?: emptyMap()
//            withEnvironment(state.configuration.env.envs + sdkEnv)
//        }
//
//        return CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)
//    }


}

