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

package org.cangnova.cangjie.macro.service

/**
 * 宏展开错误
 *
 * 表示宏展开过程中发生的各种错误
 */
sealed class MacroExpansionError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 宏未找到
     */
    class MacroNotFound(
        val macroName: String
    ) : MacroExpansionError("宏 '$macroName' 未找到")

    /**
     * 宏展开失败
     */
    class ExpansionFailed(
        val macroName: String,
        val reason: String,
        cause: Throwable? = null
    ) : MacroExpansionError("宏 '$macroName' 展开失败: $reason", cause)

    /**
     * 编译器不可用
     */
    class CompilerUnavailable(
        val reason: String
    ) : MacroExpansionError("编译器不可用: $reason")

    /**
     * SDK 未配置
     */
    class SdkNotConfigured : MacroExpansionError("项目未配置仓颉 SDK")

    /**
     * 超时
     */
    class Timeout(
        val timeoutMs: Long
    ) : MacroExpansionError("宏展开超时 (${timeoutMs}ms)")

    /**
     * 解析错误
     */
    class ParseError(
        val details: String,
        cause: Throwable? = null
    ) : MacroExpansionError("宏展开结果解析错误: $details", cause)

    /**
     * 文件未找到
     */
    class FileNotFound(
        val filePath: String
    ) : MacroExpansionError("文件未找到: $filePath")

    /**
     * 无效的宏表达式
     */
    class InvalidMacroExpression(
        val details: String
    ) : MacroExpansionError("无效的宏表达式: $details")

    /**
     * 进程执行错误
     */
    class ProcessExecutionError(
        val commandLine: String,
        val exitCode: Int,
        val stderr: String,
        cause: Throwable? = null
    ) : MacroExpansionError("进程执行失败 (退出码: $exitCode): $stderr", cause)

    /**
     * 内部错误
     */
    class InternalError(
        val details: String,
        cause: Throwable? = null
    ) : MacroExpansionError("内部错误: $details", cause)

    /**
     * 不支持的操作
     */
    class UnsupportedOperation(
        val operation: String
    ) : MacroExpansionError("不支持的操作: $operation")
}
