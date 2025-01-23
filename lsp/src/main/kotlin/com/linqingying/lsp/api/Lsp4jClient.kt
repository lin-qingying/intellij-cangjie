/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.lsp.api

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient

import java.util.concurrent.CompletableFuture

/**
 * [org.eclipse.lsp4j.services.LanguageClient] 接口的实现。
 * 它处理 LSP 服务器发送给 IDE 的所有标准请求和通知。
 * "标准" 请求和通知是指在官方 [LSP 规范](https://microsoft.github.io/language-server-protocol/specification) 中记录的那些。
 *
 * 如果需要处理服务器的自定义未文档化请求/通知，插件需要重写 [LspServerDescriptor.createLsp4jClient]，
 * 并返回此 [Lsp4jClient] 的子类。这个子类应该包含特别注解的方法，
 * 当 LSP 服务器发送相应的请求/通知时，`lsp4j` 库将通过反射调用这些方法。
 *
 * 示例：
 *
 *    @JsonNotification("@/foo/bar")
 *    fun fooBar(fooBar: FooBarNotification) { ... }
 *
 * @see LspServerDescriptor.createLsp4jClient
 */

open class Lsp4jClient(private val serverNotificationsHandler: LspServerNotificationsHandler) : LanguageClient {
  final override fun applyEdit(params: ApplyWorkspaceEditParams) = serverNotificationsHandler.applyEdit(params)
  final override fun registerCapability(params: RegistrationParams) = serverNotificationsHandler.registerCapability(params)
  final override fun unregisterCapability(params: UnregistrationParams) = serverNotificationsHandler.unregisterCapability(params)
  override fun telemetryEvent(`object`: Any) = serverNotificationsHandler.telemetryEvent(`object`)
  final override fun publishDiagnostics(params: PublishDiagnosticsParams) = serverNotificationsHandler.publishDiagnostics(params)
  final override fun showMessage(params: MessageParams) = serverNotificationsHandler.showMessage(params)
  final override fun showMessageRequest(params: ShowMessageRequestParams) = serverNotificationsHandler.showMessageRequest(params)
  final override fun showDocument(params: ShowDocumentParams) = serverNotificationsHandler.showDocument(params)
  final override fun logMessage(params: MessageParams) = serverNotificationsHandler.logMessage(params)
  final override fun workspaceFolders() = serverNotificationsHandler.workspaceFolders()
  final override fun configuration(params: ConfigurationParams) = serverNotificationsHandler.configuration(params)
  final override fun createProgress(params: WorkDoneProgressCreateParams) = serverNotificationsHandler.createProgress(params)
  final override fun notifyProgress(params: ProgressParams) = serverNotificationsHandler.notifyProgress(params)
  final override fun logTrace(params: LogTraceParams) = serverNotificationsHandler.logTrace(params)
  final override fun refreshSemanticTokens() = serverNotificationsHandler.refreshSemanticTokens()
  final override fun refreshCodeLenses() = serverNotificationsHandler.refreshCodeLenses()
  final override fun refreshInlayHints() = serverNotificationsHandler.refreshInlayHints()
  final override fun refreshInlineValues() = serverNotificationsHandler.refreshInlineValues()
  final override fun refreshDiagnostics() = serverNotificationsHandler.refreshDiagnostics()
}

/**
 * 插件不需要使用此接口。
 *
 * 它的内部实现处理 LSP 服务器发送给 IDE 的所有标准请求和通知
 * （记录在官方 LSP 规范中）。
 */
interface LspServerNotificationsHandler {
  fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse>
  fun registerCapability(params: RegistrationParams): CompletableFuture<Void>
  fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void>
  fun telemetryEvent(`object`: Any)
  fun publishDiagnostics(params: PublishDiagnosticsParams)
  fun showMessage(params: MessageParams)
  fun showMessageRequest(params: ShowMessageRequestParams): CompletableFuture<MessageActionItem>
  fun showDocument(params: ShowDocumentParams): CompletableFuture<ShowDocumentResult>
  fun logMessage(params: MessageParams)
  fun workspaceFolders(): CompletableFuture<List<WorkspaceFolder>>
  fun configuration(params: ConfigurationParams): CompletableFuture<List<Any?>>
  fun createProgress(params: WorkDoneProgressCreateParams): CompletableFuture<Void>
  fun notifyProgress(params: ProgressParams)
  fun logTrace(params: LogTraceParams)
  fun refreshSemanticTokens(): CompletableFuture<Void>
  fun refreshCodeLenses(): CompletableFuture<Void>
  fun refreshInlayHints(): CompletableFuture<Void>
  fun refreshInlineValues(): CompletableFuture<Void>
  fun refreshDiagnostics(): CompletableFuture<Void>
}
