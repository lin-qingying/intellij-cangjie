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

package org.cangnova.cangjie.lsp4ij.diagnostics

import org.cangnova.telemetry.api.EventCategories
import org.cangnova.telemetry.api.TelemetryService
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import java.util.concurrent.ConcurrentHashMap

/**
 * LSP诊断信息遥测工具类，用于从LSP诊断信息中收集和发送遥测数据
 */
object DiagnosticTelemetry {
    private val logger = Logger.getInstance(DiagnosticTelemetry::class.java)

    // 用于去重，避免重复发送相同的错误
    private val reportedDiagnostics = ConcurrentHashMap<String, Long>()

    // 每个错误码的报告间隔时间（毫秒）
    private const val REPORT_INTERVAL_MS = 3600000L // 1小时

    // 代码片段上下文行数
    private const val CONTEXT_LINES = 2

    /**
     * 从LSP诊断信息中收集和发送遥测数据
     *
     * @param diagnostic LSP诊断信息
     * @param source 诊断信息来源
     * @param code 诊断代码或HTML代码片段
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendDiagnostic(
        diagnostic: Diagnostic,
        source: String = "lsp",
        code: String = ""
    ): Boolean {
        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 提取错误码
            val errorCode = extractErrorCode(diagnostic)

            // 检查是否需要去重
            val now = System.currentTimeMillis()
            val lastReportTime = reportedDiagnostics.getOrDefault(errorCode, 0L)
            if (now - lastReportTime < REPORT_INTERVAL_MS) {
                return false
            }

            // 更新最后报告时间
            reportedDiagnostics[errorCode] = now

            // 构建属性
            val properties = HashMap<String, String>()
            properties["error_code"] = errorCode
            properties["message"] = diagnostic.message
            properties["severity"] = getSeverityString(diagnostic.severity)
            properties["source"] = source

            // 如果提供了代码片段，添加到属性中
            if (code.isNotEmpty()) {
                properties["code_snippet"] = code
            } else {
                // 否则仅添加位置信息
                diagnostic.range?.let { range ->
                    properties["start_line"] = range.start.line.toString()
                    properties["start_character"] = range.start.character.toString()
                    properties["end_line"] = range.end.line.toString()
                    properties["end_character"] = range.end.character.toString()
                }
            }

            // 发送遥测事件
            return telemetryService.sendEvent(
                category = EventCategories.DIAGNOSTICS,
                name = "compiler_diagnostic",
                value = errorCode,
                properties = properties
            )
        } catch (e: Exception) {
            logger.warn("Failed to send diagnostic telemetry", e)
            return false
        }
    }

    /**
     * 从LSP诊断信息中收集和发送遥测数据，并立即发送到服务器
     *
     * @param diagnostic LSP诊断信息
     * @param source 诊断信息来源
     * @param code 诊断代码或HTML代码片段
     * @return 如果成功发送，则返回true；否则返回false
     */
    fun sendDiagnosticImmediately(
        diagnostic: Diagnostic,
        source: String = "lsp",
        code: String = ""
    ): Boolean {
        try {
            val telemetryService = TelemetryService.getInstance()
            if (!telemetryService.isEnabled()) {
                return false
            }

            // 先收集诊断信息
            if (!sendDiagnostic(diagnostic, source, code)) {
                return false
            }

            // 立即发送到服务器
            return telemetryService.sendImmediately()
        } catch (e: Exception) {
            logger.warn("Failed to send diagnostic telemetry immediately", e)
            return false
        }
    }


