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

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * 命名管道接口
 *
 * 该接口定义了命名管道（Named Pipe）的标准契约，用于进程间通信（IPC）。
 * 命名管道是一种特殊的文件，允许不相关的进程之间进行数据交换。
 * 在调试器中，命名管道主要用于调试器前端与后端之间的通信。
 *
 * 使用场景：
 * - 调试器与调试服务器之间的通信
 * - 进程间数据传输和命令传递
 * - 跨平台进程间通信的抽象层
 * - 调试器DAP（Debug Adapter Protocol）通信
 *
 * 主要功���：
 * - 提供双向数据流的抽象接口
 * - 支持进程间的同步和异步通信
 * - 处理管道的创建、连接和关闭
 * - 提供跨平台的命名管道实现
 *
 * 实现说明：
 * - Windows平台通常使用实际的命名管道
 * - Unix/Linux平台可以使用FIFO文件或Unix域套接字
 * - 不同平台的具体实现可能有所不同
 *
 * 线程安全：
 * - 实现类应确保 close() 方法是线程安全的
 * - waitForConnection() 方法应该可以被其他线程中断
 *
 * @deprecated 请使用新的 PseudoTerminal 接口替代，提供更完善的PTY功能和跨平台支持
 * @see PseudoTerminal
 * @see PtyFactory
 */

interface NamedPipe : Closeable {
    /**
     * 获取管道的名称
     *
     * 返回命名管道的唯一标识名称。
     * 这个名称用于识别和连接特定的管道实例。
     *
     * 使用场景：
     * - 管道的识别和寻址
     * - 调试日志和���误报告
     * - 管道管理和监控
     *
     * @return 管道的名称字符串
     */
    val name: String

    /**
     * 检查管道是否已连接
     *
     * @return true 如果管道已成功连接，false 否则
     */
    val isConnected: Boolean

    /**
     * 检查管道是否已关闭
     *
     * @return true 如果管道已关闭，false 否则
     */
    val isClosed: Boolean

    /**
     * 获取输入流
     *
     * 返回用于从管道读取数据的输入流。
     * 通过这个流，可以接收来自另一端进程的数据。
     *
     * 使用场景：
     * - 接收调试器的响应消息
     * - 读取调试输出和状态信息
     * - 处理异步数据接收
     *
     * 注意：
     * - 只有在管道已连接后才能获取输入流
     * - 多次调用应返回同一个实例
     *
     * @return 管道的输入流
     * @throws IOException 当无法创建或访问输入流时抛出
     * @throws IllegalStateException 当管道未连接或已关闭时抛出
     */
    @get:Throws(IOException::class, IllegalStateException::class)
    val inputStream: InputStream

    /**
     * 获取输出流
     *
     * 返回用于向管道写入数据的输出流。
     * 通过这个流，可以向另一端进程发送数据。
     *
     * 使用场景：
     * - 发送调试器命令
     * - 传输配置和控制信息
     * - 处理请求数据的发送
     *
     * 注意：
     * - 只有在管道已连接后才能获取输出流
     * - 多次调用应返回同一个实例
     *
     * @return 管道的输出流
     * @throws IOException 当无法创建或访问输出流时抛出
     * @throws IllegalStateException 当管道未连接或已关闭时抛出
     */
    @get:Throws(IOException::class, IllegalStateException::class)
    val outputStream: OutputStream

    /**
     * 等待客户端连接
     *
     * 阻塞当前线程直到客户端连接或操作被中断。
     * 此方法支持异步连接，可以被其他线程通过调用 close() 方法中断。
     *
     * 使用场景：
     * - 服务器端等待调试器后端连接
     * - 建立进程间通信通道
     * - 初始化调试会话
     *
     * 行为说明：
     * - 成功连接后，管道状态变为已连接
     * - 如果管道被关闭，此方法应立即返回 false
     * - 此方法可以被其他线程调用 close() 中断
     *
     * @return true 如果成功连接，false 如果操作被中断或管道已关闭
     * @throws IOException 当连接过程中发生错误时抛出(不包括正常中断)
     * @throws IllegalStateException 当管道已经连接时抛出
     */
    @Throws(IOException::class, IllegalStateException::class)
    fun waitForConnection(): Boolean

    /**
     * 关闭命名管道
     *
     * 关闭管道的所有流并释放相关资源。
     * 关闭后，管道将不再可用于数据传输。
     *
     * 使用场景：
     * - 调试会话结束时的清理工作
     * - 异常情况的资源释放
     * - 管道连接的重置
     * - 中断正在等待的 waitForConnection() 调用
     *
     * 线程安全：
     * - 此方法必须是线程安全的
     * - 多次调用应该是安全的(幂等性)
     * - 应该能够中断正在进行的 waitForConnection() 调用
     *
     * @throws IOException 当关闭过程中发生错误时抛出
     */
    @Throws(IOException::class)
    override fun close()
}