package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.requests.LspClientNotification
import com.linqingying.lsp.impl.LspServerImpl
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.DidCloseTextDocumentParams

class DidCloseNotification(override val lspServer: LspServerImpl, file: VirtualFile): LspClientNotification(lspServer) {
    private val params: DidCloseTextDocumentParams =
        DidCloseTextDocumentParams(lspServer.requestExecutor.getDocumentIdentifier(file))

    override fun sendNotification() {
       lspServer.lsp4jServer.textDocumentService.didClose(this.params)
    }
}
