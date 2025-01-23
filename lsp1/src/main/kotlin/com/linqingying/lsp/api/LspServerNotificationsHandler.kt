package com.linqingying.lsp.api

import org.eclipse.lsp4j.*

import java.util.concurrent.CompletableFuture


/**
 *插件不需要使用该接口。
 *
 *其内部实现处理所有标准(记录在官方LSP规范中)请求和通知。
 *LSP服务器发送到IDE。
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
