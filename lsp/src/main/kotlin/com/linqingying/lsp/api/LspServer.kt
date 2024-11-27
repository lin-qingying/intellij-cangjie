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

import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.linqingying.lsp.api.requests.LspRequestExecutor
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.util.concurrent.CompletableFuture

/**
 * IntelliJ 中一个已启动的 LSP 服务器的模型。
 *
 * 要获取 [LspServer] 的实例，请使用 [LspServerManager.getServersForProvider]
 */
interface LspServer {
  val providerClass: Class<out LspServerSupportProvider>
  val project: Project

  /**
   * 一个 [LspServerDescriptor]，用于启动和控制此 [LspServer] 的行为。
   * 返回的对象与插件传递给 [LspServerSupportProvider.LspServerStarter.ensureServerStarted] 的对象完全相同。
   */
  val descriptor: LspServerDescriptor

  val state: LspServerState

  val initializeResult: InitializeResult?

  /**
   * 从 IDE 向 LSP 服务器发送一个 [notification](https://microsoft.github.io/language-server-protocol/specification/#notificationMessage)。
   *
   * 示例：
   *
   *    lspServer.sendNotification { it.workspaceService.didChangeConfiguration(DidChangeConfigurationParams(...)) }
   *
   * 自定义（未文档化）通知示例：
   *
   *    lspServer.sendNotification { (it as FooLsp4jServer).customNotification(...) }
   *
   * @see LspServerDescriptor.lsp4jServerClass
   */
  fun sendNotification(lsp4jSender: (Lsp4jServer) -> Unit)

  /**
   * 向 LSP 服务器发送请求。
   *
   * 如果 LSP 服务器满足以下条件，则此函数将返回 `null`：
   *  - 尚未初始化或已关闭
   *  - 在 10 秒内未发送任何响应
   *  - 返回 `null` 响应
   *  - 响应出现错误（错误会显示在 IDE 日志中）
   *
   * 示例：
   *
   *    val result = lspServer.sendRequest { it.workspaceService.executeCommand(ExecuteCommandParams(...)) }
   *
   * 自定义（未文档化）请求示例：
   *
   *    val result = lspServer.sendRequest { (it as FooLsp4jServer).customRequest(...) }
   *
   * @see LspServerDescriptor.lsp4jServerClass
   */
  suspend fun <Lsp4jResponse> sendRequest(lsp4jSender: (Lsp4jServer) -> CompletableFuture<Lsp4jResponse>): Lsp4jResponse?

  /**
   * 向 LSP 服务器发送请求并同步等待响应。
   *
   * 等待是可取消的（感谢常规的 [ProgressManager.checkCanceled] 调用）。
   * 但是，如果此函数从协程中调用，则可取消性可能不起作用。
   * 如果可能，请优先使用对协程友好的 [sendRequest] 函数。
   *
   * 如果 LSP 服务器满足以下条件，此函数将返回 `null`：
   *  - 尚未初始化或已关闭
   *  - 在 [timeoutMs] 毫秒（默认 10 秒）内未发送任何响应
   *  - 返回 `null` 响应
   *  - 响应出现错误（错误会显示在 IDE 日志中）
   *
   * 示例：
   *
   *    val result = lspServer.sendRequestSync { it.workspaceService.executeCommand(ExecuteCommandParams(...)) }
   *
   * 自定义（未文档化）请求示例：
   *
   *    val result = lspServer.sendRequestSync { (it as FooLsp4jServer).customRequest(...) }
   *
   * @see LspServerDescriptor.lsp4jServerClass
   */
  @RequiresBackgroundThread
  fun <Lsp4jResponse> sendRequestSync(
    timeoutMs: Int = DEFAULT_REQUEST_TIMEOUT_MS,
    lsp4jSender: (Lsp4jServer) -> CompletableFuture<Lsp4jResponse>
  ): Lsp4jResponse?

  /**
   * 为给定的 [file] 创建一个 [TextDocumentIdentifier](https://microsoft.github.io/language-server-protocol/specification/#textDocumentIdentifier)，
   * 以便在各种 LSP 服务器请求中使用。
   */
  fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier

  /**
   * 返回一个文本文档版本，例如：
   * [TextDocumentItem](https://microsoft.github.io/language-server-protocol/specification/#textDocumentItem)，
   * [VersionedTextDocumentIdentifier](https://microsoft.github.io/language-server-protocol/specification/#versionedTextDocumentIdentifier)，
   * [OptionalVersionedTextDocumentIdentifier](https://microsoft.github.io/language-server-protocol/specification/#optionalVersionedTextDocumentIdentifier)，
   * 或 [PublishDiagnosticsParams](https://microsoft.github.io/language-server-protocol/specification/#publishDiagnosticsParams)。
   */
  fun getDocumentVersion(document: Document): Int

  @Deprecated("使用 sendRequest, sendRequestSync, getDocumentIdentifier 和 getDocumentVersion")
  val lsp4jServer: Lsp4jServer

  @Deprecated("使用 sendRequest, sendRequestSync, getDocumentIdentifier 和 getDocumentVersion")
  val requestExecutor: LspRequestExecutor

  companion object {
    const val DEFAULT_REQUEST_TIMEOUT_MS = 10_000
  }
}
