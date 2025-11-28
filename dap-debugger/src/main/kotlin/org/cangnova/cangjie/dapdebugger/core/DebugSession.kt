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

package org.cangnova.cangjie.dapdebugger.core

import com.intellij.openapi.Disposable
import kotlinx.coroutines.flow.StateFlow

/**
 * 调试会话接口
 *
 * 管理调试会话的完整生命周期，包括启动、暂停、继续、停止等操作。
 */
interface DebugSession : Disposable {

    /**
     * 会话唯一标识
     */
    val sessionId: String

    /**
     * 会话状态
     */
    val state: StateFlow<SessionState>

    /**
     * 调试适配器
     */
    val adapter: DebugAdapter

    /**
     * 启动调试会话
     *
     * @param config 启动配置
     * @return 启动结果
     */
    suspend fun start(config: LaunchConfig): Result<Unit>

    /**
     * 继续执行
     *
     * @param threadId 线程ID，null表示所有线程
     */
    suspend fun resume(threadId: Long? = null): Result<Unit>

    /**
     * 暂停执行
     *
     * @param threadId 线程ID，null表示所有线程
     */
    suspend fun pause(threadId: Long? = null): Result<Unit>

    /**
     * 单步执行
     */
    suspend fun stepOver(threadId: Long): Result<Unit>
    suspend fun stepInto(threadId: Long): Result<Unit>
    suspend fun stepOut(threadId: Long): Result<Unit>

    /**
     * 停止调试会话
     */
    suspend fun stop(): Result<Unit>

    /**
     * 获取当前线程列表
     */
    suspend fun getThreads(): Result<List<ThreadInfo>>

    /**
     * 获取堆栈帧
     */
    suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>>

    /**
     * 订阅会话事件
     */
    fun subscribeEvents(handler: (DebugEvent) -> Unit): Subscription
}

/**
 * 会话状态
 */
sealed class SessionState {
    object Idle : SessionState()
    object Starting : SessionState()
    object Running : SessionState()
    data class Paused(val threadId: Long, val reason: PauseReason) : SessionState()
    object Stopping : SessionState()
    object Stopped : SessionState()
    data class Error(val error: Throwable) : SessionState()
}

/**
 * 暂停原因
 */
enum class PauseReason {
    BREAKPOINT,
    STEP,
    PAUSE,
    EXCEPTION,
    ENTRY
}

/**
 * 启动配置
 */
data class LaunchConfig(
    val program: String,
    val arguments: List<String> = emptyList(),
    val workingDirectory: String,
    val environment: Map<String, String> = emptyMap(),
    val adapterConfig: AdapterConfig
)

/**
 * 适配器配置
 */
data class AdapterConfig(
    val host: String = "localhost",
    val port: Int,
    val connectionConfig: ConnectionConfig
)

/**
 * 连接配置
 */
data class ConnectionConfig(
    val maxRetries: Int = 10,
    val retryDelayMs: Long = 500,
    val connectionTimeoutMs: Long = 5000
)

/**
 * 线程信息
 */
data class ThreadInfo(
    val id: Long,
    val name: String
)

/**
 * 堆栈帧信息
 */
data class StackFrameInfo(
    val id: Long,
    val name: String,
    val source: SourceInfo?,
    val line: Int,
    val column: Int
)

/**
 * 源文件信息
 */
data class SourceInfo(
    val path: String?,
    val name: String?
)

/**
 * 调试事件
 */
sealed class DebugEvent {
    object Started : DebugEvent()
    data class Paused(val threadId: Long, val reason: PauseReason) : DebugEvent()
    object Resumed : DebugEvent()
    object Stopped : DebugEvent()
    data class Output(val text: String, val category: String) : DebugEvent()
}

/**
 * 订阅接口
 */
interface Subscription {
    fun unsubscribe()
}