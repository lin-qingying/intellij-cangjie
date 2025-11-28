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

package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.pty.PtyProcessHandler
import org.cangnova.cangjie.protodebugger.services.AttachService
import java.io.OutputStream

/**
 * AttachService 的默认实现
 *
 * 使用 PtyProcessHandler 提供跨平台的进程管理和终端模拟功能。
 * 该实现管理单个进程的生命周期。
 *
 * ## 实现细节
 *
 * ### 进程管理
 * - 使用 ProcessHandler（PtyProcessHandler 或 OSProcessHandler）管理进程
 * - 支持 PTY 模式和标准进程模式
 * - 自动清理进程资源
 *
 * ### 终端模拟
 * - Windows: 使用 pty4j 的 WinPty/ConPTY 实现
 * - Linux/macOS: 使用 pty4j 的 POSIX PTY 实现
 * - 统一的 ProcessHandler API，跨平台兼容
 *
 * ### 资源管理
 * - ProcessHandler 在关闭时自动释放资源
 * - 支持优雅清理和强制终止
 * - 异常处理确保资源不泄漏
 *
 * @see AttachService
 * @see PtyProcessHandler
 */
class AttachServiceImpl : AttachService {

    companion object {
        private val LOG = Logger.getInstance(AttachServiceImpl::class.java)
    }

    // ==================== 状态管理 ====================

    /**
     * 当前管理的进程处理器
     *
     * 在 ATTACH 模式下，通常只管理一个进程（被调试的目标程序）
     */
    @Volatile
    private var currentProcessHandler: ProcessHandler? = null

    /**
     * 服务是否已关闭
     */
    @Volatile
    private var closed = false

    // ==================== AttachService 接口实现 ====================

    override fun startProcess(
        commandLine: GeneralCommandLine,
        emulateTerminal: Boolean,
        environment: Map<String, String>?
    ): OSProcessHandler {
        check(!closed) { "AttachService is closed" }

        LOG.info("Starting process in ATTACH mode: ${commandLine.commandLineString}, emulateTerminal=$emulateTerminal")

        try {
            // 合并环境变量
            environment?.let { env ->
                commandLine.environment.putAll(env)
            }

            // 根据配置创建进程处理器
            val processHandler = if (emulateTerminal) {
                // 使用 PTY 启动进程
                startWithPty(commandLine)
            } else {
                // 使用标准方式启动进程
                startWithoutPty(commandLine)
            }

            // 获取进程 PID（用于验证）
            val pid = when (processHandler) {
                is PtyProcessHandler -> processHandler.getPid()
                else -> processHandler.process.pid()
            }

            if (pid <= 0) {
                processHandler.destroyProcess()
                throw ExecutionException(DebuggerBundle.message("error.cannot.get.process.id"))
            }

            // 保存当前进程处理器
            currentProcessHandler = processHandler

            // 启动进程监听
            processHandler.startNotify()

            LOG.info("Process started successfully: PID=$pid, emulateTerminal=$emulateTerminal")
            return processHandler

        } catch (e: Exception) {
            LOG.error("Failed to start process: ${commandLine.commandLineString}", e)
            throw ExecutionException("Failed to start process for attach debugging", e)
        }
    }

    override fun getProcessInput(): OutputStream? {
        val handler = currentProcessHandler ?: return null

        return try {
            handler.processInput
        } catch (e: Exception) {
            LOG.warn("Failed to get process input stream", e)
            null
        }
    }

    override fun isProcessAlive(): Boolean {
        val handler = currentProcessHandler ?: return false

        return try {
            !handler.isProcessTerminated && !handler.isProcessTerminating
        } catch (e: Exception) {
            LOG.warn("Failed to check process alive status", e)
            false
        }
    }

    override fun cleanupAll() {
        LOG.info("Cleaning up attach service")

        val handler = currentProcessHandler
        if (handler != null) {
            try {
                // 如果进程还在运行，先终止它
                if (!handler.isProcessTerminated) {
                    LOG.info("Terminating process before cleanup")
                    handler.destroyProcess()
                }

                // 等待进程结束（带超时）
                if (!handler.waitFor(5000)) {
                    LOG.warn("Process did not terminate within timeout, destroying forcibly")
                    handler.destroyProcess()
                }

                LOG.info("Process cleanup completed")
            } catch (e: Exception) {
                LOG.error("Error during process cleanup", e)
            } finally {
                currentProcessHandler = null
            }
        }
    }

    override fun close() {
        if (closed) {
            return // 幂等性
        }

        closed = true
        LOG.info("Closing AttachService")

        try {
            cleanupAll()
        } catch (e: Exception) {
            LOG.error("Error during AttachService close", e)
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 使用 PTY 启动进程
     *
     * 创建 PtyProcessHandler 来管理进程，提供终端模拟功能。
     * 适用于需要实时输出和交互的场景。
     *
     * @param commandLine 命令行配置
     * @return PtyProcessHandler 实例
     * @throws ExecutionException 启动失败时抛出
     */
    private fun startWithPty(commandLine: GeneralCommandLine): PtyProcessHandler {
        LOG.debug("Starting process with PTY: ${commandLine.commandLineString}")

        return try {
            PtyProcessHandler(commandLine)
        } catch (e: Exception) {
            LOG.error("Failed to create PTY process", e)
            throw ExecutionException("Failed to create PTY process: ${e.message}", e)
        }
    }

    /**
     * 使用标准方式启动进程（不使用 PTY）
     *
     * 创建标准的 OSProcessHandler 来管理进程。
     * 适用于简单的调试场景，输出可能被缓冲。
     *
     * @param commandLine 命令行配置
     * @return OSProcessHandler 实例
     * @throws ExecutionException 启动失败时抛出
     */
    private fun startWithoutPty(commandLine: GeneralCommandLine): OSProcessHandler {
        LOG.debug("Starting process without PTY: ${commandLine.commandLineString}")

        return try {
            val process = commandLine.createProcess()
            OSProcessHandler(process, commandLine.commandLineString, commandLine.charset)
        } catch (e: Exception) {
            LOG.error("Failed to create standard process", e)
            throw ExecutionException("Failed to create process: ${e.message}", e)
        }
    }
}