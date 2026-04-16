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

package org.cangnova.cangjie.debugger.protobuf.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import java.io.OutputStream

/**
 * 附加调试服务接口
 *
 * 负责管理 ATTACH 模式下的进程启动和生命周期。在 ATTACH 模式下，调试器不直接启动目标程序，
 * 而是先启动程序进程，然后再附加调试器。这种模式在 Windows 上配合"模拟终端"选项可以实现行缓冲输出。
 *
 * ## 工作流程
 *
 * 1. **启动进程** - 使用 [startProcess] 启动目标程序，返回进程信息
 * 2. **附加调试器** - 使用返回的 PID 附加调试器（通过 SessionService.loadForAttach）
 * 3. **管理进程** - 在调试期间监控进程状态
 * 4. **清理资源** - 调试结束后清理进程和相关资源
 *
 * ## 与 SessionService 的关系
 *
 * - AttachService: 负责启动和管理目标进程
 * - SessionService: 负责附加调试器到进程
 *
 * ## 平台差异
 *
 * ### Windows
 * - 可选择启用终端模拟（PTY）实现行缓冲输出
 * - 使用 ConPTY 或命名管道进行进程通信
 * - 需要特殊处理标准输入/输出重定向
 *
 * ### Linux/macOS
 * - LLDB 原生支持 PTY，自动提供行缓冲
 * - 使用 POSIX PTY 进行进程通信
 * - 终端模拟由系统自动处理
 *
 * ## 使用场景
 *
 * - **实时输出** - Windows 用户需要实时查看程序输出
 * - **独立进程** - 需要程序在独立终端中运行
 * - **交互式程序** - 程序需要标准输入交互
 * - **调试运行中程序** - 附加到已经运行的进程
 *
 * @see org.cangnova.cangjie.debugger.protobuf.settings.LaunchMode
 * @see SessionService.loadForAttach
 */
interface AttachService : AutoCloseable {

    /**
     * 启动目标进程用于附加调试
     *
     * 在 ATTACH 模式下启动目标程序。根据配置决定是否使用终端模拟（PTY）。
     * 启动成功后返回进程信息，调用者可以使用返回的 PID 附加调试器。
     *
     * ### 终端模拟说明
     *
     * - **启用终端模拟** (emulateTerminal = true)
     *   - Windows: 创建 PTY 并注入到进程，实现行缓冲
     *   - Linux/macOS: 使用系统 PTY
     *   - 适用于需要实时输出的场景
     *
     * - **禁用终端模拟** (emulateTerminal = false)
     *   - 使用标准进程启动方式
     *   - 输出可能被缓冲，不适合实时查看
     *   - 适用于简单的调试场景
     *
     * @param commandLine 目标程序的命令行配置
     * @param emulateTerminal 是否启用终端模拟（PTY）
     * @param environment 额外的环境变量（可选）
     * @return 启动的进程信息
     * @throws com.intellij.execution.ExecutionException 启动失败时抛出
     */
    fun startProcess(
        commandLine: GeneralCommandLine,
        emulateTerminal: Boolean = false,
        environment: Map<String, String>? = null
    ): OSProcessHandler

    /**
     * 获取已启动进程的输入流
     *
     * 用于向目标进程发送输入数据。仅在终端模拟模式下可用。
     *
     * @param processId 进程 ID
     * @return 进程的输入流，如果不支持或进程不存在则返回 null
     */
    fun getProcessInput(): OutputStream?

    /**
     * 检查进程是否仍在运行
     *
     * @param processId 进程 ID
     * @return true 如果进程仍在运行，false 否则
     */
    fun isProcessAlive(): Boolean


    /**
     * 清理所有由此服务管理的进程资源
     *
     * 在调试会话结束时调用，清理所有相关资源。
     */
    fun cleanupAll()
}
