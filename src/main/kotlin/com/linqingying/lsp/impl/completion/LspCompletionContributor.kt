package com.linqingying.lsp.impl.completion

import com.linqingying.lsp.api.customization.LspCompletionSupport
import com.linqingying.lsp.api.customization.requests.util.getLsp4jPosition
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.impl.requests.LspRequestExecutorImpl
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class LspCompletionContributor : CompletionContributor(), DumbAware {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val psiFile = parameters.originalFile
        val project = psiFile.project
        if (!project.isDefault) {
            val virtualFile = psiFile.originalFile.virtualFile
            val actualFile = if (virtualFile is VirtualFileWindow) virtualFile.delegate else virtualFile
            actualFile?.let { file ->
                val document = FileDocumentManager.getInstance().getDocument(file) ?: return
                val offset = InjectedLanguageManager.getInstance(project).injectedToHost(psiFile, parameters.offset)
                val servers = LspServerManagerImpl.getInstanceImpl(project).getServersWithThisFileOpen(file)
                for (server in servers) {
                    ProgressManager.checkCanceled()
                    val lspCompletionSupport = server.descriptor.lspCompletionSupport
                    if (lspCompletionSupport?.shouldRunCodeCompletion(parameters) == true) {
                        val completionItems = (server.requestExecutor as LspRequestExecutorImpl).getCompletionItems(
                            file,
                            offset,
                            parameters.isAutoPopup
                        )
                        processCompletionItems(
                            lspCompletionSupport,
                            parameters,
                            document,
                            offset,
                            completionItems,
                            result
                        )
                    }
                }
            }
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

    private fun processCompletionItems(
        lspCompletionSupport: LspCompletionSupport,
        completionParameters: CompletionParameters,
        document: Document,
        offset: Int,
        completionItems: List<CompletionItem>,
        resultSet: CompletionResultSet
    ) {
        var currentResultSet = resultSet
        var currentRange: Range? = null
        val lspPosition = getLsp4jPosition(document, offset)

        for (item in completionItems) {
            val lookupElement = lspCompletionSupport.createLookupElement(completionParameters, item)
            if (lookupElement != null) {
                val range = getRange(item)
                if (range != currentRange) {
                    currentRange = range
                    currentResultSet = if (range == null) {
                        resultSet
                    } else {
                        val prefix = getCompletionPrefix(document, lspPosition, range)
                        prefix?.let { resultSet.withPrefixMatcher(it) } ?: resultSet
                    }
                }
                currentResultSet.addElement(lookupElement)
            }
        }
    }

    private fun getCompletionPrefix(document: Document, position: Position, range: Range): String? {
        return if (range.start.line == range.end.line && range.start.line == position.line &&
            range.start.character <= position.character && range.end.character >= position.character
        ) {
            val lineStartOffset = document.getLineStartOffset(position.line)
            val startOffset = lineStartOffset + range.start.character
            val endOffset = lineStartOffset + position.character
            document.charsSequence.subSequence(startOffset, endOffset).toString()
        } else {
            null
        }
    }
}
