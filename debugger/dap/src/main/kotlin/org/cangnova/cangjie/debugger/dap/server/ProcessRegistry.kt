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
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 进程注册表
 *
 * 全局注册表，跟踪所有启动的DAP服务器进程，防止进程孤儿化
 * 在IDE关闭时自动清理所有注册的进程
 */
object ProcessRegistry {

    private val LOG = Logger.getInstance(ProcessRegistry::class.java)

    private val processes = ConcurrentHashMap<String, RegisteredProcess>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 注册JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                LOG.info("JVM shutting down, cleaning up ${processes.size} registered processes")
                forceKillAll(timeout = 5.seconds)
            }
        })
    }

    /**
     * 注册进程
     *
     * @param id 进程ID
     * @param process Process对象
     * @param pid 进程PID
     * @param metadata 元数据
     */
    fun register(
        id: String,
        process: Process,
        pid: Long? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val registered = RegisteredProcess(
            id = id,
            process = process,
            pid = pid,
            registeredAt = System.currentTimeMillis(),
            metadata = metadata
        )

        processes[id] = registered
        LOG.info("Registered process: id=$id, pid=$pid")

        // 启动进程监控
        scope.launch {
            monitorProcess(registered)
        }
    }

    /**
     * 注销进程
     *
     * @param id 进程ID
     */
    fun unregister(id: String) {
        processes.remove(id)?.let {
            LOG.info("Unregistered process: id=$id, pid=${it.pid}")
        }
    }

    /**
     * 获取进程
     *
     * @param id 进程ID
     */
    fun getProcess(id: String): RegisteredProcess? = processes[id]

    /**
     * 获取所有进程
     */
    fun getAllProcesses(): List<RegisteredProcess> = processes.values.toList()

    /**
     * 获取所有活跃进程
     */
    fun getAliveProcesses(): List<RegisteredProcess> =
        processes.values.filter { it.process.isAlive }.toList()

    /**
     * 检查进程是否注册
     */
    fun isRegistered(id: String): Boolean = processes.containsKey(id)

    /**
     * 停止进程
     *
     * @param id 进程ID
     * @param gracefulTimeoutMs 优雅停止超时
     * @return 是否成功停止
     */
    suspend fun stopProcess(id: String, gracefulTimeoutMs: Long = 3000): Boolean {
        val registered = processes[id] ?: return false
        return stopProcessInternal(registered, gracefulTimeoutMs)
    }

    /**
     * 强制终止所有进程
     *
     * @param timeout 总超时时间
     */
    suspend fun forceKillAll(timeout: Duration = 10.seconds) = withContext(Dispatchers.IO) {
        val processList = processes.values.toList()
        if (processList.isEmpty()) {
            return@withContext
        }

        LOG.warn("Force killing ${processList.size} processes")

        withTimeoutOrNull(timeout) {
            processList.map { registered ->
                async {
                    try {
                        stopProcessInternal(registered, gracefulTimeoutMs = 1000)
                    } catch (e: Exception) {
                        LOG.error("Failed to stop process ${registered.id}", e)
                        false
                    }
                }
            }.awaitAll()
        }

        // 清理注册表
        processes.clear()
        LOG.info("All processes cleaned up")
    }

    /**
     * 监控进程
     *
     * 当进程退出时自动注销
     */
    private suspend fun monitorProcess(registered: RegisteredProcess) = withContext(Dispatchers.IO) {
        try {
            // 等待进程退出
            registered.process.waitFor()

            val exitCode = registered.process.exitValue()
            LOG.info("Process ${registered.id} exited with code $exitCode")

            // 自动注销
            unregister(registered.id)
        } catch (e: InterruptedException) {
            LOG.debug("Process monitoring interrupted for ${registered.id}")
        } catch (e: Exception) {
            LOG.error("Error monitoring process ${registered.id}", e)
        }
    }

    /**
     * 停止进程（内部实现）
     */
    private suspend fun stopProcessInternal(
        registered: RegisteredProcess,
        gracefulTimeoutMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val process = registered.process

        if (!process.isAlive) {
            unregister(registered.id)
            return@withContext true
        }

        try {
            // 尝试优雅停止
            LOG.info("Gracefully stopping process ${registered.id} (pid=${registered.pid})")
            process.destroy()

            // 等待进程退出
            val exited = withTimeoutOrNull(gracefulTimeoutMs) {
                process.waitFor()
                true
            } ?: false

            if (exited) {
                LOG.info("Process ${registered.id} stopped gracefully")
                unregister(registered.id)
                return@withContext true
            }

            // 强制终止
            LOG.warn("Process ${registered.id} did not stop gracefully, force killing")
            process.destroyForcibly()

            // 等待强制终止
            val forceKilled = withTimeoutOrNull(2000) {
                process.waitFor()
                true
            } ?: false

            if (forceKilled) {
                LOG.info("Process ${registered.id} force killed")
                unregister(registered.id)
                return@withContext true
            }

            LOG.error("Failed to stop process ${registered.id}")
            false
        } catch (e: Exception) {
            LOG.error("Error stopping process ${registered.id}", e)
            false
        }
    }

    /**
     * 获取统计信息
     */
    fun getStatistics(): RegistryStatistics {
        val all = processes.values
        val alive = all.count { it.process.isAlive }
        val dead = all.size - alive

        return RegistryStatistics(
            total = all.size,
            alive = alive,
            dead = dead,
            oldestProcessAge = all.minOfOrNull { System.currentTimeMillis() - it.registeredAt } ?: 0
        )
    }
}

/**
 * 注册的进程
 */
data class RegisteredProcess(
    val id: String,
    val process: Process,
    val pid: Long?,
    val registeredAt: Long,
    val metadata: Map<String, Any>
) {
    fun isAlive(): Boolean = process.isAlive

    fun ageMs(): Long = System.currentTimeMillis() - registeredAt
}

/**
 * 注册表统计信息
 */
data class RegistryStatistics(
    val total: Int,
    val alive: Int,
    val dead: Int,
    val oldestProcessAge: Long
)