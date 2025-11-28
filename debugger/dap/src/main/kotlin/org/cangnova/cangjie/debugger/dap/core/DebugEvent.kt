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

package org.cangnova.cangjie.debugger.dap.core

import org.eclipse.lsp4j.debug.*

/**
 * 调试事件系统
 * 所有组件通过事件进行通信，解耦依赖
 */
sealed interface DebugEvent {
    /**
     * 事件时间戳
     */
    val timestamp: Long
        get() = System.currentTimeMillis()

    // ===== 服务器生命周期事件 =====

    /**
     * 服务器正在启动
     */
    data class ServerStarting(val port: Int) : DebugEvent

    /**
     * 服务器已启动并就绪
     */
    data class ServerStarted(val host: String, val port: Int, val pid: Long) : DebugEvent

    /**
     * 服务器启动失败
     */
    data class ServerStartFailed(val error: Throwable) : DebugEvent

    /**
     * 服务器正在停止
     */
    data class ServerStopping(val reason: String) : DebugEvent

    /**
     * 服务器已停止
     */
    data class ServerStopped(val reason: String) : DebugEvent

    /**
     * 服务器异常退出
     */
    data class ServerCrashed(val exitCode: Int, val error: Throwable?) : DebugEvent

    // ===== 连接生命周期事件 =====

    /**
     * 正在连接到服务器
     */
    data class Connecting(val host: String, val port: Int, val attempt: Int) : DebugEvent

    /**
     * 已连接到服务器
     */
    data class Connected(val host: String, val port: Int) : DebugEvent

    /**
     * 连接失败
     */
    data class ConnectionFailed(val host: String, val port: Int, val attempt: Int, val error: Throwable) : DebugEvent

    /**
     * 连接丢失
     */
    data class ConnectionLost(val error: Throwable) : DebugEvent

    /**
     * 连接已断开
     */
    data class Disconnected(val reason: String) : DebugEvent

    /**
     * 健康检查失败
     */
    data class HealthCheckFailed(val consecutiveFailures: Int, val error: Throwable) : DebugEvent

    // ===== 会话生命周期事件 =====

    /**
     * 会话初始化中
     */
    data object SessionInitializing : DebugEvent

    /**
     * 会话已初始化
     */
    data class SessionInitialized(val capabilities: Capabilities) : DebugEvent

    /**
     * 会话初始化失败
     */
    data class SessionInitializeFailed(val error: Throwable) : DebugEvent

    /**
     * 程序启动中
     */
    data class ProgramLaunching(val program: String) : DebugEvent

    /**
     * 程序已启动
     */
    data class ProgramLaunched(val program: String) : DebugEvent

    /**
     * 程序启动失败
     */
    data class ProgramLaunchFailed(val error: Throwable) : DebugEvent

    /**
     * 调试会话已就绪（配置完成）
     */
    data object SessionReady : DebugEvent

    /**
     * 会话正在终止
     */
    data class SessionTerminating(val reason: String) : DebugEvent

    /**
     * 会话已终止
     */
    data class SessionTerminated(val reason: String) : DebugEvent

    // ===== DAP协议事件 =====

    /**
     * 调试器已初始化（DAP initialized事件）
     */
    data object DebuggerInitialized : DebugEvent

    /**
     * 程序暂停
     */
    data class Stopped(
        val reason: String,
        val threadId: Long?,
        val allThreadsStopped: Boolean,
        val description: String?,
        val text: String?,
        val hitBreakpointIds: List<Int>?
    ) : DebugEvent {
        companion object {
            fun from(event: StoppedEventArguments): Stopped {
                return Stopped(
                    reason = event.reason ?: "unknown",
                    threadId = event.threadId?.toLong(),
                    allThreadsStopped = event.allThreadsStopped ?: false,
                    description = event.description,
                    text = event.text,
                    hitBreakpointIds = event.hitBreakpointIds?.map { it.toInt() }
                )
            }
        }
    }

    /**
     * 程序继续运行
     */
    data class Continued(val threadId: Long?, val allThreadsContinued: Boolean) : DebugEvent {
        companion object {
            fun from(event: ContinuedEventArguments): Continued {
                return Continued(
                    threadId = event.threadId?.toLong(),
                    allThreadsContinued = event.allThreadsContinued ?: true
                )
            }
        }
    }

    /**
     * 线程启动
     */
    data class ThreadStarted(val threadId: Long, val reason: String?) : DebugEvent

    /**
     * 线程退出
     */
    data class ThreadExited(val threadId: Long, val reason: String?) : DebugEvent

    /**
     * 程序退出
     */
    data class Exited(val exitCode: Int) : DebugEvent {
        companion object {
            fun from(event: ExitedEventArguments): Exited {
                return Exited(exitCode = event.exitCode ?: 0)
            }
        }
    }

    /**
     * 调试会话终止（DAP terminated事件）
     */
    data class Terminated(val restart: Boolean) : DebugEvent {
        companion object {
            fun from(event: TerminatedEventArguments): Terminated {
                return Terminated(restart = (event.restart as? Boolean) ?: false)
            }
        }
    }

    /**
     * 断点变化
     */
    data class BreakpointChanged(val reason: String, val breakpoint: Breakpoint) : DebugEvent {
        companion object {
            fun from(event: BreakpointEventArguments): BreakpointChanged {
                return BreakpointChanged(
                    reason = event.reason.toString(),
                    breakpoint = event.breakpoint
                )
            }
        }
    }

    /**
     * 输出信息
     */
    data class Output(
        val category: String?,
        val output: String,
        val source: Source?,
        val line: Int?,
        val column: Int?
    ) : DebugEvent {
        companion object {
            fun from(event: OutputEventArguments): Output {
                return Output(
                    category = event.category?.toString(),
                    output = event.output ?: "",
                    source = event.source,
                    line = event.line,
                    column = event.column
                )
            }
        }
    }

    /**
     * 模块加载
     */
    data class ModuleLoaded(val reason: String, val module: Module) : DebugEvent

    /**
     * 进程启动
     */
    data class ProcessStarted(val name: String, val systemProcessId: Long?, val isLocalProcess: Boolean) : DebugEvent

    // ===== 错误事件 =====

    /**
     * 请求失败
     */
    data class RequestFailed(val request: String, val error: Throwable) : DebugEvent

    /**
     * 协议错误
     */
    data class ProtocolError(val message: String, val error: Throwable?) : DebugEvent

    /**
     * 未知错误
     */
    data class UnexpectedError(val context: String, val error: Throwable) : DebugEvent
}

/**
 * 事件监听器
 */
fun interface DebugEventListener {
    suspend fun onEvent(event: DebugEvent)
}

/**
 * 事件总线接口
 */
interface DebugEventBus {
    /**
     * 订阅事件
     * @return 取消订阅的函数
     */
    fun subscribe(listener: DebugEventListener): () -> Unit

    /**
     * 订阅特定类型的事件
     */
    fun <T : DebugEvent> subscribe(eventClass: Class<T>, listener: suspend (T) -> Unit): () -> Unit

    /**
     * 发布事件
     */
    suspend fun publish(event: DebugEvent)

    /**
     * 同步发布事件（阻塞直到所有监听器处理完成）
     */
    suspend fun publishAndWait(event: DebugEvent)
}