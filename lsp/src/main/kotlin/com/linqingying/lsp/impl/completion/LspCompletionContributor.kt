package com.linqingying.lsp.impl.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.util.getLsp4jPosition
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class LspCompletionContributor : CompletionContributor(), DumbAware {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {


        val originalFile = parameters.originalFile
        val project = originalFile.project

        if (!project.isDefault) {
            val virtualFile = originalFile.virtualFile ?: return
            val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

            val hostOffset =
                InjectedLanguageManager.getInstance(project).injectedToHost(originalFile, parameters.offset)

            LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(virtualFile).forEach { lspServer ->
                ProgressManager.checkCanceled()
                val serverCapabilities = lspServer.serverCapabilities
                if (serverCapabilities?.completionProvider != null) {
                    val completionSupport = lspServer.descriptor.lspCompletionSupport
                    if (completionSupport != null && completionSupport.shouldRunCodeCompletion(parameters)) {
                        val completionItems = lspServer.requestExecutor.getCompletionList(
                            virtualFile,
                            hostOffset,
                            parameters.isAutoPopup
                        )
                        completionItems?.let {
                            processCompletionItemsImpl(
                                lspServer,
                                document,
                                hostOffset,
                                result,
                                it.items
                            ) { completionItem ->
                                completionSupport.createLookupElement(parameters, completionItem)
                            }
                        }
                    }
                }
            }
        }
    }


}


fun <T> processCompletionItemsImpl(
    lspServer: LspServerImpl,
    document: Document,
    offset: Int,
    originalResultSet: CompletionResultSet,
    items: List<T>,
    createLookupElement: (T) -> LookupElement?
) {
    val lspPosition = getLsp4jPosition(document, offset)
    var currentResultSet = originalResultSet
    var previousRange: Range? = null

    for (item in items) {
        val lookupElement = createLookupElement.invoke(item)
        if (lookupElement != null) {
            val completionItem = lookupElement.getObject() as? CompletionItem
            if (completionItem != null) {
                val itemRange = getRange(completionItem)

                if (itemRange != previousRange) {
                    previousRange = itemRange
                    val prefixMatcher = if (itemRange != null) {
                        val prefix = extractPrefix(document, lspPosition, itemRange)
                        if (prefix != null) originalResultSet.withPrefixMatcher(prefix) else null
                    } else {
                        originalResultSet
                    }

                    currentResultSet = prefixMatcher ?: originalResultSet
                }

                currentResultSet.addElement(LspLookupElementDecorator(lspServer, lookupElement, completionItem))
            }

        }
    }
}

private fun extractPrefix(document: Document, position: Position, range: Range): String? {
    if (range.start.line == range.end.line &&
        range.start.line == position.line &&
        range.start.character <= position.character &&
        range.end.character >= position.character
    ) {
        val lineStartOffset = document.getLineStartOffset(position.line)
        val startOffset = lineStartOffset + range.start.character
        val endOffset = lineStartOffset + position.character
        return document.charsSequence.subSequence(startOffset, endOffset).toString()
    } else {
        return null
    }
}


private fun getRange(completionItem: CompletionItem): Range? {
    val textEdit = completionItem.textEdit
    return if (textEdit != null) {
        if (textEdit.isLeft) {
            textEdit.left.range
        } else {
            textEdit.right.insert
        }
    } else {
        null
    }
}
