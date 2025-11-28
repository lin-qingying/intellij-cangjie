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

package org.cangnova.cangjie.debugger.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import org.cangnova.cangjie.debugger.RunParameters
import org.cangnova.cangjie.debugger.configurable.CangJieDebuggerEngineServices
import org.cangnova.cangjie.debugger.configurable.DebuggerEngine.DAP
import org.cangnova.cangjie.debugger.configurable.DebuggerEngine.PROTOBUF
import org.cangnova.cangjie.debugger.dap.ui.DapDebugProcess
import org.cangnova.cangjie.debugger.protobuf.core.ProtoDebugProcess
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.run.CangJieRunConfigurationBase
import org.cangnova.cangjie.run.CangJieRunState


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
                    when (CangJieDebuggerEngineServices.getInstance(state.project).debuggerEngine) {
                        DAP -> DapDebugProcess(
                            runParameters, session,
                        )

                        PROTOBUF -> ProtoDebugProcess(
                            runParameters, session,
                            consoleBuilder = state.consoleBuilder
                        ).apply {
                            ProcessTerminatedListener.attach(processHandler, environment.project)

                            start()

                        }
                    }

            }
        ).runContentDescriptor
    }
}
