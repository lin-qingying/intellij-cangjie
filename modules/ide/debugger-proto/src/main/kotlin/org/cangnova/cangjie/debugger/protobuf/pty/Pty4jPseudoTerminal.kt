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

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import com.pty4j.WinSize
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * 基于 pty4j 的伪终端实现
 *
 * 使用 pty4j 的 PtyProcess 提供跨平台的 PTY 功能。
 * PtyProcess 会根据平台自动选择合适的实现（Windows/Unix/Linux/macOS）。
 *
 * 主要特性：
 * - 跨平台支持（Windows/Unix/Linux/macOS/FreeBSD）
 * - 实现 NamedPipe 接口，提供向后兼容
 * - 提供终端窗口大小控制
 * - 线程安全的资源管理
 * - 基于 pty4j 的成熟实现
 *
 * 使用场景：
 * - 调试器终端
 * - 进程输出捕获和重定向
 * - 需要终端模拟的应用
 * - 替代旧的 NamedPipe 实现
 *
 * 实现说明：
 * - Windows: 使用 WinPty
 * - Unix/Linux/macOS: 使用 POSIX PTY
 * - 所有平台使用统一的 PtyProcess 接口
 * - PTY 在构造时立即启动，因此 isConnected 始终为 true
 *
 * 线程安全：
 * - close() 方法是线程安全的
 * - 使用 @Volatile 保证状态可见性
 *
 * 示例：
 * ```kotlin
 * // 创建 PTY
 * val pty = Pty4jPseudoTerminal(arrayOf("cmd.exe"))
 *
 * // 作为 NamedPipe 使用
 * pty.use { pipe ->
 *     val input = pipe.inputStream
 *     val output = pipe.outputStream
 *     // ... 使用管道
 * }
 *
 * // 高级功能
 * pty.setWindowSize(120, 30)
 * val (cols, rows) = pty.getWindowSize() ?: (80 to 24)
 * ```
 *
 * @property command 要执行的命令数组
 * @property environment 环境变量
 * @property initialColumns 初始列数
 * @property initialRows 初始行数
 * @property directory 工作目录
 * @property console 是否使用console模式（仅Unix有效）
 */
class Pty4jPseudoTerminal @JvmOverloads constructor(
    private val command: Array<String>,
    private val environment: Map<String, String>? = null,
    private val initialColumns: Int = DEFAULT_COLUMNS,
    private val initialRows: Int = DEFAULT_ROWS,
    private val directory: String? = null,
    private val console: Boolean = true
) : NamedPipe {

    companion object {
        /** 默认终端列数 */
        const val DEFAULT_COLUMNS = 80

        /** 默认终端行数 */
        const val DEFAULT_ROWS = 24

        /** 最小终端列数 */
        const val MIN_COLUMNS = 1

        /** 最小终端行数 */
        const val MIN_ROWS = 1
    }

    // ==================== 核心资源 ====================

    /**
     * 底层的 PtyProcess 实例
     */
    private val ptyProcess: PtyProcess

    /**
     * 缓存的输入流实例
     */
    private val cachedInputStream: InputStream by lazy {
        ptyProcess.inputStream
    }

    /**
     * 缓存的输出流实例
     */
    private val cachedOutputStream: OutputStream by lazy {
        ptyProcess.outputStream
    }

    // ==================== 状态管理 ====================

    /**
     * 关闭状态
     */
    @Volatile
    private var closed = false

    // ==================== 初始化 ====================

    init {
        require(command.isNotEmpty()) { "Command must not be empty" }
        require(initialColumns >= MIN_COLUMNS) { "Columns must be >= $MIN_COLUMNS" }
        require(initialRows >= MIN_ROWS) { "Rows must be >= $MIN_ROWS" }

        // 创建 PtyProcess
        ptyProcess = try {
            // 准备环境变量
            val env = environment?.toMutableMap() ?: LinkedHashMap(System.getenv())

            // 确保设置 TERM 环境变量
            if (!env.containsKey("TERM")) {
                env["TERM"] = "xterm"
            }

            // 构建 PTY 进程
            val builder = PtyProcessBuilder(command)
                .setEnvironment(env)
                .setInitialColumns(initialColumns)
                .setInitialRows(initialRows)
                .setConsole(console)

            // 设置工作目录（如果指定）
            if (directory != null) {
                builder.setDirectory(directory)
            }

            // 启动进程
            builder.start()
        } catch (e: Exception) {
            throw IOException("Failed to create PTY with command: ${command.joinToString(" ")}", e)
        }
    }

    // ==================== NamedPipe 接口实现 ====================

    override val name: String
        get() = "Pty4j-${command.firstOrNull() ?: "unknown"}"

    override val isConnected: Boolean
        get() = !closed // PTY 创建时就已"连接"，直到关闭

    override val isClosed: Boolean
        get() = closed

    override val inputStream: InputStream
        get() {
            check(!closed) { "PTY '$name' is closed" }
            return cachedInputStream
        }

    override val outputStream: OutputStream
        get() {
            check(!closed) { "PTY '$name' is closed" }
            return cachedOutputStream
        }

    override fun waitForConnection(): Boolean {
        // PTY 在构造时就已启动进程，无需等待连接
        // 如果已关闭，返回 false；否则返回 true
        return !closed
    }

    @Synchronized
    override fun close() {
        if (closed) {
            return // 幂等性
        }

        closed = true

        // 关闭底层 PTY 进程
        try {
            ptyProcess.destroy()
        } catch (e: Exception) {
            // 忽略关闭异常
        }
    }

    // ==================== PTY 特有的功能（扩展 NamedPipe） ====================

    /**
     * 设置终端窗口大小
     *
     * @param columns 列数（字符宽度）
     * @param rows 行数（字符高度）
     * @throws IOException 当设置失败时抛出
     * @throws IllegalStateException 当PTY已关闭时抛出
     */
    @Throws(IOException::class, IllegalStateException::class)
    fun setWindowSize(columns: Int, rows: Int) {
        check(!closed) { "PTY '$name' is closed" }
        require(columns >= MIN_COLUMNS) { "Columns must be >= $MIN_COLUMNS" }
        require(rows >= MIN_ROWS) { "Rows must be >= $MIN_ROWS" }

        try {
            val winSize = WinSize(columns, rows)
            ptyProcess.winSize = winSize
        } catch (e: Exception) {
            throw IOException("Failed to set window size for PTY '$name'", e)
        }
    }


    /**
     * 获取从终端设备名称/路径
     *
     * @return 从终端的路径，如 "/dev/pts/0" 或进程标识
     */
    fun getSlaveName(): String {
        check(!closed) { "PTY is closed" }
        // PtyProcess 没有直接的 slaveName 属性，返回进程标识
        return try {
            "pty-pid-${ptyProcess.pid()}"
        } catch (e: Exception) {
            "pty-${System.identityHashCode(this)}"
        }
    }

    /**
     * 获取进程 PID
     *
     * @return 进程 PID，如果进程未启动或已结束则返回 -1
     */
    fun getPid(): Long {
        return try {
            ptyProcess.pid()
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * 检查进程是否存活
     *
     * @return true 如果进程仍在运行，false 否则
     */
    fun isAlive(): Boolean {
        return try {
            ptyProcess.isAlive
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 等待进程结束
     *
     * @return 进程退出码
     * @throws InterruptedException 如果等待被中断
     */
    @Throws(InterruptedException::class)
    fun waitFor(): Int {
        return ptyProcess.waitFor()
    }

    /**
     * 获取底层进程对象
     *
     * @return PtyProcess 实例
     */
    fun getProcess(): PtyProcess = ptyProcess
}