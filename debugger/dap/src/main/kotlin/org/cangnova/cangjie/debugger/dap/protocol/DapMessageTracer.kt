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

package org.cangnova.cangjie.debugger.dap.protocol

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.debugger.util.writeStringToFileDap
import org.eclipse.lsp4j.jsonrpc.MessageIssueHandler
import org.eclipse.lsp4j.jsonrpc.messages.Message
import org.eclipse.lsp4j.jsonrpc.messages.MessageIssue
import org.eclipse.lsp4j.jsonrpc.messages.RequestMessage
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage
import org.eclipse.lsp4j.jsonrpc.validation.ReflectiveMessageValidator
import java.io.PrintWriter
import java.io.StringWriter

/**
 * DAP消息跟踪器
 *
 * 用于记录DAP协议的请求和响应消息到文件，便于调试
 */
class DapMessageTracer : MessageIssueHandler {

    companion object {
        private val LOG = Logger.getInstance(DapMessageTracer::class.java)
    }

    /**
     * 跟踪消息
     */
    fun traceMessage(message: Message, direction: String) {
        try {
            // 记录消息到文件
            val messageType = when (message) {
                is RequestMessage -> "请求: ${message.method}"
                is ResponseMessage -> "响应: id=${message.id}"
                is NotificationMessage -> "通知: ${message.method}"
                else -> "消息"
            }

            // 格式化消息内容
            val formattedMessage = formatMessage(message)
            writeStringToFileDap(formattedMessage, "$direction $messageType")

            LOG.debug("DAP $direction $messageType")
        } catch (e: Exception) {
            LOG.warn("Failed to trace DAP message", e)
        }
    }

    /**
     * 格式化消息内容
     */
    private fun formatMessage(message: Message): String {
        return when (message) {
            is RequestMessage -> {
                buildString {
                    appendLine("Request:")
                    appendLine("  ID: ${message.id}")
                    appendLine("  Method: ${message.method}")
                    appendLine("  Params: ${message.params}")
                }
            }
            is ResponseMessage -> {
                buildString {
                    appendLine("Response:")
                    appendLine("  ID: ${message.id}")
                    if (message.error != null) {
                        appendLine("  Error: ${message.error}")
                    } else {
                        appendLine("  Result: ${message.result}")
                    }
                }
            }
            is NotificationMessage -> {
                buildString {
                    appendLine("Notification:")
                    appendLine("  Method: ${message.method}")
                    appendLine("  Params: ${message.params}")
                }
            }
            else -> {
                message.toString()
            }
        }
    }



    override fun handle(message: Message, issues: MutableList<org.eclipse.lsp4j.jsonrpc.messages.MessageIssue>) {
        if (issues.isNotEmpty()) {
            LOG.warn("Message has ${issues.size} validation issues: ${issues.joinToString { it.text }}")
        }
    }
}