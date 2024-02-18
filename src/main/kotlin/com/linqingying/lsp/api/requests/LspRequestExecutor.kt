package com.linqingying.lsp.api.customization.requests

import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.linqingying.lsp.api.requests.LspClientNotification
import org.eclipse.lsp4j.TextDocumentIdentifier


/**
 * Sends [notifications](https://microsoft.github.io/language-server-protocol/specification/#notificationMessage)
 * and [requests](https://microsoft.github.io/language-server-protocol/specification/#requestMessage)
 * from the IDE to the LSP server.
 *
 * @see LspServer.requestExecutor
 */
interface LspRequestExecutor {
    /**
     * Creates [TextDocumentIdentifier](https://microsoft.github.io/language-server-protocol/specification/#textDocumentIdentifier)
     * for the given [file] to be used in various LSP server requests.
     */
    fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier

    /**
     * Returns text document version as specified by, for example,
     * [TextDocumentItem](https://microsoft.github.io/language-server-protocol/specification/#textDocumentItem),
     * [VersionedTextDocumentIdentifier](https://microsoft.github.io/language-server-protocol/specification/#versionedTextDocumentIdentifier),
     * [OptionalVersionedTextDocumentIdentifier](https://microsoft.github.io/language-server-protocol/specification/#optionalVersionedTextDocumentIdentifier),
     * or [PublishDiagnosticsParams](https://microsoft.github.io/language-server-protocol/specification/#publishDiagnosticsParams)
     */
    fun getDocumentVersion(document: Document): Int

    /**
     * 像lspServer发送通知
     * [text document synchronization](https://microsoft.github.io/language-server-protocol/specification/#textDocument_synchronization)
     * notifications.
     */
    fun sendNotification(notification: LspClientNotification)

    /**
     * Sends a request to the LSP server asynchronously. If a response is received, it's handled by the
     * [LspRequest.preprocessResponse] and passed to the `resultConsumer`.
     * In several cases, `resultConsumer` may receive `null`:
     *  - this `LspServer` is not running or not initialized yet
     *  - server returns `null` response
     *  - server responds with an error (the error will appear in the IDE logs)
     */
    fun <Result> sendRequestAsync(lspRequest: LspRequest<*, Result>, resultConsumer: (Result?) -> Unit)




    /**
     * Sends a request to the LSP server and waits for the response for no more than 10 seconds.
     * Waiting is cancelable (thanks to regular [ProgressManager.checkCanceled] calls).
     * If the response arrives in time, it's handled by the [LspRequest.preprocessResponse] and returned.
     *
     * This function may return `null` in several cases:
     *  - the server is not running or not initialized yet
     *  - no response received from the server in 10 seconds
     *  - server returns `null` response
     *  - server responds with an error (the error will appear in the IDE logs)
     */
    @RequiresBackgroundThread
    fun <ServerResponse, Result> sendRequestSync(lspRequest: LspRequest<ServerResponse, Result>): Result?
}
