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

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.debugger.dap.connection.ManagedConnection
import org.cangnova.cangjie.debugger.dap.core.*
import org.cangnova.cangjie.debugger.dap.dap.DapAdapter
import org.cangnova.cangjie.debugger.dap.server.ServerProcess
import org.cangnova.cangjie.debugger.dap.service.BreakpointService
import org.cangnova.cangjie.debugger.dap.service.EvaluationService
import java.util.*

/**
 * 默认托管调试会话实现
 */
class DefaultManagedDebugSession(
    override val id: String = UUID.randomUUID().toString(),
    override val config: DebugSessionConfig,
    override val serverProcess: ServerProcess,
    override val connection: ManagedConnection
) : ManagedDebugSession {

    companion object {
        private val LOG = Logger.getInstance(DefaultManagedDebugSession::class.java)
    }

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override lateinit var adapter: DebugAdapter
        private set

    // 服务延迟初始化（在adapter初始化后创建）
    private lateinit var _breakpointManager: BreakpointService
    private lateinit var _evaluationEngine: EvaluationService

    override val breakpointManager: BreakpointManager
        get() = _breakpointManager

    override val evaluationEngine: EvaluationEngine
        get() = _evaluationEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventHandlers = mutableListOf<(AdapterEvent) -> Unit>()

    override suspend fun start(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Starting debug session $id")
                _state.value = SessionState.Initializing

                // 初始化适配器
                adapter = DapAdapter(config.project)

                // 初始化服务
                _breakpointManager = BreakpointService(config.project, adapter)
                _evaluationEngine = EvaluationService(adapter)
                LOG.info("Services initialized for session $id")

                // 订阅适配器事件
                adapter.subscribeEvents { event ->
                    handleAdapterEvent(event)
                }

                // 初始化适配器
                val capabilities = withTimeout(config.timeouts.initializationTimeoutMs) {
                    adapter.initialize(config.adapterConfig).getOrThrow()
                }

                LOG.info("Adapter initialized with capabilities: $capabilities")
                _state.value = SessionState.Configuring

                // 发送configurationDone
                adapter.configurationDone().getOrThrow()

                _state.value = SessionState.Launching

                // 启动调试目标
                withTimeout(config.timeouts.launchTimeoutMs) {
                    adapter.launch(config.launchArguments).getOrThrow()
                }

                LOG.info("Debug session $id started successfully")
                _state.value = SessionState.Running()

                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                LOG.error("Session start timeout", e)
                _state.value = SessionState.Error(e, recoverable = false)
                Result.failure(e)
            } catch (e: Exception) {
                LOG.error("Failed to start session", e)
                _state.value = SessionState.Error(e, recoverable = false)
                Result.failure(e)
            }
        }
    }

    override suspend fun stop(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (_state.value.isTerminated()) {
                    LOG.debug("Session $id already terminated")
                    return@withContext Result.success(Unit)
                }

                LOG.info("Stopping debug session $id")
                _state.value = SessionState.Terminating

                // 终止调试目标
                withTimeoutOrNull(config.timeouts.stopTimeoutMs) {
                    if (::adapter.isInitialized) {
                        adapter.terminate()
                        adapter.disconnect()
                    }
                }

                _state.value = SessionState.Terminated()
                LOG.info("Debug session $id stopped")

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Error stopping session $id", e)
                _state.value = SessionState.Error(e, recoverable = false)
                Result.failure(e)
            }
        }
    }

    override suspend fun resume(threadId: Long?): Result<Unit> {
        return executeDebugOperation("resume") {
            if (threadId != null) {
                adapter.continueExecution(threadId)
            } else {
                // 恢复所有线程
                val threads = adapter.getThreads().getOrThrow()
                threads.forEach { thread ->
                    adapter.continueExecution(thread.id)
                }
                Result.success(Unit)
            }
        }
    }

    override suspend fun pause(threadId: Long?): Result<Unit> {
        return executeDebugOperation("pause") {
            if (threadId != null) {
                adapter.pause(threadId)
            } else {
                // 暂停所有线程
                val threads = adapter.getThreads().getOrThrow()
                threads.forEach { thread ->
                    adapter.pause(thread.id)
                }
                Result.success(Unit)
            }
        }
    }

    override suspend fun stepOver(threadId: Long): Result<Unit> {
        return executeDebugOperation("stepOver") {
            adapter.next(threadId)
        }
    }

    override suspend fun stepInto(threadId: Long): Result<Unit> {
        return executeDebugOperation("stepInto") {
            adapter.stepIn(threadId)
        }
    }

    override suspend fun stepOut(threadId: Long): Result<Unit> {
        return executeDebugOperation("stepOut") {
            adapter.stepOut(threadId)
        }
    }

    override suspend fun getScopes(frameId: Long): Result<List<Scope>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_state.value.isActive()) {
                    return@withContext Result.failure(
                        IllegalStateException("Cannot get scopes: session is not active")
                    )
                }

                withTimeout(config.timeouts.operationTimeoutMs) {
                    adapter.getScopes(frameId)
                }
            } catch (e: TimeoutCancellationException) {
                LOG.error("Get scopes timeout", e)
                Result.failure(e)
            } catch (e: Exception) {
                LOG.error("Get scopes failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getVariables(variablesReference: Long): Result<List<Variable>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_state.value.isActive()) {
                    return@withContext Result.failure(
                        IllegalStateException("Cannot get variables: session is not active")
                    )
                }

                withTimeout(config.timeouts.operationTimeoutMs) {
                    adapter.getVariables(variablesReference)
                }
            } catch (e: TimeoutCancellationException) {
                LOG.error("Get variables timeout", e)
                Result.failure(e)
            } catch (e: Exception) {
                LOG.error("Get variables failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun setVariable(
        variablesReference: Long,
        name: String,
        value: String
    ): Result<Variable> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_state.value.isActive()) {
                    return@withContext Result.failure(
                        IllegalStateException("Cannot set variable: session is not active")
                    )
                }

                withTimeout(config.timeouts.operationTimeoutMs) {
                    adapter.setVariable(variablesReference, name, value)
                }
            } catch (e: TimeoutCancellationException) {
                LOG.error("Set variable timeout", e)
                Result.failure(e)
            } catch (e: Exception) {
                LOG.error("Set variable failed", e)
                Result.failure(e)
            }
        }
    }

    override fun subscribeEvents(handler: (AdapterEvent) -> Unit): Subscription {
        eventHandlers.add(handler)
        return object : Subscription {
            override fun unsubscribe() {
                eventHandlers.remove(handler)
            }
        }
    }

    /**
     * 处理适配器事件
     */
    private fun handleAdapterEvent(event: AdapterEvent) {
        scope.launch {
            try {
                when (event) {
                    is AdapterEvent.Initialized -> {
                        LOG.info("Session $id initialized")
                    }

                    is AdapterEvent.Stopped -> {
                        LOG.info("Session $id stopped: threadId=${event.threadId}, reason=${event.reason}")
                        _state.value = SessionState.Suspended(
                            threadId = event.threadId,
                            reason = event.reason.name,
                            description = event.text
                        )
                    }

                    is AdapterEvent.Continued -> {
                        LOG.info("Session $id continued: threadId=${event.threadId}")
                        _state.value = SessionState.Running(event.threadId)
                    }

                    is AdapterEvent.Terminated -> {
                        LOG.info("Session $id terminated")
                        _state.value = SessionState.Terminated()
                    }

                    is AdapterEvent.Exited -> {
                        LOG.info("Session $id exited with code ${event.exitCode}")
                        _state.value = SessionState.Terminated(event.exitCode)
                    }

                    is AdapterEvent.Output -> {
                        // 输出事件不改变状态，只转发
                    }

                    else -> {
                        LOG.debug("Unhandled event: $event")
                    }
                }

                // 转发事件给订阅者
                eventHandlers.forEach { handler ->
                    try {
                        handler(event)
                    } catch (e: Exception) {
                        LOG.error("Error in event handler", e)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error handling adapter event", e)
            }
        }
    }

    /**
     * 执行调试操作（带超时和状态检查）
     */
    private suspend fun executeDebugOperation(
        operationName: String,
        operation: suspend () -> Result<Unit>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_state.value.isActive()) {
                    return@withContext Result.failure(
                        IllegalStateException("Cannot $operationName: session is not active")
                    )
                }

                withTimeout(config.timeouts.operationTimeoutMs) {
                    operation()
                }
            } catch (e: TimeoutCancellationException) {
                LOG.error("Operation $operationName timeout", e)
                Result.failure(e)
            } catch (e: Exception) {
                LOG.error("Operation $operationName failed", e)
                Result.failure(e)
            }
        }
    }

    override fun dispose() {
        LOG.info("Disposing debug session $id")
        scope.cancel()
        eventHandlers.clear()

        if (::adapter.isInitialized) {
            adapter.dispose()
        }
    }
}