package com.linqingying.lsp.api






import com.linqingying.lsp.api.customization.requests.LspRequestExecutor
import com.intellij.openapi.project.Project

import org.eclipse.lsp4j.services.LanguageServer

/**
 * IntelliJ's model of the running LSP server.
 */
interface LspServer {

    val project: Project

    /**
     * An [LspServerDescriptor] that is used to start and control the behavior of this [LspServer].
     * The returned object is exactly the one that the plugin passed to [LspServerSupportProvider.LspServerStarter.ensureServerStarted].
     */
    val descriptor: com.linqingying.lsp.api.LspServerDescriptor

    /**
     * The instance of the [LspServerDescriptor.lsp4jServerClass], which is used for communication with the LSP server.
     * Plugins may provide a custom subclass of the [org.eclipse.lsp4j.services.LanguageServer] class if they need to send
     * some custom undocumented notifications or requests to the LSP server.
     */
    val lsp4jServer: LanguageServer

    /**
     * Plugins may need to use [LspRequestExecutor] only if they need to send
     * some custom undocumented notifications or requests to the LSP server.
     */
    val requestExecutor: LspRequestExecutor

    /**
     * Plugins don't need to use this property.
     *
     * The internal implementation of the [LspServerNotificationsHandler] interface handles all standard
     * (documented in the official LSP specification) requests and notifications that the LSP server sends to the IDE.
     */
    val serverNotificationsHandler: com.linqingying.lsp.api.LspServerNotificationsHandler
}
