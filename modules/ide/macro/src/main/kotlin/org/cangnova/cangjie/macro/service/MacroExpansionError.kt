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

import org.cangnova.cangjie.macro.messages.CangJieMacroBundle

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
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.not.found", macroName))

    /**
     * 宏展开失败
     */
    class ExpansionFailed(
        val macroName: String,
        val reason: String,
        cause: Throwable? = null
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.expansion.failed", macroName, reason), cause)

    /**
     * 编译器不可用
     */
    class CompilerUnavailable(
        val reason: String
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.compiler.unavailable", reason))

    /**
     * SDK 未配置
     */
    class SdkNotConfigured : MacroExpansionError(CangJieMacroBundle.message("macro.error.sdk.not.configured"))

    /**
     * 超时
     */
    class Timeout(
        val timeoutMs: Long
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.timeout", timeoutMs))

    /**
     * 解析错误
     */
    class ParseError(
        val details: String,
        cause: Throwable? = null
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.parse.error", details), cause)

    /**
     * 文件未找到
     */
    class FileNotFound(
        val filePath: String
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.file.not.found", filePath))

    /**
     * 无效的宏表达式
     */
    class InvalidMacroExpression(
        val details: String
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.invalid.expression", details))

    /**
     * 进程执行错误
     */
    class ProcessExecutionError(
        val commandLine: String,
        val exitCode: Int,
        val stderr: String,
        cause: Throwable? = null
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.process.execution", exitCode, stderr), cause)

    /**
     * 内部错误
     */
    class InternalError(
        val details: String,
        cause: Throwable? = null
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.internal", details), cause)

    /**
     * 不支持的操作
     */
    class UnsupportedOperation(
        val operation: String
    ) : MacroExpansionError(CangJieMacroBundle.message("macro.error.unsupported.operation", operation))
}