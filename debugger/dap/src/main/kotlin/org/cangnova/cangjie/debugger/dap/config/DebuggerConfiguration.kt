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

package org.cangnova.cangjie.debugger.dap.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 调试器配置系统
 * 提供可配置的参数，支持运行时修改
 */
@Service(Service.Level.PROJECT)
@State(
    name = "CangJieDebuggerConfiguration",
    storages = [Storage("cangjie-debugger.xml")]
)
class DebuggerConfiguration : PersistentStateComponent<DebuggerConfiguration.State> {

    private var state = State()

    data class State(
        // 服务器配置
        var serverStartupTimeout: Long = 10000,
        var serverPortRangeStart: Int = 58920,
        var serverPortRangeEnd: Int = 58930,
        var serverLogEnabled: Boolean = true,
        var serverDebuggerType: String = "lldbapi",

        // 连接配置
        var connectionMaxRetries: Int = 10,
        var connectionRetryDelay: Long = 500,
        var connectionTimeout: Long = 5000,
        var connectionHealthCheckInterval: Long = 5000,
        var connectionHealthCheckTimeout: Long = 2000,

        // 会话配置
        var sessionInitTimeout: Long = 10000,
        var sessionLaunchTimeout: Long = 30000,
        var sessionRequestTimeout: Long = 10000,

        // 功能开关
        var enableAutoReconnect: Boolean = true,
        var enableHealthCheck: Boolean = true,
        var enableDetailedLogging: Boolean = false,
        var enableServerStderrLogging: Boolean = true
    )

    override fun getState(): State = state
    override fun loadState(state: State) {
        this.state = state
    }

    // 服务器配置
    val serverStartupTimeout: Duration
        get() = state.serverStartupTimeout.milliseconds

    val serverPortRange: IntRange
        get() = state.serverPortRangeStart..state.serverPortRangeEnd

    val serverLogEnabled: Boolean
        get() = state.serverLogEnabled

    val serverDebuggerType: String
        get() = state.serverDebuggerType

    // 连接配置
    val connectionMaxRetries: Int
        get() = state.connectionMaxRetries

    val connectionRetryDelay: Duration
        get() = state.connectionRetryDelay.milliseconds

    val connectionTimeout: Duration
        get() = state.connectionTimeout.milliseconds

    val connectionHealthCheckInterval: Duration
        get() = state.connectionHealthCheckInterval.milliseconds

    val connectionHealthCheckTimeout: Duration
        get() = state.connectionHealthCheckTimeout.milliseconds

    // 会话配置
    val sessionInitTimeout: Duration
        get() = state.sessionInitTimeout.milliseconds

    val sessionLaunchTimeout: Duration
        get() = state.sessionLaunchTimeout.milliseconds

    val sessionRequestTimeout: Duration
        get() = state.sessionRequestTimeout.milliseconds

    // 功能开关
    val enableAutoReconnect: Boolean
        get() = state.enableAutoReconnect

    val enableHealthCheck: Boolean
        get() = state.enableHealthCheck

    val enableDetailedLogging: Boolean
        get() = state.enableDetailedLogging

    val enableServerStderrLogging: Boolean
        get() = state.enableServerStderrLogging

    companion object {
        fun getInstance(project: Project): DebuggerConfiguration =
            project.service<DebuggerConfiguration>()
    }
}