    /**
     * 提取代码片段并使用HTML格式标记出错误位置
     */
    fun extractHtmlCodeSnippet(project: Project, file: VirtualFile, diagnostic: Diagnostic): String {
        try {
            val document = FileDocumentManager.getInstance().getDocument(file) ?: return ""
            val range = diagnostic.range ?: return ""

            val startLine = maxOf(0, range.start.line - CONTEXT_LINES)
            val endLine = minOf(document.lineCount - 1, range.end.line + CONTEXT_LINES)

            // 构建HTML代码片段
            val sb = StringBuilder()
            sb.append("<div style='font-family: monospace; white-space: pre; background-color: #f5f5f5; padding: 10px; border-radius: 5px;'>")
            
            // 添加文件信息
            sb.append("<div style='color: #888; margin-bottom: 5px;'>File: <strong>${file.name}</strong></div>")

            for (i in startLine..endLine) {
                val lineStartOffset = document.getLineStartOffset(i)
                val lineEndOffset = document.getLineEndOffset(i)
                val lineText = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, lineEndOffset))
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")

                // 行号样式
                sb.append("<div>")
                sb.append("<span style='color: #888; margin-right: 10px; user-select: none;'>${i + 1}</span>")

                // 如果是错误行，高亮显示错误部分
                if (i >= range.start.line && i <= range.end.line) {
                    if (i == range.start.line && i == range.end.line) {
                        // 错误在同一行内
                        if (range.start.character > 0) {
                            sb.append(lineText.substring(0, range.start.character))
                        }
                        
                        // 高亮错误部分
                        sb.append("<span style='background-color: #ffcccc; color: #cc0000; font-weight: bold;'>")
                        sb.append(lineText.substring(range.start.character, minOf(range.end.character, lineText.length)))
                        sb.append("</span>")
                        
                        if (range.end.character < lineText.length) {
                            sb.append(lineText.substring(range.end.character))
                        }
                    } else if (i == range.start.line) {
                        // 错误的起始行
                        if (range.start.character > 0) {
                            sb.append(lineText.substring(0, range.start.character))
                        }
                        
                        // 高亮错误部分
                        sb.append("<span style='background-color: #ffcccc; color: #cc0000; font-weight: bold;'>")
                        sb.append(lineText.substring(range.start.character))
                        sb.append("</span>")
                    } else if (i == range.end.line) {
                        // 错误的结束行
                        if (range.end.character > 0) {
                            sb.append("<span style='background-color: #ffcccc; color: #cc0000; font-weight: bold;'>")
                            sb.append(lineText.substring(0, minOf(range.end.character, lineText.length)))
                            sb.append("</span>")
                            
                            if (range.end.character < lineText.length) {
                                sb.append(lineText.substring(range.end.character))
                            }
                        } else {
                            sb.append(lineText)
                        }
                    } else {
                        // 错误的中间行，全部高亮
                        sb.append("<span style='background-color: #ffcccc; color: #cc0000; font-weight: bold;'>")
                        sb.append(lineText)
                        sb.append("</span>")
                    }
                } else {
                    // 非错误行，正常显示
                    sb.append(lineText)
                }
                
                sb.append("</div>")
            }
            
            // 添加错误信息
            sb.append("<div style='margin-top: 8px; color: #cc0000; font-weight: bold;'>")
            sb.append("Error: ${diagnostic.message.replace("<", "&lt;").replace(">", "&gt;")}")
            sb.append("</div>")
            
            sb.append("</div>")
            
            return sb.toString()
        } catch (e: Exception) {
            logger.warn("Failed to extract HTML code snippet", e)
            return ""
        }
    }

    /**
     * 从诊断信息中提取错误码
     */
    private fun extractErrorCode(diagnostic: Diagnostic): String {
        // 尝试从诊断信息的code字段中提取错误码
        diagnostic.code?.let { code ->
            return code.right?.toString() ?: code.toString()
        }

        // 尝试从消息中提取错误码
        // 例如 "UNRESOLVED_REFERENCE: Cannot resolveName symbol 'foo'"
        val message = diagnostic.message
        val colonIndex = message.indexOf(':')
        if (colonIndex > 0) {
            val potentialCode = message.substring(0, colonIndex).trim()
            // 如果潜在的错误代码全部是大写字母、数字和下划线，则认为它是错误代码
            if (potentialCode.matches(Regex("[A-Z0-9_]+"))) {
                return potentialCode
            }
        }

        // 如果无法提取错误码，则使用消息的哈希码
        return "LSP_ERROR_${Math.abs(message.hashCode())}"
    }

    /**
     * 获取诊断严重程度的字符串表示
     */
    private fun getSeverityString(severity: DiagnosticSeverity?): String {
        return when (severity) {
            DiagnosticSeverity.Error -> "ERROR"
            DiagnosticSeverity.Warning -> "WARNING"
            DiagnosticSeverity.Information -> "INFORMATION"
            DiagnosticSeverity.Hint -> "HINT"
            null -> "UNKNOWN"
        }
    }

    /**
     * 清除诊断缓存
     */
    fun clearCache() {
        reportedDiagnostics.clear()
    }
} 