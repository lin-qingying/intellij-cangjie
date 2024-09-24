package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.requests.LspClientNotification
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.TextDocumentItem


internal class DidOpenNotification :
    LspClientNotification {


    private val documentText: String

    private val documentVersion: Int

    private val file: VirtualFile

    @RequiresReadLock
    constructor(lspServer: LspServer, file: VirtualFile, document: Document) : super(lspServer) {
        this.file = file
        this.documentText = document.text
        this.documentVersion = lspServer.requestExecutor.getDocumentVersion(document)
    }


    override fun sendNotification() {
        val fileUri = lspServer.descriptor.getFileUri(file)
        val languageId = lspServer.descriptor.getLanguageId(file)
        val textDocumentItem = TextDocumentItem(fileUri, languageId, documentVersion, documentText)
        lspServer.lsp4jServer.textDocumentService.didOpen(DidOpenTextDocumentParams(textDocumentItem))
    }

}

