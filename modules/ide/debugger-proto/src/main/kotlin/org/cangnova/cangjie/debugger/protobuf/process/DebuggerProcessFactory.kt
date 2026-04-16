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

package org.cangnova.cangjie.debugger.protobuf.process

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.util.io.BaseOutputReader
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.debugger.DebuggerProvider
import org.cangnova.cangjie.debugger.protobuf.settings.ArchitectureType
import org.cangnova.cangjie.debugger.protobuf.settings.ArchitectureType.*
import org.cangnova.cangjie.debugger.protobuf.settings.ProtoDebuggerService
import java.nio.charset.StandardCharsets

/**
 * 调试器进程工厂
 *
 * 职责：
 * - 创建调试器前端进程
 * - 构建命令行参数
 * - 配置跨平台环境变量
 */
class DebuggerProcessFactory(
    val project: Project,
    private val debuggerProvider: DebuggerProvider
) {
    private var debugModeEnabled = false

    // ==================== 公共 API ====================

    /**
     * 创建调试器命令行
     * @param port 调试器监听端口
     * @param arch 架构类型
     */
    fun createDriverCommandLine(port: Int, arch: ArchitectureType = ARM64): GeneralCommandLine {
        // 使用 debuggerProvider 获取服务器路径
        val serverPath = runBlocking {
            debuggerProvider.getServerPath()
        }

        return GeneralCommandLine().apply {
            exePath = serverPath.toString()
            workDirectory = serverPath.parent.toFile()

            setupEnvironment()
            setupCommonParameters()

            // 添加端口参数
            addParameter(port.toString())
            if (debugModeEnabled) addParameter("--debug")
        }
    }

    /**
     * 创建调试进程处理器
     */
    fun createDebugProcessHandler(
        commandLine: GeneralCommandLine,
        isElevated: Boolean = false,
        hostMachine: HostMachine = LocalHost
    ): BaseProcessHandler<*> {
        return when {
            !hostMachine.isRemote && !isElevated -> createLocalProcessHandler(commandLine)
            else -> hostMachine.createProcessBuilder()
                .withElevated(isElevated)
                .withRunDebugEnvSetup(true)
                .build(commandLine)
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 设置环境变量
     *
     * 从 debuggerProvider 获取通用环境变量（如 LD_LIBRARY_PATH, DYLD_FRAMEWORK_PATH）
     * 以及特定于 Proto 调试器的环境变量
     */
    private fun GeneralCommandLine.setupEnvironment() {
        // 添加 DebuggerProvider 提供的通用环境变量
        environment.putAll(debuggerProvider.getEnvironmentVariables(project))

        // 添加 Proto 调试器特定的环境变量
        environment["LLDB_LAUNCH_INFERIORS_WITHOUT_CONSOLE"] = "1"

        // ASLR 配置
        if (ProtoDebuggerService.getInstance().disableASLR) {
            environment["LLDB_LAUNCH_FLAG_DISABLE_ASLR"] = "1"
        }
    }

    private fun GeneralCommandLine.setupCommonParameters() {
        charset = StandardCharsets.UTF_8
    }

    private fun createLocalProcessHandler(commandLine: GeneralCommandLine) =
        object : OSProcessHandler(commandLine) {
            override fun readerOptions() = BaseOutputReader.Options.BLOCKING
        }.apply {
            setShouldDestroyProcessRecursively(false)
        }
}