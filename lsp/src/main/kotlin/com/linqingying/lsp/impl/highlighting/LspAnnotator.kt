package com.linqingying.lsp.impl.highlighting

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.linqingying.lsp.api.customization.LspDiagnosticsSupport
import com.linqingying.lsp.api.customization.LspSemanticTokensSupport
import com.linqingying.lsp.util.getRangeInDocument
import com.linqingying.lsp.impl.LspServerManagerImpl
import org.eclipse.lsp4j.Diagnostic
import kotlin.math.max


private val LSP_DIAGNOSTIC_INFOS: Key<MutableList<LspDiagnosticInfo>> = Key.create("LSP_DIAGNOSTIC_INFOS")

private val LSP_SEMANTIC_TOKEN_INFOS: Key<MutableList<LspSemanticTokenInfo>> = Key.create("LSP_SEMANTIC_TOKEN_INFOS")

private class LspSemanticTokenInfo(
    textRange: TextRange,
    val semanticTokensSupport: LspSemanticTokensSupport,
    val tokenType: String,
    val tokenModifiers: List<String>
) : LspHighlightingInfo(textRange)


class LspAnnotator : Annotator {
    override fun annotate(
        element: PsiElement,
        holder: AnnotationHolder
    ) {
        val virtualFile = element.containingFile.virtualFile
        if (virtualFile != null && virtualFile !is VirtualFileWindow) {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            document?.let {
                processAnnotations(element, virtualFile, holder)
                processDiagnostics(element, virtualFile, it, holder)
            }
        }
    }

    private fun processDiagnostics(
        element: PsiElement,
        virtualFile: VirtualFile,
        document: Document,
        holder: AnnotationHolder
    ) {
        var diagnosticInfos = holder.currentAnnotationSession.getUserData(LSP_DIAGNOSTIC_INFOS)

        if (diagnosticInfos == null) {
            val project = element.project

            val lspServerManager = LspServerManagerImpl.getInstanceImpl(project)
            val servers = lspServerManager.getServersWithThisFileOpen(virtualFile)

            val allDiagnostics = mutableListOf<LspDiagnosticInfo>()

            for (server in servers) {
                val diagnosticsSupport = server.descriptor.lspDiagnosticsSupport
                val diagnostics = diagnosticsSupport?.let {
                    server.getDiagnosticsAndQuickFixes(virtualFile).mapNotNull { diagnosticAndQuickFixes ->
                        val range = diagnosticAndQuickFixes.diagnostic.range
                        val textRange = getRangeInDocument(document, range)

                        textRange?.let { tr ->
                            LspDiagnosticInfo(
                                tr,
                                it,
                                diagnosticAndQuickFixes.diagnostic,
                                diagnosticAndQuickFixes.quickFixes
                            )
                        }
                    }
                } ?: emptyList()

                allDiagnostics.addAll(diagnostics)
            }

            diagnosticInfos = allDiagnostics.sortedWith { a, b ->

                a.textRange.startOffset.compareTo(b.textRange.startOffset)
            }.toMutableList()
            holder.currentAnnotationSession.putUserData(LSP_DIAGNOSTIC_INFOS, diagnosticInfos)
        }

        val textRange = element.textRange.also {
            requireNotNull(it) { "Text range cannot be null" }
        }

        processTokenInfos(diagnosticInfos, textRange) { diagnosticInfo ->
            diagnosticInfo.diagnosticsSupport.createAnnotation(
                holder,
                diagnosticInfo.diagnostic,
                diagnosticInfo.textRange,
                diagnosticInfo.quickFixes
            )

        }
    }

    private fun processAnnotations(
        element: PsiElement,
        virtualFile: VirtualFile,
        holder: AnnotationHolder
    ) {
        var semanticTokenInfos = holder.currentAnnotationSession.getUserData(LSP_SEMANTIC_TOKEN_INFOS)

        if (semanticTokenInfos == null) {
            val project = element.project
            val lspServerManager = LspServerManagerImpl.getInstanceImpl(project)
            val servers = lspServerManager.getServersWithThisFileOpen(virtualFile)

            val tokenInfos = mutableListOf<LspSemanticTokenInfo>()

            for (server in servers) {
                val semanticTokensSupport = server.descriptor.lspSemanticTokensSupport
                val semanticTokens = semanticTokensSupport?.let {
                    server.getSemanticTokens(virtualFile).map { token ->
                        LspSemanticTokenInfo(token.textRange, it, token.tokenType, token.tokenModifiers)
                    }
                } ?: emptyList()

                tokenInfos.addAll(semanticTokens)
            }

            semanticTokenInfos = tokenInfos.sortedWith { a, b ->
                val tokenInfoA = a as LspSemanticTokenInfo
                val tokenInfoB = b as LspSemanticTokenInfo
                tokenInfoA.textRange.startOffset.compareTo(tokenInfoB.textRange.startOffset)
            }.toMutableList()
            holder.currentAnnotationSession.putUserData(LSP_SEMANTIC_TOKEN_INFOS, semanticTokenInfos)
        }

        val textRange = element.textRange ?: return
        processTokenInfos(semanticTokenInfos, textRange) { tokenInfo ->
            val textAttributesKey =
                tokenInfo.semanticTokensSupport.getTextAttributesKey(tokenInfo.tokenType, tokenInfo.tokenModifiers)
            textAttributesKey?.let { key ->
                holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
                    .range(tokenInfo.textRange)
                    .textAttributes(key)
                    .create()
            }
        }
    }

    private fun <T : LspHighlightingInfo> processTokenInfos(
        highlightingInfos: MutableList<T>,
        elementRange: TextRange,
        processor: (T) -> Unit
    ) {
        if (highlightingInfos.isNotEmpty()) {
            val index = highlightingInfos.binarySearch { info ->
                when {
                    info.textRange.startOffset < elementRange.startOffset -> -1
                    else -> 1
                }
            }.let { max(0, -it - 1) }

            if (index < highlightingInfos.size) {
                var currentIndex = index
                while (currentIndex < highlightingInfos.size) {
                    val highlightingInfo = highlightingInfos[currentIndex]
                    if (highlightingInfo.textRange.startOffset >= elementRange.endOffset) break

                    ProgressManager.checkCanceled()

                    if (elementRange.contains(highlightingInfo.textRange)) {
                        highlightingInfos.removeAt(currentIndex)
                        processor(highlightingInfo)
                    } else {
                        currentIndex++
                    }
                }
            }
        }
    }

}

private class LspDiagnosticInfo(
    textRange: TextRange,
    val diagnosticsSupport: LspDiagnosticsSupport,
    val diagnostic: Diagnostic,
    val quickFixes: List<IntentionAction>
) : LspHighlightingInfo(textRange)

private open class LspHighlightingInfo(val textRange: TextRange)

