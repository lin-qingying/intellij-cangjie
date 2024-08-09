package com.linqingying.lsp.api.customization

import com.intellij.openapi.application.Application
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.Command
import org.jetbrains.annotations.ApiStatus

/**
 * Handles [Command](https://microsoft.github.io/language-server-protocol/specification#command) objects received from the LSP server.
 */

open class LspCommandsSupport {
    /**
     * Handles [Command](https://microsoft.github.io/language-server-protocol/specification#command) objects received from the LSP server.
     * In the spec v.3.17 there are 4 responses that may include [Command] object:
     * - [CodeAction](https://microsoft.github.io/language-server-protocol/specification#codeAction), for example a quick fix, an intention
     * action or a refactoring
     * - [CompletionItem](https://microsoft.github.io/language-server-protocol/specification#completionItem)
     * - [InlayHintLabelPart](https://microsoft.github.io/language-server-protocol/specification#inlayHintLabelPart)
     * - [CodeLens](https://microsoft.github.io/language-server-protocol/specification#codeLens)
     *
     * Note that this function is called in Event Dispatch Thread. Implementations that perform time-consuming tasks should switch to
     * a background thread, for example, using [Application.executeOnPooledThread]
     */
    @RequiresEdt
    open fun executeCommand(server: LspServer, contextFile: VirtualFile, command: Command) {
        // TODO send `workspace/executeCommand` request to the server
    }
}
