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

package org.cangnova.cangjie.debugger.core

import com.intellij.openapi.Disposable
import kotlinx.coroutines.flow.StateFlow

/**
 * 调试适配器接口
 *
 * 抽象不同调试协议的实现细节，提供统一的调试操作接口。
 */
interface DebugAdapter : Disposable {

    /**
     * 适配器类型
     */
    val type: AdapterType

    /**
     * 连接状态
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * 初始化适配器
     */
    suspend fun initialize(config: AdapterConfig): Result<Capabilities>

    /**
     * 配置完成
     */
    suspend fun configurationDone(): Result<Unit>

    /**
     * 启动调试目标
     */
    suspend fun launch(args: LaunchArguments): Result<Unit>

    /**
     * 附加到运行中的进程
     */
    suspend fun attach(args: AttachArguments): Result<Unit>

    /**
     * 断开连接
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * 设置断点
     */
    suspend fun setBreakpoints(
        source: SourceFile,
        breakpoints: List<BreakpointSpec>
    ): Result<List<BreakpointResult>>

    /**
     * 求值表达式
     */
    suspend fun evaluate(
        expression: String,
        frameId: Long,
        context: EvaluationType
    ): Result<EvaluationResult>

    /**
     * 获取变量
     */
    suspend fun getVariables(
        variablesReference: Long
    ): Result<List<Variable>>

    /**
     * 获取堆栈跟踪
     */
    suspend fun getStackTrace(threadId: Long): Result<List<StackFrameInfo>>

    /**
     * 获取线程列表
     */
    suspend fun getThreads(): Result<List<ThreadInfo>>

    /**
     * 继续执行
     */
    suspend fun continueExecution(threadId: Long): Result<Unit>

    /**
     * 暂停执行
     */
    suspend fun pause(threadId: Long): Result<Unit>

    /**
     * 单步执行（跳过）
     */
    suspend fun next(threadId: Long): Result<Unit>

    /**
     * 单步执行（步入）
     */
    suspend fun stepIn(threadId: Long): Result<Unit>

    /**
     * 单步执行（步出）
     */
    suspend fun stepOut(threadId: Long): Result<Unit>

    /**
     * 获取作用域
     */
    suspend fun getScopes(frameId: Long): Result<List<Scope>>

    /**
     * 设置变量值
     */
    suspend fun setVariable(
        variablesReference: Long,
        name: String,
        value: String
    ): Result<Variable>

    /**
     * 终止调试目标程序
     */
    suspend fun terminate(): Result<Unit>

    /**
     * 订阅适配器事件
     */
    fun subscribeEvents(handler: (AdapterEvent) -> Unit): Subscription
}

/**
 * 适配器类型
 */
enum class AdapterType {
    DAP,    // Debug Adapter Protocol
    GDB,    // GDB/MI Protocol
    LLDB    // LLDB Protocol
}

/**
 * 连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Failed(val error: Throwable) : ConnectionState()
}

/**
 * 适配器能力
 */
data class Capabilities(
    val supportsConfigurationDoneRequest: Boolean = false,
    val supportsFunctionBreakpoints: Boolean = false,
    val supportsConditionalBreakpoints: Boolean = false,
    val supportsHitConditionalBreakpoints: Boolean = false,
    val supportsEvaluateForHovers: Boolean = false,
    val supportsStepBack: Boolean = false,
    val supportsSetVariable: Boolean = false,
    val supportsRestartFrame: Boolean = false,
    val supportsGotoTargetsRequest: Boolean = false,
    val supportsStepInTargetsRequest: Boolean = false,
    val supportsCompletionsRequest: Boolean = false,
    val supportsModulesRequest: Boolean = false,
    val supportsRestartRequest: Boolean = false,
    val supportsExceptionOptions: Boolean = false,
    val supportsValueFormattingOptions: Boolean = false,
    val supportsExceptionInfoRequest: Boolean = false,
    val supportTerminateDebuggee: Boolean = false,
    val supportSuspendDebuggee: Boolean = false,
    val supportsDelayedStackTraceLoading: Boolean = false,
    val supportsLoadedSourcesRequest: Boolean = false,
    val supportsLogPoints: Boolean = false,
    val supportsTerminateThreadsRequest: Boolean = false,
    val supportsSetExpression: Boolean = false,
    val supportsTerminateRequest: Boolean = false,
    val supportsDataBreakpoints: Boolean = false,
    val supportsReadMemoryRequest: Boolean = false,
    val supportsWriteMemoryRequest: Boolean = false,
    val supportsDisassembleRequest: Boolean = false,
    val supportsCancelRequest: Boolean = false,
    val supportsBreakpointLocationsRequest: Boolean = false,
    val supportsClipboardContext: Boolean = false,
    val supportsSteppingGranularity: Boolean = false,
    val supportsInstructionBreakpoints: Boolean = false,
    val supportsExceptionFilterOptions: Boolean = false
)

