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

package org.cangnova.cangjie.debugger.protobuf.transport


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
