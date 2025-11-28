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

package org.cangnova.cangjie.debugger.protobuf.pty

import com.pty4j.windows.cygwin.CygwinPTYInputStream
import com.pty4j.windows.cygwin.CygwinPTYOutputStream
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Windows 命名管道实现
 *
 * 基于 JNA (Java Native Access) 封装 Windows 命名���道 API，
 * 提供异步连接和双向通信功能。
 *
 * 主要特性：
 * - 支持异步连接（使用 Overlapped I/O）
 * - 线程安全的状态管理
 * - 支持可中断的连接等待
 * - 自动资源清理
 *
 * 使用场景：
 * - 调试器前端与后端进程间通信
 * - 本地进程间高性能数据传输
 * - 需要全双工通信的应用场景
 *
 * 线程安全：
 * - close() 方法是线程安全的，可从任意线程调用
 * - 多次调用 close() 是安全的（幂等性）
 * - 状态属性使用 @Volatile 保证可见性
 *
 * 实现说明：
 * - 使用 CygwinPTYInputStream/OutputStream 包装底层管道
 * - 支持 Overlapped I/O 实现异步操作
 * - 管道名称包含进程 ID 和唯一计数器，确保唯一性
 *
 * @property direction 管道方向（入站/出站/双工）
 * @property nameSuffix 管道名称后缀，用于标识管道用途
 *
 * @deprecated 请使用新的 PseudoTerminal 和 WindowsPty 替代，提供更完善的PTY功能
 * @see PseudoTerminal
 * @see WindowsPty
 * @see PtyFactory
 */

