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

package org.cangnova.cangjie.debugger.dap.server

import com.intellij.openapi.Disposable
import kotlinx.coroutines.flow.StateFlow

/**
 * 服务器生命周期管理器
 *
 * 负责DAP服务器进程的启动、停止和监控
 */
interface ServerLifecycleManager : Disposable {

    /**
     * 启动DAP服务器
     *
     * @param config 服务器配置
     * @param debuggerProvider 调试器提供者
     * @return 服务器进程，失败返回错误
     */
    suspend fun startServer(
        config: ServerConfig,
        debuggerProvider: org.cangnova.cangjie.debugger.DebuggerProvider
    ): Result<ServerProcess>

    /**
     * 停止服务器
     *
     * @param processId 进程ID
     * @param gracefulTimeoutMs 优雅停止超时时间
     * @return 停止结果
     */
    suspend fun stopServer(processId: String, gracefulTimeoutMs: Long = 3000): Result<Unit>

    /**
     * 获取服务器进程
     *
     * @param processId 进程ID
     */
    fun getServer(processId: String): ServerProcess?

    /**
     * 获取所有运行中的服务器
     */
    fun getAllServers(): List<ServerProcess>

    /**
     * 停止所有服务器
     */
    suspend fun stopAllServers(gracefulTimeoutMs: Long = 3000)
}

/**
 * 服务器进程
 */
interface ServerProcess {

    /**
     * 进程ID
     */
    val id: String

    /**
     * 服务器配置
     */
    val config: ServerConfig

    /**
     * 服务器状态
     */
    val state: StateFlow<ServerState>

    /**
     * 服务器端口
     */
    val port: Int

    /**
     * 进程PID
     */
    val pid: Long?

    /**
     * 检查服务器是否运行
     */
    fun isRunning(): Boolean

    /**
     * 等待服务器就绪
     *
     * @param timeoutMs 超时时间
     * @return 是否就绪
     */
    suspend fun waitForReady(timeoutMs: Long = 10000): Result<Unit>

    /**
     * 停止服务器
     */
    suspend fun stop(gracefulTimeoutMs: Long = 3000): Result<Unit>
}

/**
 * 服务器配置
 */
data class ServerConfig(
    /**
     * 服务器可执行文件路径
     */
    val executablePath: String,

    /**
     * 服务器参数
     */
    val arguments: List<String> = emptyList(),

    /**
     * 工作目录
     */
    val workingDirectory: String? = null,

    /**
     * 环境变量
     */
    val environment: Map<String, String> = emptyMap(),

    /**
     * 端口分配策略
     */
    val portStrategy: PortStrategy = PortStrategy.AutoAllocate,

    /**
     * 启动超时（毫秒）
     */
    val startupTimeoutMs: Long = 10000,

    /**
     * 调试器类型
     */
    val debuggerType: String = "lldbapi"
)

/**
 * 端口分配策略
 */
sealed class PortStrategy {
    /**
     * 自动分配
     */
    object AutoAllocate : PortStrategy()

    /**
     * 使用指定端口
     */
    data class UsePort(val port: Int) : PortStrategy()

    /**
     * 从范围中分配
     */
    data class AllocateFromRange(val startPort: Int, val endPort: Int) : PortStrategy()
}

/**
 * 服务器状态
 */
sealed class ServerState {
    /**
     * 未启动
     */
    object Idle : ServerState()

    /**
     * 启动中
     */
    object Starting : ServerState()

    /**
     * 运行中
     */
    data class Running(val port: Int, val pid: Long?) : ServerState()

    /**
     * 停止中
     */
    object Stopping : ServerState()

    /**
     * 已停止
     */
    object Stopped : ServerState()

    /**
     * 启动失败
     */
    data class Failed(val error: Throwable) : ServerState()

    /**
     * 崩溃
     */
    data class Crashed(val exitCode: Int) : ServerState()
}