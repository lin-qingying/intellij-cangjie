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

package org.cangnova.cangjie.debugger.dap.connection

import com.intellij.openapi.Disposable
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer

/**
 * 连接管理器接口
 *
 * 负责管理DAP连接的生命周期，提供连接复用、健康监控和故障恢复
 */
interface ConnectionManager : Disposable {

    /**
     * 获取或创建到DAP服务器的连接
     *
     * @param config 连接配置
     * @return 连接结果，成功返回ManagedConnection，失败返回错误
     */
    suspend fun acquireConnection(config: ConnectionConfig): Result<ManagedConnection>

    /**
     * 释放连接
     *
     * 连接会被放回连接池复用，而不是立即关闭
     *
     * @param connection 要释放的连接
     */
    suspend fun releaseConnection(connection: ManagedConnection)

    /**
     * 关闭并移除连接
     *
     * @param connectionId 连接ID
     */
    suspend fun closeConnection(connectionId: String): Result<Unit>

    /**
     * 获取连接状态
     *
     * @param connectionId 连接ID
     * @return 连接状态流
     */
    fun getConnectionState(connectionId: String): StateFlow<ConnectionState>?

    /**
     * 获取所有活跃连接
     */
    fun getActiveConnections(): List<ManagedConnection>
}

/**
 * 托管的连接对象
 *
 * 封装了原始连接，提供额外的管理功能
 */
interface ManagedConnection : Disposable {

    /**
     * 连接ID
     */
    val id: String

    /**
     * 连接配置
     */
    val config: ConnectionConfig

    /**
     * 连接状态
     */
    val state: StateFlow<ConnectionState>

    /**
     * DAP协议服务器接口
     */
    val server: IDebugProtocolServer

    /**
     * 检查连接是否健康
     */
    suspend fun isHealthy(): Boolean

    /**
     * 最后活跃时间
     */
    val lastActiveTime: Long
}

/**
 * 连接配置
 */
data class ConnectionConfig(
    /**
     * 服务器主机
     */
    val host: String = "localhost",

    /**
     * 服务器端口
     */
    val port: Int,

    /**
     * 连接超时（毫秒）
     */
    val connectionTimeoutMs: Long = 5000,

    /**
     * 最大重试次数
     */
    val maxRetries: Int = 3,

    /**
     * 重试延迟（毫秒）
     */
    val retryDelayMs: Long = 500,

    /**
     * 是否启用健康检查
     */
    val enableHealthCheck: Boolean = true,

    /**
     * 健康检查间隔（毫秒）
     */
    val healthCheckIntervalMs: Long = 10000,

    /**
     * 读取超时（毫秒）
     */
    val readTimeoutMs: Long = 30000
)

/**
 * 连接状态
 */
sealed class ConnectionState {
    /**
     * 断开连接
     */
    object Disconnected : ConnectionState()

    /**
     * 连接中
     */
    object Connecting : ConnectionState()

    /**
     * 已连接
     */
    object Connected : ConnectionState()

    /**
     * 重连中
     */
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()

    /**
     * 连接失败
     */
    data class Failed(val error: Throwable) : ConnectionState()

    /**
     * 连接已关闭
     */
    object Closed : ConnectionState()
}