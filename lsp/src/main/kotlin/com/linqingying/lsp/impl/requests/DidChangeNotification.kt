package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.util.getLsp4jRange
import com.linqingying.lsp.api.requests.LspClientNotification
import com.linqingying.lsp.impl.LspServerImpl
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import kotlin.jvm.internal.DefaultConstructorMarker

class DidChangeNotification(
    override val lspServer: LspServer,
    documentIdentifier: VersionedTextDocumentIdentifier,
    changeEvent: TextDocumentContentChangeEvent
) :
    LspClientNotification(lspServer) {


//    constructor(
//        lspServer: LspServer,
//        documentIdentifier: VersionedTextDocumentIdentifier?,
//        changeEvent: TextDocumentContentChangeEvent?,
//        `$constructor_marker`: DefaultConstructorMarker?
//    ) : this(lspServer, documentIdentifier!!, changeEvent!!)
//

    companion object {

        @JvmStatic
        @RequiresReadLock
        fun createFull(lspServer: LspServer, document: Document, virtualFile: VirtualFile): DidChangeNotification {

            return DidChangeNotification(
                lspServer,
                getVersionedTextDocumentIdentifier(lspServer, document, virtualFile),
                TextDocumentContentChangeEvent(document.text),

            )

        }

        @JvmStatic
        @RequiresEdt
        fun createIncrementalNotificationBeforeRealDocumentChange(
            lspServer: LspServer,
            documentEvent: DocumentEvent,
            virtualFile: VirtualFile
        ): DidChangeNotification {

            val documentIdentifier = getVersionedTextDocumentIdentifier(lspServer, documentEvent.document, virtualFile)
            documentIdentifier.version += 1
            val range = getLsp4jRange(documentEvent.document, documentEvent.offset, documentEvent.oldLength)
            val newFragment = documentEvent.newFragment.toString()

            return DidChangeNotification(
                lspServer,
                documentIdentifier,
                TextDocumentContentChangeEvent(range, newFragment)
            )

        }

        private fun getVersionedTextDocumentIdentifier(
            lspServer: LspServer,
            document: Document,
            virtualFile: VirtualFile
        ): VersionedTextDocumentIdentifier {
            return VersionedTextDocumentIdentifier(
                lspServer.descriptor.getFileUri(virtualFile),
                lspServer.requestExecutor.getDocumentVersion(document)
            )
        }

    }

    private val params: DidChangeTextDocumentParams =
        DidChangeTextDocumentParams(documentIdentifier, listOf(changeEvent))


    override fun sendNotification() {
        lspServer.lsp4jServer.textDocumentService.didChange(params)
    }
}
