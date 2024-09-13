package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.requests.LspClientNotification
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.FileEvent


class DidChangeWatchedFilesNotification(override val lspServer: LspServer, val lsp4jFileEvents: List<FileEvent?>) :
    LspClientNotification(lspServer) {


    override fun sendNotification() {
        lspServer.lsp4jServer.workspaceService.didChangeWatchedFiles(DidChangeWatchedFilesParams(lsp4jFileEvents))
    }
}

