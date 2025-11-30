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

package org.cangnova.cangjie.debugger

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState

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
     * 要调试的程序路径或名称
     * 标识需要调试的目标程序
     */
    val program: String,

    /**
     * 可运行的可执行文件命令行配置
     * 如果需要直接运行可执行文件而不是通过调试器，则使用此参数
     */
    val runExecutable: GeneralCommandLine,

    /**
     * 运行状态对象
     * 包含当前运行会话的状态信息和配置
     */
    val state: RunProfileState,

    val debuggerProvider:DebuggerProvider
)