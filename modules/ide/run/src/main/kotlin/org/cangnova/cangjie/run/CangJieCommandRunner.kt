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

/**
 * 仓颉默认程序运行器基类
 *
 * 提供程序运行的基础实现
 */
abstract class CjDefaultProgramRunnerBase : GenericProgramRunner<RunnerSettings>() {

    /**
     * 执行运行状态
     *
     * @param state 运行配置状态
     * @param environment 执行环境
     * @return 运行内容描述符，如果执行失败则返回 null
     */
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        return executeState(state, environment, this)
    }
}

/**
 * 仓颉程序运行器
 *
 * 处理所有仓颉运行配置的执行，包括命令配置和程序配置
 */
class CangJieProgramRunner : CjExecutableRunner(
    DefaultRunExecutor.EXECUTOR_ID,
    "CangJie Run Error"
) {
    /**
     * 执行程序
     *
     * 根据构建产物执行程序，如果有可执行文件则直接运行，否则使用默认执行方式
     *
     * @param state 运行配置状态
     * @param environment 执行环境
     * @return 运行内容描述符，如果执行失败则返回 null
     */
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        // 检查状态类型
        if (state !is CangJieProgramRunState) return null

        // 检查构建配置是否存在
        if (state.configuration.buildConfiguration?.runState == null) return null

        val buildEnvironment = state.configuration.buildConfiguration?.runState!!.environment

        // 获取构建产物
        val artifacts = buildEnvironment.artifacts.orEmpty()
        if (artifacts.isEmpty()) {
            // 没有可用的产物，直接运行配置
            return super.doExecute(state, environment)
        }

        // 获取第一个产物
        val artifact = artifacts.firstOrNull() ?: return null
        val binaries = artifact.executables

        // 检查是否有可执行文件
        if (binaries.isEmpty()) {
            return super.doExecute(state, environment)
        }

        // 构建运行命令
        val runExecutable = GeneralCommandLine().apply {
            exePath = binaries.first()
            workDirectory = state.configuration.workingDirectory?.toFile()
                ?: File(environment.project.basePath ?: ".")

            // 添加环境变量（合并配置的环境变量和 SDK 环境变量）
            val sdkEnv = environment.project.cjSdk?.getEnvironment() ?: emptyMap()
            withEnvironment(state.configuration.env.envs + sdkEnv)
        }

        return showRunContent(state, environment, runExecutable)
    }

    /**
     * 检查是否可以运行指定的配置
     *
     * @param executorId 执行器 ID
     * @param profile 运行配置
     * @return 如果可以运行则返回 true
     */
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is CangJieProgramRunConfiguration && super.canRun(executorId, profile)
    }

    /**
     * 获取运行器 ID
     *
     * @return 运行器的唯一标识符
     */
    override fun getRunnerId(): String = RUNNER_ID

    companion object {
        /**
         * 运行器的唯一标识符
         */
        val RUNNER_ID: String = "CangJieProgramRunner"
    }
}

/**
 * 仓颉命令运行器
 *
 * 专门用于执行仓颉命令配置（如 build、test 等）
 */
class CangJieCommandRunner : CjExecutableRunner(
    DefaultRunExecutor.EXECUTOR_ID,
    "CangJie Run Error"
) {
    /**
     * 检查是否可以运行指定的配置
     *
     * @param executorId 执行器 ID
     * @param profile 运行配置
     * @return 如果可以运行则返回 true
     */
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return profile is CangJieCommandRunConfiguration && super.canRun(executorId, profile)
    }

    /**
     * 执行命令
     *
     * @param state 运行配置状态
     * @param environment 执行环境
     * @return 运行内容描述符，如果执行失败则返回 null
     */
    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        // 检查状态类型
        if (state !is CangJieCommandRunState) return null

        return super.doExecute(state, environment)
    }

    /**
     * 获取运行器 ID
     *
     * @return 运行器的唯一标识符
     */
    override fun getRunnerId(): String = "CangJieCommandRunner"
}