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

package org.cangnova.cangjie.debugger.protobuf.util

/**
 * Shell命令执行结果类
 *
 * 该数据类封装了Shell命令执行的结果信息，包括标准输出、退出状态码和终止信号。
 * 它是调试器与系统交互的重要工具，用于执行外部命令并获取执行结果。
 *
 * @param output 命令的标准输出内容
 * @param status 命令的退出状态码，0通常表示成功
 * @param signal 终止进程的信号编号，0表示正常退出
 */
data class ShellCommandResult(
    val output: String,
    val status: Int,
    val signal: Int
) {

    /**
     * 检查命令是否成功执行
     */
    fun isSuccessful(): Boolean = status == 0 && signal == 0

    /**
     * 检查进程是否因信号而终止
     */
    fun isSignaled(): Boolean = signal != 0

    override fun toString(): String {
        return "ShellCommandResult(status=$status, signal=$signal, output='${output.take(100)}${if (output.length > 100) "..." else ""}')"
    }
}