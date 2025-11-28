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

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.StateFlow
import org.cangnova.cangjie.debugger.dap.connection.ManagedConnection
import org.cangnova.cangjie.debugger.dap.core.*
import org.cangnova.cangjie.debugger.dap.server.ServerProcess

/**
 * 调试会话协调器
 *
 * 核心组件，负责编排服务器启动、连接建立、协议初始化和会话生命周期管理
 */
interface DebugSessionCoordinator : Disposable {

    /**
     * 创建新的调试会话
     *
     * @param config 调试配置
     * @return 会话ID，失败返回错误
     */
    suspend fun createSession(config: DebugSessionConfig): Result<String>

    /**
     * 获取会话
     *
     * @param sessionId 会话ID
     */
    fun getSession(sessionId: String): ManagedDebugSession?

    /**
     * 销毁会话
     *
     * @param sessionId 会话ID
     * @param timeoutMs 超时时间
     */
    suspend fun destroySession(sessionId: String, timeoutMs: Long = 5000): Result<Unit>

    /**
     * 获取所有活跃会话
     */
    fun getActiveSessions(): List<ManagedDebugSession>

    /**
     * 获取会话状态
     *
     * @param sessionId 会话ID
     */
    fun getSessionState(sessionId: String): StateFlow<SessionState>?

    companion object {
        /**
         * 获取项目的协调器实例
         */
        fun getInstance(project: Project): DebugSessionCoordinator {
            return project.getService(DebugSessionCoordinator::class.java)
        }
    }
}

/**
 * 托管的调试会话
 *
 * 封装了调试会话的所有资源和操作
 */
interface ManagedDebugSession : Disposable {

    /**
     * 会话ID
     */
    val id: String

    /**
     * 会话配置
     */
    val config: DebugSessionConfig

    /**
     * 会话状态
     */
    val state: StateFlow<SessionState>

    /**
     * 服务器进程
     */
    val serverProcess: ServerProcess

    /**
     * DAP连接
     */
    val connection: ManagedConnection

    /**
     * 调试适配器
     */
    val adapter: DebugAdapter

    /**
     * 断点管理器
     */
    val breakpointManager: BreakpointManager

    /**
     * 表达式求值引擎
     */
    val evaluationEngine: EvaluationEngine

    /**
     * 启动会话
     */
    suspend fun start(): Result<Unit>

    /**
     * 停止会话
     */
    suspend fun stop(): Result<Unit>

    /**
     * 恢复执行
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
     * 步过
     *
     * @param threadId 线程ID
     */
    suspend fun stepOver(threadId: Long): Result<Unit>

    /**
     * 步进
     *
     * @param threadId 线程ID
     */
    suspend fun stepInto(threadId: Long): Result<Unit>

    /**
     * 步出
     *
     * @param threadId 线程ID
     */
    suspend fun stepOut(threadId: Long): Result<Unit>

    /**
     * 获取作用域
     *
     * @param frameId 栈帧ID
     */
    suspend fun getScopes(frameId: Long): Result<List<Scope>>

    /**
     * 获取变量
     *
     * @param variablesReference 变量引用ID
     */
    suspend fun getVariables(variablesReference: Long): Result<List<Variable>>

    /**
     * 设置变量值
     *
     * @param variablesReference 变量引用ID
     * @param name 变量名
     * @param value 新值
     */
    suspend fun setVariable(
        variablesReference: Long,
        name: String,
        value: String
    ): Result<Variable>

    /**
     * 订阅会话事件
     */
    fun subscribeEvents(handler: (AdapterEvent) -> Unit): Subscription
}

/**
 * 调试会话配置
 */
data class DebugSessionConfig(
    /**
     * 项目
     */
    val project: Project,

    /**
     * 服务器配置
     */
    val serverConfig: org.cangnova.cangjie.debugger.dap.server.ServerConfig,

    /**
     * 启动参数
     */
    val launchArguments: LaunchArguments,

    /**
     * 适配器配置
     */
    val adapterConfig: AdapterConfig,

    /**
     * 会话超时配置
     */
    val timeouts: SessionTimeouts = SessionTimeouts()
)

/**
 * 会话超时配置
 */
data class SessionTimeouts(
    /**
     * 初始化超时（毫秒）
     */
    val initializationTimeoutMs: Long = 10000,

    /**
     * 启动超时（毫秒）
     */
    val launchTimeoutMs: Long = 30000,

    /**
     * 操作超时（毫秒）
     */
    val operationTimeoutMs: Long = 5000,

    /**
     * 停止超时（毫秒）
     */
    val stopTimeoutMs: Long = 5000
)

/**
 * 会话生命周期监听器
 */
interface SessionLifecycleListener {
    /**
     * 会话创建时调用
     */
    fun onSessionCreated(sessionId: String) {}

    /**
     * 会话启动时调用
     */
    fun onSessionStarted(sessionId: String) {}

    /**
     * 会话状态变化时调用
     */
    fun onSessionStateChanged(sessionId: String, oldState: SessionState, newState: SessionState) {}

    /**
     * 会话停止时调用
     */
    fun onSessionStopped(sessionId: String) {}

    /**
     * 会话销毁时调用
     */
    fun onSessionDestroyed(sessionId: String) {}
}