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
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.cangnova.cangjie.process.CjProcessHandler
import org.cangnova.cangjie.run.target.startProcess

/**
 * 仓颉程序运行状态
 *
 * 处理编译后的仓颉模块的执行
 */
class CangJieProgramRunState(
    environment: ExecutionEnvironment,
    configuration: CangJieProgramRunConfiguration
) : CangJieRunState<CangJieProgramRunConfiguration>(environment, configuration) {

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
        // 在执行程序之前保存所有文档，确保运行的是最新代码
        // 必须在 EDT（事件调度线程）上执行
        ApplicationManager.getApplication().invokeAndWait {
            FileDocumentManager.getInstance().saveAllDocuments()
        }

        // 创建命令行
        val commandLine = createCommandLine()
            ?: throw IllegalStateException("Failed to create command line for program execution")

        LOG.debug("执行程序: `${commandLine.commandLineString}`")

        // 支持远程目标平台环境
        val processHandler = if (configuration.targetEnvironment != null) {
            // 远程执行 - 需要上传可执行文件
            commandLine.startProcess(
                project = environment.project,
                config = configuration.targetEnvironment,
                processColors = processColors,
                uploadExecutable = true  // 程序运行时上传可执行文件
            )
        } else {
            // 本地执行
            val handler = CjProcessHandler(commandLine, processColors = processColors)
            ProcessTerminatedListener.attach(handler)
            handler
        }

        // 为构建工具窗口集成附加构建适配器
        CangJieBuildManager.attachBuildAdapter(
            project = environment.project,
            taskName = "运行 ${configuration.name}",
            workingDirectory = configuration.workingDirectory ?: java.nio.file.Paths.get(
                environment.project.basePath ?: "."
            ),
            environment = environment,
            processHandler = processHandler,
            buildProfile = if (environment.isDebug) BuildProfile.DEBUG else BuildProfile.RELEASE
        )

        return processHandler
    }

    /**
     * 创建命令行
     *
     * 根据配置创建用于执行程序的命令行对象
     *
     * @return 配置好的命令行对象，如果创建失败则返回 null
     */
    private fun createCommandLine(): GeneralCommandLine? {
        // 获取模块和工作目录
        val module = configuration.getCjModule() ?: return null
        val workingDir = configuration.workingDirectory ?: return null

        val commandLine = GeneralCommandLine()
        commandLine.workDirectory = workingDir.toFile()

        // 设置环境变量
        commandLine.environment.putAll(configuration.env.envs)
        if (configuration.env.isPassParentEnvs) {
            commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        }

        // TODO: 构建实际运行程序的命令
        // 这将取决于构建系统（cjpm、cjc 等）
        // 目前这是一个占位符，需要根据特定构建系统的要求来实现

        return commandLine
    }

    companion object {
        private val LOG: Logger = logger<CangJieProgramRunState>()
    }
}