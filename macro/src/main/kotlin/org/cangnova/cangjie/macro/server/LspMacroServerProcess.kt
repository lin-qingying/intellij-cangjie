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
 */

package org.cangnova.cangjie.macro.server

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * LSPMacroServer 进程生命周期管理器
 *
 * 负责启动和停止 `LSPMacroServer` 进程，并暴露其 stdin/stdout 流供通信使用。
 *
 * ## 进程通信方案
 *
 * 通过 `ProcessBuilder` 将子进程的 stdin/stdout 设置为管道模式，
 * 并将 fd 0（stdin）和 fd 1（stdout）作为参数传给 LSPMacroServer：
 *
 * - 客户端写 `process.outputStream` → 服务端从 fd 0 读
 * - 服务端写 fd 1 → 客户端从 `process.inputStream` 读
 *
 * ## 启动命令
 *
 * **Linux/macOS**：
 * ```
 * LSPMacroServer 0 1 <enable_parallel> <cjc_folder> <ppid>
 * ```
 *
 * **Windows**：
 * ```
 * LSPMacroServer.exe 0 1 <enable_parallel> <cjc_folder>
 * ```
 *
 * ## 注意
 *
 * fd 0/1 通过 ProcessBuilder 创建的匿名管道，在 Unix 上 `fstat` 可验证为 FIFO，
 * 在 Windows 上匿名管道可通过 `GetNamedPipeInfo` 验证。
 */
class LspMacroServerProcess(
    private val sdk: CjSdk,
    private val enableParallel: Boolean = true,
) : Closeable {

    private val logger = Logger.getInstance(LspMacroServerProcess::class.java)

    private var process: Process? = null

    /** 子进程的 stdin（客户端写入 → 服务端读取） */
    val outputStream get() = process?.outputStream
        ?: error(CangJieMacroBundle.message("macro.lsp.error.not.started"))

    /** 子进程的 stdout（服务端写入 → 客户端读取） */
    val inputStream get() = process?.inputStream
        ?: error(CangJieMacroBundle.message("macro.lsp.error.not.started"))

    /** 进程是否正在运行 */
    val isAlive: Boolean get() = process?.isAlive == true

    /**
     * 启动 LSPMacroServer 进程
     *
     * @throws IllegalStateException 进程已在运行
     * @throws java.io.IOException 进程启动失败（可执行文件不存在等）
     */
    fun start() {
        check(process == null || !process!!.isAlive) { CangJieMacroBundle.message("macro.lsp.error.already.running") }

        val executable = resolveExecutable()
        check(executable.exists()) {
            CangJieMacroBundle.message("macro.lsp.error.executable.not.found", executable)
        }

        val cjcFolder = sdk.binPath.toString()
        val ppid = ProcessHandle.current().pid().toString()
        val parallelFlag = if (enableParallel) "1" else "0"

        // fd 0 = stdin（服务端读端），fd 1 = stdout（服务端写端）
        val command = buildCommand(executable.absolutePath, parallelFlag, cjcFolder, ppid)
        logger.debug("启动 LSPMacroServer: ${command.joinToString(" ")}")

        process = ProcessBuilder(command)
            .apply {
                environment().putAll(sdk.getEnvironment())
                // 不继承父进程的 stdin/stdout/stderr（保持管道模式）
                redirectErrorStream(false)
            }
            .start()

        logger.info("LSPMacroServer 已启动 (PID=${process!!.pid()})")
    }

    /**
     * 优雅停止进程（发送 ExitTask 后等待退出，超时则强制终止）
     */
    fun stopGracefully(timeoutMs: Long = 3000) {
        val proc = process ?: return
        if (!proc.isAlive) return

        logger.debug("等待 LSPMacroServer 正常退出...")
        if (!proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            logger.warn("LSPMacroServer 未在 ${timeoutMs}ms 内退出，强制终止")
            proc.destroyForcibly()
        }
        process = null
    }

    /** 强制终止进程 */
    fun forceStop() {
        process?.destroyForcibly()
        process = null
        logger.debug("LSPMacroServer 已强制终止")
    }

    override fun close() {
        stopGracefully()
    }

    private fun resolveExecutable(): java.io.File {
        return sdk.getExecutable("LSPMacroServer","tools","bin").toFile()
    }

    private fun buildCommand(
        execPath: String,
        parallelFlag: String,
        cjcFolder: String,
        ppid: String,
    ): List<String> {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        return if (isWindows) {
            // Windows：无 ppid 参数
            listOf(execPath, "0", "1", parallelFlag, cjcFolder)
        } else {
            // Linux/macOS：需要传入 ppid 供服务端监控父进程存活
            listOf(execPath, "0", "1", parallelFlag, cjcFolder, ppid)
        }
    }
}
