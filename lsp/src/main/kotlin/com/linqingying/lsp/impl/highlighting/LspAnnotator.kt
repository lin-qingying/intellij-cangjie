package com.linqingying.lsp.impl.highlighting

import com.linqingying.lsp.api.customization.requests.util.getRangeInDocument
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.*
import kotlin.math.max

class LspAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile.virtualFile
        if (file != null && file !is VirtualFileWindow) {
            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document != null) {
                var diagnosticInfos = holder.currentAnnotationSession.getUserData(LSP_DIAGNOSTIC_INFOS)
                if (diagnosticInfos == null) {
                    val servers = LspServerManagerImpl.getInstanceImpl(element.project).getServersWithThisFileOpen(file)
                    val allDiagnostics = servers.flatMap { server ->
                        val support = server.descriptor.lspDiagnosticsSupport
                        val diagnosticsAndQuickFixes =
                            support?.let { server.getDiagnosticsAndQuickFixes(file) } ?: emptyList()
                        diagnosticsAndQuickFixes.mapNotNull { dqf ->
                            val range = getRangeInDocument(document, dqf.diagnostic.range)
                            range?.let {
                                support?.let { it1 ->
                                    DiagnosticInfo(
                                        it1,
                                        dqf.diagnostic,
                                        it,
                                        dqf.quickFixes
                                    )
                                }
                            }
                        }
                    }
                    diagnosticInfos = allDiagnostics.sortedBy { it.textRange.startOffset }.toMutableList()
                    holder.currentAnnotationSession.putUserData(LSP_DIAGNOSTIC_INFOS, diagnosticInfos)
                }
                processDiagnosticsInRange(diagnosticInfos, element.textRange, holder)
            }
        }
    }


    private fun processDiagnosticsInRange(
        diagnostics: MutableList<DiagnosticInfo>,
        range: TextRange,
        holder: AnnotationHolder
    ) {
        if (diagnostics.isNotEmpty()) {

            var index = Collections.binarySearch(diagnostics, null, compareBy { it?.textRange?.startOffset })
                .let { if (it < 0) -it - 1 else it }
            index = max(0, index)
            if (index < diagnostics.size) {
                var diagnosticInfo: DiagnosticInfo? = diagnostics[index]
                while (diagnosticInfo != null && diagnosticInfo.textRange.startOffset < range.endOffset) {
                    ProgressManager.checkCanceled()
                    if (range.contains(diagnosticInfo.textRange)) {
                        diagnostics.removeAt(index)
                        diagnosticInfo.diagnosticsSupport.createAnnotation(
                            holder,
                            diagnosticInfo.diagnostic,
                            diagnosticInfo.textRange,
                            diagnosticInfo.quickFixes
                        )
                    } else {
                        index++
                    }
                    diagnosticInfo = if (index < diagnostics.size) diagnostics[index] else null
                }
            }
        }
    }
}


private val LSP_DIAGNOSTIC_INFOS: Key<MutableList<DiagnosticInfo>> = Key.create("LSP_DIAGNOSTIC_INFOS")

