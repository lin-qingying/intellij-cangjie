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

package org.cangnova.cangjie.lsp4ij

import org.cangnova.cangjie.lsp4ij.diagnostics.DiagnosticTelemetry
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.LSPDiagnosticFeature
import org.eclipse.lsp4j.Diagnostic

class CangJieLSPDiagnosticFeature : LSPDiagnosticFeature() {
    private val logger = Logger.getInstance(CangJieLSPDiagnosticFeature::class.java)

    override fun isEnabled(file: PsiFile): Boolean {
        val enabled = super.isEnabled(file)
        logger.info("Diagnostic feature isEnabled for file ${file.name}: $enabled")
        return enabled
    }

    override fun getHighlightSeverity(diagnostic: Diagnostic): HighlightSeverity? {
        val severity = super.getHighlightSeverity(diagnostic)
        logger.info("Diagnostic severity for message '${diagnostic.message}': $severity (LSP severity: ${diagnostic.severity})")
        return severity
    }

    override fun createAnnotation(
        diagnostic: Diagnostic,
        document: Document,
        fixes: List<IntentionAction?>,
        holder: AnnotationHolder
    ) {
        logger.info("Creating annotation for diagnostic: ${diagnostic.message} at ${diagnostic.range}")
        logger.info("  Severity: ${diagnostic.severity}, Code: ${diagnostic.code}, Source: ${diagnostic.source}")
        logger.info("  File: ${holder.currentAnnotationSession.file.virtualFile.url}")

        // 收集错误码和错误信息用于遥测
        try {
            // 获取文件信息
            val virtualFile = holder.currentAnnotationSession.file.virtualFile
            // 使用文档内容生成HTML格式的代码片段
            if (diagnostic.range != null) {
                // 直接使用传入的document参数生成代码片段
                val htmlCodeSnippet = generateHtmlSnippet(document, diagnostic, virtualFile.name)
                // 发送带有HTML代码片段的诊断信息
                DiagnosticTelemetry.sendDiagnostic(diagnostic, "cangjie_lsp", code = htmlCodeSnippet)
            } else {
                // 如果没有范围信息，只发送基本诊断信息
                DiagnosticTelemetry.sendDiagnostic(diagnostic, "cangjie_lsp")
            }
        } catch (e: Exception) {
            // 确保遥测不会影响正常功能
            logger.warn("Failed to send diagnostic telemetry", e)
        }

        // 调用父类方法创建注解
        try {
            super.createAnnotation(diagnostic, document, fixes, holder)
            logger.info("Successfully created annotation for diagnostic")
        } catch (e: Exception) {
            logger.error("Failed to create annotation for diagnostic", e)
            throw e
        }
    }

    /**
     * 生成带有HTML格式高亮的代码片段
     */
    private fun generateHtmlSnippet(document: Document, diagnostic: Diagnostic, fileName: String): String {
        val range = diagnostic.range ?: return ""
        val contextLines = 2  // 错误上下文行数

        val startLine = maxOf(0, range.start.line - contextLines)
        val endLine = minOf(document.lineCount - 1, range.end.line + contextLines)

        // 构建HTML代码片段
        val sb = StringBuilder()
        sb.append("<div style='font-family: monospace; white-space: pre; background-color: #f5f5f5; padding: 10px; border-radius: 5px;'>")

        // 添加文件信息
        sb.append("<div style='color: #888; margin-bottom: 5px;'>File: <strong>${fileName}</strong></div>")

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
    }
}