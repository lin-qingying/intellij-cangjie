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
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 连接健康监控器
 *
 * 定期检查连接健康状态，自动处理不健康的连接
 */
class HealthMonitor(
    private val checkInterval: Duration = 10.seconds,
    private val unhealthyThreshold: Int = 3,
    private val onUnhealthy: suspend (ManagedConnection) -> Unit
) : Disposable {

    companion object {
        private val LOG = Logger.getInstance(HealthMonitor::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monitoredConnections = mutableMapOf<String, MonitoredConnection>()
    private var monitorJob: Job? = null

    /**
     * 开始监控
     */
    fun start() {
        if (monitorJob?.isActive == true) {
            LOG.warn("Health monitor is already running")
            return
        }

        LOG.info("Starting health monitor (interval=${checkInterval.inWholeSeconds}s)")

        monitorJob = scope.launch {
            while (isActive) {
                try {
                    performHealthCheck()
                    delay(checkInterval)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LOG.error("Error during health check", e)
                }
            }
        }
    }

    /**
     * 停止监控
     */
    fun stop() {
        LOG.info("Stopping health monitor")
        monitorJob?.cancel()
        monitorJob = null
    }

    /**
     * 添加连接到监控
     */
    fun monitor(connection: ManagedConnection) {
        monitoredConnections[connection.id] = MonitoredConnection(
            connection = connection,
            failureCount = 0,
            lastCheckTime = System.currentTimeMillis()
        )
        LOG.debug("Added connection ${connection.id} to monitoring")
    }

    /**
     * 移除连接监控
     */
    fun unmonitor(connectionId: String) {
        monitoredConnections.remove(connectionId)
        LOG.debug("Removed connection $connectionId from monitoring")
    }

    /**
     * 执行健康检查
     */
    private suspend fun performHealthCheck() {
        val connections = monitoredConnections.values.toList()
        if (connections.isEmpty()) {
            return
        }

        LOG.debug("Performing health check on ${connections.size} connections")

        connections.forEach { monitored ->
            try {
                val isHealthy = monitored.connection.isHealthy()
                val currentTime = System.currentTimeMillis()

                if (isHealthy) {
                    // 连接健康，重置失败计数
                    if (monitored.failureCount > 0) {
                        LOG.info("Connection ${monitored.connection.id} recovered")
                    }
                    monitoredConnections[monitored.connection.id] = monitored.copy(
                        failureCount = 0,
                        lastCheckTime = currentTime
                    )
                } else {
                    // 连接不健康，增加失败计数
                    val newFailureCount = monitored.failureCount + 1
                    LOG.warn("Connection ${monitored.connection.id} health check failed ($newFailureCount/$unhealthyThreshold)")

                    monitoredConnections[monitored.connection.id] = monitored.copy(
                        failureCount = newFailureCount,
                        lastCheckTime = currentTime
                    )

                    // 达到阈值，触发回调
                    if (newFailureCount >= unhealthyThreshold) {
                        LOG.error("Connection ${monitored.connection.id} is unhealthy, triggering callback")
                        onUnhealthy(monitored.connection)
                        // 从监控中移除
                        unmonitor(monitored.connection.id)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error checking health for connection ${monitored.connection.id}", e)
            }
        }
    }

    /**
     * 获取监控统计
     */
    fun getStatistics(): HealthStatistics {
        val connections = monitoredConnections.values
        return HealthStatistics(
            totalMonitored = connections.size,
            healthyCount = connections.count { it.failureCount == 0 },
            degradedCount = connections.count { it.failureCount in 1 until unhealthyThreshold },
            unhealthyCount = connections.count { it.failureCount >= unhealthyThreshold }
        )
    }

    override fun dispose() {
        LOG.info("Disposing health monitor")
        stop()
        scope.cancel()
        monitoredConnections.clear()
    }
}

/**
 * 被监控的连接
 */
private data class MonitoredConnection(
    val connection: ManagedConnection,
    val failureCount: Int,
    val lastCheckTime: Long
)

/**
 * 健康统计
 */
data class HealthStatistics(
    val totalMonitored: Int,
    val healthyCount: Int,
    val degradedCount: Int,
    val unhealthyCount: Int
)