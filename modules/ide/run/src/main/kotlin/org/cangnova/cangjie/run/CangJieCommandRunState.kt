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

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.process.CjProcessHandler
import org.cangnova.cangjie.run.target.startProcess

/**
 * 仓颉运行状态基类
 *
 * 提供运行配置执行的基础功能
 *
 * @param T 运行配置类型
 * @property configuration 运行配置实例
 * @property project 当前项目
 */
abstract class CangJieRunState<T : CangJieRunConfigurationBase>(
    environment: ExecutionEnvironment,
    val configuration: T,
) : CommandLineState(environment) {
    val project: Project = environment.project
}

/**
 * 仓颉命令运行状态
 *
 * 处理仓颉命令的执行，如 build、test、clean 等
 *
 * @property commandExecutor 命令执行器，负责创建和执行具体的命令
 */
class CangJieCommandRunState(
    environment: ExecutionEnvironment,
    configuration: CangJieCommandRunConfiguration,
    private val commandExecutor: CangJieCommandExecutor
) : CangJieRunState<CangJieCommandRunConfiguration>(environment, configuration) {

    /**
     * 启动进程
     *
     * @return 进程处理器实例
     */
    override fun startProcess(): ProcessHandler {
        return startProcess(processColors = true)
    }

    /**
     * 启动进程（支持配置是否启用颜色输出）
     *
     * @param processColors 是否启用颜色输出
     * @return 进程处理器实例
     */
    fun startProcess(processColors: Boolean): ProcessHandler {
        // 在执行命令之前保存所有文档，确保运行的是最新代码
        // 必须在 EDT（事件调度线程）上执行
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeAndWait {
            com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveAllDocuments()
        }

        // 创建命令行
        val commandLine = createCommandLine()
            ?: throw IllegalStateException("Failed to create command line for command execution")

        LOG.debug("执行命令: `${commandLine.commandLineString}`")

        // 支持远程目标平台环境
        val processHandler = if (configuration.targetEnvironment != null) {
            // 远程执行
            commandLine.startProcess(
                project = environment.project,
                config = configuration.targetEnvironment,
                processColors = processColors,
                uploadExecutable = false
            )
        } else {
            // 本地执行
            val handler = CjProcessHandler(commandLine, processColors = processColors)
            ProcessTerminatedListener.attach(handler)
            handler
        }

        // 为构建工具窗口集成附加构建适配器
        // 仅为生成构建产物的命令附加
        if (commandExecutor.shouldAttachBuildAdapter(configuration)) {
            CangJieBuildManager.attachBuildAdapter(
                project = environment.project,
                taskName = "${configuration.name}",
                workingDirectory = configuration.workingDirectory
                    ?: java.nio.file.Paths.get(environment.project.basePath ?: "."),
                environment = environment,
                processHandler = processHandler,
                buildProfile = if (environment.isDebug) BuildProfile.DEBUG else BuildProfile.RELEASE
            )
        }

        return processHandler
    }

    /**
     * 创建命令行
     *
     * 委托给子系统特定的执行器创建命令行，并设置工作目录和环境变量
     *
     * @return 配置好的命令行对象，如果创建失败则返回 null
     */
    private fun createCommandLine(): GeneralCommandLine? {
        // 委托给子系统特定的执行器创建命令行
        val commandLine = commandExecutor.createCommandLine(configuration) ?: return null

        // 如果尚未设置工作目录，则设置工作目录
        if (commandLine.workDirectory == null) {
            configuration.workingDirectory?.let {
                commandLine.workDirectory = it.toFile()
            }
        }

        // 设置环境变量
        commandLine.environment.putAll(configuration.env.envs)
        if (configuration.env.isPassParentEnvs) {
            commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        }

        return commandLine
    }

    companion object {
        private val LOG: Logger = logger<CangJieCommandRunState>()
    }
}

/**
 * 判断执行环境是否为调试模式
 *
 * @return 如果是调试模式则返回 true
 */
val ExecutionEnvironment.isDebug: Boolean
    get() {
        return this.executor.id == DefaultDebugExecutor.EXECUTOR_ID
    }