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

package org.cangnova.cangjie.protodebugger.core

import org.cangnova.cangjie.messages.CangJieBundle
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import org.cangnova.cangjie.protodebugger.CangJieDebuggerServerManager
import org.cangnova.cangjie.run.CangJieProgramRunState
import org.cangnova.cangjie.run.CangJieRunConfigurationBase
import org.cangnova.cangjie.run.CangJieRunState
import org.jetbrains.annotations.Nls

/**
 * 运行参数数据类
 *
 * 该数据类包含了启动调试会话所需的所有参数信息，包括调试器命令、端口号、程序路径等。
 * 这些参数被用于创建和配置调试进程。
 *
 * @property command 调试器命令行配置
 * @property port 调试器监听的端口号
 * @property program 要调试的程序路径或名称
 * @property runExecutable 可运行的可执行文件命令行配置，可选
 * @property state 运行状态对象，可选
 */
data class RunParameters(
    /**
     * 调试器命令行配置
     * 包含启动调试器所需的所有命令行参数和环境配置
     */
    val command: GeneralCommandLine,
    
    /**
     * 调试器监听的端口号
     * 用于与被调试程序建立通信连接
     */
    val port: Int,
    
    /**
     * 要调试的程序路径或名称
     * 标识需要调试的目标程序
     */
    val program: String,
    
    /**
     * 可运行的可执行文件命令行配置
     * 如果需要直接运行可执行文件而不是通过调试器，则使用此参数
     */
    val runExecutable: GeneralCommandLine? = null,
    
    /**
     * 运行状态对象
     * 包含当前运行会话的状态信息和配置
     */
    val state: RunProfileState? = null
)

/**
 * 仓颉调试运行器工具类
 *
 * 该工具类提供了启动和管理仓颉语言调试会话的实用方法。它负责创建调试进程、
 * 配置调试参数以及处理调试会话的初始化工作。
 */
object CjDebugRunnerUtils {

    /**
     * 错误消息标题
     * 
     * 当调试器无法启动时显示的错误消息标题，从资源文件中获取本地化文本。
     */
    val ERROR_MESSAGE_TITLE: String = CangJieBundle.message("unable.to.run.debugger")

    /**
     * 显示运行内容并启动调试会话
     *
     * 该方法负责创建并启动一个调试会话。它会构建调试进程所需的参数，
     * 启动调试服务器，并初始化调试过程。
     *
     * @param T 仓颉运行配置的类型参数
     * @param state 仓颉运行状态对象，包含运行配置和项目信息
     * @param environment 执行环境，提供IDE上下文和配置
     * @param runExecutable 可执行文件的命令行配置
     * @return 运行内容描述符，包含调试会话的相关信息
     */
    fun <T : CangJieRunConfigurationBase> showRunContent(
        state: CangJieRunState<T>,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor {
        val runParameters = RunParameters(
            CangJieDebuggerServerManager.getCommandLine(state.project),
            CangJieDebuggerServerManager.DEBUGPORT,
            runExecutable.commandLineString,
            runExecutable
        )

        // 启动调试会话并返回运行内容描述符
        return XDebuggerManager.getInstance(environment.project).startSession(
            environment, object : XDebugProcessStarter() {
                /**
                 * 启动调试进程
                 *
                 * @param session 调试会话对象
                 * @return 创建的仓颉调试进程实例
                 */
                override fun start(session: XDebugSession): XDebugProcess =
                    CangJieDebugProcess(
                        runParameters, session,
                        consoleBuilder = state.consoleBuilder
                    ).apply {
                        ProcessTerminatedListener.attach(processHandler, environment.project)
                        start()
                    }
            }
        ).runContentDescriptor
    }
}
