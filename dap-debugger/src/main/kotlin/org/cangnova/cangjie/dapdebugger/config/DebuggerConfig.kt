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

package org.cangnova.cangjie.dapdebugger.config

import com.intellij.openapi.project.Project

/**
 * 调试器配置
 *
 * 包含所有调试器相关的配置选项
 */
data class DebuggerConfig(
    val server: ServerConfig = ServerConfig(),
    val connection: ConnectionConfig = ConnectionConfig(),
    val logging: LoggingConfig = LoggingConfig()
) {
    companion object {
        /**
         * 从项目设置加载配置
         */
        fun load(project: Project): DebuggerConfig {
            // TODO: 从项目设置加载配置
            return DEFAULT
        }

        /**
         * 默认配置
         */
        val DEFAULT = DebuggerConfig()
    }

    /**
     * 保存配置到项目设置
     */
    fun save(project: Project) {
        // TODO: 保存配置到项目设置
    }
}

/**
 * 服务器配置
 */
data class ServerConfig(
    val portRange: IntRange = 58920..58930,
    val debuggerType: String = "lldbapi",
    val startupTimeoutMs: Long = 10000
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
 * 日志配置
 */
data class LoggingConfig(
    val level: LogLevel = LogLevel.INFO,
    val logToFile: Boolean = true,
    val logPath: String? = null
)

/**
 * 日志级别
 */
enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}