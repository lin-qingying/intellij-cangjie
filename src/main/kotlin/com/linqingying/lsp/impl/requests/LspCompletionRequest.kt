package com.linqingying.lsp.impl.requests

import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.LspRequest
import com.linqingying.lsp.api.customization.requests.util.getLsp4jPosition
import com.linqingying.lsp.impl.LspServerImpl
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture

class LspCompletionRequest(
    override val lspServer: com.linqingying.lsp.api.LspServer,
    file: VirtualFile,
    document: Document,
    offset: Int,
    isAutoPopup: Boolean
) :
    LspRequest<Either<List<CompletionItem>, CompletionList>, List<CompletionItem>>(lspServer) {


    private val caretPosition: Position = getLsp4jPosition(document, offset)

    private val completionContext: CompletionContext

    private val documentIdentifier: TextDocumentIdentifier = lspServer.requestExecutor.getDocumentIdentifier(file)

    init {
        completionContext = if (isAutoPopup) {
            val triggerCharacter = getCharacterAtPosition(document, offset)?.takeIf { isTriggerCharacter(it) }
            if (triggerCharacter != null) {
                CompletionContext(CompletionTriggerKind.TriggerCharacter, triggerCharacter)
            } else {
                CompletionContext(CompletionTriggerKind.Invoked)
            }
        } else {
            CompletionContext(CompletionTriggerKind.Invoked)
        }
    }

    private fun getCharacterAtPosition(document: Document, position: Int): String? {
        return if (position > 0 && position <= document.textLength) {
            document.charsSequence[position - 1].toString()
        } else {
            null
        }
    }

    private fun isTriggerCharacter(character: String): Boolean {
        val serverCapabilities = (lspServer as? LspServerImpl)?.getServerCapabilities()
        val triggerCharacters = serverCapabilities?.completionProvider?.triggerCharacters
        return triggerCharacters?.contains(character) ?: false
    }

    override fun sendRequest(): CompletableFuture<Either<List<CompletionItem>, CompletionList>> =
        lspServer.lsp4jServer.textDocumentService.completion(
            CompletionParams(
                this.documentIdentifier,
                this.caretPosition,
                this.completionContext
            )
        )

    private fun updateCompletionItem(item: CompletionItem, defaults: CompletionItemDefaults) {
        if (item.commitCharacters == null) {
            item.commitCharacters = defaults.commitCharacters
        }

        if (item.textEdit == null && defaults.editRange != null) {
            val text = item.textEditText ?: item.label
            item.textEdit = if (defaults.editRange.isLeft) {
                Either.forLeft(TextEdit(defaults.editRange.left, text))
            } else {
                val insertReplaceRange = defaults.editRange.right
                Either.forRight(InsertReplaceEdit(text, insertReplaceRange.insert, insertReplaceRange.replace))
            }
        }

        if (item.insertTextFormat == null) {
            item.insertTextFormat = defaults.insertTextFormat
        }

        if (item.insertTextMode == null) {
            item.insertTextMode = defaults.insertTextMode
        }

        if (item.data == null) {
            item.data = defaults.data
        }
    }

    override fun preprocessResponse(serverResponse: Either<List<CompletionItem>, CompletionList>): List<CompletionItem> {
        return when {
            serverResponse.isLeft -> serverResponse.left
            else -> {
                val items = (serverResponse.right as CompletionList).items
                val itemDefaults = (serverResponse.right as CompletionList).itemDefaults
                itemDefaults?.let {
                    items.forEach { item ->
                        updateCompletionItem(item, it)
                    }
                }
                items
            }
        }
    }

}
