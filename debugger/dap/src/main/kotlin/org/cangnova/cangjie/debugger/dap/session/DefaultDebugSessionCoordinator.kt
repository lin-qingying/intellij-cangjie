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

package org.cangnova.cangjie.debugger.dap.session

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import org.cangnova.cangjie.debugger.dap.connection.*
import org.cangnova.cangjie.debugger.dap.dap.DapClient
import org.cangnova.cangjie.debugger.dap.protocol.DapProtocolClient
import org.cangnova.cangjie.debugger.dap.server.DefaultServerLifecycleManager
import org.cangnova.cangjie.debugger.dap.server.ServerLifecycleManager
import org.cangnova.cangjie.debugger.dap.server.ServerProcess
import java.util.*

/**
 * 默认调试会话协调器（单服务器模式）
 *
 * 作为Project级别的服务，协调服务器、连接和会话的完整生命周期。
 * 每次只维护一个调试会话，创建新会话时会自动销毁现有会话。
 */
@Service(Service.Level.PROJECT)
class DefaultDebugSessionCoordinator(
    private val project: Project
) : DebugSessionCoordinator {

    companion object {
        private val LOG = Logger.getInstance(DefaultDebugSessionCoordinator::class.java)

        fun getInstance(project: Project): DefaultDebugSessionCoordinator {
            return project.getService(DefaultDebugSessionCoordinator::class.java)
        }
    }

    // 单服务器模式：只维护一个会话
    private var currentSession: ManagedDebugSession? = null
    private var currentSessionId: String? = null
    private val listeners = mutableListOf<SessionLifecycleListener>()

    private val serverManager: ServerLifecycleManager by lazy {
        DefaultServerLifecycleManager.getInstance(project)
    }

    private val connectionManager: ConnectionManager by lazy {
        DefaultConnectionManager.getInstance()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun createSession(config: DebugSessionConfig): Result<String> {
        return withContext(Dispatchers.IO) {
            // 单服务器模式：创建新会话前先销毁现有会话
            currentSessionId?.let { oldSessionId ->
                LOG.info("Destroying existing session $oldSessionId before creating new one")
                try {
                    destroySession(oldSessionId, timeoutMs = 3000)
                } catch (e: Exception) {
                    LOG.warn("Failed to destroy existing session: ${e.message}")
                }
            }

            val sessionId = UUID.randomUUID().toString()

            try {
                LOG.info("Creating debug session $sessionId (single server mode)")
                notifyListeners { onSessionCreated(sessionId) }

                // 步骤1: 启动服务器
                val serverProcess = withTimeoutOrNull(config.serverConfig.startupTimeoutMs) {
                    LOG.info("[$sessionId] Starting debug server")
                    serverManager.startServer(config.serverConfig, config.debuggerProvider).getOrThrow()
                } ?: throw TimeoutException("Server startup timeout")

                LOG.info("[$sessionId] Server started on port ${serverProcess.port} (pid=${serverProcess.pid})")

                // 步骤2: 获取连接
                val connectionConfig = ConnectionConfig(
                    host = "localhost",
                    port = serverProcess.port,
                    connectionTimeoutMs = config.timeouts.initializationTimeoutMs,
                    enableHealthCheck = true
                )

                val connection = withTimeoutOrNull(config.timeouts.initializationTimeoutMs) {
                    LOG.info("[$sessionId] Acquiring connection to port ${serverProcess.port}")
                    connectionManager.acquireConnection(connectionConfig).getOrThrow()
                } ?: throw TimeoutException("Connection acquisition timeout")

                LOG.info("[$sessionId] Connection acquired: ${connection.id}")

                // 步骤3: 创建会话（稍后将建立DAP连接）
                val session = DefaultManagedDebugSession(
                    id = sessionId,
                    config = config,
                    serverProcess = serverProcess,
                    connection = connection
                )

                // 注册会话（单服务器模式）
                currentSession = session
                currentSessionId = sessionId

                // 步骤4: 建立DAP连接（必须在创建session之后，这样DapClient才能将事件转发给DapProtocolClient）
                if (connection is AsyncDapConnection) {
                    // 创建DAP客户端，将事件转发给session的协议客户端
                    val dapClient = DapClient { event ->
                        // 事件会被session的adapter（DapProtocolClient）处理
                        try {
                            (session.adapter as? DapProtocolClient)?.dispatchEvent(event)
                        } catch (e: UninitializedPropertyAccessException) {
                            LOG.debug("[$sessionId] DAP event received before adapter initialization: $event")
                        }
                    }

                    // 连接到服务器
                    withTimeoutOrNull(config.timeouts.initializationTimeoutMs) {
                        LOG.info("[$sessionId] Connecting to DAP server")
                        connection.connect(dapClient).getOrThrow()
                    } ?: throw TimeoutException("DAP connection timeout")

                    LOG.info("[$sessionId] DAP connection established")
                }

                // 步骤5: 初始化adapter（在连接建立之后）
                session.initializeAdapter()

                // 注意：不再在这里调用session.start()，由DapDebugProcess在startPausing()中调用DAP协议初始化
                LOG.info("[$sessionId] Debug session created (server and connection ready)")
                notifyListeners { onSessionStarted(sessionId) }

                // 监听会话状态
                scope.launch {
                    monitorSessionState(sessionId, session)
                }

                Result.success(sessionId)
            } catch (e: TimeoutException) {
                LOG.error("[$sessionId] Session creation timeout", e)
                Result.failure(e)
            } catch (e: Exception) {
                LOG.error("[$sessionId] Failed to create session", e)
                Result.failure(e)
            }
        }
    }

    override fun getSession(sessionId: String): ManagedDebugSession? {
        // 单服务器模式：只检查当前会话
        return if (sessionId == currentSessionId) currentSession else null
    }

    override suspend fun destroySession(sessionId: String, timeoutMs: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 单服务器模式：检查是否是当前会话
                if (sessionId != currentSessionId) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Session $sessionId not found")
                    )
                }

                val session = currentSession
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Session $sessionId not found")
                    )

                LOG.info("Destroying session $sessionId")
                notifyListeners { onSessionStopped(sessionId) }

                // 清除当前会话引用
                currentSession = null
                currentSessionId = null

                // 优雅关闭序列（带超时）
                withTimeoutOrNull(timeoutMs) {
                    // 1. 停止会话（发送disconnect请求）
                    LOG.debug("[$sessionId] Stopping session (sending disconnect)")
                    try {
                        session.stop().onFailure { error ->
                            LOG.warn("[$sessionId] Session stop failed: ${error.message}")
                        }
                    } catch (e: TimeoutException) {
                        LOG.warn("[$sessionId] Session stop timeout")
                    }

                    // 2. 关闭连接（而不是放回池中）
                    LOG.debug("[$sessionId] Closing connection")
                    connectionManager.closeConnection(session.connection.id).onFailure { error ->
                        LOG.warn("[$sessionId] Failed to close connection: ${error.message}")
                    }

                    // 3. 停止服务器进程
                    LOG.debug("[$sessionId] Stopping server")
                    serverManager.stopServer(
                        session.serverProcess.id,
                        gracefulTimeoutMs = 3000
                    ).onFailure { error ->
                        LOG.warn("[$sessionId] Failed to stop server: ${error.message}")
                    }

                    // 4. Dispose会话资源
                    LOG.debug("[$sessionId] Disposing session")
                    session.dispose()
                }

                LOG.info("Session $sessionId destroyed")
                notifyListeners { onSessionDestroyed(sessionId) }

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Error destroying session $sessionId", e)
                Result.failure(e)
            }
        }
    }

    override fun getActiveSessions(): List<ManagedDebugSession> {
        // 单服务器模式：返回当前会话（如果存在）
        return currentSession?.let { listOf(it) } ?: emptyList()
    }

    override fun getSessionState(sessionId: String): StateFlow<SessionState>? {
        // 单服务器模式：只检查当前会话
        return if (sessionId == currentSessionId) currentSession?.state else null
    }

    /**
     * 添加生命周期监听器
     */
    fun addLifecycleListener(listener: SessionLifecycleListener) {
        listeners.add(listener)
    }

    /**
     * 移除生命周期监听器
     */
    fun removeLifecycleListener(listener: SessionLifecycleListener) {
        listeners.remove(listener)
    }

    /**
     * 监控会话状态
     */
    private suspend fun monitorSessionState(sessionId: String, session: ManagedDebugSession) {
        try {
            var previousState: SessionState = SessionState.Idle

            session.state.collect { newState ->
                notifyListeners {
                    onSessionStateChanged(sessionId, previousState, newState)
                }
                previousState = newState

                // 自动清理已终止的会话
                if (newState.isTerminated()) {
                    LOG.info("Session $sessionId terminated, scheduling cleanup")
                    delay(1000) // 延迟1秒让UI有时间响应
                    destroySession(sessionId)
                }
            }
        } catch (e: Exception) {
            LOG.error("Error monitoring session $sessionId state", e)
        }
    }

    /**
     * 清理会话资源
     */
    private suspend fun cleanupSession(
        sessionId: String,
        serverProcess: ServerProcess,
        connection: ManagedConnection
    ) {
        try {
            LOG.warn("[$sessionId] Cleaning up failed session")

            withTimeoutOrNull(5000) {
                connectionManager.closeConnection(connection.id)
                serverManager.stopServer(serverProcess.id, gracefulTimeoutMs = 2000)
            }
        } catch (e: Exception) {
            LOG.error("[$sessionId] Error during cleanup", e)
        }
    }

    /**
     * 通知监听器
     */
    private fun notifyListeners(action: SessionLifecycleListener.() -> Unit) {
        listeners.forEach { listener ->
            try {
                listener.action()
            } catch (e: Exception) {
                LOG.error("Error in lifecycle listener", e)
            }
        }
    }

    override fun dispose() {
        LOG.info("Disposing DebugSessionCoordinator")
        scope.cancel()

        // 单服务器模式：销毁当前会话（如果存在）
        ApplicationManager.getApplication().executeOnPooledThread {
            runBlocking {
                currentSessionId?.let { sessionId ->
                    LOG.info("Destroying current session $sessionId")
                    try {
                        destroySession(sessionId, timeoutMs = 3000)
                    } catch (e: Exception) {
                        LOG.error("Error destroying session $sessionId during dispose", e)
                    }
                }

                listeners.clear()
            }
        }
    }
}

/**
 * 超时异常
 */
class TimeoutException(message: String) : Exception(message)