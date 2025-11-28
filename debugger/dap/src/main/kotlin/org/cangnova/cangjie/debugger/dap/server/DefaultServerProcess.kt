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

package org.cangnova.cangjie.debugger.dap.server

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.process.CjProcessHandler
import java.io.IOException
import java.net.Socket

/**
 * 默认服务器进程实现
 */
class DefaultServerProcess(
    override val id: String,
    override val config: ServerConfig,
    override val port: Int,
    private val processHandler: CjProcessHandler,
    private val portLease: PortLease
) : ServerProcess {

    companion object {
        private val LOG = Logger.getInstance(DefaultServerProcess::class.java)
    }

    private val _state = MutableStateFlow<ServerState>(ServerState.Starting)
    override val state: StateFlow<ServerState> = _state.asStateFlow()

    override val pid: Long? = try {
        processHandler.process.pid()
    } catch (e: Exception) {
        LOG.warn("Failed to get process PID", e)
        null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // 监控进程状态
        scope.launch {
            monitorProcess()
        }
    }

    override fun isRunning(): Boolean = processHandler.process.isAlive

    override suspend fun waitForReady(timeoutMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // 检查进程是否还活着
                if (!isRunning()) {
                    _state.value = ServerState.Failed(
                        IllegalStateException("Server process died during startup")
                    )
                    return@withContext Result.failure(
                        IllegalStateException("Server process died during startup")
                    )
                }

                // 尝试连接服务器
                try {
                    Socket("localhost", port).use {
                        LOG.info("Server $id is ready on port $port (pid=$pid)")
                        _state.value = ServerState.Running(port, pid)
                        return@withContext Result.success(Unit)
                    }
                } catch (e: IOException) {
                    // 服务器还未就绪，继续等待
                    delay(100)
                }
            }

            _state.value = ServerState.Failed(
                TimeoutException("Server did not start within ${timeoutMs}ms")
            )
            Result.failure(TimeoutException("Server did not start within ${timeoutMs}ms"))
        }
    }

    override suspend fun stop(gracefulTimeoutMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isRunning()) {
                    _state.value = ServerState.Stopped
                    cleanup()
                    return@withContext Result.success(Unit)
                }

                _state.value = ServerState.Stopping
                LOG.info("Stopping server $id (pid=$pid)")

                // 尝试优雅停止
                processHandler.destroyProcess()

                // 等待进程退出
                val exited = withTimeoutOrNull(gracefulTimeoutMs) {
                    processHandler.waitFor()
                    true
                } ?: false

                if (exited) {
                    LOG.info("Server $id stopped gracefully")
                    _state.value = ServerState.Stopped
                    cleanup()
                    return@withContext Result.success(Unit)
                }

                // 强制终止
                LOG.warn("Server $id did not stop gracefully, force killing")
                processHandler.killProcess()

                // 等待强制终止
                val forceKilled = withTimeoutOrNull(2000) {
                    processHandler.waitFor()
                    true
                } ?: false

                if (forceKilled) {
                    LOG.info("Server $id force killed")
                    _state.value = ServerState.Stopped
                    cleanup()
                    return@withContext Result.success(Unit)
                }

                LOG.error("Failed to stop server $id")
                _state.value = ServerState.Failed(
                    IllegalStateException("Failed to stop server")
                )
                Result.failure(IllegalStateException("Failed to stop server"))
            } catch (e: Exception) {
                LOG.error("Error stopping server $id", e)
                _state.value = ServerState.Failed(e)
                Result.failure(e)
            }
        }
    }

    /**
     * 监控进程状态
     */
    private suspend fun monitorProcess() {
        try {
            // 等待进程退出
            processHandler.waitFor()

            val exitCode = processHandler.process.exitValue()

            if (_state.value !is ServerState.Stopping && _state.value !is ServerState.Stopped) {
                LOG.warn("Server $id crashed with exit code $exitCode")
                _state.value = ServerState.Crashed(exitCode)
            }

            cleanup()
        } catch (e: InterruptedException) {
            LOG.debug("Process monitoring interrupted for server $id")
        } catch (e: Exception) {
            LOG.error("Error monitoring server process $id", e)
        }
    }

    /**
     * 清理资源
     */
    private suspend fun cleanup() {
        try {
            // 注销进程
            ProcessRegistry.unregister(id)

            // 释放端口
            portLease.release()

            // 取消监控
            scope.cancel()

            LOG.debug("Server $id cleanup completed")
        } catch (e: Exception) {
            LOG.error("Error during server cleanup", e)
        }
    }
}

/**
 * 超时异常
 */
class TimeoutException(message: String) : Exception(message)