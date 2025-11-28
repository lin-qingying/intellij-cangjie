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

package org.cangnova.cangjie.debugger.dap.ui

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import kotlinx.coroutines.*
import org.cangnova.cangjie.debugger.RunParameters
import org.cangnova.cangjie.debugger.dap.core.AdapterConfig
import org.cangnova.cangjie.debugger.dap.core.AdapterEvent
import org.cangnova.cangjie.debugger.dap.core.ConnectionConfig
import org.cangnova.cangjie.debugger.dap.core.LaunchArguments
import org.cangnova.cangjie.debugger.dap.provider.LocalDebuggerProvider
import org.cangnova.cangjie.debugger.dap.server.PortStrategy
import org.cangnova.cangjie.debugger.dap.server.ServerConfig
import org.cangnova.cangjie.debugger.dap.session.*
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.absolutePathString

/**
 * 仓颉调试进程（重构版）
 *
 * 完全异步，无阻塞调用
 */
class DapDebugProcess(
    private val runConfig: RunParameters,
    session: XDebugSession
) : XDebugProcess(session) {

    companion object {
        private val LOG = Logger.getInstance(DapDebugProcess::class.java)
    }

    // 协程作用域 - 绑定到进程生命周期
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 会话协调器
    private val coordinator: DebugSessionCoordinator by lazy {
        DefaultDebugSessionCoordinator.getInstance(session.project)
    }

    // 托管的调试会话
    private var managedSession: ManagedDebugSession? = null
    private var sessionId: String? = null

    // UI组件
    private val editorsProvider = CangJieDebuggerEditorsProvider()
    private lateinit var breakpointHandler: CangJieBreakpointHandler

    // 初始化状态
    @Volatile
    private var initialized = false

    init {
        // 异步初始化，不阻塞构造函数
        startInitialization()
    }

    /**
     * 启动异步初始化
     */
    private fun startInitialization() {
        LOG.info("Starting asynchronous initialization of CangJie debug process")

        // 在后台线程执行初始化
        ApplicationManager.getApplication().executeOnPooledThread {
            scope.launch {
                try {
                    initializeAsync()
                } catch (e: CancellationException) {
                    LOG.info("Initialization cancelled")
                } catch (e: Exception) {
                    LOG.error("Failed to initialize debug process", e)
                    session.reportError("Failed to start debugger: ${e.message}")
                    session.stop()
                }
            }
        }
    }

    /**
     * 异步初始化（完全非阻塞）
     */
    private suspend fun initializeAsync() = withContext(Dispatchers.IO) {
        try {
            LOG.info("Initializing debug session")

            // 创建会话配置
            val sessionConfig = createSessionConfig()

            // 使用协调器创建会话
            val result = coordinator.createSession(sessionConfig)

            if (result.isFailure) {
                throw result.exceptionOrNull()
                    ?: IllegalStateException("Session creation failed")
            }

            sessionId = result.getOrThrow()
            managedSession = coordinator.getSession(sessionId!!)
                ?: throw IllegalStateException("Session not found after creation")

            LOG.info("Debug session created: $sessionId")

            // 订阅会话事件
            managedSession!!.subscribeEvents { event ->
                handleDebugEvent(event)
            }

            // 创建断点处理器（在EDT上）
            withContext(Dispatchers.EDT) {
                breakpointHandler = CangJieBreakpointHandler(this@DapDebugProcess)
            }

            // 标记为已初始化
            initialized = true

            // 在EDT上更新UI
            withContext(Dispatchers.EDT) {
                session.consoleView?.print(
                    "Debugger attached successfully\n",
                    ConsoleViewContentType.SYSTEM_OUTPUT
                )
            }

            LOG.info("CangJie debug process initialized successfully")
        } catch (e: Exception) {
            LOG.error("Async initialization failed", e)
            throw e
        }
    }

    /**
     * 创建会话配置
     */
    private fun createSessionConfig(): DebugSessionConfig {
        // 使用 LocalDebuggerProvider 获取服务器路径（executablePath 留空，由 ServerLifecycleManager 处理）
        val debuggerProvider = LocalDebuggerProvider.getInstance()

        return DebugSessionConfig(
            project = session.project,
            serverConfig = ServerConfig(
                executablePath = "", // 留空，DefaultServerLifecycleManager 会使用 LocalDebuggerProvider
                debuggerType = debuggerProvider.debuggerType,
                portStrategy = PortStrategy.AutoAllocate,
                startupTimeoutMs = 10000
            ),
            launchArguments = LaunchArguments(
                program = runConfig.runExecutable.exePath,
                arguments = runConfig.runExecutable.parametersList.parameters,
                workingDirectory = runConfig.runExecutable.workingDirectory?.absolutePathString() ?: "",
                environment = runConfig.runExecutable.environment
            ),
            adapterConfig = AdapterConfig(
                host = "localhost",
                port = 0, // 会被服务器端口覆盖
                connectionConfig = ConnectionConfig()
            ),
            timeouts = SessionTimeouts(
                initializationTimeoutMs = 10000,
                launchTimeoutMs = 30000,
                operationTimeoutMs = 5000,
                stopTimeoutMs = 5000
            )
        )
    }

    /**
     * 处理调试事件
     */
    private fun handleDebugEvent(event: AdapterEvent) {
        scope.launch {
            try {
                when (event) {
                    is AdapterEvent.Stopped -> {
                        LOG.info("Program stopped: ${event.reason}")

                        withContext(Dispatchers.EDT) {
                            val suspendContext = CangJieSuspendContext(
                                debugProcess = this@DapDebugProcess,
                                activeThreadId = event.threadId
                            )
                            session.positionReached(suspendContext)
                        }
                    }

                    is AdapterEvent.Continued -> {
                        LOG.info("Program continued")
                    }

                    is AdapterEvent.Terminated -> {
                        LOG.info("Program terminated")
                        withContext(Dispatchers.EDT) {
                            session.stop()
                        }
                    }

                    is AdapterEvent.Exited -> {
                        LOG.info("Program exited with code ${event.exitCode}")
                        withContext(Dispatchers.EDT) {
                            session.consoleView?.print(
                                "\nProcess exited with code ${event.exitCode}\n",
                                ConsoleViewContentType.SYSTEM_OUTPUT
                            )
                            session.stop()
                        }
                    }

                    is AdapterEvent.Output -> {
                        withContext(Dispatchers.EDT) {
                            val contentType = when (event.category) {
                                "stderr" -> ConsoleViewContentType.ERROR_OUTPUT
                                "console" -> ConsoleViewContentType.NORMAL_OUTPUT
                                else -> ConsoleViewContentType.SYSTEM_OUTPUT
                            }
                            session.consoleView?.print(event.output, contentType)
                        }
                    }

                    is AdapterEvent.BreakpointChanged -> {
                        LOG.debug("Breakpoint changed: ${event.breakpoint}")
                    }

                    else -> {
                        LOG.debug("Unhandled event: $event")
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error handling debug event", e)
            }
        }
    }

    // ==================== XDebugProcess 接口实现 ====================

    override fun getEditorsProvider(): XDebuggerEditorsProvider {
        return editorsProvider
    }

    override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> {
        return if (::breakpointHandler.isInitialized) {
            arrayOf(breakpointHandler)
        } else {
            emptyArray()
        }
    }

    override fun resume(context: XSuspendContext?) {
        executeAsync("resume") {
            val session = requireSession()
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId

            session.resume(threadId).onFailure { error ->
                reportError("Failed to resume: ${error.message}")
            }
        }
    }

    override fun startStepOver(context: XSuspendContext?) {
        executeAsync("step over") {
            val session = requireSession()
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId
                ?: return@executeAsync

            session.stepOver(threadId).onFailure { error ->
                reportError("Failed to step over: ${error.message}")
            }
        }
    }

    override fun startStepInto(context: XSuspendContext?) {
        executeAsync("step into") {
            val session = requireSession()
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId
                ?: return@executeAsync

            session.stepInto(threadId).onFailure { error ->
                reportError("Failed to step into: ${error.message}")
            }
        }
    }

    override fun startStepOut(context: XSuspendContext?) {
        executeAsync("step out") {
            val session = requireSession()
            val threadId = (context as? CangJieSuspendContext)?.activeThreadId
                ?: return@executeAsync

            session.stepOut(threadId).onFailure { error ->
                reportError("Failed to step out: ${error.message}")
            }
        }
    }

    override fun stop() {
        LOG.info("Stopping debug process")

        // 异步停止，不阻塞EDT
        ApplicationManager.getApplication().executeOnPooledThread {
            scope.launch {
                try {
                    sessionId?.let { id ->
                        coordinator.destroySession(id, timeoutMs = 5000)
                    }

                    // 取消所有协程
                    scope.cancel()

                    LOG.info("Debug process stopped")
                } catch (e: Exception) {
                    LOG.error("Error stopping debug process", e)
                }
            }
        }
    }

    override fun runToPosition(position: XSourcePosition, context: XSuspendContext?) {
        // TODO: 实现运行到光标位置
        LOG.warn("runToPosition not implemented yet")
    }

    // ==================== 辅助方法 ====================

    /**
     * 执行异步操作（不阻塞EDT）
     */
    private fun executeAsync(operationName: String, operation: suspend () -> Unit) {
        if (!initialized) {
            LOG.warn("Cannot $operationName: debug process not initialized")
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            scope.launch {
                try {
                    operation()
                } catch (e: CancellationException) {
                    LOG.debug("$operationName cancelled")
                } catch (e: Exception) {
                    LOG.error("Error during $operationName", e)
                    reportError("$operationName failed: ${e.message}")
                }
            }
        }
    }

    /**
     * 获取会话（如果未初始化则抛出异常）
     */
    private fun requireSession(): ManagedDebugSession {
        return managedSession
            ?: throw IllegalStateException("Debug session not initialized")
    }

    /**
     * 报告错误到UI（在EDT上）
     */
    private fun reportError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            session.reportError(message)
        }
    }

    // ==================== 公共API ====================

    /**
     * 获取托管会话（用于UI组件）
     */
    fun getManagedSession(): ManagedDebugSession? = managedSession

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
}


/**
 * EDT Dispatcher
 */
private val Dispatchers.EDT: CoroutineDispatcher
    get() = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            ApplicationManager.getApplication().invokeLater(block)
        }
    }