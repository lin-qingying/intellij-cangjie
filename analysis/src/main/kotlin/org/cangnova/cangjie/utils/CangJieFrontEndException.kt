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

package org.cangnova.cangjie.utils

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.PsiDiagnosticUtils
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments

/**
 * 构建异常消息
 *
 * 为编译器各个子系统（前端、后端、分析器等）生成统一格式的异常消息。
 * 消息包含子系统名称、错误描述、文件位置和根本原因的堆栈信息。
 *
 * **消息格式**：
 * ```
 * <subsystemName> Internal error: <message>
 * File being compiled: <location>
 * The root cause <causeClass> was thrown at: <stackTrace>
 * ```
 *
 * **使用场景**：
 * - 编译器前端（语法分析、语义分析）错误报告
 * - 代码生成阶段错误报告
 * - IDE 后台分析任务的错误日志
 *
 * @param subsystemName 子系统名称（如 "Front-end", "Backend", "Analyzer"）
 * @param message 错误描述信息
 * @param cause 根本原因异常，null 表示没有根本原因
 * @param location 错误发生的文件位置（文件路径和行号），null 表示位置未知
 * @return String 格式化的异常消息
 */
fun getExceptionMessage(
    subsystemName: String,
    message: String,
    cause: Throwable?,
    location: String?
): String =
    buildString {
        append(subsystemName).append(" Internal error: ").appendLine(message)

        if (location != null) {
            append("File being compiled: ").appendLine(location)
        } else {
            appendLine("File is unknown")
        }

        if (cause != null) {
            append("The root cause ${cause::class.java.name} was thrown at: ")
            append(cause.stackTrace?.firstOrNull()?.toString() ?: "unknown")
        }
    }


/**
 * 仓颉编译器前端异常
 *
 * 表示编译器前端（词法分析、语法分析、语义分析）阶段发生的内部错误。
 * 此异常携带详细的上下文信息，包括出错位置的 PSI 元素附件，便于调试和问题追踪。
 *
 * **前端阶段包括**：
 * - 词法分析（Lexing）：将源码转换为 token 流
 * - 语法分析（Parsing）：构建 PSI 树
 * - 语义分析（Semantic Analysis）：类型检查、名称解析等
 *
 * **异常特点**：
 * - 继承自 [CangJieExceptionWithAttachments]，支持附加文件和诊断信息
 * - 自动捕获出错的 PSI 元素，生成 ".cj" 后缀的附件
 * - 包含详细的错误位置信息（文件路径、行号、列号）
 *
 * **使用示例**：
 * ```kotlin
 * try {
 *     analyzeExpression(psiElement)
 * } catch (e: Exception) {
 *     throw CangJieFrontEndException("Failed to analyze expression", e, psiElement)
 * }
 * ```
 *
 * @property message 异常消息（由 [getExceptionMessage] 生成）
 * @property cause 根本原因异常
 * @constructor 创建前端异常，附加 PSI 元素上下文
 * @param message 错误描述
 * @param cause 根本原因异常
 * @param element 出错的 PSI 元素，将作为附件添加到异常中
 */
class CangJieFrontEndException(message: String, cause: Throwable) :
    CangJieExceptionWithAttachments(message, cause) {
    constructor(
        message: String,
        cause: Throwable,
        element: PsiElement
    ) : this(getExceptionMessage("Front-end", message, cause, PsiDiagnosticUtils.atLocation(element)), cause) {
        withPsiAttachment("element.cj", element)
    }
}
