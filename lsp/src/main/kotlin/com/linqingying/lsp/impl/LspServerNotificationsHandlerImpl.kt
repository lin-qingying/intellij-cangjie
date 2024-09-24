package com.linqingying.lsp.impl

import com.linqingying.lsp.api.LspServerNotificationsHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import org.eclipse.lsp4j.*
import java.util.concurrent.CompletableFuture

class LspServerNotificationsHandlerImpl(private val lspServer: LspServerImpl) :
    LspServerNotificationsHandler {


    companion object {

        val LOG = Logger.getInstance(LspServerNotificationsHandlerImpl::class.java)

        private const val LOG_ERRORS_WARNINGS_NOTIFICATION_GROUP: String = "LSP window/logMessage: errors, warnings"

        private const val LOG_INFO_TRACE_NOTIFICATION_GROUP: String = "LSP window/logMessage: info, log; $/logTrace"

        private const val SHOW_MESSAGE_NOTIFICATION_GROUP: String = "LSP window/showMessage"
    }

    override fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse> {
        return CompletableFuture.completedFuture(null)
    }

    override fun registerCapability(params: RegistrationParams): CompletableFuture<Void> {
        val registrations = params.registrations
        requireNotNull(registrations) { "Registrations must not be null" }
        for (registration in registrations) {
            val dynamicCapabilities = lspServer.dynamicCapabilities
            dynamicCapabilities.registerCapability(registration)
        }
        return CompletableFuture.completedFuture(null)
    }

    override fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void> {
        val unregistrations = params.unregisterations
        for (unregistration in unregistrations) {
            this.lspServer.dynamicCapabilities.unregisterCapability(unregistration)
        }

        return CompletableFuture.completedFuture(null)
    }

    override fun telemetryEvent(`object`: Any) {
        TODO("Not yet implemented")
    }

    override fun publishDiagnostics(params: PublishDiagnosticsParams) {
        if (!lspServer.project.isDisposed) {
            lspServer.diagnosticsReceived(params)
        }
    }

    private fun getNotificationType(params: MessageParams): NotificationType {
        val messageType = params.type ?: throw IllegalArgumentException("Message type cannot be null")
        return when (messageType) {
            MessageType.Error -> NotificationType.ERROR
            MessageType.Warning -> NotificationType.WARNING
            MessageType.Info, MessageType.Log -> NotificationType.INFORMATION
            else -> throw IllegalArgumentException("Unknown message type: $messageType")
        }
    }

    override fun showMessage(params: MessageParams) {
        if (!lspServer.project.isDisposed) {
            LOG.info(params.message)
            val message = params.message
            requireNotNull(message) { "params.message cannot be null" }

            notifyWithActions(message, getNotificationType(params), SHOW_MESSAGE_NOTIFICATION_GROUP, null)
        }
    }

    private fun notifyWithActions(
        @NlsSafe message: String,
        notificationType: NotificationType,
        groupId: String,
        actionItems: List<MessageActionItem>?
    ): CompletableFuture<MessageActionItem> {
        val future = CompletableFuture<MessageActionItem>()
        val notificationTitle = "${lspServer.descriptor.presentableName}: $message"
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(groupId)
            .createNotification(notificationTitle, notificationType)

        actionItems?.forEach { actionItem ->
            val actionTitle = actionItem.title
            requireNotNull(actionTitle) { "actionItem.title cannot be null" }
            notification.addAction(object : AnAction(actionTitle) {
                override fun actionPerformed(e: AnActionEvent) {
                    notification.expire()
                    future.complete(actionItem)
                }
            })
        }

        notification.notify(lspServer.project)
        return future
    }

    override fun showMessageRequest(params: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
        return if (lspServer.project.isDisposed) {
            CompletableFuture.completedFuture(null)
        } else {
            val message = params.message
            requireNotNull(message) { "params.message cannot be null" }
            val actions = params.actions?.joinToString() ?: "null"
            LOG.info("$message: $actions")
            notifyWithActions(message, getNotificationType(params), "LSP window/showMessage", params.actions)
        }
    }

    override fun showDocument(params: ShowDocumentParams): CompletableFuture<ShowDocumentResult> =
        CompletableFuture.completedFuture(null)

    override fun logMessage(params: MessageParams) {
        if (!lspServer.project.isDisposed) {
            LOG.info(params.message)
            if (params.type != MessageType.Error && params.type != MessageType.Warning) {
                notifyWithActions(

                    params.message,
                    NotificationType.INFORMATION,
                    LOG_INFO_TRACE_NOTIFICATION_GROUP,
                    null
                )
            } else {
                notifyWithActions(
                    params.message,
                    getNotificationType(params),
                    LOG_ERRORS_WARNINGS_NOTIFICATION_GROUP,
                    null
                )
            }
        }
    }

    override fun workspaceFolders(): CompletableFuture<List<WorkspaceFolder>> {
        return if (lspServer.project.isDisposed) {
            CompletableFuture.completedFuture(emptyList())
        } else {
            val roots = lspServer.descriptor.roots
            val folders = roots.map { root ->
                WorkspaceFolder(lspServer.descriptor.getFileUri(root), root.name)
            }
            CompletableFuture.completedFuture(folders)
        }
    }

    override fun configuration(params: ConfigurationParams): CompletableFuture<List<Any?>> {
        return if (lspServer.project.isDisposed) {
            CompletableFuture.completedFuture(emptyList())
        } else {
            val configurations = params.items.map { item ->
                lspServer.descriptor.getWorkspaceConfiguration(item)
            }
            CompletableFuture.completedFuture(configurations)
        }
    }

    override fun createProgress(params: WorkDoneProgressCreateParams): CompletableFuture<Void> =
        CompletableFuture.completedFuture(null)

    override fun notifyProgress(params: ProgressParams) {

    }

    override fun logTrace(params: LogTraceParams) {
        if (!lspServer.project.isDisposed) {
            val message = params.verbose?.let { "${params.message}\n$it" } ?: params.message
            notifyWithActions(

                message,
                NotificationType.INFORMATION,
                LOG_INFO_TRACE_NOTIFICATION_GROUP,
                null
            )
        }
    }

    override fun refreshSemanticTokens(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun refreshCodeLenses(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun refreshInlayHints(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun refreshInlineValues(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

    override fun refreshDiagnostics(): CompletableFuture<Void> = CompletableFuture.completedFuture(null)

}
