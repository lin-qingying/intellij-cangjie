package com.linqingying.lsp.impl

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.util.getLsp4jRange
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier


object LspDidChangeUtil {
    @RequiresReadLock
    internal fun createFullDidChangeParams(
        lspServer: LspServer,
        document: Document,
        virtualFile: VirtualFile
    ): DidChangeTextDocumentParams {
        return DidChangeTextDocumentParams(
            createVersionedTextDocumentIdentifier(lspServer, document, virtualFile),
            listOf(TextDocumentContentChangeEvent(document.text))
        )

    }

    @RequiresEdt
    fun createIncrementalDidChangeParamsBeforeDocumentChange(
        lspServer: LspServer,
        documentEvent: DocumentEvent,
        virtualFile: VirtualFile
    ): DidChangeTextDocumentParams {
        val document = documentEvent.document
        val versionedIdentifier = createVersionedTextDocumentIdentifier(lspServer, document, virtualFile).apply {
            version += 1
        }

        val range = getLsp4jRange(document, documentEvent.offset, documentEvent.oldLength)
        val newFragment = documentEvent.newFragment.toString()
        val contentChangeEvent = TextDocumentContentChangeEvent(range, newFragment)

        return DidChangeTextDocumentParams(versionedIdentifier, listOf(contentChangeEvent))

    }

    fun getFileToHandle(
        event:
        DocumentEvent
    ): VirtualFile? {
        val file = FileDocumentManager.getInstance().getFile(event.document)
        return if (file != null && file.isInLocalFileSystem && event.oldFragment != event.newFragment) {
            file
        } else {
            null
        }
    }

    private fun createVersionedTextDocumentIdentifier(
        lspServer: LspServer,
        document: Document,
        virtualFile: VirtualFile
    ): VersionedTextDocumentIdentifier {
        return VersionedTextDocumentIdentifier(
            lspServer.descriptor.getFileUri(virtualFile),
            lspServer.getDocumentVersion(document)
        )

    }
}

