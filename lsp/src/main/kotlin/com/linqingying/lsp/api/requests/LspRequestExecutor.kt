
package com.linqingying.lsp.api.requests

import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.lsp.api.LspServer
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.eclipse.lsp4j.TextDocumentIdentifier

/**
 * This class has been deprecated and scheduled for removal.
 * The replacements are:
 * - [LspServer.sendNotification]
 *  - [LspServer.sendRequest]
 *  - [LspServer.sendRequestSync]
 *  - [LspServer.getDocumentVersion]
 *  - [LspServer.getDocumentIdentifier]
 */
interface LspRequestExecutor {

  fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier


  fun sendNotification(notification: LspClientNotification)


  fun <Lsp4jResponse, Result> sendRequestAsync(lspRequest: LspRequest<Lsp4jResponse, Result>, resultConsumer: (Result?) -> Unit)

  @RequiresBackgroundThread

  fun <Lsp4jResponse, Result> sendRequestSync(lspRequest: LspRequest<Lsp4jResponse, Result>): Result?
}
