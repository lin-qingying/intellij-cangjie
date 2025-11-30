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

package org.cangnova.cangjie.debugger.dap.connection

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.debugger.dap.protocol.DapTraceWriter
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.Future

/**
 * 重构的DAP连接 - 完全异步，无阻塞
 */
class AsyncDapConnection(
    override val id: String = UUID.randomUUID().toString(),
    override val config: ConnectionConfig
) : ManagedConnection {

    companion object {
        private val LOG = Logger.getInstance(AsyncDapConnection::class.java)
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var socket: Socket? = null
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private lateinit var _server: IDebugProtocolServer
    private var listenFuture: Future<Void>? = null

    override val server: IDebugProtocolServer
        get() = _server

    override var lastActiveTime: Long = System.currentTimeMillis()
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 连接到DAP服务器
     *
     * @param client DAP客户端实现
     * @return 连接结果
     */
    suspend fun connect(client: IDebugProtocolClient): Result<IDebugProtocolServer> {
        return withContext(Dispatchers.IO) {
            try {
                _state.value = ConnectionState.Connecting
                LOG.info("Connecting to DAP server at ${config.host}:${config.port}")

                // 异步建立socket连接
                val sock = withTimeout(config.connectionTimeoutMs) {
                    establishConnection()
                }

                socket = sock
                inputStream = sock.getInputStream()
                outputStream = sock.getOutputStream()

                // 创建消息跟踪PrintWriter
                val traceWriter = DapTraceWriter()
                val tracePrintWriter = java.io.PrintWriter(traceWriter, true)

                // 启动LSP4J launcher with tracing
                val launcher = DSPLauncher.createClientLauncher(
                    client,
                    inputStream,
                    outputStream,
                    true, // validate messages
                    tracePrintWriter // trace output
                )

                _server = launcher.remoteProxy

                // 启动监听（非阻塞）
                listenFuture = launcher.startListening()

                // 监听连接状态
                scope.launch {
                    monitorConnection()
                }

                _state.value = ConnectionState.Connected
                lastActiveTime = System.currentTimeMillis()

                LOG.info("Connected to DAP server successfully (id=$id)")
                Result.success(_server)
            } catch (e: TimeoutCancellationException) {
                LOG.error("Connection timeout: ${e.message}")
                _state.value = ConnectionState.Failed(e)
                cleanup()
                Result.failure(ConnectionTimeoutException("Connection timeout after ${config.connectionTimeoutMs}ms"))
            } catch (e: Exception) {
                LOG.error("Failed to connect to DAP server", e)
                _state.value = ConnectionState.Failed(e)
                cleanup()
                Result.failure(e)
            }
        }
    }

    /**
     * 建立Socket连接（可中断的异步操作）
     */
    private suspend fun establishConnection(): Socket = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        val maxRetries = config.maxRetries

        for (attempt in 1..maxRetries) {
            if (!isActive) {
                throw CancellationException("Connection cancelled")
            }

            try {
                LOG.debug("Connection attempt $attempt/$maxRetries to ${config.host}:${config.port}")

                // 异步创建socket（使用线程池，但不阻塞协程）
                val socket = withContext(Dispatchers.IO) {
                    Socket(config.host, config.port).apply {
                        soTimeout = config.readTimeoutMs.toInt()
                        tcpNoDelay = true
                    }
                }

                LOG.info("Socket connection established on attempt $attempt")
                return@withContext socket
            } catch (e: Exception) {
                lastException = e
                LOG.debug("Connection attempt $attempt failed: ${e.message}")

                if (attempt < maxRetries) {
                    delay(config.retryDelayMs)
                }
            }
        }

        throw lastException ?: ConnectionFailedException("Failed to connect after $maxRetries attempts")
    }

    /**
     * 监控连接状态
     */
    private suspend fun monitorConnection() {
        try {
            // 等待监听完成（连接断开时会完成）
            withContext(Dispatchers.IO) {
                listenFuture?.get()
            }

            if (_state.value == ConnectionState.Connected) {
                LOG.info("Connection $id disconnected")
                _state.value = ConnectionState.Disconnected
            }
        } catch (e: Exception) {
            if (_state.value == ConnectionState.Connected) {
                LOG.error("Connection $id failed", e)
                _state.value = ConnectionState.Failed(e)
            }
        }
    }

    override suspend fun isHealthy(): Boolean {
        return socket?.isConnected == true &&
                socket?.isClosed == false &&
                _state.value == ConnectionState.Connected
    }

    /**
     * 关闭连接
     */
    suspend fun close(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Closing connection $id")
                _state.value = ConnectionState.Closed
                cleanup()
                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Error closing connection $id", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 清理资源
     */
    private fun cleanup() {
        try {
            listenFuture?.cancel(true)
            inputStream.close()
            outputStream.close()
            socket?.close()
        } catch (e: Exception) {
            LOG.debug("Error during cleanup", e)
        }
    }

    override fun dispose() {
        scope.cancel()
        runBlocking {
            close()
        }
    }

    /**
     * 更新活跃时间
     */
    fun updateActiveTime() {
        lastActiveTime = System.currentTimeMillis()
    }
}

/**
 * 连接超时异常
 */
class ConnectionTimeoutException(message: String) : Exception(message)

/**
 * 连接失败异常
 */
class ConnectionFailedException(message: String) : Exception(message)