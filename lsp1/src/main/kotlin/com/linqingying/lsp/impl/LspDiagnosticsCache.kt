package com.linqingying.lsp.impl

import com.linqingying.lsp.impl.quickFix.LspQuickFixSet
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.containers.BidirectionalMap
import com.intellij.util.containers.MultiMap
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import java.util.Collections.synchronizedMap

class LspDiagnosticsCache {
    private val uriToDiagnostics: MutableMap<String, List<DiagnosticAndLazyQuickFixes>> = synchronizedMap(HashMap())
    private val filePathToUri: BidirectionalMap<String, String> = BidirectionalMap()
    private val filePathToPendingDocEdits: MultiMap<String, DocEdit> = MultiMap.create()

    companion object {
        val LOG = Logger.getInstance(LspDiagnosticsCache::class.java)
    }

    @RequiresBackgroundThread
    fun getDiagnostics(file: VirtualFile): List<DiagnosticAndLazyQuickFixes> {
        val uri = filePathToUri[file.path]
        return if (uri != null) {
            uriToDiagnostics[uri] ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun fileEdited(file: VirtualFile, e: DocumentEvent) {


        synchronized(this) {
            if (this.filePathToUri[file.path] != null) {
                val startLine = e.document.getLineNumber(e.offset)
                val startChar = e.offset - e.document.getLineStartOffset(startLine)
                val endLine = e.document.getLineNumber(e.offset + e.oldLength)
                val endChar = e.offset + e.oldLength - e.document.getLineStartOffset(endLine)
                val oldLines = e.oldFragment.count { it == '\n' }
                val newLines = e.newFragment.count { it == '\n' }
                val lineDelta = newLines - oldLines
                val newEndLine = endLine + lineDelta
                val newEndChar = if (newLines == 0) {
                    startChar + e.newLength
                } else {
                    e.newLength - e.newFragment.lastIndexOf('\n') - 1
                }
                this.filePathToPendingDocEdits.putValue(
                    file.path,
                    DocEdit(
                        Position(startLine, startChar),
                        Position(endLine, endChar),
                        Position(newEndLine, newEndChar)
                    )
                )
            }
        }
    }


    @Synchronized
    fun diagnosticsReceived(file: VirtualFile?, params: PublishDiagnosticsParams) {
        if (params.diagnostics.isEmpty()) {
            this.uriToDiagnostics.remove(params.uri)
            this.filePathToUri.removeValue(params.uri)
            file?.let {
                this.filePathToPendingDocEdits.remove(it.path)
            }
        } else {
            val diagnosticList = params.diagnostics.map { DiagnosticAndLazyQuickFixes(it) }

            this.uriToDiagnostics[params.uri] = diagnosticList
            if (file != null) {
                filePathToUri[file.path] = params.uri
                filePathToPendingDocEdits.remove(file.path)
            } else {
                this.filePathToUri.removeValue(params.uri)
                LOG.warn("Could not find file with diagnostics: ${params.uri}")
            }
        }
    }


    data class DocEdit(val start: Position, val endOld: Position, val endNew: Position)


    class DiagnosticAndLazyQuickFixes(val diagnostic: Diagnostic) {


        private var quickFixes: List<IntentionAction>? = null


        /**
         * 获取快速修复
         */
        fun getQuickFixes(lspServer: LspServerImpl, file: VirtualFile): List<IntentionAction> {
            if (quickFixes != null) {
                return quickFixes!!
            } else if (lspServer.descriptor.lspCodeActionsSupport == null) {
                return emptyList()
            } else {
                val fixes = LspQuickFixSet(lspServer, file, diagnostic).quickFixes
                quickFixes = fixes
                return fixes
            }
        }
    }
}

private operator fun <K, V> MultiMap<K, V>.set(uri: K, value: V) {
    putValue(uri, value)

}


