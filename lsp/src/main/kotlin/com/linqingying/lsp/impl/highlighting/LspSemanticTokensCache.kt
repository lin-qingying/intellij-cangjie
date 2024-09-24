package com.linqingying.lsp.impl.highlighting

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.util.getOffsetInDocument
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SemanticTokensLegend
import org.eclipse.lsp4j.SemanticTokensParams

private class LspSemanticTokensForFile(
    val psiModCount: Long,
    val semanticTokens: List<LspSemanticToken>
)


internal class LspSemanticTokensCache(val lspServer: LspServerImpl) :
    LspHighlightingCache() {
    private val fileToSemanticTokens: MutableMap<VirtualFile, LspSemanticTokensForFile> = mutableMapOf()


    override fun clearCache() {
        synchronized(this) {

            super.clearCache()
            fileToSemanticTokens.clear()

        }
    }

    @RequiresReadLock
    private fun processSemanticTokens(
        document: Document,
        tokensData: List<Int>,
        legend: SemanticTokensLegend
    ): List<LspSemanticToken> {
        val logger = Logger.getInstance(LspSemanticTokensCache::class.java)

        if (tokensData.size % 5 != 0) {
            logger.warn("Unexpected semantic tokens data length from the server: ${tokensData.size}")
            return emptyList()
        }

        val tokenCount = tokensData.size / 5
        val semanticTokens = mutableListOf<LspSemanticToken>()
        var cumulativeLine = 0
        var cumulativeCharacter = 0

        for (i in 0 until tokenCount) {
            if (i % 100 == 0) {
                ProgressManager.checkCanceled()
            }

            val line = (tokensData[5 * i] as Number).toInt()
            val character = (tokensData[5 * i + 1] as Number).toInt()
            val length = (tokensData[5 * i + 2] as Number).toInt()
            val tokenTypeIndex = (tokensData[5 * i + 3] as Number).toInt()
            val tokenModifiers = (tokensData[5 * i + 4] as Number).toInt()

            cumulativeLine += line
            cumulativeCharacter = if (line == 0) cumulativeCharacter + character else character

            if (tokenTypeIndex >= legend.tokenTypes.size) {
                logger.warn("Unexpected encodedTokenType: $tokenTypeIndex, legend.tokenTypes.size = ${legend.tokenTypes.size}")
            } else {
                val tokenType = legend.tokenTypes[tokenTypeIndex]
                val modifiers =
                    legend.tokenModifiers.filterIndexed { index, _ -> (tokenModifiers and (1 shl index)) != 0 }

                val offset = getOffsetInDocument(document, Position(cumulativeLine, cumulativeCharacter))
                if (offset != null) {
                    val textRange = TextRange.from(offset, length)
                    semanticTokens.add(LspSemanticToken(textRange, tokenType, modifiers))
                }
            }
        }

        return semanticTokens
    }

    internal fun fileEdited(
        file: VirtualFile,
        e: DocumentEvent
    ) {
        synchronized(this) {

            if (fileToSemanticTokens[file] != null) {
                this.addPendingEdit(file, e)
            }

        }
    }

    @RequiresBackgroundThread
    internal fun getSemanticTokens(file: VirtualFile): List<LspSemanticToken> {

        if (lspServer.descriptor.lspSemanticTokensSupport == null) {
            return emptyList()
        }

        val semanticTokensProvider = lspServer.serverCapabilities?.semanticTokensProvider
        val semanticTokensSupport = semanticTokensProvider?.full

        if (semanticTokensSupport != null && semanticTokensSupport.left != true) {
            synchronized(this) {
                val semanticTokensForFile = fileToSemanticTokens[file]

                if (semanticTokensForFile == null ||
                    semanticTokensForFile.psiModCount != PsiModificationTracker.getInstance(lspServer.project).modificationCount
                ) {
                    fetchSemanticTokens(file)
                }

                if (semanticTokensForFile != null && semanticTokensForFile.semanticTokens.isNotEmpty()) {
                    val updatedTokens = applyPendingEdits(file, semanticTokensForFile.semanticTokens, false)
                    { oldTextRangeOwner, newTextRange ->

                        LspSemanticToken(newTextRange, oldTextRangeOwner.tokenType, oldTextRangeOwner.tokenModifiers)
                    }
                    fileToSemanticTokens[file] =
                        LspSemanticTokensForFile(semanticTokensForFile.psiModCount, updatedTokens)
                    removePendingEdits(file)
                    return updatedTokens
                }
            }
        }

        return emptyList()
    }

    private fun fetchSemanticTokens(file: VirtualFile) {
        LspSemanticTokensService.getInstance(lspServer.project).cs.launch {
            val modificationCount = PsiModificationTracker.getInstance(lspServer.project).modificationCount

            val lspServer = this@LspSemanticTokensCache.lspServer

            val semanticTokens = lspServer.sendRequest {
                val params = SemanticTokensParams(this@LspSemanticTokensCache.lspServer.getDocumentIdentifier(file))
                it.textDocumentService.semanticTokensFull(params)
            }
            val data = semanticTokens?.data ?: return@launch
            updateSemanticTokens(file, modificationCount, data)
        }

    }


    private suspend fun updateSemanticTokens(
        file: VirtualFile,
        psiModCount: Long,
        data: List<Int>
    ) {

        readAction {
            return@readAction if (psiModCount != PsiModificationTracker.getInstance(lspServer.project).modificationCount) {


                fetchSemanticTokens(file)
                null

            } else {
                lspServer.serverCapabilities?.let {
                    it.semanticTokensProvider?.legend
                }?.let {
                    FileDocumentManager.getInstance().getDocument(file)?.let { document ->

                        processSemanticTokens(document, data, it)

                    }
                }
            }
        }?.let {
            synchronized(this) {
                fileToSemanticTokens[file] = LspSemanticTokensForFile(psiModCount, it)
                removePendingEdits(file)
            }
            DaemonCodeAnalyzer.getInstance(lspServer.project).restart()

        }

    }
}