/**
 * 启动参数
 */
data class LaunchArguments(
    val program: String,
    val arguments: List<String> = emptyList(),
    val workingDirectory: String,
    val environment: Map<String, String> = emptyMap()
)

/**
 * 附加参数
 */
data class AttachArguments(
    val processId: Long
)

/**
 * 源文件
 */
data class SourceFile(
    val path: String,
    val name: String
)

/**
 * 断点规格
 */
data class BreakpointSpec(
    val line: Int,
    val column: Int?,
    val condition: String?,
    val logMessage: String?
)

/**
 * 断点结果
 */
data class BreakpointResult(
    val id: Int,
    val verified: Boolean,
    val line: Int,
    val message: String?
)

/**
 * 求值类型
 */
enum class EvaluationType {
    WATCH,      // 监视表达式
    REPL,       // REPL控制台
    HOVER,      // 悬停提示
    CONDITIONAL // 条件断点
}

/**
 * 求值结果
 */
data class EvaluationResult(
    val value: String,
    val type: String?,
    val variablesReference: Long,
    val presentationHint: PresentationHint? = null
)

/**
 * 展示提示
 */
data class PresentationHint(
    val kind: String?,
    val attributes: List<String>?
)

/**
 * 变量
 */
data class Variable(
    val name: String,
    val value: String,
    val type: String,
    val variablesReference: Long,
    val presentationHint: String? = null
)

/**
 * 适配器事件
 */
sealed class AdapterEvent {
    object Initialized : AdapterEvent()

    data class Stopped(
        val reason: StopReason,
        val threadId: Long,
        val allThreadsStopped: Boolean,
        val hitBreakpointIds: List<Int> = emptyList(),
        val description: String?,
        val text: String?
    ) : AdapterEvent()

    data class Continued(
        val threadId: Long,
        val allThreadsContinued: Boolean
    ) : AdapterEvent()

    data class Exited(val exitCode: Int) : AdapterEvent()
    object Terminated : AdapterEvent()

    data class ThreadStarted(val threadId: Long) : AdapterEvent()
    data class ThreadExited(val threadId: Long) : AdapterEvent()

    data class Output(
        val category: String,
        val output: String,
        val source: String?,
        val line: Int?,
        val column: Int?
    ) : AdapterEvent()

    data class BreakpointChanged(
        val reason: String,
        val breakpoint: BreakpointInfo
    ) : AdapterEvent()

    data class Invalidated(val areas: List<String>) : AdapterEvent()
}

/**
 * 停止原因
 */
enum class StopReason {
    STEP,
    BREAKPOINT,
    EXCEPTION,
    PAUSE,
    ENTRY,
    UNKNOWN
}

/**
 * 断点信息
 */
data class BreakpointInfo(
    val id: Int,
    val verified: Boolean,
    val line: Int,
    val column: Int?,
    val message: String?,
    val source: String?
)

/**
 * 作用域
 */
data class Scope(
    val name: String,
    val variablesReference: Long,
    val expensive: Boolean = false,
    val presentationHint: String? = null
)
