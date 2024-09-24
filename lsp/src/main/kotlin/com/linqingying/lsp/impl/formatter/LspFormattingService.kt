package com.linqingying.lsp.impl.formatter


import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.linqingying.lsp.api.LspBundle
import com.linqingying.lsp.api.LspServerState
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.util.applyTextEdits
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.services.LanguageServer
import org.jetbrains.annotations.Nls

private class LspFormattingService :
    AsyncDocumentFormattingService() {
    override fun canFormat(psiFile: PsiFile): Boolean {
        return findLspServer(psiFile) != null
    }

    override fun createFormattingTask(formattingRequest: AsyncFormattingRequest): FormattingTask? {

        val virtualFile = formattingRequest.context.virtualFile ?: return null
        val psiFile = formattingRequest.context.containingFile

        val lspServer = findLspServer(psiFile) ?: return null

        return LspFormattingTask(lspServer, virtualFile, formattingRequest)
    }

    override fun getFeatures(): Set<FormattingService.Feature> {
        return emptySet()
    }


    override fun getName(): @Nls String {
        return LspBundle.message("lsp.based.formatter", arrayOfNulls<Any>(0))

    }

    override fun getNotificationGroupId(): String {
        return "LSP window/showMessage"

    }

    private fun findLspServer(psiFile: PsiFile): LspServerImpl? {
        val virtualFile = psiFile.virtualFile ?: return null

        if (virtualFile.isInLocalFileSystem && virtualFile !is VirtualFileWindow) {
            val project = psiFile.project
            val lspServerManager = LspServerManagerImpl.getInstanceImpl(project)
            val lspServers = lspServerManager.lspServers

            for (server in lspServers) {
                if (server.state == LspServerState.Running) {
                    val formattingSupport = server.descriptor.lspFormattingSupport
                    if (formattingSupport != null) {
                        val isSupportedFile = server.descriptor.isSupportedFile(virtualFile)
                        val formattingModelBuilder = LanguageFormatting.INSTANCE.forContext(psiFile)
                        val hasFormattingCapabilities =
                            server.hasFormattingRelatedCapabilities()
                        val wantsToFormat =
                            server.doesServerExplicitlyWantToFormatThisFile(virtualFile)

                        if (hasFormattingCapabilities && isSupportedFile &&
                            formattingSupport.shouldFormatThisFileExclusivelyByServer(
                                virtualFile,
                                formattingModelBuilder != null,
                                wantsToFormat
                            )
                        ) {
                            return server
                        }
                    }
                }
            }
        }

        return null
    }


    private class LspFormattingTask(
        val lspServer: LspServerImpl,
        val file: VirtualFile,
        val formattingRequest: AsyncFormattingRequest
    ) : FormattingTask {

        override fun cancel(): Boolean = true

        override fun isRunUnderProgress(): Boolean = true

        override fun run() {
            val documentText = formattingRequest.documentText
            val codeStyleSettings = formattingRequest.context.codeStyleSettings
            val documentIdentifier = lspServer.getDocumentIdentifier(file)
            val formattingOptions = FormattingOptions().apply {
                tabSize = codeStyleSettings.getIndentSize(file.fileType)
                isInsertSpaces = !codeStyleSettings.useTabCharacter(file.fileType)

            }

            val formattingParams = DocumentFormattingParams(documentIdentifier, formattingOptions)

            val formattedTextEdits = lspServer.sendRequestSync { server: LanguageServer ->
                server.textDocumentService.formatting(formattingParams).also {
                    requireNotNull(it) { "Formatting request returned null" }
                }
            }

            if (formattedTextEdits.isNullOrEmpty()) {
                formattingRequest.onTextReady(null)
            } else {
                val document = DocumentImpl(documentText, false, true)
                applyTextEdits(document, formattedTextEdits)
                formattingRequest.onTextReady(document.text)
            }
        }
    }
}

