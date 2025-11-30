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

import org.cangnova.cangjie.debugger.dap.connection.ConnectionConfig

/**
 * 适配器配置
 *
 * 注意：在新架构中，此配置已不再实际使用，保留仅为接口兼容
 */
data class AdapterConfig(
    val host: String = "localhost",
    val port: Int,
    val connectionConfig: ConnectionConfig
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
 * 订阅接口
 */
interface Subscription {
    fun unsubscribe()
}