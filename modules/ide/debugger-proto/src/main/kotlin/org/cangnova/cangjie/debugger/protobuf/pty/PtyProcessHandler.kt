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

package org.cangnova.cangjie.debugger.protobuf.pty

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.util.io.BaseOutputReader
import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder

/**
 * PTY 进程处理器
 *
 * 基于 pty4j 的进程处理器，继承 OSProcessHandler。
 * 提供完整的 PTY 功能，包括终端窗口大小控制、进程管理等。
 *
 * 主要特性：
 * - 继承 OSProcessHandler，完全兼容 IntelliJ 平台进程管理
 * - 支持 PTY 特有功能（窗口大小调整等）
 * - 跨平台支持（Windows/Unix/Linux/macOS/FreeBSD）
 * - 自动处理 PTY 进程的输入输出流
 *
 * 使用场景：
 * - 调试器终端进程管理
 * - 需要 PTY 的命令行工具执行
 * - 终端模拟器实现
 * - 交互式进程管理
 *
 * 实现说明：
 * - 参考 IntelliJ OSProcessHandler 的实现模式
 * - 使用 PtyProcessBuilder 创建 PtyProcess
 * - 支持终端特性（回显、窗口大小等）
 *
 * 示例：
 * ```kotlin
 * // 从 GeneralCommandLine 创建
 * val commandLine = GeneralCommandLine("bash", "-l")
 * val handler = PtyProcessHandler(commandLine)
 *
 * // 使用
 * handler.addProcessListener(...)
 * handler.startNotify()
 * handler.setWindowSize(120, 30)
 * ```
 *
 * @param commandLine 命令行配置
 * @param initialColumns 初始终端列数（默认 80）
 * @param initialRows 初始终端行数（默认 24）
 * @throws ExecutionException 当进程创建失败时抛出
 */
open class PtyProcessHandler @JvmOverloads constructor(
    commandLine: GeneralCommandLine,
    initialColumns: Int = DEFAULT_COLUMNS,
    initialRows: Int = DEFAULT_ROWS
) : OSProcessHandler(
    startPtyProcess(commandLine, initialColumns, initialRows),
    commandLine.commandLineString,
    commandLine.charset
) {

    companion object {
        /** 默认终端列数 */
        const val DEFAULT_COLUMNS = 80

        /** 默认终端行数 */
        const val DEFAULT_ROWS = 24

        /**
         * 启动 PTY 进程
         *
         * 类似 OSProcessHandler.startProcess()，但创建 PtyProcess。
         *
         * @param commandLine 命令行配置
         * @param columns 终端列数
         * @param rows 终端行数
         * @return 创建的 PtyProcess
         * @throws ExecutionException 当进程创建失败时抛出
         */
        @Throws(ExecutionException::class)
        private fun startPtyProcess(
            commandLine: GeneralCommandLine,
            columns: Int = DEFAULT_COLUMNS,
            rows: Int = DEFAULT_ROWS
        ): PtyProcess {
            try {
                // 构建命令数组
                val command = buildList {
                    add(commandLine.exePath)
                    addAll(commandLine.parametersList.list)
                }.toTypedArray()

                // 准备环境变量
                val environment = commandLine.environment.toMutableMap()

                // 确保设置 TERM 环境变量
                if (!environment.containsKey("TERM")) {
                    environment["TERM"] = "xterm"
                }

                // 构建 PTY 进程
                val builder = PtyProcessBuilder(command)
                    .setEnvironment(environment)
                    .setInitialColumns(columns)
                    .setInitialRows(rows)
                    .setUseWinConPty(true)
                    .setConsole(true)

                // 设置工作目录（如果指定）
                commandLine.workDirectory?.let {
                    builder.setDirectory(it.absolutePath)
                }

                // 启动进程
                return builder.start()
            } catch (e: Exception) {
                throw ExecutionException("Failed to start PTY process: ${e.message}", e)
            }
        }
    }

    /**
     * 配置输出读取器选项
     *
     * 为 PTY 进程配置合适的读取器选项，启用终端 PTY 模式。
     * 使用阻塞读取，因为 PTY 进程的 InputStream.available() 不可靠。
     */
    override fun readerOptions(): BaseOutputReader.Options {
        return BaseOutputReader.Options.forTerminalPtyProcess()
    }

    // ==================== PTY 特有功能 ====================

    /**
     * 获取底层的 PtyProcess
     *
     * @return PtyProcess 实例
     */
    fun getPtyProcess(): PtyProcess {
        val process = process
        require(process is PtyProcess) { "Process is not a PtyProcess" }
        return process
    }


    /**
     * 获取进程 PID
     *
     * @return 进程 PID
     */
    fun getPid(): Long {
        return try {
            getPtyProcess().pid()
        } catch (e: Exception) {
            -1L
        }
    }
}