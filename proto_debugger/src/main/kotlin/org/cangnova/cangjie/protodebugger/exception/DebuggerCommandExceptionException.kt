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

package org.cangnova.cangjie.protodebugger.exception

/**
 * 调试器命令异常类
 *
 * 该异常类用于表示调试器命令执行过程中发生的错误。
 * 它是所有调试器相关异常的基类，提供了统一的错误处理机制。
 *
 * 使用场景：
 * - 调试器命令执行失败
 * - 与调试器通信时发生错误
 * - 调试器状态异常或不可用
 * - 调试器操作超时或中断
 *
 * @param s 错误消息，描述异常的具体原因
 * @param throwable 导致异常的根本原因，可能为null
 */
open class DebuggerCommandExceptionException @JvmOverloads constructor(
    s: String,
    throwable: Throwable? = null
) : Exception(s, throwable) {

    /**
     * 基于另一个异常创建调试器命令异常
     *
     * 当捕获到其他异常时，可以将其包装为DebuggerCommandException。
     * 如果原始异常没有消息，则使用空字符串。
     *
     * @param throwable 原始异常对象
     */
    constructor(throwable: Throwable) : this(throwable.message ?: "", throwable)
}
