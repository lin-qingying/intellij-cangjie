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

package org.cangnova.cangjie.dapdebugger.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cangnova.cangjie.dapdebugger.core.*
import org.cangnova.cangjie.dapdebugger.exception.DebugSessionException
import java.util.*

/**
 * 调试会话服务
 *
 * 管理调试会话的生命周期和状态
 */
class DebugSessionService(
    private val project: Project,
    private val xDebugSession: XDebugSession,
    private val debugProcess: org.cangnova.cangjie.dapdebugger.ui.CangJieDebugProcess? = null
) : DebugSession {

    companion object {
        private val LOG = Logger.getInstance(DebugSessionService::class.java)
    }

    override val sessionId: String = UUID.randomUUID().toString()

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    override lateinit var adapter: DebugAdapter

    private val eventSubscriptions = mutableListOf<Subscription>()
    private val eventHandlers = mutableListOf<(DebugEvent) -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 初始化会话
     */
    fun initialize(adapter: DebugAdapter) {
        this.adapter = adapter

        // 订阅适配器事件
        val subscription = adapter.subscribeEvents { event ->
            handleAdapterEvent(event)
        }
        eventSubscriptions.add(subscription)
    }

    private var capabilities: Capabilities? = null

    override suspend fun start(config: LaunchConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Starting debug session: $sessionId")
                _state.value = SessionState.Starting

                // 初始化适配器
                capabilities = adapter.initialize(config.adapterConfig).getOrThrow()
                LOG.info("Adapter initialized with capabilities: ${capabilities?.supportsConfigurationDoneRequest}")

                // 启动调试目标
                val launchArgs = LaunchArguments(
                    program = config.program,
                    arguments = config.arguments,
                    workingDirectory = config.workingDirectory,
                    environment = config.environment
                )

                adapter.launch(launchArgs).getOrThrow()

                // 注意：configurationDone应该在接收到initialized事件后发送，
                // 但是launch会触发initialized事件，所以我们在这里等待
                // 实际的configurationDone会在handleAdapterEvent中处理

                _state.value = SessionState.Running
                LOG.info("Debug session started successfully")

                // 通知事件处理器
                notifyEvent(DebugEvent.Started)

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to start debug session", e)
                _state.value = SessionState.Error(e)
                Result.failure(DebugSessionException("Failed to start session", e))
            }
        }
    }

    override suspend fun resume(threadId: Long?): Result<Unit> {
        return executeCommand("resume") {
            val targetThreadId = threadId ?: 1 // 默认使用主线程
            adapter.continueExecution(targetThreadId).getOrThrow()
            _state.value = SessionState.Running
            notifyEvent(DebugEvent.Resumed)
            Result.success(Unit)
        }
    }

    override suspend fun pause(threadId: Long?): Result<Unit> {
        return executeCommand("pause") {
            val targetThreadId = threadId ?: 1 // 默认使用主线程
            adapter.pause(targetThreadId).getOrThrow()
            Result.success(Unit)
        }
    }

    override suspend fun stepOver(threadId: Long): Result<Unit> {
        return executeCommand("stepOver") {
            adapter.next(threadId).getOrThrow()
            Result.success(Unit)
        }
    }

    override suspend fun stepInto(threadId: Long): Result<Unit> {
        return executeCommand("stepInto") {
            adapter.stepIn(threadId).getOrThrow()
            Result.success(Unit)
        }
    }

    override suspend fun stepOut(threadId: Long): Result<Unit> {
        return executeCommand("stepOut") {
            adapter.stepOut(threadId).getOrThrow()
            Result.success(Unit)
        }
    }

    override suspend fun stop(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Stopping debug session: $sessionId")
                _state.value = SessionState.Stopping

//                // 1. 先发送终止请求给服务器（ terminateDebuggee = true ）
//                try {
//                    LOG.debug("Sending terminate request to DAP server")
//                    adapter.terminate().getOrThrow()
//                } catch (e: Exception) {
//                    LOG.warn("Failed to send terminate request: ${e.message}")
//                }

                // 2. 断开连接
                LOG.debug("Disconnecting from DAP server")
                adapter.disconnect().getOrThrow()

                _state.value = SessionState.Stopped
                LOG.info("Debug session stopped")


//                xDebugSession.stop()


                notifyEvent(DebugEvent.Stopped)

                Result.success(Unit)
            } catch (e: Exception) {
                LOG.error("Failed to stop debug session", e)
                Result.failure(DebugSessionException("Failed to stop session", e))
            }
        }
    }

    override suspend fun getThreads(): Result<List<ThreadInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                adapter.getThreads()
            } catch (e: Exception) {
                LOG.error("Failed to get threads", e)
                Result.failure(DebugSessionException("Failed to get threads", e))
            }
        }
    }

    override suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                adapter.getStackTrace(threadId)
            } catch (e: Exception) {
                LOG.error("Failed to get stack trace", e)
                Result.failure(DebugSessionException("Failed to get stack trace", e))
            }
        }
    }

    override fun subscribeEvents(handler: (DebugEvent) -> Unit): Subscription {
        eventHandlers.add(handler)
        return object : Subscription {
            override fun unsubscribe() {
                eventHandlers.remove(handler)
            }
        }
    }

    private fun handleAdapterEvent(event: AdapterEvent) {
        scope.launch {
            when (event) {
                is AdapterEvent.Initialized -> {
                    LOG.info("Received initialized event from DAP server")

                    // 如果适配器支持configurationDone请求，则发送configurationDone
                    capabilities?.let { caps ->
                        if (caps.supportsConfigurationDoneRequest) {
                            try {
                                LOG.info("Sending configuration done request")
                                adapter.configurationDone().getOrThrow()
                                LOG.info("Configuration done completed successfully")
                            } catch (e: Exception) {
                                LOG.error("Failed to send configuration done", e)
                            }
                        }
                    }
                }

                is AdapterEvent.Stopped -> {
                    _state.value = SessionState.Paused(
                        threadId = event.threadId,
                        reason = mapPauseReason(event.reason)
                    )

                    // 记录详细的停止信息
                    val stopInfo = buildString {
                        append("Execution stopped: ")
                        append("reason=${event.reason}")
                        append(", threadId=${event.threadId}")
                        if (event.allThreadsStopped) {
                            append(", allThreadsStopped=true")
                        }
                        if (event.hitBreakpointIds.isNotEmpty()) {
                            append(", hitBreakpointIds=${event.hitBreakpointIds.joinToString()}")
                        }
                        event.description?.let { append(", description=$it") }
                        event.text?.let { append(", text=$it") }
                    }
                    LOG.info(stopInfo)

                    // 异步获取堆栈信息并创建完整的暂停上下文
                    scope.launch(Dispatchers.IO) {
                        try {
                            // 获取堆栈跟踪
                            val stackTraceResult = adapter.getStackTrace(event.threadId)
                            if (stackTraceResult.isSuccess) {
                                val frames = stackTraceResult.getOrThrow()
                                LOG.info("Retrieved ${frames.size} stack frames for thread ${event.threadId}")

                                // 获取线程列表信息
                                val threadsResult = adapter.getThreads()
                                val threads: List<ThreadInfo> = if (threadsResult.isSuccess) {
                                    threadsResult.getOrThrow()
                                } else {
                                    emptyList()
                                }

                                // 创建暂停上下文
                                val suspendContext = if (debugProcess != null) {
                                    org.cangnova.cangjie.dapdebugger.ui.CangJieSuspendContext(
                                        debugProcess = debugProcess,
                                        activeThreadId = event.threadId,
                                        threads = threads,
                                        frames = frames
                                    )
                                } else {
                                    // 如果没有debugProcess，创建一个简化的暂停上下文
                                    // 这里需要创建一个不依赖debugProcess的替代实现
                                    null
                                }

                                // 在UI线程中通知IntelliJ平台
                                ApplicationManager.getApplication().invokeLater {
                                    if (suspendContext != null && frames.isNotEmpty()) {
                                        // 通知IntelliJ平台到达断点位置，使用完整的暂停上下文
                                        xDebugSession.positionReached(suspendContext)
                                    } else {
                                        // 没有完整的暂停上下文或没有堆栈帧，使用简单暂停
                                        xDebugSession.pause()
                                    }
                                }
                            } else {
                                LOG.warn("Failed to get stack trace for thread ${event.threadId}")
                                // 如果获取堆栈失败，仍然要通知IntelliJ平台暂停
                                ApplicationManager.getApplication().invokeLater {
                                    xDebugSession.pause()
                                }
                            }
                        } catch (e: Exception) {
                            LOG.error("Error while processing stopped event", e)
                            // 出错时也要通知IntelliJ平台暂停
                            ApplicationManager.getApplication().invokeLater {
                                xDebugSession.pause()
                            }
                        }
                    }

                    notifyEvent(
                        DebugEvent.Paused(
                            threadId = event.threadId,
                            reason = mapPauseReason(event.reason)
                        )
                    )
                }

                is AdapterEvent.Continued -> {
                    _state.value = SessionState.Running
                    notifyEvent(DebugEvent.Resumed)
                }

                is AdapterEvent.Exited -> {
                    LOG.info("Target program exited with code: ${event.exitCode}")

                    // 根据退出码更新状态
                    if (event.exitCode == 0) {
                        LOG.info("Program completed successfully")
                    } else {
                        LOG.warn("Program exited with error code: ${event.exitCode}")
                    }

                    // 通知IntelliJ平台进程已结束

                    xDebugSession.stop()


                    // 通知上层程序已退出
                    val category = if (event.exitCode != 0) "stderr" else "stdout"
                    notifyEvent(
                        DebugEvent.Output(
                            text = "Program exited with code: ${event.exitCode}",
                            category = category
                        )
                    )
                }

                is AdapterEvent.Terminated -> {
                    LOG.info("Debug session terminated by server")
                    _state.value = SessionState.Stopped

                    // 通知 IntelliJ 平台清理调试上下文
                    ApplicationManager.getApplication().invokeLater {
                        xDebugSession.stop()
                    }

                    notifyEvent(DebugEvent.Stopped)
                }

                is AdapterEvent.Output -> {
                    notifyEvent(
                        DebugEvent.Output(
                            text = event.output,
                            category = event.category
                        )
                    )
                }

                else -> {
                    // 处理其他事件
                    LOG.debug("Unhandled adapter event: $event")
                }
            }
        }
    }

    private fun mapPauseReason(reason: StopReason): PauseReason {
        return when (reason) {
            StopReason.STEP -> PauseReason.STEP
            StopReason.BREAKPOINT -> PauseReason.BREAKPOINT
            StopReason.EXCEPTION -> PauseReason.EXCEPTION
            StopReason.PAUSE -> PauseReason.PAUSE
            StopReason.ENTRY -> PauseReason.ENTRY
            StopReason.UNKNOWN -> PauseReason.PAUSE
        }
    }

    private suspend fun <T> executeCommand(
        name: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        return try {
            LOG.debug("Executing command: $name")
            block()
        } catch (e: Exception) {
            LOG.error("Command failed: $name", e)
            Result.failure(e)
        }
    }


    private fun notifyEvent(event: DebugEvent) {
        eventHandlers.forEach { handler ->
            try {
                handler(event)
            } catch (e: Exception) {
                LOG.error("Error notifying event handler", e)
            }
        }
    }

    override fun dispose() {
        eventSubscriptions.forEach { it.unsubscribe() }
        scope.cancel()
        if (::adapter.isInitialized) {
            adapter.dispose()
        }
    }
}