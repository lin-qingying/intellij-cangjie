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
import kotlinx.coroutines.flow.Flow


/**
 * 传输层抽象接口
 *
 * 统一封装Socket和命名管道等不同传输方式，提供统一的消息收发接口
 */
interface Transport : AutoCloseable {
    /**
     * 发送消息并等待响应
     *
     * @param message 要发送的protobuf消息
     * @param responseClass 期望的响应类型
     * @param timeoutMs 超时时间（毫秒），0表示使用默认超时
     * @return 响应消息
     * @throws TransportException 传输失败
     * @throws TransportTimeoutException 超时
     */
    suspend fun <T : Message> sendAndWait(
        message: Message,
        responseClass: Class<T>,
        timeoutMs: Long = 0L
    ): T

    /**
     * 发送消息但不等待响应
     *
     * @param message 要发送的protobuf消息
     * @throws TransportException 传输失败
     */
    suspend fun send(message: Message)

    /**
     * 获取广播事件流
     *
     * @return 广播事件的Flow
     */
    fun broadcasts(): Flow<Message>

    /**
     * 传输层是否已连接
     */
    val isConnected: Boolean

    /**
     * 等待传输层连接建立
     */
    suspend fun waitForConnection()
}
