/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.dap.exception

/**
 * 调试器异常基类
 *
 * 所有调试器相关异常的基类，提供统一的异常处理接口
 */
sealed class DebuggerException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * 调试会话异常
 *
 * 当调试会话操作失败时抛出
 */
class DebugSessionException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 适配器异常基类
 *
 * 所有调试适配器相关异常的基类
 */
sealed class AdapterException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * DAP适配器异常
 *
 * 当DAP协议适配器操作失败时抛出
 */
class DapAdapterException(
    message: String,
    cause: Throwable? = null
) : AdapterException(message, cause)

/**
 * DAP连接异常
 *
 * 当无法建立或维护与DAP服务器的连接时抛出
 */
class DapConnectionException(
    message: String,
    cause: Throwable? = null
) : AdapterException(message, cause)

/**
 * DAP客户端异常
 *
 * 当DAP客户端处理请求或响应时出现问题时抛出
 */
class DapClientException(
    message: String,
    cause: Throwable? = null
) : AdapterException(message, cause)

/**
 * 断点异常
 *
 * 当断点操作失败时抛出
 */
class BreakpointException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 表达式求值异常
 *
 * 当表达式求值失败时抛出
 */
class EvaluationException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 变量异常
 *
 * 当变量操作失败时抛出
 */
class VariableException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 服务器异常
 *
 * 当调试服务器启动、配置或运行时出现问题时抛出
 */
class ServerException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)

/**
 * 端口异常
 *
 * 当端口分配或检测失败时抛出
 */
class PortException(
    message: String,
    cause: Throwable? = null
) : DebuggerException(message, cause)