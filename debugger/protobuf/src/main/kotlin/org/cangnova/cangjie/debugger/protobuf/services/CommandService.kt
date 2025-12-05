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

package org.cangnova.cangjie.debugger.protobuf.services

/**
 * 命令服务接口
 *
 * 负责执行 LLDB 控制台命令，提供对调试器命令行接口的访问
 */
interface CommandService {
    /**
     * 执行 LLDB 命令
     *
     * 执行任意 LLDB 控制台命令，支持所有内置命令。
     * 这提供了对 LLDB 功能的完全访问。
     *
     * 支持的命令示例：
     *   - "thread list" - 列出所有线程
     *   - "bt" - 显示调用栈
     *   - "expr myVar" - 求值表达式
     *   - "p *ptr" - 打印指针指向的内容
     *   - "memory read 0x1234" - 读取内存
     *   - "disassemble -n main" - 反汇编函数
     *   - "settings set targetPlatform.x86-disassembly-flavor intel" - 设置选项
     *
     * @param command LLDB 命令字符串
     * @param echoCommand 是否在命令执行时回显命令（类似控制台）
     * @param asyncExecution 是否异步执行（立即返回，适用于长时间运行的命令）
     * @param threadId 可选：执行上下文的线程 ID（某些命令需要线程上下文）
     * @param frameIndex 可选：执行上下文的栈帧索引（某些命令需要栈帧上下文）
     * @return 命令执行结果，包括标准输出和错误输出
     */
    suspend fun executeCommand(
        command: String,
        echoCommand: Boolean = false,
        asyncExecution: Boolean = false,
        threadId: Long? = null,
        frameIndex: Int? = null
    ): CommandResult

    /**
     * 在指定线程和栈帧上下文中执行命令
     *
     * 这是一个便捷方法，用于在特定上下文中执行命令。
     *
     * @param threadId 线程 ID
     * @param frameIndex 栈帧索引
     * @param command 命令字符串
     * @return 命令执行结果
     */
    suspend fun executeCommandInContext(
        threadId: Long,
        frameIndex: Int,
        command: String
    ): CommandResult = executeCommand(
        command = command,
        threadId = threadId,
        frameIndex = frameIndex
    )

    /**
     * 获取 LLDB 命令补全建议
     *
     * 根据部分输入的命令获取自动补全建议，支持命令、参数、选项等的补全。
     *
     * 补全示例：
     *   - "bre" -> ["break", "breakpoint"]
     *   - "breakpoint set -n mai" -> ["main"]
     *   - "thread " -> ["list", "select", "backtrace", ...]
     *
     * @param partialCommand 部分输入的命令字符串
     * @param cursorPosition 光标在命令字符串中的位置（字符索引），0 表示末尾
     * @param maxResults 最大返回结果数量，0 表示返回所有结果
     * @return 命令补全结果
     */
    suspend fun getCommandCompletions(
        partialCommand: String,
        cursorPosition: Int = 0,
        maxResults: Int = 50
    ): CommandCompletionResult
}

/**
 * 命令执行结果
 *
 * 封装 LLDB 命令执行的结果信息
 */
data class CommandResult(
    /**
     * 命令是否成功执行
     */
    val success: Boolean,

    /**
     * 标准输出内容
     */
    val output: String,

    /**
     * 错误输出内容（如果有）
     */
    val error: String = "",

    /**
     * 错误消息（如果命令失败）
     */
    val errorMessage: String = ""
) {
    /**
     * 获取完整输出（标准输出 + 错误输出）
     */
    val fullOutput: String
        get() = buildString {
            if (output.isNotEmpty()) {
                append(output)
            }
            if (error.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(error)
            }
        }

    companion object {
        /**
         * 创建成功的命令结果
         */
        fun success(output: String, error: String = ""): CommandResult =
            CommandResult(
                success = true,
                output = output,
                error = error
            )

        /**
         * 创建失败的命令结果
         */
        fun failure(errorMessage: String, output: String = "", error: String = ""): CommandResult =
            CommandResult(
                success = false,
                output = output,
                error = error,
                errorMessage = errorMessage
            )
    }
}

/**
 * 命令补全结果
 *
 * 封装命令补全的结果信息
 */
data class CommandCompletionResult(
    /**
     * 补全是否成功
     */
    val success: Boolean,

    /**
     * 补全选项列表
     * 所有可能的补全候选项
     */
    val completions: List<String>,

    /**
     * 所有补全选项的公共前缀
     * 用于自动填充用户输入
     */
    val commonPrefix: String = "",

    /**
     * 补全的起始位置
     * 补全文本应该插入的位置（相对于原始命令字符串）
     */
    val completionStart: Int = 0,

    /**
     * 错误消息（如果补全失败）
     */
    val errorMessage: String = ""
) {
    companion object {
        /**
         * 创建成功的补全结果
         */
        fun success(
            completions: List<String>,
            commonPrefix: String = "",
            completionStart: Int = 0
        ): CommandCompletionResult =
            CommandCompletionResult(
                success = true,
                completions = completions,
                commonPrefix = commonPrefix,
                completionStart = completionStart
            )

        /**
         * 创建失败的补全结果
         */
        fun failure(errorMessage: String): CommandCompletionResult =
            CommandCompletionResult(
                success = false,
                completions = emptyList(),
                errorMessage = errorMessage
            )

        /**
         * 创建空的补全结果（没有匹配项）
         */
        fun empty(): CommandCompletionResult =
            CommandCompletionResult(
                success = true,
                completions = emptyList()
            )
    }
}