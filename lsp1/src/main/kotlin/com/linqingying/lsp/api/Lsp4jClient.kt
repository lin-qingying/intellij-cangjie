package com.linqingying.lsp.api


import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.CompletableFuture

/**
 * Implementation of the [org.eclipse.lsp4j.services.LanguageClient] interface. It handles all standard requests and notifications that
 * the LSP server sends to the IDE. 'Standard' requests and notifications are the ones, which are documented
 * in the official [LSP specification](https://microsoft.github.io/language-server-protocol/specification).
 *
 * To handle custom undocumented requests/notifications from the server, plugins may override [LspServerDescriptor.createLsp4jClient]
 * and return their subclass of this [Lsp4jClient]. This subclass should contain specially annotated functions, which will be called
 * via reflection by the `lsp4j` library once the corresponding request/notification arrives from the LSP server. Example:
 *
 *    @JsonNotification("@/foo/bar")
 *    fun fooBar(fooBar: FooBarNotification) { ... }
 */
@ApiStatus.OverrideOnly
open class Lsp4jClient(private val serverNotificationsHandler: LspServerNotificationsHandler) : LanguageClient {
    final override fun applyEdit(params: ApplyWorkspaceEditParams) = serverNotificationsHandler.applyEdit(params)
    final override fun registerCapability(params: RegistrationParams) =
        serverNotificationsHandler.registerCapability(params)

    final override fun unregisterCapability(params: UnregistrationParams) =
        serverNotificationsHandler.unregisterCapability(params)

    final override fun telemetryEvent(`object`: Any) = serverNotificationsHandler.telemetryEvent(`object`)
    final override fun publishDiagnostics(params: PublishDiagnosticsParams) =
        serverNotificationsHandler.publishDiagnostics(params)

    final override fun showMessage(params: MessageParams) = serverNotificationsHandler.showMessage(params)
    final override fun showMessageRequest(params: ShowMessageRequestParams) =
        serverNotificationsHandler.showMessageRequest(params)

    final override fun showDocument(params: ShowDocumentParams) = serverNotificationsHandler.showDocument(params)
    final override fun logMessage(params: MessageParams) = serverNotificationsHandler.logMessage(params)
    final override fun workspaceFolders() = serverNotificationsHandler.workspaceFolders()
    final override fun configuration(params: ConfigurationParams) = serverNotificationsHandler.configuration(params)
    final override fun createProgress(params: WorkDoneProgressCreateParams) =
        serverNotificationsHandler.createProgress(params)

    final override fun notifyProgress(params: ProgressParams) = serverNotificationsHandler.notifyProgress(params)
    final override fun logTrace(params: LogTraceParams) = serverNotificationsHandler.logTrace(params)
    final override fun refreshSemanticTokens() = serverNotificationsHandler.refreshSemanticTokens()
    final override fun refreshCodeLenses() = serverNotificationsHandler.refreshCodeLenses()
    final override fun refreshInlayHints() = serverNotificationsHandler.refreshInlayHints()
    final override fun refreshInlineValues() = serverNotificationsHandler.refreshInlineValues()
    final override fun refreshDiagnostics() = serverNotificationsHandler.refreshDiagnostics()
}

