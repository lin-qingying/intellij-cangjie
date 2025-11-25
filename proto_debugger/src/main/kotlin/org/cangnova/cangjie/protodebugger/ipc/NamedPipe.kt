package org.cangnova.cangjie.protodebugger.ipc

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
 * 主要功能：
 * - 提供双向数据流的抽象接口
 * - 支持进程间的同步和异步通信
 * - 处理管道的创建、连接和关闭
 * - 提供跨平台的命名管道实现
 *
 * 实现说明：
 * - Windows平台通常使用实际的命名管道
 * - Unix/Linux平台可以使用FIFO文件或Unix域套接字
 * - 不同平台的具体实现可能有所不同
 */
interface NamedPipe {
    /**
     * 获取管道的名称
     *
     * 返回命名管道的唯一标识名称。
     * 这个名称用于识别和连接特定的管道实例。
     *
     * 使用场景：
     * - 管道的识别和寻址
     * - 调试日志和错误报告
     * - 管道管理和监控
     *
     * @return 管道的名称字符串
     */
    val name: String

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
     * @return 管道的输入流
     * @throws IOException 当无法创建或访问输入流时抛出
     */
    @get:Throws(IOException::class)
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
     * @return 管道的输出流
     * @throws IOException 当无法创建或访问输出流时抛出
     */
    @get:Throws(IOException::class)
    val outputStream: OutputStream

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
     *
     * @throws IOException 当关闭过程中发生错误时抛出
     */
    @Throws(IOException::class)
    fun close()



    @Throws(IOException::class)
    fun waitForConnection(): Boolean
}

