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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * 默认连接管理器
 *
 * 作为Application级别的服务注册，提供连接池和健康监控
 */
@Service(Service.Level.APP)
class DefaultConnectionManager : ConnectionManager {

    companion object {
        private val LOG = Logger.getInstance(DefaultConnectionManager::class.java)
        private const val MAX_IDLE_CONNECTIONS = 5
        private const val CONNECTION_IDLE_TIMEOUT_MS = 5 * 60 * 1000L // 5分钟

        fun getInstance(): DefaultConnectionManager {
            return ApplicationManager.getApplication().getService(DefaultConnectionManager::class.java)
        }
    }

    private val connections = ConcurrentHashMap<String, AsyncDapConnection>()
    private val connectionStates = ConcurrentHashMap<String, MutableStateFlow<ConnectionState>>()
    private val idleConnections = ConcurrentHashMap<String, PooledConnection>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val healthMonitor = HealthMonitor(
        checkInterval = 10.seconds,
        unhealthyThreshold = 3,
        onUnhealthy = { connection ->
            handleUnhealthyConnection(connection)
        }
    )

    init {
        // 启动健康监控
        if (connections.isNotEmpty()) {
            healthMonitor.start()
        }

        // 启动空闲连接清理
        scope.launch {
            cleanupIdleConnections()
        }
    }

    override suspend fun acquireConnection(config: ConnectionConfig): Result<ManagedConnection> = mutex.withLock {
        try {
            LOG.info("Acquiring connection for ${config.host}:${config.port}")

            // 尝试复用空闲连接
            val reuseKey = connectionKey(config)
            idleConnections.remove(reuseKey)?.let { pooled ->
                if (pooled.connection.isHealthy()) {
                    LOG.info("Reusing idle connection ${pooled.connection.id}")
                    connections[pooled.connection.id] = pooled.connection
                    healthMonitor.monitor(pooled.connection)
                    return Result.success(pooled.connection)
                } else {
                    LOG.warn("Idle connection ${pooled.connection.id} is unhealthy, closing")
                    pooled.connection.close()
                }
            }

            // 创建新连接
            val connection = AsyncDapConnection(config = config)
            connections[connection.id] = connection
            connectionStates[connection.id] = MutableStateFlow(ConnectionState.Disconnected)

            // 监听连接状态变化
            scope.launch {
                connection.state.collect { state ->
                    connectionStates[connection.id]?.value = state
                }
            }

            LOG.info("Created new connection ${connection.id}")
            Result.success(connection as ManagedConnection)
        } catch (e: Exception) {
            LOG.error("Failed to acquire connection", e)
            Result.failure(e)
        }
    }

    override suspend fun releaseConnection(connection: ManagedConnection) = mutex.withLock {
        if (connection !is AsyncDapConnection) {
            LOG.warn("Cannot release non-AsyncDapConnection")
            return@withLock
        }

        try {
            // 检查连接是否健康
            if (!connection.isHealthy()) {
                LOG.info("Released connection ${connection.id} is unhealthy, closing")
                closeConnectionInternal(connection.id)
                return@withLock
            }

            // 检查是否达到最大空闲连接数
            if (idleConnections.size >= MAX_IDLE_CONNECTIONS) {
                LOG.info("Max idle connections reached, closing ${connection.id}")
                closeConnectionInternal(connection.id)
                return@withLock
            }

            // 放入空闲池
            val key = connectionKey(connection.config)
            idleConnections[key] = PooledConnection(
                connection = connection,
                idleSince = System.currentTimeMillis()
            )

            // 从活跃连接中移除
            connections.remove(connection.id)
            healthMonitor.unmonitor(connection.id)

            LOG.info("Connection ${connection.id} released to idle pool")
        } catch (e: Exception) {
            LOG.error("Error releasing connection ${connection.id}", e)
        }
    }

    override suspend fun closeConnection(connectionId: String): Result<Unit> = mutex.withLock {
        closeConnectionInternal(connectionId)
        Result.success(Unit)
    }

    override fun getConnectionState(connectionId: String): StateFlow<ConnectionState>? {
        return connectionStates[connectionId]
    }

    override fun getActiveConnections(): List<ManagedConnection> {
        return connections.values.toList()
    }

    /**
     * 处理不健康的连接
     */
    private suspend fun handleUnhealthyConnection(connection: ManagedConnection) {
        LOG.warn("Handling unhealthy connection ${connection.id}")
        closeConnection(connection.id)
    }

    /**
     * 关闭连接（内部方法）
     */
    private suspend fun closeConnectionInternal(connectionId: String) {
        val connection = connections.remove(connectionId) as? AsyncDapConnection
            ?: idleConnections.values.find { it.connection.id == connectionId }?.connection

        connection?.let {
            LOG.info("Closing connection $connectionId")
            healthMonitor.unmonitor(connectionId)
            connectionStates.remove(connectionId)
            it.close()
        }
    }

    /**
     * 清理空闲连接
     */
    private suspend fun cleanupIdleConnections() {
        while (scope.isActive) {
            try {
                delay(1.minutes)

                val now = System.currentTimeMillis()
                val toRemove = mutableListOf<String>()

                idleConnections.forEach { (key, pooled) ->
                    if (now - pooled.idleSince > CONNECTION_IDLE_TIMEOUT_MS) {
                        toRemove.add(key)
                    }
                }

                if (toRemove.isNotEmpty()) {
                    LOG.info("Cleaning up ${toRemove.size} idle connections")
                    mutex.withLock {
                        toRemove.forEach { key ->
                            idleConnections.remove(key)?.let { pooled ->
                                pooled.connection.close()
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LOG.error("Error during idle connection cleanup", e)
            }
        }
    }

    /**
     * 连接键（用于连接复用）
     */
    private fun connectionKey(config: ConnectionConfig): String {
        return "${config.host}:${config.port}"
    }

    override fun dispose() {
        LOG.info("Disposing ConnectionManager")
        scope.cancel()
        healthMonitor.dispose()

        // 异步关闭所有连接
        ApplicationManager.getApplication().executeOnPooledThread {
            runBlocking {
                mutex.withLock {
                    LOG.info("Closing ${connections.size} active connections and ${idleConnections.size} idle connections")

                    connections.values.forEach { it.close() }
                    connections.clear()

                    idleConnections.values.forEach { it.connection.close() }
                    idleConnections.clear()

                    connectionStates.clear()
                }
            }
        }
    }
}

/**
 * 池化的连接
 */
private data class PooledConnection(
    val connection: AsyncDapConnection,
    val idleSince: Long
)

/**
 * 扩展函数 - 为AsyncDapConnection包装client并连接
 */
suspend fun AsyncDapConnection.connectWithClient(client: IDebugProtocolClient): Result<ManagedConnection> {
    return connect(client).map { this }
}