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

package org.cangnova.cangjie.debugger.dap.server

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 端口分配器
 *
 * 提供线程安全的端口分配和释放，防止TOCTOU竞态条件
 */
class PortAllocator(
    private val defaultStartPort: Int = 58920,
    private val defaultEndPort: Int = 58950,
    private val leaseTimeout: Duration = 5.minutes
) {

    companion object {
        private val LOG = Logger.getInstance(PortAllocator::class.java)
        private val INSTANCE = PortAllocator()

        fun getInstance(): PortAllocator = INSTANCE
    }

    private val mutex = Mutex()
    private val leasedPorts = ConcurrentHashMap<Int, PortLease>()

    /**
     * 分配可用端口
     *
     * @param startPort 起始端口（包含）
     * @param endPort 结束端口（包含）
     * @return 成功返回端口租约，失败返回错误
     */
    suspend fun allocatePort(
        startPort: Int = defaultStartPort,
        endPort: Int = defaultEndPort
    ): Result<PortLease> = mutex.withLock {
        // 清理过期租约
        cleanupExpiredLeases()

        // 查找可用端口
        for (port in startPort..endPort) {
            if (leasedPorts.containsKey(port)) {
                continue
            }

            if (tryBindPort(port)) {
                val lease = PortLease(
                    port = port,
                    allocatedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + leaseTimeout.inWholeMilliseconds,
                    onRelease = { releasePort(port) }
                )
                leasedPorts[port] = lease
                LOG.info("Allocated port $port")
                return Result.success(lease)
            }
        }

        Result.failure(NoAvailablePortException("No available port in range $startPort-$endPort"))
    }

    /**
     * 释放端口
     *
     * @param port 端口号
     */
    suspend fun releasePort(port: Int) = mutex.withLock {
        leasedPorts.remove(port)?.let {
            LOG.info("Released port $port")
        }
    }

    /**
     * 续约端口
     *
     * @param port 端口号
     * @return 是否成功续约
     */
    suspend fun renewLease(port: Int): Boolean = mutex.withLock {
        leasedPorts[port]?.let { lease ->
            val renewed = lease.copy(
                expiresAt = System.currentTimeMillis() + leaseTimeout.inWholeMilliseconds
            )
            leasedPorts[port] = renewed
            LOG.debug("Renewed lease for port $port")
            true
        } ?: false
    }

    /**
     * 获取所有已租用的端口
     */
    fun getLeasedPorts(): List<Int> = leasedPorts.keys.toList()

    /**
     * 清理过期租约
     */
    private fun cleanupExpiredLeases() {
        val now = System.currentTimeMillis()
        leasedPorts.entries.removeIf { (port, lease) ->
            if (lease.expiresAt < now) {
                LOG.warn("Port $port lease expired, cleaning up")
                true
            } else {
                false
            }
        }
    }

    /**
     * 尝试绑定端口以检查可用性
     *
     * 使用try-with-resources确保ServerSocket立即关闭
     *
     * @param port 端口号
     * @return 端口是否可用
     */
    private fun tryBindPort(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: IOException) {
            LOG.debug("Port $port is not available: ${e.message}")
            false
        }
    }

    /**
     * 释放所有端口
     */
    suspend fun releaseAll() = mutex.withLock {
        val count = leasedPorts.size
        leasedPorts.clear()
        LOG.info("Released all $count ports")
    }
}

/**
 * 端口租约
 *
 * 表示对端口的临时占用
 */
data class PortLease(
    val port: Int,
    val allocatedAt: Long,
    val expiresAt: Long,
    private val onRelease: suspend () -> Unit
) {
    /**
     * 释放租约
     */
    suspend fun release() {
        onRelease()
    }

    /**
     * 检查租约是否过期
     */
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    /**
     * 剩余时间（毫秒）
     */
    fun remainingTimeMs(): Long = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)

    override fun toString(): String = "PortLease(port=$port, allocatedAt=$allocatedAt, expiresAt=$expiresAt)"
}

/**
 * 无可用端口异常
 */
class NoAvailablePortException(message: String) : Exception(message)