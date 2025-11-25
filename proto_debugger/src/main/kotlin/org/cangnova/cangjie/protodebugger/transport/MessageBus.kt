package org.cangnova.cangjie.protodebugger.transport


import com.google.protobuf.Message
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout


/**
 * 消息总线
 *
 * 封装Transport，提供更高级的消息路由和请求/响应管理
 */
class MessageBus(private val transport: Transport) : AutoCloseable {
    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30000L
    }

    /**
     * 发送请求并等待响应
     *
     * @param request 请求消息
     * @param responseClass 响应类型
     * @param timeoutMs 超时时间（毫秒）
     * @return 响应消息
     */
    suspend fun <T : Message> request(
        request: Message,
        responseClass: Class<T>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T {
        return try {
            withTimeout(timeoutMs) {
                transport.sendAndWait(request, responseClass, timeoutMs)
            }
        } catch (e: TimeoutCancellationException) {
            throw TransportTimeoutException("Request timed out after ${timeoutMs}ms")
        }
    }

    /**
     * 发送单向消息（不等待响应）
     *
     * @param message 消息
     */
    suspend fun send(message: Message) {
        transport.send(message)
    }

    /**
     * 获取广播事件流
     *
     * @return 广播事件的Flow
     */
    fun broadcasts(): Flow<Message> = transport.broadcasts()

    /**
     * 等待连接建立
     */
    suspend fun waitForConnection() {
        transport.waitForConnection()
    }

    /**
     * 是否已连接
     */
    val isConnected: Boolean
        get() = transport.isConnected

    override fun close() {
        transport.close()
    }
}