class WindowsPipe private constructor(
    private val direction: PipeDirection,
    private val nameSuffix: String
) : NamedPipe {

    companion object {
        /**
         * FILE_FLAG_OVERLAPPED 标志
         * 启用异步（重叠）I/O 操作
         */
        private const val FILE_FLAG_OVERLAPPED = 0x40000000

        /**
         * 进程内管道计数器
         * 用于生成唯一的管道名称
         */
        private val processCounter = AtomicInteger()

        /**
         * 创建出站管道（服务器向客户端发送数据）
         *
         * @param nameSuffix 管道名称后缀
         * @return 新创建的 WindowsPipe 实例
         * @throws IOException 当管道创建失败时抛出
         */
        @Throws(IOException::class)
        fun createOutboundPipe(nameSuffix: String): WindowsPipe =
            WindowsPipe(PipeDirection.Outbound, nameSuffix)

        /**
         * 创建入站管道（从客户端接收数据）
         *
         * @param nameSuffix 管道名称后缀
         * @return 新创建的 WindowsPipe 实��
         * @throws IOException 当管道创建失败时抛出
         */
        @Throws(IOException::class)
        fun createInboundPipe(nameSuffix: String): WindowsPipe =
            WindowsPipe(PipeDirection.Inbound, nameSuffix)
    }

    // ==================== 管道核心资源 ====================

    /**
     * 管道的完整名称
     * 格式: \\.\pipe\cangjie-debugger-{PID}-{Counter}-{Suffix}
     */
    private val pipeName: String

    /**
     * Windows 管道句柄
     */
    private val handle: WinNT.HANDLE

    /**
     * Pty4j 管道包装器
     */
    private val namedPipe: com.pty4j.windows.winpty.NamedPipe

    /**
     * 关闭事件句柄
     * 用于中断正在等待的连接操作
     */
    private var shutdownEvent: WinNT.HANDLE? = null

    // ==================== 状态管理 ====================

    /**
     * 连接状态
     * 当客户端成功连接后设置为 true
     */
    @Volatile
    private var connected = false

    /**
     * 关闭状态
     * 调用 close() 后设置为 true
     */
    @Volatile
    private var closed = false

    /**
     * 缓存的输入流实例
     * 保证多次调用 inputStream 返回同一实例
     */
    private val cachedInputStream: InputStream by lazy {
        CygwinPTYInputStream(namedPipe)
    }

    /**
     * 缓存的输出流实例
     * 保证多次调用 outputStream 返回同一实例
     */
    private val cachedOutputStream: OutputStream by lazy {
        CygwinPTYOutputStream(namedPipe)
    }

    // ==================== 初始化 ====================

    init {
        // 生成唯一的管道名称
        pipeName =
            "\\\\.\\pipe\\cangjie-debugger-${Kernel32.INSTANCE.GetCurrentProcessId()}-${processCounter.getAndIncrement()}-$nameSuffix"

        // 设置打开模式（方向 + 异步标志）
        val openMode = direction.flag or FILE_FLAG_OVERLAPPED

        // 创建命名管道
        handle = Kernel32.INSTANCE.CreateNamedPipe(
            pipeName,           // 管道名称
            openMode,           // 打开模式
            0,                  // 管道模式（默认字节流模式）
            1,                  // 最大实例数
            0,                  // 输出缓冲区大小（系统默认）
            0,                  // 输入缓冲区大小（系统默认）
            0,                  // 超时时间（使用默认）
            WinBase.SECURITY_ATTRIBUTES() // 安全属性
        )

        if (handle == WinBase.INVALID_HANDLE_VALUE) {
            val errorCode = Kernel32.INSTANCE.GetLastError()
            throw IOException("Failed to create named pipe '$pipeName', error code: $errorCode")
        }

        // 包装为 Pty4j 管道
        namedPipe = com.pty4j.windows.winpty.NamedPipe(handle, false)

        // 创建关闭事件（用于中断连接等待）
        shutdownEvent = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
        if (shutdownEvent == null) {
            // 如果事件创建失败，清理已创建的管道
            try {
                namedPipe.close()
            } catch (e: Exception) {
                // 忽略清理过程中的异常
            }
            throw IOException("Failed to create shutdown event for pipe '$pipeName'")
        }
    }

    // ==================== NamedPipe 接口实现 ====================

    override val name: String
        get() = pipeName

    override val isConnected: Boolean
        get() = connected && !closed

    override val isClosed: Boolean
        get() = closed

    override val inputStream: InputStream
        get() {
            check(!isClosed) { "Pipe '$pipeName' is closed" }
            // 入站管道（Inbound）：inputStream 可在连接前访问（服务器接收数据的流）
            // 出站管道（Outbound）：inputStream 需要连接后访问（从客户端读取的流）
            // 双工管道（Duplex）：inputStream 需要连接后访问
            if (direction != PipeDirection.Inbound) {
                check(isConnected) { "Pipe '$pipeName' is not connected" }
            }
            return cachedInputStream
        }

    override val outputStream: OutputStream
        get() {
            check(!isClosed) { "Pipe '$pipeName' is closed" }
            // 出站管道（Outbound）：outputStream 可在连接前访问（服务器发送数据的流）
            // 入站管道（Inbound）：outputStream 需要连接后访问（向客户端写入的流）
            // 双工管道（Duplex）：outputStream 需要连接后访问
            if (direction != PipeDirection.Outbound) {
                check(isConnected) { "Pipe '$pipeName' is not connected" }
            }
            return cachedOutputStream
        }

    /**
     * 等待客户端连接
     *
     * 使用异步 I/O（Overlapped）方式等待客户端连接。
     * 支持通过调用 close() 方法中断等待。
     *
     * 实现细节：
     * - 创建连接事件用于异步连接完成通知
     * - 使用 WaitForMultipleObjects 同时等待连接和关闭事件
     * - 连接成功后更新 connected 状态
     *
     * @return true 如果客户端成功连接，false 如果被 close() 中断
     * @throws IOException 当连接过程发生错误时抛出
     * @throws IllegalStateException 当管道已连接或已关闭时抛出
     */
    @Throws(IOException::class, IllegalStateException::class)
    override fun waitForConnection(): Boolean {
        check(!closed) { "Pipe '$pipeName' is already closed" }
        check(!connected) { "Pipe '$pipeName' is already connected" }

        val connectEvent = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
            ?: throw IOException("Failed to create connect event for pipe '$pipeName'")

        val overlapped = WinBase.OVERLAPPED()
        overlapped.hEvent = connectEvent

        val waitHandles = arrayOf(connectEvent, shutdownEvent)
        try {
            // 开始异步连接
            val immediateConnect = Kernel32.INSTANCE.ConnectNamedPipe(handle, overlapped)
            if (!immediateConnect) {
                val err = Kernel32.INSTANCE.GetLastError()
                // ERROR_PIPE_CONNECTED: 客户端已连接（在 ConnectNamedPipe 调用前）
                // ERROR_IO_PENDING: 异步操作已启动
                if (err != WinError.ERROR_PIPE_CONNECTED && err != WinError.ERROR_IO_PENDING) {
                    throw IOException("ConnectNamedPipe failed for pipe '$pipeName', error code: $err")
                }
                // 如果已连接，直接设置状态并返回
                if (err == WinError.ERROR_PIPE_CONNECTED) {
                    connected = true
                    return true
                }
            }

            // 等待连接完成或关闭信号
            val waitResult = Kernel32.INSTANCE.WaitForMultipleObjects(
                waitHandles.size,
                waitHandles,
                false,              // waitAll = false (任意一个事件触发即返回)
                WinBase.INFINITE    // 无限等待
            )

            return when (waitResult) {
                WinBase.WAIT_OBJECT_0 -> {
                    // 连接事件触发 - 客户端已连接
                    connected = true
                    true
                }

                WinBase.WAIT_OBJECT_0 + 1 -> {
                    // 关闭事件触发 - close() 被调用
                    false
                }

                else -> {
                    // 其他错误
                    val err = Kernel32.INSTANCE.GetLastError()
                    throw IOException("WaitForMultipleObjects failed for pipe '$pipeName', error code: $err")
                }
            }
        } finally {
            // 清理连接事件
            Kernel32.INSTANCE.CloseHandle(connectEvent)
        }
    }

    /**
     * 关闭命名管道
     *
     * 线程安全的关闭操作：
     * - 支持从任意线程调用
     * - 多次调用是安全的（幂等性）
     * - 会中断正在等待连接的 waitForConnection() 调用
     *
     * 清理步��：
     * 1. 设置关闭事件，中断等待的连接操作
     * 2. 更新关闭状态
     * 3. 关闭底层管道资源
     * 4. 清理事件句柄
     *
     * @throws IOException 当关闭过程中发生错误时抛出
     */
    @Throws(IOException::class)
    @Synchronized
    override fun close() {
        if (closed) {
            return // 已关闭，直接返回（幂等性）
        }

        closed = true

        // 触发关闭事件，中断等待的 waitForConnection()
        shutdownEvent?.let { event ->
            Kernel32.INSTANCE.SetEvent(event)
        }

        // 关闭底层管道
        try {
            namedPipe.close()
        } finally {
            // 清理关闭事件句柄
            shutdownEvent?.let { event ->
                Kernel32.INSTANCE.CloseHandle(event)
                shutdownEvent = null
            }
        }
    }
}

/**
 * 管道方向枚举
 *
 * 定义命名管道的数据流方向。
 *
 * @property flag Windows API 标志值
 */
enum class PipeDirection(val flag: Int) {
    /**
     * 入站管道
     * 服务器从客户端接收数据
     */
    Inbound(WinNT.PIPE_ACCESS_INBOUND),

    /**
     * 出站管道
     * 服务器向客户端发送数据
     */
    Outbound(WinNT.PIPE_ACCESS_OUTBOUND),

    /**
     * 双工管道
     * 服务器可以同时发送和接收数据
     */
    Duplex(WinNT.PIPE_ACCESS_DUPLEX)
}
