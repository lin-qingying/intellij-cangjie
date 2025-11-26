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

package org.cangnova.cangjie.debugger.runner

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import org.cangnova.cangjie.debugger.ui.CangJieDebugProcess
import org.cangnova.cangjie.debugger.ui.CangJieRunConfiguration
import org.cangnova.cangjie.project.model.cjSdk
import org.cangnova.cangjie.run.CangJieProgramRunConfiguration
import org.cangnova.cangjie.run.CangJieProgramRunState
import org.cangnova.cangjie.run.CjExecutableRunner
import kotlin.io.path.pathString

/**
 * 仓颉 DAP 调试运行器
 *
 * 负责启动调试会话并创建调试进程
 */
class CjDAPDebugRunner : CjExecutableRunner(
    DefaultDebugExecutor.EXECUTOR_ID,
    ERROR_MESSAGE_TITLE
) {

    companion object {
        private val LOG = Logger.getInstance(CjDAPDebugRunner::class.java)
        private const val RUNNER_ID = "CjDAPDebugRunner"
        private const val ERROR_MESSAGE_TITLE = "Unable to run debugger"
    }

    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is CangJieProgramRunConfiguration && super.canRun(executorId, profile)
    }

    override fun doExecute(
        state: RunProfileState,
        environment: ExecutionEnvironment
    ): RunContentDescriptor? {
        if (state !is CangJieProgramRunState) {
            LOG.warn("Invalid run state type: ${state::class.java}")
            return null
        }

        // 检查构建配置
        if (state.configuration.buildConfiguration?.runState == null) {
            LOG.warn("No build configuration available")
            return null
        }

        val buildEnvironment = state.configuration.buildConfiguration?.runState!!.environment
        val artifacts = buildEnvironment.artifacts.orEmpty()

        if (artifacts.isEmpty()) {
            LOG.warn("No artifacts available")
            return super.doExecute(state, environment)
        }

        val artifact = artifacts.firstOrNull() ?: return null
        val binaries = artifact.executables

        if (binaries.isEmpty()) {
            LOG.warn("No executables found in artifact")
            return super.doExecute(state, environment)
        }

        // 获取可执行文件路径
        val programPath = binaries.first()
        LOG.info("Starting debugger for program: $programPath")

        // 创建运行配置
        val runConfig = CangJieRunConfiguration(
            programPath = programPath,
            programArguments = state.configuration.programArgs?.split(" ")?.filter { it.isNotBlank() } ?: emptyList(),
            workingDirectory = state.configuration.workingDirectory?.pathString ?: environment.project.basePath ?: ".",
            environmentVariables = buildEnvironmentVariables(state, environment)
        )

        // 启动调试会话
        return startDebugSession(environment, runConfig)
    }

    private fun buildEnvironmentVariables(
        state: CangJieProgramRunState,
        environment: ExecutionEnvironment
    ): Map<String, String> {
        val envVars = mutableMapOf<String, String>()

        // 添加SDK环境变量
        val sdkEnv = environment.project.cjSdk?.getEnvironment() ?: emptyMap()
        envVars.putAll(sdkEnv)

        // 添加用户配置的环境变量
        envVars.putAll(state.configuration.env.envs)

        return envVars
    }

    private fun startDebugSession(
        environment: ExecutionEnvironment,
        runConfig: CangJieRunConfiguration
    ): RunContentDescriptor {
        return XDebuggerManager.getInstance(environment.project).startSession(
            environment,
            object : XDebugProcessStarter() {
                override fun start(session: XDebugSession): XDebugProcess {
                    LOG.info("Creating CangJie debug process")
                    return CangJieDebugProcess(runConfig, session)
                }
            }
        ).runContentDescriptor
    }
}