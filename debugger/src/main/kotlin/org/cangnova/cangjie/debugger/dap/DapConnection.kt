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

package org.cangnova.cangjie.debugger.dap

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.cangnova.cangjie.debugger.core.ConnectionConfig
import org.cangnova.cangjie.debugger.exception.DapConnectionException
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import java.io.IOException
import java.net.Socket
import java.util.concurrent.Future

/**
 * DAP连接管理器
 *
 * 管理与DAP服务器的连接，支持异步连接和自动重试
 */
class DapConnection(
    private val host: String,
    private val port: Int,
    private val config: ConnectionConfig
) : Disposable {

    companion object {
        private val LOG = Logger.getInstance(DapConnection::class.java)
    }

    private var socket: Socket? = null
    private var launcher: Launcher<IDebugProtocolServer>? = null
    private var listenFuture: Future<Void>? = null

    /**
     * 连接到DAP服务器
     *
     * @param client DAP客户端实现
     * @return 连接成功返回服务器代理
     */
    suspend fun connect(client: IDebugProtocolClient): Result<IDebugProtocolServer> {
        return withContext(Dispatchers.IO) {
            try {
                connectWithRetry(client)
            } catch (e: Exception) {
                Result.failure(DapConnectionException("Connection failed", e))
            }
        }
    }

    /**
     * 带重试的连接
     */
    private suspend fun connectWithRetry(client: IDebugProtocolClient): Result<IDebugProtocolServer> {
        var lastException: Exception? = null

        repeat(config.maxRetries) { attempt ->
            try {
                LOG.info("Connection attempt ${attempt + 1}/${config.maxRetries} to $host:$port")

                val sock = withTimeout(config.connectionTimeoutMs) {
                    Socket(host, port)
                }

                if (!sock.isConnected) {
                    throw IOException("Socket not connected")
                }

                socket = sock

                val launch = DSPLauncher.createClientLauncher(
                    client,
                    sock.inputStream,
                    sock.outputStream
                )

                launcher = launch
                val server = launch.remoteProxy
                listenFuture = launch.startListening()

                LOG.info("Successfully connected to DAP server at $host:$port")
                return Result.success(server)

            } catch (e: Exception) {
                lastException = e
                LOG.warn("Connection attempt ${attempt + 1} failed: ${e.message}")

                // 清理失败的连接
                cleanup()

                if (attempt < config.maxRetries - 1) {
                    delay(config.retryDelayMs)
                }
            }
        }

        return Result.failure(
            DapConnectionException(
                "Failed to connect after ${config.maxRetries} attempts",
                lastException
            )
        )
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false
    }

    /**
     * 清理连接资源
     */
    private fun cleanup() {
        try {
            listenFuture?.cancel(true)
            socket?.close()
        } catch (e: Exception) {
            LOG.debug("Error during cleanup", e)
        } finally {
            listenFuture = null
            socket = null
            launcher = null
        }
    }

    override fun dispose() {
        cleanup()
        LOG.info("DAP connection closed")
    }
}