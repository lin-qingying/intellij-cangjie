package com.linqingying.lsp.impl.completion

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.util.getLsp4jPosition
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either


internal fun Either<List<CompletionItem>, CompletionList>.asCompletionList(): CompletionList {
    return if (isLeft) {
        CompletionList(false, left)
    } else {
        val completionList = right
        val itemDefaults = completionList.itemDefaults

        itemDefaults?.let {
            val items = completionList.items

            items.forEach { completionItem ->

                applyDefaults(completionItem, itemDefaults)
            }
        }


        completionList
    }
}

private fun applyDefaults(completionItem: CompletionItem, completionItemDefaults: CompletionItemDefaults) {
    if (completionItem.commitCharacters == null) {
        completionItem.commitCharacters = completionItemDefaults.commitCharacters
    }

    if (completionItem.textEdit == null && completionItemDefaults.editRange != null) {
        val text = completionItem.textEditText ?: completionItem.label
        when {
            completionItemDefaults.editRange.isLeft -> {
                completionItem.textEdit = Either.forLeft(TextEdit(completionItemDefaults.editRange.left as Range, text))
            }

            else -> {
                val insertReplaceRange = completionItemDefaults.editRange.right as InsertReplaceRange
                completionItem.textEdit =
                    Either.forRight(InsertReplaceEdit(text, insertReplaceRange.insert, insertReplaceRange.replace))
            }
        }
    }

    if (completionItem.insertTextFormat == null) {
        completionItem.insertTextFormat = completionItemDefaults.insertTextFormat
    }

    if (completionItem.insertTextMode == null) {
        completionItem.insertTextMode = completionItemDefaults.insertTextMode
    }

    if (completionItem.data == null) {
        completionItem.data = completionItemDefaults.data
    }
}

internal fun createCompletionParams(
    lspServer: LspServerImpl,
    file: VirtualFile,
    document: Document,
    offset: Int,
    isAutoPopup: Boolean
): CompletionParams {
    val documentIdentifier = lspServer.getDocumentIdentifier(file)
    val position =  getLsp4jPosition(document, offset)

    val completionContext = if (isAutoPopup) {
        val prefix = getCharacterAtOffset(document, offset)
        if (prefix != null && isTriggerCharacterSupported(lspServer, prefix)) {
            CompletionContext(CompletionTriggerKind.TriggerCharacter, prefix)
        } else {
            CompletionContext(CompletionTriggerKind.Invoked)
        }
    } else {
        CompletionContext(CompletionTriggerKind.Invoked)
    }

    return CompletionParams(documentIdentifier, position, completionContext)
}
private fun getCharacterAtOffset(document: Document, offset: Int): String? {
    return if (offset > 0 && offset <= document.textLength) {
        document.charsSequence[offset - 1].toString()
    } else {
        null
    }
}
private fun isTriggerCharacterSupported(lspServer: LspServerImpl, character: String): Boolean {
    val capabilities = lspServer.serverCapabilities
    return capabilities?.completionProvider?.triggerCharacters?.contains(character) == true
}
